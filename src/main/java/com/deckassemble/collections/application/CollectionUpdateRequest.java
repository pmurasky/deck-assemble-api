package com.deckassemble.collections.application;

import jakarta.validation.constraints.Size;

public record CollectionUpdateRequest(
        @Size(max = 255) String name,
        @Size(max = 2000) String description,
        Boolean defaultCollection) {}
