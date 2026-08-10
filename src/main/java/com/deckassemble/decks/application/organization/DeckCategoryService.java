package com.deckassemble.decks.application.organization;

import com.deckassemble.cards.domain.CardFunctionalCategory;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryAssignment;
import com.deckassemble.decks.domain.organization.DeckCategoryAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages a deck's card-organization categories and their card assignments. Every deck is lazily
 * seeded, on first touch, with one system-owned category per {@link CardFunctionalCategory} so
 * legality/recommendation facts always have a home; users may rename any category (presentation
 * only) and add their own, but system categories cannot be deleted.
 */
@Service
@Transactional
public class DeckCategoryService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCategoryRepository deckCategoryRepository;
    private final DeckCategoryAssignmentRepository assignmentRepository;
    private final DeckCardRepository deckCardRepository;
    private final DeckRevisionService deckRevisionService;

    public DeckCategoryService(
            DeckAccessGuard deckAccessGuard,
            DeckCategoryRepository deckCategoryRepository,
            DeckCategoryAssignmentRepository assignmentRepository,
            DeckCardRepository deckCardRepository,
            DeckRevisionService deckRevisionService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCategoryRepository = deckCategoryRepository;
        this.assignmentRepository = assignmentRepository;
        this.deckCardRepository = deckCardRepository;
        this.deckRevisionService = deckRevisionService;
    }

    public List<CategoryView> list(long deckId) {
        ensureDefaultCategories(deckId);
        int revisionNumber = deckRevisionService.currentRevisionNumberUnchecked(deckId);
        return viewsFor(
                deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(deckId),
                revisionNumber);
    }

    /**
     * Read-only view, for presentation surfaces outside this module (e.g. deck analysis), of which
     * deck cards the user has explicitly filed into a category and what that category is named.
     * Unlike {@link #list(long)} this never seeds the default categories: a deck with no explicit
     * assignments yet simply has no overrides to report. Canonical {@link CardFunctionalCategory}
     * facts (recommendation quotas, raw card categorization) never read this map and stay
     * unaffected by user renames or assignments.
     */
    public Map<Long, String> explicitCategoryNamesByDeckCard(long deckId) {
        deckAccessGuard.owned(deckId);
        List<DeckCategory> categories =
                deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(deckId);
        if (categories.isEmpty()) {
            return Map.of();
        }
        return namesByDeckCardId(categories, deckCardIdsByCategoryId(categories));
    }

    private Map<Long, List<Long>> deckCardIdsByCategoryId(List<DeckCategory> categories) {
        List<Long> categoryIds = categories.stream().map(DeckCategory::getId).toList();
        return assignmentRepository.findByDeckCategoryIdIn(categoryIds).stream()
                .collect(
                        Collectors.groupingBy(
                                DeckCategoryAssignment::getDeckCategoryId,
                                Collectors.mapping(
                                        DeckCategoryAssignment::getDeckCardId,
                                        Collectors.toList())));
    }

    // Earliest-display-order category wins for a card assigned to more than one.
    // Justified: method-local map, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<Long, String> namesByDeckCardId(
            List<DeckCategory> categories, Map<Long, List<Long>> deckCardIdsByCategoryId) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (DeckCategory category : categories) {
            for (Long deckCardId :
                    deckCardIdsByCategoryId.getOrDefault(category.getId(), List.of())) {
                names.putIfAbsent(deckCardId, category.getName());
            }
        }
        return names;
    }

    public CategoryView create(long deckId, String name, @Nullable Integer expectedRevision) {
        Deck deck = ensureDefaultCategories(deckId);
        deckRevisionService.assertExpectedRevision(deckId, expectedRevision);
        assertNameAvailable(deckId, name);
        int order = (int) deckCategoryRepository.countByDeckId(deckId);
        DeckCategory saved =
                deckCategoryRepository.save(new DeckCategory(deckId, name, order, false));
        recordChange(deck);
        return viewOf(saved, List.of(), currentRevisionNumber(deckId));
    }

    public CategoryView rename(
            long deckId, long categoryId, String name, @Nullable Integer expectedRevision) {
        Deck deck = ensureDefaultCategories(deckId);
        deckRevisionService.assertExpectedRevision(deckId, expectedRevision);
        DeckCategory category = ownedCategory(deckId, categoryId);
        boolean changed = !category.getName().equals(name);
        if (changed) {
            assertNameAvailable(deckId, name);
            category.setName(name);
        }
        DeckCategory saved = deckCategoryRepository.save(category);
        if (changed) {
            recordChange(deck);
        }
        return viewsFor(List.of(saved), currentRevisionNumber(deckId)).get(0);
    }

    // Deletion stays owner-only (M4 global constraint: "owners alone manage visibility,
    // collaborators, deletion, and ownership-affecting operations"), unlike create/rename/
    // assignCards which now allow an EDITOR collaborator through editableLocked. So this resolves
    // the locked deck via ownedLocked directly rather than through ensureDefaultCategories.
    public void delete(long deckId, long categoryId) {
        Deck deck = seedDefaultCategories(deckAccessGuard.ownedLocked(deckId));
        DeckCategory category = ownedCategory(deckId, categoryId);
        if (category.isSystemOwned()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "System-owned categories cannot be deleted");
        }
        assignmentRepository.deleteByDeckCategoryId(categoryId);
        deckCategoryRepository.delete(category);
        recordChange(deck);
    }

    public CategoryView assignCards(
            long deckId,
            long categoryId,
            List<Long> deckCardIds,
            @Nullable Integer expectedRevision) {
        Deck deck = ensureDefaultCategories(deckId);
        deckRevisionService.assertExpectedRevision(deckId, expectedRevision);
        DeckCategory category = ownedCategory(deckId, categoryId);
        List<Long> distinctIds = deckCardIds.stream().distinct().toList();
        assertCardsInDeck(deckId, distinctIds);
        replaceAssignments(deck, categoryId, distinctIds);
        return viewOf(category, distinctIds, currentRevisionNumber(deckId));
    }

    private void replaceAssignments(Deck deck, long categoryId, List<Long> distinctIds) {
        Set<Long> before = assignedDeckCardIds(categoryId);
        // Hibernate defers deletes until after inserts within one flush, so a bare delete-then-
        // save here would race the new rows against the old ones on (category, card). Flushing
        // the delete first keeps the "replace" semantics correct and idempotent.
        assignmentRepository.deleteByDeckCategoryId(categoryId);
        assignmentRepository.flush();
        distinctIds.forEach(
                deckCardId ->
                        assignmentRepository.save(
                                new DeckCategoryAssignment(categoryId, deckCardId)));
        if (!before.equals(Set.copyOf(distinctIds))) {
            recordChange(deck);
        }
    }

    private Set<Long> assignedDeckCardIds(long categoryId) {
        return assignmentRepository.findByDeckCategoryIdIn(List.of(categoryId)).stream()
                .map(DeckCategoryAssignment::getDeckCardId)
                .collect(Collectors.toSet());
    }

    private int currentRevisionNumber(long deckId) {
        return deckRevisionService.currentRevisionNumberUnchecked(deckId);
    }

    private void recordChange(Deck deck) {
        deckRevisionService.record(
                deck, deckAccessGuard.profileId(), DeckChangeType.CATEGORY_CHANGED);
    }

    // ponytail: seeded lazily on first touch rather than eagerly at deck creation, so this
    // service stays self-contained and DeckService (out of this task's file map) is untouched.
    //
    // The check-then-act below (existsByDeckId, then insert 6 rows) would otherwise race two
    // concurrent first-touch calls for the same brand-new deck into both seeding and tripping
    // uq_deck_categories_deck_name. editableLocked() takes a PESSIMISTIC_WRITE row lock on the deck
    // first, serializing them exactly like DeckImportCommitService/CollectionImportService
    // already serialize their own first-touch check-then-act paths via lockedProfileId(). It also
    // holds the lock every caller (including the mutations) needs for the expectedRevision check,
    // and returns the locked deck so those callers can record a revision against it directly.
    private Deck ensureDefaultCategories(long deckId) {
        return seedDefaultCategories(deckAccessGuard.editableLocked(deckId));
    }

    // Seeding logic split out from the deck-acquisition/authorization step above so delete() can
    // reuse it against an owner-only-locked Deck (see delete()'s comment) instead of the
    // editable-locked one every other caller here uses.
    private Deck seedDefaultCategories(Deck deck) {
        long deckId = deck.getId();
        if (deckCategoryRepository.existsByDeckId(deckId)) {
            return deck;
        }
        int order = 0;
        for (CardFunctionalCategory functionalCategory : CardFunctionalCategory.values()) {
            DeckCategory category =
                    new DeckCategory(deckId, displayName(functionalCategory), order++, true);
            category.setFunctionalCategory(functionalCategory);
            deckCategoryRepository.save(category);
        }
        return deck;
    }

    private DeckCategory ownedCategory(long deckId, long categoryId) {
        return deckCategoryRepository
                .findByIdAndDeckId(categoryId, deckId)
                .orElseThrow(DeckCategoryNotFoundException::new);
    }

    private void assertNameAvailable(long deckId, String name) {
        if (deckCategoryRepository.existsByDeckIdAndName(deckId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "A category named '" + name + "' already exists");
        }
    }

    private void assertCardsInDeck(long deckId, List<Long> deckCardIds) {
        for (Long deckCardId : deckCardIds) {
            if (deckCardRepository.findByIdAndDeckId(deckCardId, deckId).isEmpty()) {
                throw new DeckCardNotFoundException();
            }
        }
    }

    private List<CategoryView> viewsFor(List<DeckCategory> categories, int revisionNumber) {
        List<Long> ids = categories.stream().map(DeckCategory::getId).toList();
        Map<Long, List<Long>> assignedByCategory =
                assignmentRepository.findByDeckCategoryIdIn(ids).stream()
                        .collect(
                                Collectors.groupingBy(
                                        DeckCategoryAssignment::getDeckCategoryId,
                                        Collectors.mapping(
                                                DeckCategoryAssignment::getDeckCardId,
                                                Collectors.toList())));
        return categories.stream()
                .map(
                        category ->
                                viewOf(
                                        category,
                                        assignedByCategory.getOrDefault(
                                                category.getId(), List.of()),
                                        revisionNumber))
                .toList();
    }

    private CategoryView viewOf(
            DeckCategory category, List<Long> assignedDeckCardIds, int revisionNumber) {
        return new CategoryView(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isSystemOwned(),
                category.getFunctionalCategory(),
                assignedDeckCardIds,
                revisionNumber);
    }

    private static String displayName(CardFunctionalCategory category) {
        String lower = category.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Read-only projection of a category; no JPA entities escape this service. */
    public record CategoryView(
            @Nullable Long id,
            String name,
            int displayOrder,
            boolean systemOwned,
            @Nullable CardFunctionalCategory functionalCategory,
            List<Long> assignedDeckCardIds,
            int revisionNumber) {}
}
