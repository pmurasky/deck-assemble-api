package com.deckassemble.recommendations.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Builds the ordered explanation factors attached to commander suggestions. */
final class CommanderSuggestionExplainer {

    private CommanderSuggestionExplainer() {}

    static List<ScoreContribution> explain(
            CommanderSuggestion suggestion, @Nullable Instant fetchedAt) {
        var contributions = new ArrayList<ScoreContribution>();
        contributions.add(coverage(suggestion));
        contributions.add(missing(suggestion));
        contributions.add(cost(suggestion));
        contributions.add(rank(suggestion));
        contributions.add(colorSupport(suggestion));
        contributions.add(freshness(fetchedAt));
        return List.copyOf(contributions);
    }

    private static ScoreContribution coverage(CommanderSuggestion suggestion) {
        return new ScoreContribution(
                RecommendationReasonCode.COLLECTION_COVERAGE,
                suggestion.coveragePercent(),
                Map.of("coveragePercent", suggestion.coveragePercent().toPlainString()));
    }

    private static ScoreContribution missing(CommanderSuggestion suggestion) {
        return new ScoreContribution(
                RecommendationReasonCode.MISSING_COUNT,
                BigDecimal.valueOf(suggestion.missingCardCount()),
                Map.of(
                        "missingCardCount", String.valueOf(suggestion.missingCardCount()),
                        "unpricedMissingCardCount",
                                String.valueOf(suggestion.unpricedMissingCardCount())));
    }

    private static ScoreContribution cost(CommanderSuggestion suggestion) {
        return new ScoreContribution(
                RecommendationReasonCode.COMPLETION_COST,
                suggestion.estimatedCompletionCostUsd(),
                Map.of(
                        "estimatedCompletionCostUsd",
                        suggestion.estimatedCompletionCostUsd().toPlainString()));
    }

    private static ScoreContribution rank(CommanderSuggestion suggestion) {
        Integer commanderRank = suggestion.commanderRank();
        return new ScoreContribution(
                RecommendationReasonCode.COMMANDER_RANK,
                commanderRank == null ? BigDecimal.ZERO : BigDecimal.valueOf(commanderRank),
                Map.of(
                        "commanderRank",
                        commanderRank == null ? "unranked" : commanderRank.toString()));
    }

    private static ScoreContribution colorSupport(CommanderSuggestion suggestion) {
        String colorIdentity = suggestion.colorIdentity();
        return new ScoreContribution(
                RecommendationReasonCode.COLOR_SUPPORT,
                BigDecimal.ZERO,
                Map.of("colorIdentity", colorIdentity == null ? "colorless" : colorIdentity));
    }

    private static ScoreContribution freshness(@Nullable Instant fetchedAt) {
        return new ScoreContribution(
                RecommendationReasonCode.SYNERGY_DATA_FRESHNESS,
                BigDecimal.ZERO,
                Map.of("fetchedAt", fetchedAt == null ? "unknown" : fetchedAt.toString()));
    }
}
