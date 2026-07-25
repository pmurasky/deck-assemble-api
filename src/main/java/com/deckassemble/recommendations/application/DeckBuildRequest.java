package com.deckassemble.recommendations.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record DeckBuildRequest(
        @NotNull Long commanderCardId,
        Long secondaryCommanderCardId,
        @Min(1) @Max(10) Integer desiredPowerLevel,
        @Size(max = 50) String playStyle,
        @Nullable Boolean useOwnedCardsOnly,
        @Nullable BigDecimal budgetLimit) {}
