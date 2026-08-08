package com.deckassemble.decks.application.history;

import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.history.DeckRevision;
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Records an immutable, append-only {@link DeckRevision} for a meaningful deck mutation, in the
 * same transaction as the mutation itself. Callers decide *whether* a change is meaningful (no-op
 * detection lives with the mutation, not here); this service decides how the canonical snapshot is
 * assembled, how the sequential revision number is allocated, and persists the result.
 *
 * <p>Concurrent mutations to the same deck are serialized by taking a {@code PESSIMISTIC_WRITE} row
 * lock on the {@code Deck} row before computing the next revision number, the same locking
 * primitive {@code DeckAccessGuard.ownedLocked} already uses for other per-deck check-then-act
 * races in this module.
 */
@Service
public class DeckRevisionService {

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    private final DeckRevisionRepository revisionRepository;
    private final DeckRepository deckRepository;
    private final DeckCardRepository deckCardRepository;
    private final DeckCategoryRepository deckCategoryRepository;
    private final DeckTagAssignmentRepository deckTagAssignmentRepository;
    private final DeckTagRepository deckTagRepository;
    private final ObjectMapper objectMapper;

    // Suppressed: cohesive snapshot-recording collaborators — one repository per canonical-state
    // fragment (deck core fields, cards, categories, tags) plus the revision store and JSON codec.
    // No natural subgrouping without an artificial wrapper; same precedent as DeckController.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckRevisionService(
            DeckRevisionRepository revisionRepository,
            DeckRepository deckRepository,
            DeckCardRepository deckCardRepository,
            DeckCategoryRepository deckCategoryRepository,
            DeckTagAssignmentRepository deckTagAssignmentRepository,
            DeckTagRepository deckTagRepository,
            ObjectMapper objectMapper) {
        this.revisionRepository = revisionRepository;
        this.deckRepository = deckRepository;
        this.deckCardRepository = deckCardRepository;
        this.deckCategoryRepository = deckCategoryRepository;
        this.deckTagAssignmentRepository = deckTagAssignmentRepository;
        this.deckTagRepository = deckTagRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Records a revision for the given deck unless recording is currently suppressed by {@link
     * #withoutRecording}. No-op detection is the caller's responsibility.
     */
    public void record(long deckId, long profileId, DeckChangeType changeType) {
        if (Boolean.TRUE.equals(SUPPRESSED.get())) {
            return;
        }
        Deck deck =
                deckRepository
                        .findLockedByIdAndProfileId(deckId, profileId)
                        .orElseThrow(DeckNotFoundException::new);
        int nextRevisionNumber = nextRevisionNumber(deckId);
        Integer baseRevisionNumber = nextRevisionNumber == 1 ? null : nextRevisionNumber - 1;
        revisionRepository.save(
                new DeckRevision(
                        deckId,
                        profileId,
                        nextRevisionNumber,
                        baseRevisionNumber,
                        new DeckRevision.Content(changeType, null, snapshotJson(deck))));
    }

    /**
     * Runs {@code action} with revision recording suppressed, so a caller that composes several
     * already-instrumented mutations (e.g. deck import: create + N card adds) can record exactly
     * one revision itself afterward instead of one per composed call.
     */
    public <T> T withoutRecording(Supplier<T> action) {
        boolean previouslySuppressed = SUPPRESSED.get();
        SUPPRESSED.set(true);
        try {
            return action.get();
        } finally {
            SUPPRESSED.set(previouslySuppressed);
        }
    }

    private int nextRevisionNumber(long deckId) {
        return revisionRepository
                .findFirstByDeckIdOrderByRevisionNumberDesc(deckId)
                .map(revision -> revision.getRevisionNumber() + 1)
                .orElse(1);
    }

    private String snapshotJson(Deck deck) {
        return objectMapper.writeValueAsString(buildSnapshot(deck));
    }

    private DeckSnapshot buildSnapshot(Deck deck) {
        return new DeckSnapshot(
                deck.getName(),
                deck.getFormatCode(),
                deck.getDescription(),
                deck.getCommanderCardId(),
                deck.getSecondaryCommanderCardId(),
                deck.getFolderId(),
                deck.isUseOwnedCardsOnly(),
                deck.getBudgetLimit(),
                deck.getDesiredPowerLevel(),
                deck.getPlayStyle(),
                deck.getStatus().name(),
                cardEntries(deck.getId()),
                categoryNames(deck.getId()),
                tagNames(deck.getId()));
    }

    private List<DeckSnapshot.CardEntry> cardEntries(Long deckId) {
        return deckCardRepository.findByDeckId(deckId).stream()
                .sorted(Comparator.comparing(DeckCard::getId))
                .map(
                        card ->
                                new DeckSnapshot.CardEntry(
                                        card.getCardPrintingId(),
                                        card.getQuantity(),
                                        card.getDeckSection().name(),
                                        card.getOwnershipStatus().name()))
                .toList();
    }

    private List<String> categoryNames(Long deckId) {
        return deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(deckId).stream()
                .map(DeckCategory::getName)
                .toList();
    }

    private List<String> tagNames(Long deckId) {
        List<Long> tagIds =
                deckTagAssignmentRepository.findByDeckId(deckId).stream()
                        .map(DeckTagAssignment::getTagId)
                        .toList();
        return deckTagRepository.findAllById(tagIds).stream()
                .map(DeckTag::getName)
                .sorted()
                .toList();
    }
}
