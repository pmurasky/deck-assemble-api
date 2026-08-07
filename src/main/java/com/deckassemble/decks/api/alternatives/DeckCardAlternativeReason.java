package com.deckassemble.decks.api.alternatives;

import com.deckassemble.recommendations.application.RecommendationReasonCode;
import com.deckassemble.recommendations.application.ScoreContribution;
import java.math.BigDecimal;
import java.util.Map;

/** A single reason explaining an alternative card's ranking. */
public record DeckCardAlternativeReason(
        RecommendationReasonCode code, BigDecimal points, Map<String, String> evidence) {

    public DeckCardAlternativeReason {
        evidence = Map.copyOf(evidence);
    }

    static DeckCardAlternativeReason from(ScoreContribution contribution) {
        return new DeckCardAlternativeReason(
                contribution.code(), contribution.points(), contribution.evidence());
    }
}
