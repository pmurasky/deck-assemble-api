package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.ManaColorParser;
import java.util.Set;
import java.util.TreeSet;

/**
 * Color availability, scoped to land cards (the simulation's color-availability stat is "which
 * colors can my mana base produce," not general mana-ability detection — see {@link
 * DeckSimulationService}, which only calls this for cards {@code DeckLibraryResolver.isLand}
 * accepts). Delegates the actual "add clause" oracle-text parsing to {@link ManaColorParser},
 * shared with {@code decks.application.analysis.ManaProductionCalculator} so both read raw {@link
 * Card} facts through the identical regex logic.
 */
final class ColorAvailabilityCalculator {

    private ColorAvailabilityCalculator() {}

    /** The WUBRG colors a land's oracle text (front face plus any other faces) can produce. */
    static Set<String> producedColors(Card card) {
        Set<String> colors = new TreeSet<>(ManaColorParser.producedColors(card.getOracleText()));
        for (CardFace face : card.getFaces()) {
            colors.addAll(ManaColorParser.producedColors(face.getOracleText()));
        }
        return colors;
    }
}
