package com.deckassemble.recommendations.application;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record CommanderSuggestion(
        long commanderCardId,
        String commanderName,
        @Nullable String colorIdentity,
        BigDecimal coveragePercent,
        int missingCardCount,
        BigDecimal estimatedCompletionCostUsd,
        int unpricedMissingCardCount,
        @Nullable Integer commanderRank) {}
