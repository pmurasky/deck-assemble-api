package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

/** Body for creating a category or renaming one; display order is always server-assigned. */
public record DeckCategoryRequest(
        @NotBlank @Size(max = 100) String name, @Nullable Integer expectedRevision) {}
