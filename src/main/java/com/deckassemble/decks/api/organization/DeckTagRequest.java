package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating a tag or renaming one. */
public record DeckTagRequest(@NotBlank @Size(max = 100) String name) {}
