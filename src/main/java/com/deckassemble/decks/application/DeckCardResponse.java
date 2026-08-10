package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.decks.domain.DeckCard;
import org.jspecify.annotations.Nullable;

public record DeckCardResponse(
        @Nullable Long id,
        Long cardPrintingId,
        int quantity,
        String deckSection,
        String ownershipStatus,
        CardSummaryResponse card,
        int revisionNumber) {

    public static DeckCardResponse from(
            DeckCard deckCard, CardSummaryResponse card, int revisionNumber) {
        return new DeckCardResponse(
                deckCard.getId(),
                deckCard.getCardPrintingId(),
                deckCard.getQuantity(),
                deckCard.getDeckSection().name(),
                deckCard.getOwnershipStatus().name(),
                card,
                revisionNumber);
    }
}
