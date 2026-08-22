package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.simulation.DeckLibraryResolver;

/**
 * Main-phase plays at sorcery speed: land drops and immediate spell resolution — no stack and no
 * mana costs (both arrive in #45).
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

    /**
     * Casts a card: commanders from the command zone (adding tax), other cards from hand. Spells
     * resolve immediately — creatures and permanents enter the battlefield, instants and sorceries
     * go to the graveyard.
     */
    static void castSpell(PlayerState player, long printingId) {
        if (player.commander().printingId() == printingId && player.commanderInCommandZone()) {
            castCommanderFromCommandZone(player);
            return;
        }
        PracticeCard card = player.requireInHand(printingId);
        if (DeckLibraryResolver.isLand(card.card())) {
            throw new IllegalArgumentException("lands are played, not cast");
        }
        player.hand().remove(card);
        resolve(player, card);
    }

    private static void castCommanderFromCommandZone(PlayerState player) {
        player.incrementCommanderTax();
        player.setCommanderInCommandZone(false);
        player.battlefield().add(new Permanent(player.commander(), player.playerId(), true));
    }

    private static void resolve(PlayerState player, PracticeCard card) {
        String typeLine = card.card().getTypeLine();
        if (typeLine != null && (typeLine.contains("Instant") || typeLine.contains("Sorcery"))) {
            player.graveyard().add(card);
        } else {
            player.battlefield().add(new Permanent(card, player.playerId(), false));
        }
    }
}
