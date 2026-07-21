package com.deckassemble.decks.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DeckUpdateRequest(
        @Size(max = 200) String name,
        @Size(max = 50) String formatCode,
        @Size(max = 2000) String description,
        Long commanderCardId,
        Long secondaryCommanderCardId,
        Boolean useOwnedCardsOnly,
        BigDecimal budgetLimit,
        @Min(1) @Max(10) Integer desiredPowerLevel,
        @Size(max = 50) String playStyle) {}
