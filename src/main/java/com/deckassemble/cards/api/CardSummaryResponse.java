package com.deckassemble.cards.api;

import com.deckassemble.cards.domain.Card;

public record CardSummaryResponse(Long id, String name, String manaCost, String typeLine) {

  public static CardSummaryResponse from(Card card) {
    return new CardSummaryResponse(card.getId(), card.getName(), card.getManaCost(), card.getTypeLine());
  }
}
