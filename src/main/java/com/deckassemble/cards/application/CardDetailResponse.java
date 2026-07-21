package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record CardDetailResponse(
        Long id,
        String oracleId,
        String name,
        String manaCost,
        BigDecimal manaValue,
        String colors,
        String colorIdentity,
        String typeLine,
        String oracleText,
        String power,
        String toughness,
        String loyalty,
        String keywords,
        @Nullable Long printingId,
        @Nullable String imageUrl,
        @Nullable String setCode,
        @Nullable String setName,
        @Nullable String rarity,
        @Nullable String flavorText) {

    // Suppressed: a 19-field record factory is one mapping per line; splitting harms readability.
    @SuppressWarnings("checkstyle:MethodLength")
    public static CardDetailResponse from(Card card, @Nullable CardPrinting latestPrinting) {
        return new CardDetailResponse(
                card.getId(),
                card.getScryfallOracleId(),
                card.getName(),
                card.getManaCost(),
                card.getManaValue(),
                card.getColors(),
                card.getColorIdentity(),
                card.getTypeLine(),
                card.getOracleText(),
                card.getPower(),
                card.getToughness(),
                card.getLoyalty(),
                card.getKeywords(),
                latestPrinting != null ? latestPrinting.getId() : null,
                latestPrinting != null ? latestPrinting.getImageUriNormal() : null,
                latestPrinting != null ? latestPrinting.getMagicSet().getSetCode() : null,
                latestPrinting != null ? latestPrinting.getMagicSet().getName() : null,
                latestPrinting != null ? latestPrinting.getRarity() : null,
                latestPrinting != null ? latestPrinting.getFlavorText() : null);
    }
}
