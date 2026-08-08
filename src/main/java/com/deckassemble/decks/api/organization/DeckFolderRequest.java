package com.deckassemble.decks.api.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for creating a folder or renaming one. */
public record DeckFolderRequest(@NotBlank @Size(max = 100) String name) {}
