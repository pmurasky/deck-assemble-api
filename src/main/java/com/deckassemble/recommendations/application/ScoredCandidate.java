package com.deckassemble.recommendations.application;

import java.math.BigDecimal;
import java.util.List;

/** A picked card's final score with the contributions that explain it. */
public record ScoredCandidate(
        long printingId, BigDecimal total, List<ScoreContribution> contributions) {

    public ScoredCandidate {
        contributions = List.copyOf(contributions);
    }
}
