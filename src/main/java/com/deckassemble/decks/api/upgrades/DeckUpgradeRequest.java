package com.deckassemble.decks.api.upgrades;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Bounds for an upgrade plan proposal: objective, budget ceiling, and maximum changes. */
public record DeckUpgradeRequest(
        @NotNull DeckUpgradeObjective objective,
        @Nullable @Positive BigDecimal budget,
        @Nullable @Pattern(regexp = "usd|usdFoil|eur|tix") String currency,
        @Nullable @Min(1) @Max(50) Integer maxChanges) {}
