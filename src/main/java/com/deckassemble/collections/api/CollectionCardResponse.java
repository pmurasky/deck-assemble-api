package com.deckassemble.collections.api;

import com.deckassemble.collections.domain.CollectionCard;

public record CollectionCardResponse(
    Long id, Long cardPrintingId, int regularQuantity, int foilQuantity) {

  public static CollectionCardResponse from(CollectionCard card) {
    return new CollectionCardResponse(
        card.getId(), card.getCardPrintingId(), card.getRegularQuantity(), card.getFoilQuantity());
  }
}
