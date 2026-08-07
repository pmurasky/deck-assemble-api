package com.deckassemble.recommendations.application;

import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CommanderSuggestion(
        long commanderCardId,
        String commanderName,
        @Nullable String colorIdentity,
        BigDecimal coveragePercent,
        int missingCardCount,
        BigDecimal estimatedCompletionCostUsd,
        int unpricedMissingCardCount,
        @Nullable Integer commanderRank,
        List<ScoreContribution> explanations) {

    public CommanderSuggestion {
        explanations = List.copyOf(explanations);
    }

    public CommanderSuggestion withExplanations(List<ScoreContribution> newExplanations) {
        return new CommanderSuggestion(
                commanderCardId,
                commanderName,
                colorIdentity,
                coveragePercent,
                missingCardCount,
                estimatedCompletionCostUsd,
                unpricedMissingCardCount,
                commanderRank,
                newExplanations);
    }
}
