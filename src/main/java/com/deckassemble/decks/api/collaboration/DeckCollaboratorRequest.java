package com.deckassemble.decks.api.collaboration;

import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;
import jakarta.validation.constraints.NotNull;

/** Body for inviting an existing profile to collaborate on a deck. */
public record DeckCollaboratorRequest(
        @NotNull Long profileId, @NotNull DeckCollaboratorRole role) {}
