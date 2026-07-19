package com.deckassemble.cards.api;

import com.deckassemble.cards.domain.Card;

public record CardDetailResponse(Long id, String name, String manaCost, String typeLine,
    String oracleText, String colorIdentity) {

  public static CardDetailResponse from(Card card) {
    return new CardDetailResponse(card.getId(), card.getName(), card.getManaCost(), card.getTypeLine(),
        card.getOracleText(), card.getColorIdentity());
  }
}
