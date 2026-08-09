package com.deckassemble.decks.application.simulation;

import java.util.List;

/**
 * One or more sample hands drawn from a deck revision's library, plus the seed that produced them
 * (the same seed against the same revision and request reproduces byte-identical output).
 */
public record DeckSampleHandResponse(long seed, List<Hand> hands) {

    public DeckSampleHandResponse {
        hands = List.copyOf(hands);
    }

    /** One drawn hand: how many London mulligans it took, and the cards kept. */
    public record Hand(int mulliganCount, List<DrawnCard> cards) {

        public Hand {
            cards = List.copyOf(cards);
        }
    }

    /** One physical card copy in a drawn hand. */
    public record DrawnCard(Long cardPrintingId, String name) {}
}
