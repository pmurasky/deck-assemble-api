package com.deckassemble.cards.infrastructure.scryfall.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallPrices(String usd, String usdFoil, String eur, String tix) {}
