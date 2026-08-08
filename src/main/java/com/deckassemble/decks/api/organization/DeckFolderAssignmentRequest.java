package com.deckassemble.decks.api.organization;

import org.jspecify.annotations.Nullable;

/** Body for assigning (or, with a null id, clearing) a deck's folder. */
public record DeckFolderAssignmentRequest(@Nullable Long folderId) {}
