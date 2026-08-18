package com.deckassemble.recommendations.application;

import java.math.BigDecimal;
import java.util.Map;

/** A single explainable component of a candidate card's score. */
public record ScoreContribution(
        RecommendationReasonCode code, BigDecimal points, Map<String, String> evidence) {

    public ScoreContribution {
        evidence = Map.copyOf(evidence);
    }

    public String reason() {
        return RecommendationReasonNarrator.render(code, evidence);
    }
}
