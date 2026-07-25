package com.deckassemble.decks.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record DeckCreateRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 50) String formatCode,
        @Size(max = 2000) @Nullable String description,
        @Nullable Long commanderCardId,
        @Nullable Long secondaryCommanderCardId,
        @Nullable Boolean useOwnedCardsOnly,
        @Nullable BigDecimal budgetLimit,
        @Min(1) @Max(10) @Nullable Integer desiredPowerLevel,
        @Size(max = 50) @Nullable String playStyle) {}
