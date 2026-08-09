package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckAccessGuardTest {

    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private DeckRepository deckRepository;
    @Mock private DeckCollaborationPolicy deckCollaborationPolicy;
    @Mock private Profile profile;
    @Mock private Deck deck;

    @Test
    void shouldLockCurrentProfile() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(profileService.lock(1L)).thenReturn(profile);

        assertThat(guard().lockedProfileId()).isEqualTo(1L);
        verify(profileService).lock(1L);
    }

    @Test
    void shouldFetchOwnedDeckThroughLockedQuery() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(deckRepository.findLockedByIdAndProfileId(7L, 1L)).thenReturn(Optional.of(deck));

        assertThat(guard().ownedLocked(7L)).isEqualTo(deck);
        verify(deckRepository).findLockedByIdAndProfileId(7L, 1L);
    }

    @Test
    void shouldRejectLockedFetchOfUnownedDeck() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(deckRepository.findLockedByIdAndProfileId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard().ownedLocked(7L)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldAllowOwnerToViewAndEditTheirOwnDeck() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(deckRepository.findById(7L)).thenReturn(Optional.of(deck));
        when(deckCollaborationPolicy.canView(deck, 1L)).thenReturn(true);
        when(deckCollaborationPolicy.canEdit(deck, 1L)).thenReturn(true);

        assertThat(guard().viewable(7L)).isEqualTo(deck);
        assertThat(guard().editable(7L)).isEqualTo(deck);
    }

    @Test
    void shouldRejectViewingADeckThePolicyDenies() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(deckRepository.findById(7L)).thenReturn(Optional.of(deck));
        when(deckCollaborationPolicy.canView(deck, 1L)).thenReturn(false);

        assertThatThrownBy(() -> guard().viewable(7L)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldRejectEditingADeckThePolicyDenies() {
        when(currentUser.subject()).thenReturn(Optional.of("subject"));
        when(profileService.getOrCreate("subject")).thenReturn(profile);
        when(profile.getId()).thenReturn(1L);
        when(deckRepository.findById(7L)).thenReturn(Optional.of(deck));
        when(deckCollaborationPolicy.canEdit(deck, 1L)).thenReturn(false);

        assertThatThrownBy(() -> guard().editable(7L)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldRejectViewingAnUnknownDeckWithoutConsultingThePolicy() {
        // Deck lookup short-circuits before profileId()/the policy are ever consulted — no
        // currentUser/profileService stubs needed, and stubbing them would be dead code Mockito's
        // strict stubbing would flag as unused.
        when(deckRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard().viewable(7L)).isInstanceOf(DeckNotFoundException.class);
    }

    private DeckAccessGuard guard() {
        return new DeckAccessGuard(
                currentUser, profileService, deckRepository, deckCollaborationPolicy);
    }
}
