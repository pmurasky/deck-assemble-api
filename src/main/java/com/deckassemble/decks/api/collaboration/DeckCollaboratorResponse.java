package com.deckassemble.decks.api.collaboration;

import com.deckassemble.decks.domain.collaboration.DeckCollaborator;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;

public record DeckCollaboratorResponse(Long profileId, DeckCollaboratorRole role) {

    public static DeckCollaboratorResponse from(DeckCollaborator collaborator) {
        return new DeckCollaboratorResponse(collaborator.getProfileId(), collaborator.getRole());
    }
}
