package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record DeckCandidate(
        long printingId,
        Card card,
        Category category,
        @Nullable CardScore score,
        List<ScoreContribution> contributions) {

    public DeckCandidate {
        contributions = List.copyOf(contributions);
    }

    public DeckCandidate(
            long printingId, Card card, Category category, @Nullable CardScore score) {
        this(printingId, card, category, score, List.of());
    }

    public boolean hasScore() {
        return score != null;
    }

    public double scoreValue() {
        return score != null && score.synergy() != null ? score.synergy() : 0.0;
    }

    public long inclusionValue() {
        return score != null && score.inclusion() != null ? score.inclusion() : 0L;
    }

    public BigDecimal totalScore() {
        return contributions.stream()
                .map(ScoreContribution::points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public DeckCandidate withContribution(ScoreContribution contribution) {
        var updated = new ArrayList<>(contributions);
        updated.add(contribution);
        return new DeckCandidate(printingId, card, category, score, updated);
    }

    public static boolean isEligible(
            Card card, Set<String> commanderOracles, Set<String> identity) {
        return !commanderOracles.contains(card.getScryfallOracleId())
                && Boolean.TRUE.equals(card.getActive())
                && isCommanderLegal(card)
                && withinIdentity(card, identity);
    }

    private static boolean isCommanderLegal(Card card) {
        return card.getLegalities().stream()
                .anyMatch(
                        legality ->
                                "commander".equalsIgnoreCase(legality.getFormatCode())
                                        && "legal".equalsIgnoreCase(legality.getLegalityStatus()));
    }

    private static boolean withinIdentity(Card card, Set<String> identity) {
        if (card.getColorIdentity() == null || card.getColorIdentity().isBlank()) {
            return true;
        }
        for (var color : card.getColorIdentity().split(",")) {
            if (!color.isBlank() && !identity.contains(color.trim())) {
                return false;
            }
        }
        return true;
    }
}
