package com.deckassemble.decks.application;

import com.deckassemble.decks.domain.Deck;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

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
        int cardCount,
        @Nullable String commanderName,
        Instant createdAt) {

    public static DeckResponse from(Deck deck, int cardCount, @Nullable String commanderName) {
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
                cardCount,
                commanderName,
                deck.getCreatedAt());
    }
}
