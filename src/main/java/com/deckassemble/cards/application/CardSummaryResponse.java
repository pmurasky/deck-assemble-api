package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record CardSummaryResponse(
        Long id,
        String oracleId,
        String name,
        String manaCost,
        BigDecimal manaValue,
        String colors,
        String colorIdentity,
        String typeLine,
        String power,
        String toughness,
        @Nullable Long printingId,
        @Nullable String imageUrl,
        @Nullable String setCode,
        @Nullable String setName,
        @Nullable String rarity) {

    public static CardSummaryResponse from(Card card, @Nullable CardPrinting latestPrinting) {
        return new CardSummaryResponse(
                card.getId(),
                card.getScryfallOracleId(),
                card.getName(),
                card.getManaCost(),
                card.getManaValue(),
                card.getColors(),
                card.getColorIdentity(),
                card.getTypeLine(),
                card.getPower(),
                card.getToughness(),
                latestPrinting != null ? latestPrinting.getId() : null,
                latestPrinting != null ? latestPrinting.getImageUriNormal() : null,
                latestPrinting != null ? latestPrinting.getMagicSet().getSetCode() : null,
                latestPrinting != null ? latestPrinting.getMagicSet().getName() : null,
                latestPrinting != null ? latestPrinting.getRarity() : null);
    }
}
