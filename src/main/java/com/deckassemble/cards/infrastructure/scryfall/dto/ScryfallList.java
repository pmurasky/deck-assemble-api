package com.deckassemble.cards.infrastructure.scryfall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.net.URI;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallList<T>(
    List<T> data,
    @JsonProperty("has_more") boolean hasMore,
    @JsonProperty("next_page") URI nextPage) {
}
