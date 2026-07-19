package com.deckassemble.cards.infrastructure.scryfall.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.List;
import java.util.Map;

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
    List<ScryfallCardFace> cardFaces,
    Map<String, String> legalities) {
}
