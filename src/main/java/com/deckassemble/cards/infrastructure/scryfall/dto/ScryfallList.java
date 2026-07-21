package com.deckassemble.cards.infrastructure.scryfall.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallList<T>(
        List<T> data,
        @JsonProperty("has_more") boolean hasMore,
        @JsonProperty("next_page") URI nextPage) {}
