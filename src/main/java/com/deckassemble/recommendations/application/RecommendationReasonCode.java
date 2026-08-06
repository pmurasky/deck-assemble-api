package com.deckassemble.recommendations.application;

/** Reason codes explaining why a candidate card received its score in a deck build. */
public enum RecommendationReasonCode {
    OWNED,
    COMMANDER_SYNERGY,
    CATEGORY_NEED,
    PLAY_STYLE,
    COMBO,
    BUDGET,
    GAME_CHANGER_POLICY
}
