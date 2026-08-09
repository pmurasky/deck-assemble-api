package com.deckassemble.decks.application.history;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCardUpdateRequest;
import com.deckassemble.decks.application.DeckStateReplacer;
import com.deckassemble.decks.application.DeckUpdateRequest;
import com.deckassemble.decks.application.organization.DeckCategoryService;
import com.deckassemble.decks.application.organization.DeckFolderService;
import com.deckassemble.decks.application.organization.DeckTagService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.history.DeckChangeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Restores a deck to an earlier revision's snapshot by composing the already-instrumented mutation
 * primitives ({@link DeckStateReplacer}, {@link DeckCardService}, {@link DeckCategoryService},
 * {@link DeckFolderService}, {@link DeckTagService}) rather than writing to the database directly —
 * restore gets each primitive's own no-op detection for free this way (or, for metadata/status,
 * applies the target state outright — see {@link DeckStateReplacer}). All composed calls run inside
 * {@link DeckRevisionService#withoutRecording}, then exactly one {@code RESTORED} revision is
 * recorded here for the whole operation — the same pattern {@code DeckImportCommitService} already
 * uses for import.
 */
@Service
public class DeckRevisionRestoreService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckRevisionService deckRevisionService;
    private final DeckStateReplacer deckStateReplacer;
    private final DeckCardService deckCardService;
    private final DeckCategoryService deckCategoryService;
    private final DeckFolderService deckFolderService;
    private final DeckTagService deckTagService;

    // Suppressed: cohesive restore-composition collaborators — one service per mutation primitive
    // (deck metadata/status, cards, categories, folder, tags) plus access control and history
    // recording; no natural subgrouping without an artificial wrapper, same precedent as
    // DeckController and DeckImportCommitService.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckRevisionRestoreService(
            DeckAccessGuard deckAccessGuard,
            DeckRevisionService deckRevisionService,
            DeckStateReplacer deckStateReplacer,
            DeckCardService deckCardService,
            DeckCategoryService deckCategoryService,
            DeckFolderService deckFolderService,
            DeckTagService deckTagService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckRevisionService = deckRevisionService;
        this.deckStateReplacer = deckStateReplacer;
        this.deckCardService = deckCardService;
        this.deckCategoryService = deckCategoryService;
        this.deckFolderService = deckFolderService;
        this.deckTagService = deckTagService;
    }

    /**
     * Restores {@code deckId} to the state captured by {@code revisionNumber}, provided the deck's
     * current revision is still {@code expectedCurrentRevision} (409 if it has moved on since the
     * client last saw it).
     */
    @Transactional
    public DeckRevisionService.RevisionView restore(
            long deckId, int revisionNumber, int expectedCurrentRevision) {
        Deck deck = deckAccessGuard.ownedLocked(deckId);
        DeckSnapshot target = deckRevisionService.snapshotAt(deckId, revisionNumber);
        assertCurrentRevisionMatches(deckId, expectedCurrentRevision);
        deckRevisionService.withoutRecording(() -> applyAll(deckId, target));
        deckRevisionService.record(deckId, deck.getProfileId(), DeckChangeType.RESTORED);
        return deckRevisionService.get(deckId, deckRevisionService.currentRevisionNumber(deckId));
    }

    private void assertCurrentRevisionMatches(long deckId, int expected) {
        int current = deckRevisionService.currentRevisionNumber(deckId);
        if (current != expected) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Deck has changed since revision "
                            + expected
                            + " (current is "
                            + current
                            + ")");
        }
    }

    private Void applyAll(long deckId, DeckSnapshot target) {
        applyState(deckId, target);
        deckFolderService.assignToDeck(deckId, target.folderId());
        applyTags(deckId, target);
        applyCategories(deckId, target);
        applyCards(deckId, target);
        return null;
    }

    private void applyState(long deckId, DeckSnapshot target) {
        deckStateReplacer.replace(
                deckId,
                new DeckUpdateRequest(
                        target.name(),
                        target.formatCode(),
                        target.description(),
                        target.commanderCardId(),
                        target.secondaryCommanderCardId(),
                        target.useOwnedCardsOnly(),
                        target.budgetLimit(),
                        target.desiredPowerLevel(),
                        target.playStyle()),
                Deck.Status.valueOf(target.status()));
    }

    private void applyTags(long deckId, DeckSnapshot target) {
        Map<String, Long> idsByName =
                deckTagService.list().stream()
                        .collect(
                                Collectors.toMap(
                                        DeckTagService.TagView::name, DeckTagService.TagView::id));
        List<Long> targetTagIds =
                target.tagNames().stream().map(idsByName::get).filter(Objects::nonNull).toList();
        deckTagService.assignToDeck(deckId, targetTagIds);
    }

    private void applyCategories(long deckId, DeckSnapshot target) {
        List<DeckCategoryService.CategoryView> current = deckCategoryService.list(deckId);
        Set<String> targetNames = Set.copyOf(target.categoryNames());
        Set<String> currentNames =
                current.stream()
                        .map(DeckCategoryService.CategoryView::name)
                        .collect(Collectors.toSet());
        targetNames.stream()
                .filter(name -> !currentNames.contains(name))
                .forEach(name -> deckCategoryService.create(deckId, name));
        current.stream()
                .filter(
                        category ->
                                !category.systemOwned() && !targetNames.contains(category.name()))
                .forEach(
                        category ->
                                deckCategoryService.delete(
                                        deckId, Objects.requireNonNull(category.id())));
    }

    private void applyCards(long deckId, DeckSnapshot target) {
        Map<CardKey, DeckCardResponse> current =
                deckCardService.listCards(deckId).stream()
                        .filter(card -> card.id() != null)
                        .collect(Collectors.toMap(CardKey::of, card -> card));
        Map<CardKey, DeckSnapshot.CardEntry> targetByKey =
                target.cards().stream().collect(Collectors.toMap(CardKey::of, entry -> entry));
        current.forEach(
                (key, card) -> {
                    if (!targetByKey.containsKey(key)) {
                        deckCardService.removeCard(deckId, Objects.requireNonNull(card.id()));
                    }
                });
        targetByKey.forEach((key, entry) -> addOrUpdateCard(deckId, current.get(key), entry));
    }

    private void addOrUpdateCard(
            long deckId, @Nullable DeckCardResponse existing, DeckSnapshot.CardEntry entry) {
        if (existing == null) {
            deckCardService.addCard(
                    deckId,
                    new DeckCardAddRequest(
                            entry.cardPrintingId(),
                            entry.quantity(),
                            DeckCard.Section.valueOf(entry.deckSection())));
        } else if (existing.quantity() != entry.quantity()) {
            deckCardService.updateCard(
                    deckId,
                    Objects.requireNonNull(existing.id()),
                    new DeckCardUpdateRequest(entry.quantity(), null));
        }
    }

    private record CardKey(Long cardPrintingId, String deckSection) {
        static CardKey of(DeckCardResponse card) {
            return new CardKey(card.cardPrintingId(), card.deckSection());
        }

        static CardKey of(DeckSnapshot.CardEntry entry) {
            return new CardKey(entry.cardPrintingId(), entry.deckSection());
        }
    }
}
