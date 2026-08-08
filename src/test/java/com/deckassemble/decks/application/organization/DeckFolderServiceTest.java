package com.deckassemble.decks.application.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.organization.DeckFolder;
import com.deckassemble.decks.domain.organization.DeckFolderRepository;
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
class DeckFolderServiceTest {

    private static final long PROFILE_ID = 42L;
    private static final long DECK_ID = 1L;
    private static final long FOLDER_ID = 100L;

    @Mock private DeckFolderRepository deckFolderRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;

    private final AtomicLong nextFolderId = new AtomicLong(FOLDER_ID);

    private DeckFolderService service;
    private Deck deck;

    @BeforeEach
    void stubCommonCollaborators() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        lenient().when(currentUser.subject()).thenReturn(Optional.of("sub"));
        lenient().when(profileService.getOrCreate("sub")).thenReturn(profile);
        deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient()
                .when(deckRepository.findByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient()
                .when(deckFolderRepository.save(any(DeckFolder.class)))
                .thenAnswer(
                        inv -> {
                            DeckFolder folder = inv.getArgument(0);
                            if (folder.getId() == null) {
                                ReflectionTestUtils.setField(
                                        folder, "id", nextFolderId.incrementAndGet());
                            }
                            return folder;
                        });
        DeckAccessGuard deckAccessGuard =
                new DeckAccessGuard(currentUser, profileService, deckRepository);
        service = new DeckFolderService(deckAccessGuard, deckFolderRepository, deckRepository);
    }

    @Test
    void shouldCreateFolder() {
        when(deckFolderRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Aggro"))
                .thenReturn(false);

        DeckFolderService.FolderView created = service.create("Aggro");

        assertThat(created.name()).isEqualTo("Aggro");
        assertThat(created.id()).isNotNull();
    }

    @Test
    void shouldRejectDuplicateNameCaseInsensitively() {
        when(deckFolderRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "aggro"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create("aggro"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldAssignDeckToFolderReplacingAnyPriorFolder() {
        DeckFolder folder = new DeckFolder(PROFILE_ID, "Aggro");
        ReflectionTestUtils.setField(folder, "id", FOLDER_ID);
        when(deckFolderRepository.findByIdAndProfileId(FOLDER_ID, PROFILE_ID))
                .thenReturn(Optional.of(folder));
        deck.setFolderId(999L);

        service.assignToDeck(DECK_ID, FOLDER_ID);

        assertThat(deck.getFolderId()).isEqualTo(FOLDER_ID);
        verify(deckRepository).save(deck);
    }

    @Test
    void shouldClearFolderWhenAssigningNull() {
        deck.setFolderId(FOLDER_ID);

        service.assignToDeck(DECK_ID, null);

        assertThat(deck.getFolderId()).isNull();
    }

    @Test
    void shouldRejectAssigningFolderNotOwnedByProfile() {
        when(deckFolderRepository.findByIdAndProfileId(FOLDER_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToDeck(DECK_ID, FOLDER_ID))
                .isInstanceOf(DeckFolderNotFoundException.class);
    }

    @Test
    void shouldRejectAssigningToForeignDeck() {
        when(deckRepository.findByIdAndProfileId(DECK_ID, PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignToDeck(DECK_ID, FOLDER_ID))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldDeleteFolderAndClearReferencesButRetainDecks() {
        DeckFolder folder = new DeckFolder(PROFILE_ID, "Aggro");
        ReflectionTestUtils.setField(folder, "id", FOLDER_ID);
        when(deckFolderRepository.findByIdAndProfileId(FOLDER_ID, PROFILE_ID))
                .thenReturn(Optional.of(folder));

        service.delete(FOLDER_ID);

        verify(deckRepository).clearFolderId(FOLDER_ID);
        verify(deckFolderRepository).delete(folder);
    }

    @Test
    void shouldRenameFolderRejectingDuplicateTarget() {
        DeckFolder folder = new DeckFolder(PROFILE_ID, "Aggro");
        ReflectionTestUtils.setField(folder, "id", FOLDER_ID);
        when(deckFolderRepository.findByIdAndProfileId(FOLDER_ID, PROFILE_ID))
                .thenReturn(Optional.of(folder));
        when(deckFolderRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Control"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.rename(FOLDER_ID, "Control"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void shouldRenameFolderToNewAvailableName() {
        DeckFolder folder = new DeckFolder(PROFILE_ID, "Aggro");
        ReflectionTestUtils.setField(folder, "id", FOLDER_ID);
        when(deckFolderRepository.findByIdAndProfileId(FOLDER_ID, PROFILE_ID))
                .thenReturn(Optional.of(folder));
        when(deckFolderRepository.existsByProfileIdAndNameIgnoreCase(PROFILE_ID, "Midrange"))
                .thenReturn(false);

        DeckFolderService.FolderView renamed = service.rename(FOLDER_ID, "Midrange");

        assertThat(renamed.name()).isEqualTo("Midrange");
        assertThat(folder.getName()).isEqualTo("Midrange");
    }

    @Test
    void shouldListFoldersForCurrentProfile() {
        DeckFolder folder = new DeckFolder(PROFILE_ID, "Aggro");
        ReflectionTestUtils.setField(folder, "id", FOLDER_ID);
        when(deckFolderRepository.findByProfileIdOrderByNameAsc(PROFILE_ID))
                .thenReturn(List.of(folder));

        List<DeckFolderService.FolderView> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Aggro");
        verify(deckRepository, never()).clearFolderId(anyLong());
    }
}
