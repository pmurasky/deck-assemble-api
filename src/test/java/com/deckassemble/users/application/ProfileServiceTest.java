package com.deckassemble.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private ProfileRepository profileRepository;

    @Test
    void shouldFindBySubject() {
        Profile profile = new Profile("sub", "User");
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.of(profile));

        assertThat(service().findBySubject("sub")).contains(profile);
    }

    @Test
    void shouldReturnExistingProfileOnGetOrCreate() {
        Profile profile = new Profile("sub", "User");
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.of(profile));

        Profile result = service().getOrCreate("sub");

        assertThat(result).isSameAs(profile);
        verify(profileRepository, never()).save(any());
    }

    @Test
    void shouldCreateProfileOnGetOrCreateWhenMissing() {
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result = service().getOrCreate("sub");

        assertThat(result.getAuthProviderSubject()).isEqualTo("sub");
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void shouldApplyOnlyProvidedFieldsOnUpdate() {
        Profile profile = new Profile("sub", "Original");
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result =
                service()
                        .update(
                                "sub",
                                new ProfileUpdateRequest("Renamed", null, "commander", null));

        assertThat(result.getDisplayName()).isEqualTo("Renamed");
        assertThat(result.getEmail()).isNull();
        assertThat(result.getPreferredFormat()).isEqualTo("commander");
        assertThat(result.getExperienceLevel()).isNull();
    }

    @Test
    void shouldCreateProfileOnUpdateWhenMissing() {
        when(profileRepository.findByAuthProviderSubject("sub")).thenReturn(Optional.empty());
        when(profileRepository.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        Profile result =
                service().update("sub", new ProfileUpdateRequest(null, "a@b.c", null, null));

        assertThat(result.getAuthProviderSubject()).isEqualTo("sub");
        assertThat(result.getEmail()).isEqualTo("a@b.c");
    }

    private ProfileService service() {
        return new ProfileService(profileRepository);
    }
}
