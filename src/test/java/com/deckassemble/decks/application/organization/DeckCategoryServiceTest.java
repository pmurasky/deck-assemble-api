package com.deckassemble.decks.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.CardFunctionalCategory;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryAssignment;
import com.deckassemble.decks.domain.organization.DeckCategoryAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DeckCategoryServiceTest {

    private static final long PROFILE_ID = 42L;
    private static final long DECK_ID = 1L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCategoryRepository deckCategoryRepository;
    @Mock private DeckCategoryAssignmentRepository assignmentRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private DeckRevisionService deckRevisionService;
    @Mock private DeckCollaborationPolicy deckCollaborationPolicy;

    private final List<DeckCategory> savedCategories = new ArrayList<>();
    private final AtomicLong nextCategoryId = new AtomicLong(100L);

    private Deck deck;

    @BeforeEach
    void stubCommonCollaborators() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileService.getOrCreate("sub")).thenReturn(profile);
        deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient()
                .when(deckRepository.findByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient().when(deckRepository.findLockedById(DECK_ID)).thenReturn(Optional.of(deck));
        lenient()
                .when(deckRepository.findLockedByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient().when(deckCollaborationPolicy.canEdit(deck, PROFILE_ID)).thenReturn(true);
        lenient()
                .when(deckCategoryRepository.save(any(DeckCategory.class)))
                .thenAnswer(
                        inv -> {
                            DeckCategory category = inv.getArgument(0);
                            if (category.getId() == null) {
                                ReflectionTestUtils.setField(
                                        category, "id", nextCategoryId.incrementAndGet());
                            }
                            savedCategories.add(category);
                            return category;
                        });
        lenient().when(assignmentRepository.findByDeckCategoryIdIn(any())).thenReturn(List.of());
    }

    @Test
    void shouldSeedDefaultCategoriesOnFirstList() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(false);
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(savedCategories);

        List<DeckCategoryService.CategoryView> result = service().list(DECK_ID);

        assertThat(result).hasSize(CardFunctionalCategory.values().length);
        assertThat(result.stream().allMatch(DeckCategoryService.CategoryView::systemOwned))
                .isTrue();
        assertThat(result.get(0).functionalCategory()).isEqualTo(CardFunctionalCategory.LAND);
        assertThat(result.get(0).name()).isEqualTo("Land");
    }

    @Test
    void shouldNotReseedWhenCategoriesAlreadyExist() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(existingCategory(1L, "Land", 0, true)));

        service().list(DECK_ID);

        verify(deckCategoryRepository, never()).save(any());
    }

    @Test
    void shouldOrderCategoriesByDisplayOrder() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory second = existingCategory(2L, "Combos", 6, false);
        DeckCategory first = existingCategory(1L, "Land", 0, true);
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(first, second));

        List<DeckCategoryService.CategoryView> result = service().list(DECK_ID);

        assertThat(result)
                .extracting(DeckCategoryService.CategoryView::name)
                .containsExactly("Land", "Combos");
    }

    @Test
    void shouldRejectDuplicateCategoryNameInSameDeck() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        when(deckCategoryRepository.existsByDeckIdAndName(DECK_ID, "Combos")).thenReturn(true);

        assertThatThrownBy(() -> service().create(DECK_ID, "Combos", null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldCreateUserCategoryAfterDefaultsAppendedInOrder() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        when(deckCategoryRepository.existsByDeckIdAndName(DECK_ID, "Combos")).thenReturn(false);
        when(deckCategoryRepository.countByDeckId(DECK_ID)).thenReturn(6L);

        DeckCategoryService.CategoryView created = service().create(DECK_ID, "Combos", null);

        assertThat(created.displayOrder()).isEqualTo(6);
        assertThat(created.systemOwned()).isFalse();
        assertThat(created.functionalCategory()).isNull();
        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.CATEGORY_CHANGED);
    }

    @Test
    void shouldRenameSystemCategoryWithoutChangingFunctionalAnchor() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory land = existingCategory(1L, "Land", 0, true);
        land.setFunctionalCategory(CardFunctionalCategory.LAND);
        when(deckCategoryRepository.findByIdAndDeckId(1L, DECK_ID)).thenReturn(Optional.of(land));
        when(deckCategoryRepository.existsByDeckIdAndName(DECK_ID, "Mana Sources"))
                .thenReturn(false);

        DeckCategoryService.CategoryView renamed =
                service().rename(DECK_ID, 1L, "Mana Sources", null);

        assertThat(renamed.name()).isEqualTo("Mana Sources");
        assertThat(renamed.functionalCategory()).isEqualTo(CardFunctionalCategory.LAND);
        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.CATEGORY_CHANGED);
    }

    @Test
    void shouldNotRecordRevisionWhenRenamingToSameName() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory land = existingCategory(1L, "Land", 0, true);
        when(deckCategoryRepository.findByIdAndDeckId(1L, DECK_ID)).thenReturn(Optional.of(land));

        service().rename(DECK_ID, 1L, "Land", null);

        verify(deckRevisionService, never()).record(any(Deck.class), anyLong(), any());
    }

    @Test
    void shouldPreventDeletingSystemOwnedCategory() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory land = existingCategory(1L, "Land", 0, true);
        when(deckCategoryRepository.findByIdAndDeckId(1L, DECK_ID)).thenReturn(Optional.of(land));

        assertThatThrownBy(() -> service().delete(DECK_ID, 1L))
                .isInstanceOf(ResponseStatusException.class);
        verify(deckCategoryRepository, never()).delete(any());
    }

    @Test
    void shouldRejectDeleteFromAnEditorCollaboratorWhoIsNotTheOwner() {
        // canEdit() is true (an EDITOR collaborator, not the owner) but findLockedByIdAndProfileId
        // — the owner-scoped lock delete() must use — finds nothing for this profile, proving
        // delete() never falls back to the editable-collaborator path create/rename/assignCards
        // use.
        when(deckRepository.findLockedByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(DECK_ID, 2L))
                .isInstanceOf(DeckNotFoundException.class);
        verify(deckCategoryRepository, never()).delete(any());
    }

    @Test
    void shouldDeleteUserCreatedCategory() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByIdAndDeckId(2L, DECK_ID)).thenReturn(Optional.of(combos));

        service().delete(DECK_ID, 2L);

        verify(assignmentRepository).deleteByDeckCategoryId(2L);
        verify(deckCategoryRepository).delete(combos);
        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.CATEGORY_CHANGED);
    }

    @Test
    void shouldReplaceAssignmentsOnBulkAssign() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByIdAndDeckId(2L, DECK_ID)).thenReturn(Optional.of(combos));
        when(deckCardRepository.findByIdAndDeckId(anyLong(), eq(DECK_ID)))
                .thenAnswer(
                        inv -> {
                            long id = inv.getArgument(0);
                            return Optional.of(
                                    new DeckCard(DECK_ID, id, 1, DeckCard.Section.MAIN_DECK));
                        });
        when(assignmentRepository.save(any(DeckCategoryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCategoryService.CategoryView result =
                service().assignCards(DECK_ID, 2L, List.of(10L, 11L), null);

        assertThat(result.assignedDeckCardIds()).containsExactlyInAnyOrder(10L, 11L);
        verify(assignmentRepository).deleteByDeckCategoryId(2L);
        verify(assignmentRepository, times(2)).save(any(DeckCategoryAssignment.class));
        verify(deckRevisionService).record(deck, PROFILE_ID, DeckChangeType.CATEGORY_CHANGED);
    }

    @Test
    void shouldBeIdempotentWhenAssigningSameCardsAgain() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByIdAndDeckId(2L, DECK_ID)).thenReturn(Optional.of(combos));
        when(deckCardRepository.findByIdAndDeckId(anyLong(), eq(DECK_ID)))
                .thenAnswer(
                        inv -> {
                            long id = inv.getArgument(0);
                            return Optional.of(
                                    new DeckCard(DECK_ID, id, 1, DeckCard.Section.MAIN_DECK));
                        });
        when(assignmentRepository.save(any(DeckCategoryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCategoryService.CategoryView first =
                service().assignCards(DECK_ID, 2L, List.of(10L, 10L, 11L), null);
        DeckCategoryService.CategoryView second =
                service().assignCards(DECK_ID, 2L, List.of(10L, 11L), null);

        assertThat(first.assignedDeckCardIds()).containsExactlyInAnyOrder(10L, 11L);
        assertThat(second.assignedDeckCardIds()).containsExactlyInAnyOrder(10L, 11L);
    }

    @Test
    void shouldNotRecordRevisionWhenReassigningSameCardSet() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByIdAndDeckId(2L, DECK_ID)).thenReturn(Optional.of(combos));
        when(deckCardRepository.findByIdAndDeckId(anyLong(), eq(DECK_ID)))
                .thenAnswer(
                        inv -> {
                            long id = inv.getArgument(0);
                            return Optional.of(
                                    new DeckCard(DECK_ID, id, 1, DeckCard.Section.MAIN_DECK));
                        });
        when(assignmentRepository.findByDeckCategoryIdIn(List.of(2L)))
                .thenReturn(
                        List.of(
                                new DeckCategoryAssignment(2L, 10L),
                                new DeckCategoryAssignment(2L, 11L)));
        when(assignmentRepository.save(any(DeckCategoryAssignment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service().assignCards(DECK_ID, 2L, List.of(10L, 11L), null);

        verify(deckRevisionService, never()).record(any(Deck.class), anyLong(), any());
    }

    @Test
    void shouldRejectAssigningCardNotInDeck() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByIdAndDeckId(2L, DECK_ID)).thenReturn(Optional.of(combos));
        when(deckCardRepository.findByIdAndDeckId(99L, DECK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().assignCards(DECK_ID, 2L, List.of(99L), null))
                .isInstanceOf(DeckCardNotFoundException.class);
    }

    @Test
    void shouldRejectAssigningToNonexistentCategory() {
        when(deckCategoryRepository.existsByDeckId(DECK_ID)).thenReturn(true);
        when(deckCategoryRepository.findByIdAndDeckId(404L, DECK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().assignCards(DECK_ID, 404L, List.of(1L), null))
                .isInstanceOf(DeckCategoryNotFoundException.class);
    }

    @Test
    void shouldEnforceEditorIsolation() {
        when(deckCollaborationPolicy.canEdit(deck, PROFILE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service().list(DECK_ID)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldReturnExplicitCategoryNamesByDeckCardId() {
        DeckCategory land = existingCategory(1L, "Mana Sources", 0, true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(land, combos));
        when(assignmentRepository.findByDeckCategoryIdIn(List.of(1L, 2L)))
                .thenReturn(
                        List.of(
                                new DeckCategoryAssignment(1L, 10L),
                                new DeckCategoryAssignment(2L, 11L)));

        Map<Long, String> result = service().explicitCategoryNamesByDeckCard(DECK_ID);

        assertThat(result)
                .containsExactlyInAnyOrderEntriesOf(Map.of(10L, "Mana Sources", 11L, "Combos"));
    }

    @Test
    void shouldReturnEmptyMapWhenNoCategoriesSeededYet() {
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of());

        assertThat(service().explicitCategoryNamesByDeckCard(DECK_ID)).isEmpty();
    }

    @Test
    void shouldNotSeedDefaultCategoriesWhenReadingExplicitAssignments() {
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of());

        service().explicitCategoryNamesByDeckCard(DECK_ID);

        verify(deckCategoryRepository, never()).existsByDeckId(anyLong());
        verify(deckCategoryRepository, never()).save(any());
    }

    @Test
    void shouldPreferEarliestDisplayOrderCategoryWhenCardAssignedTwice() {
        // A card can only be explicitly assigned once via assignCards' replace semantics, but two
        // separate assignCards calls against different categories could still both reference it;
        // resolve deterministically by earliest display order.
        DeckCategory land = existingCategory(1L, "Land", 0, true);
        DeckCategory combos = existingCategory(2L, "Combos", 6, false);
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(land, combos));
        when(assignmentRepository.findByDeckCategoryIdIn(List.of(1L, 2L)))
                .thenReturn(
                        List.of(
                                new DeckCategoryAssignment(2L, 10L),
                                new DeckCategoryAssignment(1L, 10L)));

        Map<Long, String> result = service().explicitCategoryNamesByDeckCard(DECK_ID);

        assertThat(result).containsExactly(Map.entry(10L, "Land"));
    }

    private DeckCategory existingCategory(long id, String name, int order, boolean systemOwned) {
        DeckCategory category = new DeckCategory(DECK_ID, name, order, systemOwned);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private DeckCategoryService service() {
        return new DeckCategoryService(
                new DeckAccessGuard(
                        currentUser, profileService, deckRepository, deckCollaborationPolicy),
                deckCategoryRepository,
                assignmentRepository,
                deckCardRepository,
                deckRevisionService);
    }
}
