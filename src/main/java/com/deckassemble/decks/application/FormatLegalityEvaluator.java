package com.deckassemble.decks.application;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import java.util.List;

/**
 * Evaluates a deck against the legality rules of a specific format.
 *
 * @since 1.0
 */
interface FormatLegalityEvaluator {

    /**
     * Returns the deck format code this evaluator handles (e.g. "COMMANDER", "STANDARD").
     *
     * @return the format code
     * @since 1.0
     */
    String formatCode();

    /**
     * Evaluates the given deck and its cards against the format's rules.
     *
     * @param deck the deck to evaluate
     * @param deckCards the cards assigned to the deck
     * @return the legality evaluation result
     * @since 1.0
     */
    DeckLegalityResponse evaluate(Deck deck, List<DeckCard> deckCards);
}
