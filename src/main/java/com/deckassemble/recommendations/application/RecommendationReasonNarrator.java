package com.deckassemble.recommendations.application;

import java.util.Map;
import java.util.Objects;

/** Renders deterministic plain-language sentences for recommendation reason codes. */
final class RecommendationReasonNarrator {

    private static final String MISSING = "n/a";

    private static final Map<RecommendationReasonCode, String> TEMPLATES =
            Map.ofEntries(
                    Map.entry(RecommendationReasonCode.OWNED, "You already own this card."),
                    Map.entry(
                            RecommendationReasonCode.COMMANDER_SYNERGY,
                            "Strong synergy with your commander (synergy {synergy}, included in"
                                    + " {inclusion} decks)."),
                    Map.entry(
                            RecommendationReasonCode.CATEGORY_NEED,
                            "Fills the {category} slot your deck needs."),
                    Map.entry(
                            RecommendationReasonCode.PLAY_STYLE,
                            "Matches your {playStyle} play style."),
                    Map.entry(
                            RecommendationReasonCode.COMBO,
                            "Appears in the '{edhrecCardlist}' combo list on EDHREC."),
                    Map.entry(
                            RecommendationReasonCode.BUDGET,
                            "Fits your budget (unit price {unitPrice}, running total"
                                    + " {runningCost}, limit {budgetLimit})."),
                    Map.entry(
                            RecommendationReasonCode.GAME_CHANGER_POLICY,
                            "Game Changer kept within your deck's policy (allowed:"
                                    + " {allowedGameChangers})."),
                    Map.entry(
                            RecommendationReasonCode.COLLECTION_COVERAGE,
                            "You already own {coveragePercent}% of this commander's top cards."),
                    Map.entry(
                            RecommendationReasonCode.MISSING_COUNT,
                            "{missingCardCount} recommended cards are missing from your"
                                    + " collection."),
                    Map.entry(
                            RecommendationReasonCode.COMPLETION_COST,
                            "Estimated cost to complete: {estimatedCompletionCostUsd} USD."),
                    Map.entry(
                            RecommendationReasonCode.COMMANDER_RANK,
                            "Commander rank: {commanderRank}."),
                    Map.entry(
                            RecommendationReasonCode.COLOR_SUPPORT,
                            "Supports the {colorIdentity} color identity."),
                    Map.entry(
                            RecommendationReasonCode.SYNERGY_DATA_FRESHNESS,
                            "Synergy data last fetched: {fetchedAt}."),
                    Map.entry(
                            RecommendationReasonCode.MANA_VALUE_DISTANCE,
                            "Close to your commander's typical mana value (distance"
                                    + " {manaValueDistance})."),
                    Map.entry(RecommendationReasonCode.PRICE, "Priced at {price} USD."));

    private RecommendationReasonNarrator() {}

    static String render(RecommendationReasonCode code, Map<String, String> evidence) {
        return substitute(Objects.requireNonNull(TEMPLATES.get(code)), evidence);
    }

    // ponytail: linear placeholder scan; evidence maps carry <= 4 keys.
    private static String substitute(String template, Map<String, String> evidence) {
        var result = template;
        for (var entry : evidence.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result.replaceAll("\\{[a-zA-Z]+}", MISSING);
    }
}
