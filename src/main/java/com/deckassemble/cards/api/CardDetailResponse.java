package com.deckassemble.cards.api;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import java.math.BigDecimal;

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
        Long printingId,
        String imageUrl,
        String setCode,
        String setName,
        String rarity,
        String flavorText) {

    public static CardDetailResponse from(Card card, CardPrinting latestPrinting) {
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
