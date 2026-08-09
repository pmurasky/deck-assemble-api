package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.domain.publishing.DeckVisibility;
import jakarta.validation.constraints.NotNull;

/** Desired visibility for a deck's owner-controlled publishing state. */
public record DeckPublishingRequest(@NotNull DeckVisibility visibility) {}
