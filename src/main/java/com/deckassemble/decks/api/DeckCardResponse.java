package com.deckassemble.decks.api;

import com.deckassemble.cards.api.CardSummaryResponse;
import com.deckassemble.decks.domain.DeckCard;

public record DeckCardResponse(
        Long id, Long cardPrintingId, int quantity, String deckSection, CardSummaryResponse card) {

    public static DeckCardResponse from(DeckCard deckCard, CardSummaryResponse card) {
        return new DeckCardResponse(
                deckCard.getId(),
                deckCard.getCardPrintingId(),
                deckCard.getQuantity(),
                deckCard.getDeckSection().name(),
                card);
    }
}
