package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
        @Nullable String rarity,
        @Nullable Boolean foilAvailable,
        @Nullable Boolean nonfoilAvailable,
        Map<String, String> legalities,
        List<CardFaceResponse> faces) {

    // Suppressed: a 18-field record factory is one mapping per line; splitting harms readability.
    @SuppressWarnings("checkstyle:MethodLength")
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
                PrintingFields.of(latestPrinting, CardPrinting::getId),
                PrintingFields.of(latestPrinting, CardPrinting::getImageUriNormal),
                PrintingFields.of(latestPrinting, printing -> printing.getMagicSet().getSetCode()),
                PrintingFields.of(latestPrinting, printing -> printing.getMagicSet().getName()),
                PrintingFields.of(latestPrinting, CardPrinting::getRarity),
                PrintingFields.of(latestPrinting, CardPrinting::getFoilAvailable),
                PrintingFields.of(latestPrinting, CardPrinting::getNonfoilAvailable),
                LegalityMapper.byFormat(card.getLegalities()),
                PrintingFields.of(
                        latestPrinting,
                        printing ->
                                printing.getFaces().stream().map(CardFaceResponse::from).toList(),
                        List.of()));
    }
}
