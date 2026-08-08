package com.deckassemble.decks.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
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
class DeckTagServiceTest {

    private static final long PROFILE_ID = 42L;
    private static final long DECK_ID = 1L;
    private static final long TAG_ID_A = 100L;
    private static final long TAG_ID_B = 101L;

    @Mock private DeckTagRepository deckTagRepository;
    @Mock private DeckTagAssignmentRepository assignmentRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private DeckRevisionService deckRevisionService;

    private final AtomicLong nextTagId = new AtomicLong(TAG_ID_A);

    private DeckTagService service;

    @BeforeEach
    void stubCommonCollaborators() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        lenient().when(currentUser.subject()).thenReturn(Optional.of("sub"));
        lenient().when(profileService.getOrCreate("sub")).thenReturn(profile);
        Deck deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient()
                .when(deckRepository.findByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient()
                .when(deckTagRepository.save(any(DeckTag.class)))
                .thenAnswer(
                        inv -> {
                            DeckTag tag = inv.getArgument(0);
                            if (tag.getId() == null) {
                                ReflectionTestUtils.setField(
                                        tag, "id", nextTagId.incrementAndGet());
                            }
                            return tag;
                        });
        DeckAccessGuard deckAccessGuard =
                new DeckAccessGuard(currentUser, profileService, deckRepository);
        service =
                new DeckTagService(
                        deckAccessGuard,
                        deckTagRepository,
                        assignmentRepository,
                        deckRevisionService);
    }

    @Test
    void shouldCreateTag() {
        when(deckTagRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Combo"))
                .thenReturn(false);

        DeckTagService.TagView created = service.create("Combo");

        assertThat(created.name()).isEqualTo("Combo");
        assertThat(created.id()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateNameCaseInsensitively() {
        when(deckTagRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "combo"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create("combo"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldAssignManyTagsToOneDeck() {
        stubOwnedTag(TAG_ID_A);
        stubOwnedTag(TAG_ID_B);

        service.assignToDeck(DECK_ID, List.of(TAG_ID_A, TAG_ID_B));

        verify(assignmentRepository).deleteByDeckId(DECK_ID);
        verify(assignmentRepository, times(2)).save(any(DeckTagAssignment.class));
        verify(deckRevisionService).record(DECK_ID, PROFILE_ID, DeckChangeType.TAG_CHANGED);
    }

    @Test
    void shouldNotRecordRevisionWhenReassigningSameTagSet() {
        stubOwnedTag(TAG_ID_A);
        stubOwnedTag(TAG_ID_B);
        when(assignmentRepository.findByDeckId(DECK_ID))
                .thenReturn(
                        List.of(
                                new DeckTagAssignment(DECK_ID, TAG_ID_A),
                                new DeckTagAssignment(DECK_ID, TAG_ID_B)));

        service.assignToDeck(DECK_ID, List.of(TAG_ID_A, TAG_ID_B));

        verify(deckRevisionService, never()).record(anyLong(), anyLong(), any());
    }

    @Test
    void shouldDeduplicateRepeatedTagIdsOnAssign() {
        stubOwnedTag(TAG_ID_A);

        service.assignToDeck(DECK_ID, List.of(TAG_ID_A, TAG_ID_A));

        verify(assignmentRepository, times(1)).save(any(DeckTagAssignment.class));
    }

    @Test
    void shouldRejectAssigningTagNotOwnedByProfile() {
        when(deckTagRepository.findByIdAndProfileId(TAG_ID_A, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToDeck(DECK_ID, List.of(TAG_ID_A)))
                .isInstanceOf(DeckTagNotFoundException.class);
    }

    @Test
    void shouldRejectAssigningToForeignDeck() {
        when(deckRepository.findByIdAndProfileId(DECK_ID, PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToDeck(DECK_ID, List.of(TAG_ID_A)))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldRenameTagToNewAvailableName() {
        DeckTag tag = stubOwnedTag(TAG_ID_A);
        when(deckTagRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Ramp"))
                .thenReturn(false);

        DeckTagService.TagView renamed = service.rename(TAG_ID_A, "Ramp");

        assertThat(renamed.name()).isEqualTo("Ramp");
        assertThat(tag.getName()).isEqualTo("Ramp");
    }

    @Test
    void shouldRejectRenamingTagToDuplicateName() {
        stubOwnedTag(TAG_ID_A);
        when(deckTagRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Ramp"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.rename(TAG_ID_A, "Ramp"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldDeleteTagAndItsAssignmentsButRetainDecks() {
        DeckTag tag = stubOwnedTag(TAG_ID_A);

        service.delete(TAG_ID_A);

        verify(assignmentRepository).deleteByTagId(TAG_ID_A);
        verify(deckTagRepository).delete(tag);
        verify(deckRepository, org.mockito.Mockito.never()).delete(any());
    }

    @Test
    void shouldListTagsForCurrentProfile() {
        DeckTag tag = new DeckTag(PROFILE_ID, "Combo");
        ReflectionTestUtils.setField(tag, "id", TAG_ID_A);
        when(deckTagRepository.findByProfileIdOrderByNameAsc(PROFILE_ID)).thenReturn(List.of(tag));

        List<DeckTagService.TagView> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Combo");
    }

    private DeckTag stubOwnedTag(long tagId) {
        DeckTag tag = new DeckTag(PROFILE_ID, "Tag" + tagId);
        ReflectionTestUtils.setField(tag, "id", tagId);
        lenient()
                .when(deckTagRepository.findByIdAndProfileId(tagId, PROFILE_ID))
                .thenReturn(Optional.of(tag));
        return tag;
    }
}
