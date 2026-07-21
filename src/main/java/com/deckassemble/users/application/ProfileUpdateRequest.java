package com.deckassemble.users.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @Size(max = 255) String displayName,
        @Email @Size(max = 255) String email,
        @Size(max = 50) String preferredFormat,
        @Size(max = 50) String experienceLevel) {}
