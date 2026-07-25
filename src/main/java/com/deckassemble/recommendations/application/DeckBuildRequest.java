package com.deckassemble.recommendations.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeckBuildRequest(
        @NotNull Long commanderCardId,
        Long secondaryCommanderCardId,
        @Min(1) @Max(10) Integer desiredPowerLevel,
        @Size(max = 50) String playStyle) {}
