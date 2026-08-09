package com.deckassemble.decks.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Partial update: any {@code null} field is left unchanged (see {@code DeckService.update}). */
public record DeckUpdateRequest(
        @Size(max = 200) String name,
        @Size(max = 50) String formatCode,
        @Size(max = 2000) @Nullable String description,
        @Nullable Long commanderCardId,
        @Nullable Long secondaryCommanderCardId,
        Boolean useOwnedCardsOnly,
        @Nullable BigDecimal budgetLimit,
        @Min(1) @Max(10) @Nullable Integer desiredPowerLevel,
        @Size(max = 50) @Nullable String playStyle) {}
