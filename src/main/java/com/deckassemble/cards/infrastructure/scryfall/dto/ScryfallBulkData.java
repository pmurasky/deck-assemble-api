package com.deckassemble.cards.infrastructure.scryfall.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;
import java.net.URI;
import java.time.OffsetDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallBulkData(
    String id,
    String type,
    URI downloadUri,
    OffsetDateTime updatedAt,
    Long compressedSize) {
}
