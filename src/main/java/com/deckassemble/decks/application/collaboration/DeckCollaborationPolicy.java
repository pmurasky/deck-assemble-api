package com.deckassemble.decks.application.collaboration;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.collaboration.DeckCollaborator;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRepository;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for whether a profile may view or edit a deck it does not own. Owners
 * always may; a collaborator's {@link DeckCollaboratorRole} otherwise decides it. Deliberately
 * independent of {@code DeckVisibility}/{@code DeckVisibilityPolicy} — those gate the anonymous
 * public share link, this gates authenticated access to the live deck, and a PRIVATE deck must
 * still be reachable by its invited collaborators.
 */
@Component
public class DeckCollaborationPolicy {

    private final DeckCollaboratorRepository deckCollaboratorRepository;

    public DeckCollaborationPolicy(DeckCollaboratorRepository deckCollaboratorRepository) {
        this.deckCollaboratorRepository = deckCollaboratorRepository;
    }

    public boolean isOwner(Deck deck, long profileId) {
        return deck.getProfileId() == profileId;
    }

    public boolean canView(Deck deck, long profileId) {
        return isOwner(deck, profileId) || roleOf(deck, profileId).isPresent();
    }

    public boolean canEdit(Deck deck, long profileId) {
        return isOwner(deck, profileId)
                || roleOf(deck, profileId)
                        .filter(role -> role == DeckCollaboratorRole.EDITOR)
                        .isPresent();
    }

    private Optional<DeckCollaboratorRole> roleOf(Deck deck, long profileId) {
        return deckCollaboratorRepository
                .findByDeckIdAndProfileId(deck.getId(), profileId)
                .map(DeckCollaborator::getRole);
    }
}
