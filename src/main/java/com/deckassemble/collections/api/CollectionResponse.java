package com.deckassemble.collections.api;

import com.deckassemble.collections.domain.CardCollection;
import java.time.Instant;

public record CollectionResponse(
    Long id, String name, String description, boolean defaultCollection, Instant createdAt) {

  public static CollectionResponse from(CardCollection collection) {
    return new CollectionResponse(
        collection.getId(),
        collection.getName(),
        collection.getDescription(),
        collection.isDefaultCollection(),
        collection.getCreatedAt());
  }
}
