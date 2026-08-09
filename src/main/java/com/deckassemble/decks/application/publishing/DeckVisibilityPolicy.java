package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.domain.publishing.DeckVisibility;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for whether a deck may be resolved through its public share slug. PRIVATE
 * decks never resolve, regardless of who is asking — the endpoint is anonymous-reachable (see
 * SecurityConfig) so there is no requester identity to special-case on here.
 */
@Component
public class DeckVisibilityPolicy {

    public boolean isSharedViewAllowed(DeckVisibility visibility) {
        return visibility != DeckVisibility.PRIVATE;
    }
}
