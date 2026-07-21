package com.deckassemble.users.application;

import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.infrastructure.ProfileRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Profile> findBySubject(String subject) {
        return profileRepository.findByAuthProviderSubject(subject);
    }

    public Profile getOrCreate(String subject) {
        return profileRepository
                .findByAuthProviderSubject(subject)
                .orElseGet(() -> profileRepository.save(new Profile(subject, subject)));
    }

    public Profile update(String subject, ProfileUpdateRequest request) {
        Profile profile =
                profileRepository
                        .findByAuthProviderSubject(subject)
                        .orElseGet(() -> new Profile(subject, subject));

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName());
        }
        if (request.email() != null) {
            profile.setEmail(request.email());
        }
        if (request.preferredFormat() != null) {
            profile.setPreferredFormat(request.preferredFormat());
        }
        if (request.experienceLevel() != null) {
            profile.setExperienceLevel(request.experienceLevel());
        }

        return profileRepository.save(profile);
    }
}
