package com.deckassemble.cards.infrastructure.scryfall.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallCard(
        String id,
        String oracleId,
        String name,
        String manaCost,
        Double cmc,
        String typeLine,
        String oracleText,
        String power,
        String toughness,
        String loyalty,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        String layout,
        Boolean reserved,
        String setId,
        String set,
        String setName,
        String collectorNumber,
        String rarity,
        String artist,
        String flavorText,
        ScryfallImageUris imageUris,
        LocalDate releasedAt,
        Boolean foil,
        Boolean nonfoil,
        Boolean promo,
        Boolean digital,
        String lang,
        List<ScryfallCardFace> cardFaces,
        Map<String, String> legalities) {}
