package com.deckassemble.decks.api.history;

import jakarta.validation.constraints.NotNull;

/**
 * Restores a deck to an earlier revision, provided the deck hasn't changed since the client last
 * saw {@code expectedCurrentRevision} (409 if it has).
 */
public record RestoreDeckRevisionRequest(@NotNull Integer expectedCurrentRevision) {}
