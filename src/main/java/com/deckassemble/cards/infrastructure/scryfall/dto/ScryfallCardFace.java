package com.deckassemble.cards.infrastructure.scryfall.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallCardFace(
    String name,
    String manaCost,
    String typeLine,
    String oracleText,
    String power,
    String toughness,
    String loyalty,
    List<String> colors,
    ScryfallImageUris imageUris) {
}
