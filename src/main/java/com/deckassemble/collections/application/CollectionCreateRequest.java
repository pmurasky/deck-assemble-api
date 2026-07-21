package com.deckassemble.collections.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionCreateRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description,
        Boolean defaultCollection) {}
