package com.deckassemble.cards.application;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Exact external identifiers available for a card reference. */
public record CardReference(
        @Nullable UUID scryfallId,
        @Nullable String name,
        @Nullable String setCode,
        @Nullable String collectorNumber) {}
