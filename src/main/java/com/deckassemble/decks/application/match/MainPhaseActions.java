package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.simulation.DeckLibraryResolver;

/**
 * Main-phase land plays at sorcery speed, one per turn. Spells moved onto the stack with #45 (see
 * {@link StackResolver}).
 */
final class MainPhaseActions {

    private MainPhaseActions() {}

    /** Plays a land from hand onto the battlefield, one per turn. */
    static void playLand(PlayerState player, long printingId) {
        if (player.landPlayedThisTurn()) {
            throw new IllegalArgumentException("already played a land this turn");
        }
        PracticeCard card = player.requireInHand(printingId);
        if (!DeckLibraryResolver.isLand(card.card())) {
            throw new IllegalArgumentException("card is not a land");
        }
        player.hand().remove(card);
        player.battlefield().add(new Permanent(card, player.playerId(), false));
        player.setLandPlayedThisTurn(true);
    }
}
