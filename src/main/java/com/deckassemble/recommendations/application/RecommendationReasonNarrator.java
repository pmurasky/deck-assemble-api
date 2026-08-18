package com.deckassemble.recommendations.application;

import java.util.Map;

/** Renders deterministic plain-language sentences for recommendation reason codes. */
final class RecommendationReasonNarrator {

    private static final String MISSING = "n/a";

    private RecommendationReasonNarrator() {}

    static String render(RecommendationReasonCode code, Map<String, String> evidence) {
        return substitute(template(code), evidence);
    }

    private static String template(RecommendationReasonCode code) {
        return switch (code) {
            case OWNED -> "You already own this card.";
            case COMMANDER_SYNERGY ->
                    "Strong synergy with your commander (synergy {synergy}, included in {inclusion} decks).";
            case CATEGORY_NEED -> "Fills the {category} slot your deck needs.";
            case PLAY_STYLE -> "Matches your {playStyle} play style.";
            case COMBO -> "Appears in the '{edhrecCardlist}' combo list on EDHREC.";
            case BUDGET ->
                    "Fits your budget (unit price {unitPrice}, running total {runningCost}, limit {budgetLimit}).";
            case GAME_CHANGER_POLICY ->
                    "Game Changer kept within your deck's policy (allowed: {allowedGameChangers}).";
            case COLLECTION_COVERAGE ->
                    "You already own {coveragePercent}% of this commander's top cards.";
            case MISSING_COUNT ->
                    "{missingCardCount} recommended cards are missing from your collection.";
            case COMPLETION_COST -> "Estimated cost to complete: {estimatedCompletionCostUsd} USD.";
            case COMMANDER_RANK -> "Commander rank: {commanderRank}.";
            case COLOR_SUPPORT -> "Supports the {colorIdentity} color identity.";
            case SYNERGY_DATA_FRESHNESS -> "Synergy data last fetched: {fetchedAt}.";
            case MANA_VALUE_DISTANCE ->
                    "Close to your commander's typical mana value (distance {manaValueDistance}).";
            case PRICE -> "Priced at {price} USD.";
        };
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
