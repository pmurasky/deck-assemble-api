package com.deckassemble.collections.api;

import com.deckassemble.cards.api.CardSummaryResponse;
import com.deckassemble.collections.domain.CollectionCard;

public record CollectionCardResponse(
        Long id,
        Long cardPrintingId,
        int regularQuantity,
        int foilQuantity,
        CardSummaryResponse card) {

    public static CollectionCardResponse from(CollectionCard card, CardSummaryResponse summary) {
        return new CollectionCardResponse(
                card.getId(),
                card.getCardPrintingId(),
                card.getRegularQuantity(),
                card.getFoilQuantity(),
                summary);
    }
}
