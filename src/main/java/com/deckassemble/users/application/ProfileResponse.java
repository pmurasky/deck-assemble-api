package com.deckassemble.users.application;

import java.time.Instant;

public record ProfileResponse(
        Long id,
        String displayName,
        String email,
        String preferredFormat,
        String experienceLevel,
        Instant createdAt,
        Instant updatedAt) {}
