package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import org.jspecify.annotations.Nullable;

public record DeckCandidate(
        long printingId, Card card, Category category, @Nullable CardScore score) {

    public boolean hasScore() {
        return score != null;
    }

    public double scoreValue() {
        return score != null && score.synergy() != null ? score.synergy() : 0.0;
    }

    public long inclusionValue() {
        return score != null && score.inclusion() != null ? score.inclusion() : 0L;
    }
}
