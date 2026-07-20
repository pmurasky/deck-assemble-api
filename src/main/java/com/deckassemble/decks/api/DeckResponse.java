package com.deckassemble.decks.api;

import com.deckassemble.decks.domain.Deck;
import java.math.BigDecimal;
import java.time.Instant;

public record DeckResponse(
    Long id,
    String name,
    String formatCode,
    String description,
    Long commanderCardId,
    Long secondaryCommanderCardId,
    boolean useOwnedCardsOnly,
    BigDecimal budgetLimit,
    Integer desiredPowerLevel,
    String playStyle,
    String status,
    Instant createdAt) {

  public static DeckResponse from(Deck deck) {
    return new DeckResponse(
        deck.getId(),
        deck.getName(),
        deck.getFormatCode(),
        deck.getDescription(),
        deck.getCommanderCardId(),
        deck.getSecondaryCommanderCardId(),
        deck.isUseOwnedCardsOnly(),
        deck.getBudgetLimit(),
        deck.getDesiredPowerLevel(),
        deck.getPlayStyle(),
        deck.getStatus().name(),
        deck.getCreatedAt());
  }
}
