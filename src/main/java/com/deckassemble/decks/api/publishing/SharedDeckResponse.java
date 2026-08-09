package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import org.jspecify.annotations.Nullable;

/**
 * Publishing-facing view of a deck: current state only. This does not yet pin an immutable revision
 * — that is Task 8's job, which will extend this shape. Includes the owner-supplied primer (raw
 * Markdown source only — see DeckPrimerResponse for why no rendered-HTML field exists).
 */
public record SharedDeckResponse(
        long deckId,
        String name,
        String formatCode,
        @Nullable String description,
        @Nullable Long commanderCardId,
        @Nullable Long secondaryCommanderCardId,
        DeckVisibility visibility,
        @Nullable String shareSlug,
        @Nullable String primerTitle,
        @Nullable String primerMarkdown) {

    public static SharedDeckResponse from(Deck deck) {
        return new SharedDeckResponse(
                deck.getId(),
                deck.getName(),
                deck.getFormatCode(),
                deck.getDescription(),
                deck.getCommanderCardId(),
                deck.getSecondaryCommanderCardId(),
                deck.getVisibility(),
                deck.getShareSlug(),
                deck.getPrimerTitle(),
                deck.getPrimerMarkdown());
    }
}
