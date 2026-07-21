package com.deckassemble.cards.infrastructure.scryfall.dto;

import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

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
        ScryfallImageUris imageUris) {}
