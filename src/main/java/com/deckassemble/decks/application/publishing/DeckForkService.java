package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.organization.DeckCategoryService;
import com.deckassemble.decks.application.organization.DeckTagService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Forks a published deck's pinned snapshot into a brand-new private deck owned by the caller.
 * Structurally similar to {@code DeckRevisionRestoreService} (applies a {@link DeckSnapshot}'s
 * fields onto a deck through the already-instrumented mutation primitives) but targets a new deck
 * rather than an existing one, so the call sequence differs: create, then compose cards/
 * categories/tags, all inside {@link DeckRevisionService#withoutRecording}, then exactly one {@code
 * FORKED} revision is recorded for the whole operation.
 *
 * <p>{@code folderId} and {@code status} from the snapshot are deliberately not carried over: the
 * source's folder belongs to the source owner (usually a different profile) and would 404 against
 * the caller's own folders, and a fresh fork starting DRAFT (this module's create default) rather
 * than replaying an arbitrary status is the more sensible default for a brand-new deck.
 */
@Service
public class DeckForkService {

    private final DeckPublishingService deckPublishingService;
    private final DeckRepository deckRepository;
    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final DeckCategoryService deckCategoryService;
    private final DeckTagService deckTagService;
    private final DeckRevisionService deckRevisionService;

    // Suppressed: cohesive fork-composition collaborators — gated source resolution, persistence
    // for source attribution, one service per mutation primitive (deck metadata, cards,
    // categories, tags), and history recording; no natural subgrouping without an artificial
    // wrapper, same precedent as DeckRevisionRestoreService/DeckImportCommitService.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckForkService(
            DeckPublishingService deckPublishingService,
            DeckRepository deckRepository,
            DeckService deckService,
            DeckCardService deckCardService,
            DeckCategoryService deckCategoryService,
            DeckTagService deckTagService,
            DeckRevisionService deckRevisionService) {
        this.deckPublishingService = deckPublishingService;
        this.deckRepository = deckRepository;
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckCategoryService = deckCategoryService;
        this.deckTagService = deckTagService;
        this.deckRevisionService = deckRevisionService;
    }

    @Transactional
    public Deck fork(String slug) {
        DeckPublishingService.SharedDeckView shared = deckPublishingService.getShared(slug);
        DeckSnapshot snapshot = shared.pinnedSnapshot();
        if (snapshot == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Deck has not been published yet");
        }
        long sourceDeckId = shared.deck().getId();
        Integer sourceRevisionNumber = shared.deck().getPublishedRevisionNumber();
        long forkedDeckId = deckRevisionService.withoutRecording(() -> buildFork(snapshot)).id();
        Deck forked = attributeSource(forkedDeckId, sourceDeckId, sourceRevisionNumber);
        deckRevisionService.record(forkedDeckId, forked.getProfileId(), DeckChangeType.FORKED);
        return forked;
    }

    private DeckResponse buildFork(DeckSnapshot snapshot) {
        DeckResponse created = deckService.create(createRequestFrom(snapshot));
        applyCategories(created.id(), snapshot);
        applyTags(created.id(), snapshot);
        applyCards(created.id(), snapshot);
        return created;
    }

    private static DeckCreateRequest createRequestFrom(DeckSnapshot snapshot) {
        return new DeckCreateRequest(
                snapshot.name(),
                snapshot.formatCode(),
                snapshot.description(),
                snapshot.commanderCardId(),
                snapshot.secondaryCommanderCardId(),
                snapshot.useOwnedCardsOnly(),
                snapshot.budgetLimit(),
                snapshot.desiredPowerLevel(),
                snapshot.playStyle());
    }

    private void applyCategories(long deckId, DeckSnapshot snapshot) {
        // list() lazily seeds the default system categories, whose names snapshot.categoryNames()
        // may already include; only genuinely custom category names need creating.
        Set<String> currentNames =
                deckCategoryService.list(deckId).stream()
                        .map(DeckCategoryService.CategoryView::name)
                        .collect(Collectors.toSet());
        snapshot.categoryNames().stream()
                .filter(name -> !currentNames.contains(name))
                .forEach(name -> deckCategoryService.create(deckId, name));
    }

    private void applyTags(long deckId, DeckSnapshot snapshot) {
        // Tags are per-profile, so only names the caller already has their own tag for carry over;
        // this is a best-effort match, not tag creation on the caller's behalf.
        Map<String, Long> idsByName =
                deckTagService.list().stream()
                        .collect(
                                Collectors.toMap(
                                        DeckTagService.TagView::name, DeckTagService.TagView::id));
        List<Long> tagIds =
                snapshot.tagNames().stream().map(idsByName::get).filter(Objects::nonNull).toList();
        if (!tagIds.isEmpty()) {
            deckTagService.assignToDeck(deckId, tagIds);
        }
    }

    private void applyCards(long deckId, DeckSnapshot snapshot) {
        snapshot.cards()
                .forEach(
                        entry ->
                                deckCardService.addCard(
                                        deckId,
                                        new DeckCardAddRequest(
                                                entry.cardPrintingId(),
                                                entry.quantity(),
                                                DeckCard.Section.valueOf(entry.deckSection()))));
    }

    private Deck attributeSource(
            long forkedDeckId, long sourceDeckId, Integer sourceRevisionNumber) {
        Deck forked = deckRepository.findById(forkedDeckId).orElseThrow(DeckNotFoundException::new);
        forked.setSourceDeckId(sourceDeckId);
        forked.setSourceRevisionNumber(sourceRevisionNumber);
        return deckRepository.save(forked);
    }
}
