package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private DeckAccessGuard guard() {
        return new DeckAccessGuard(currentUser, profileService, deckRepository);
    }
}
