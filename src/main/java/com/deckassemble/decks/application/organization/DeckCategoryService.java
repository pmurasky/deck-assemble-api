package com.deckassemble.decks.application.organization;

import com.deckassemble.cards.domain.CardFunctionalCategory;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.history.DeckRevisionService;
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
        deckAccessGuard.owned(deckId);
        ensureDefaultCategories(deckId);
        return viewsFor(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(deckId));
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

    public CategoryView create(long deckId, String name) {
        deckAccessGuard.owned(deckId);
        ensureDefaultCategories(deckId);
        assertNameAvailable(deckId, name);
        int order = (int) deckCategoryRepository.countByDeckId(deckId);
        DeckCategory saved =
                deckCategoryRepository.save(new DeckCategory(deckId, name, order, false));
        recordChange(deckId);
        return viewOf(saved, List.of());
    }

    public CategoryView rename(long deckId, long categoryId, String name) {
        deckAccessGuard.owned(deckId);
        ensureDefaultCategories(deckId);
        DeckCategory category = ownedCategory(deckId, categoryId);
        boolean changed = !category.getName().equals(name);
        if (changed) {
            assertNameAvailable(deckId, name);
            category.setName(name);
        }
        CategoryView view = viewsFor(List.of(deckCategoryRepository.save(category))).get(0);
        if (changed) {
            recordChange(deckId);
        }
        return view;
    }

    public void delete(long deckId, long categoryId) {
        deckAccessGuard.owned(deckId);
        ensureDefaultCategories(deckId);
        DeckCategory category = ownedCategory(deckId, categoryId);
        if (category.isSystemOwned()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "System-owned categories cannot be deleted");
        }
        assignmentRepository.deleteByDeckCategoryId(categoryId);
        deckCategoryRepository.delete(category);
        recordChange(deckId);
    }

    public CategoryView assignCards(long deckId, long categoryId, List<Long> deckCardIds) {
        deckAccessGuard.owned(deckId);
        ensureDefaultCategories(deckId);
        DeckCategory category = ownedCategory(deckId, categoryId);
        List<Long> distinctIds = deckCardIds.stream().distinct().toList();
        assertCardsInDeck(deckId, distinctIds);
        replaceAssignments(deckId, categoryId, distinctIds);
        return viewOf(category, distinctIds);
    }

    private void replaceAssignments(long deckId, long categoryId, List<Long> distinctIds) {
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
            recordChange(deckId);
        }
    }

    private Set<Long> assignedDeckCardIds(long categoryId) {
        return assignmentRepository.findByDeckCategoryIdIn(List.of(categoryId)).stream()
                .map(DeckCategoryAssignment::getDeckCardId)
                .collect(Collectors.toSet());
    }

    private void recordChange(long deckId) {
        deckRevisionService.record(
                deckId, deckAccessGuard.profileId(), DeckChangeType.CATEGORY_CHANGED);
    }

    // ponytail: seeded lazily on first touch rather than eagerly at deck creation, so this
    // service stays self-contained and DeckService (out of this task's file map) is untouched.
    //
    // The check-then-act below (existsByDeckId, then insert 6 rows) would otherwise race two
    // concurrent first-touch calls for the same brand-new deck into both seeding and tripping
    // uq_deck_categories_deck_name. ownedLocked() takes a PESSIMISTIC_WRITE row lock on the deck
    // first, serializing them exactly like DeckImportCommitService/CollectionImportService
    // already serialize their own first-touch check-then-act paths via lockedProfileId().
    private void ensureDefaultCategories(long deckId) {
        deckAccessGuard.ownedLocked(deckId);
        if (deckCategoryRepository.existsByDeckId(deckId)) {
            return;
        }
        int order = 0;
        for (CardFunctionalCategory functionalCategory : CardFunctionalCategory.values()) {
            DeckCategory category =
                    new DeckCategory(deckId, displayName(functionalCategory), order++, true);
            category.setFunctionalCategory(functionalCategory);
            deckCategoryRepository.save(category);
        }
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

    private List<CategoryView> viewsFor(List<DeckCategory> categories) {
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
                                                category.getId(), List.of())))
                .toList();
    }

    private CategoryView viewOf(DeckCategory category, List<Long> assignedDeckCardIds) {
        return new CategoryView(
                category.getId(),
                category.getName(),
                category.getDisplayOrder(),
                category.isSystemOwned(),
                category.getFunctionalCategory(),
                assignedDeckCardIds);
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
            List<Long> assignedDeckCardIds) {}
}
