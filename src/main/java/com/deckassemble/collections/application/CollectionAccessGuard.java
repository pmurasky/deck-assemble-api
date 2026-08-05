package com.deckassemble.collections.application;

import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import org.springframework.stereotype.Component;

/** Resolves the current profile for collection import operations. */
@Component
public class CollectionAccessGuard {

    private final CurrentUser currentUser;
    private final ProfileService profileService;

    public CollectionAccessGuard(CurrentUser currentUser, ProfileService profileService) {
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public long profileId() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }

    public long lockedProfileId() {
        long profileId = profileId();
        return profileService.lock(profileId).getId();
    }
}
