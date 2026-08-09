package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.domain.Deck;
import org.jspecify.annotations.Nullable;

/** The newly created private deck, plus where it came from. */
public record DeckForkResponse(
        long deckId, String name, Long sourceDeckId, @Nullable Integer sourceRevisionNumber) {

    public static DeckForkResponse from(Deck forked) {
        return new DeckForkResponse(
                forked.getId(),
                forked.getName(),
                forked.getSourceDeckId(),
                forked.getSourceRevisionNumber());
    }
}
