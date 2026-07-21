package com.deckassemble.users.application;

import com.deckassemble.users.api.ProfileResponse;
import com.deckassemble.users.domain.Profile;

public final class ProfileMapper {

    private ProfileMapper() {}

    public static ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getDisplayName(),
                profile.getEmail(),
                profile.getPreferredFormat(),
                profile.getExperienceLevel(),
                profile.getCreatedAt(),
                profile.getUpdatedAt());
    }
}
