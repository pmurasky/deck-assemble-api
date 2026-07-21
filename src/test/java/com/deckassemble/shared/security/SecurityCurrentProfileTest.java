package com.deckassemble.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityCurrentProfileTest {

    @Mock private CurrentUser currentUser;
    @Mock private ProfileRepository profileRepository;

    private SecurityCurrentProfile currentProfile() {
        return new SecurityCurrentProfile(currentUser, profileRepository);
    }

    @Test
    void shouldReturnProfileForSubject() {
        Profile profile = new Profile("sub", "User");
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.of(profile));

        assertThat(currentProfile().profile()).contains(profile);
    }

    @Test
    void shouldReturnEmptyProfileWhenNoSubject() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        assertThat(currentProfile().profile()).isEmpty();
    }

    @Test
    void shouldRequireProfileWhenPresent() {
        Profile profile = new Profile("sub", "User");
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.of(profile));

        assertThat(currentProfile().requireProfile()).isSameAs(profile);
    }

    @Test
    void shouldThrowOnRequireProfileWhenNoAuthenticatedUser() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentProfile().requireProfile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user");
    }

    @Test
    void shouldThrowOnRequireProfileWhenProfileMissing() {
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> currentProfile().requireProfile())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated profile not found");
    }
}
