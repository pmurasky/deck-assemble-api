package com.deckassemble.cards.infrastructure.scryfall.dto;

import java.net.URI;
import java.time.OffsetDateTime;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ScryfallBulkData(
        String id, String type, URI downloadUri, OffsetDateTime updatedAt, Long compressedSize) {}
