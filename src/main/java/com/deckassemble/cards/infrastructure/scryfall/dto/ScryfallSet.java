package com.deckassemble.cards.infrastructure.scryfall.dto;

import java.time.LocalDate;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallSet(
        String id,
        String code,
        String name,
        String setType,
        LocalDate releasedAt,
        Integer cardCount,
        Boolean digital,
        Boolean foilOnly,
        Boolean nonfoilOnly,
        String iconSvgUri) {}
