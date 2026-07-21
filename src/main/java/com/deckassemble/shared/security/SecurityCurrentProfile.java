package com.deckassemble.shared.security;

import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.infrastructure.ProfileRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentProfile implements CurrentProfile {

    private final CurrentUser currentUser;
    private final ProfileRepository profileRepository;

    public SecurityCurrentProfile(CurrentUser currentUser, ProfileRepository profileRepository) {
        this.currentUser = currentUser;
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<Profile> profile() {
        return currentUser.subject().flatMap(profileRepository::findByAuthProviderSubject);
    }

    @Override
    public Profile requireProfile() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileRepository
                .findByAuthProviderSubject(subject)
                .orElseThrow(() -> new IllegalStateException("Authenticated profile not found"));
    }
}
