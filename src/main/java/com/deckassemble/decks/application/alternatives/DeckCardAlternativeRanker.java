package com.deckassemble.decks.application.alternatives;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import com.deckassemble.recommendations.application.CardScore;
import com.deckassemble.recommendations.application.RecommendationReasonCode;
import com.deckassemble.recommendations.application.ScoreContribution;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Ranks alternative candidates and builds the reasons attached to each alternative. */
final class DeckCardAlternativeRanker {

    private static final BigDecimal MAX_MANA_VALUE_BONUS = new BigDecimal("2");

    private DeckCardAlternativeRanker() {}

    static List<DeckCardAlternative> rank(
            List<AlternativeCandidate> candidates,
            Card target,
            Category targetCategory,
            boolean ownedFirst) {
        var ranked =
                new ArrayList<>(
                        candidates.stream()
                                .map(candidate -> score(candidate, target, targetCategory))
                                .toList());
        ranked.sort(order(ownedFirst));
        return List.copyOf(ranked);
    }

    private static DeckCardAlternative score(
            AlternativeCandidate candidate, Card target, Category targetCategory) {
        var contributions = new ArrayList<ScoreContribution>();
        if (candidate.owned()) {
            contributions.add(ownedMarker());
        }
        categoryMatch(candidate, targetCategory).ifPresent(contributions::add);
        contributions.add(manaValueDistance(candidate.card(), target));
        contributions.add(commanderSynergy(candidate.score()));
        contributions.add(price(candidate.priceUsd()));
        if (candidate.breaksCombo()) {
            contributions.add(comboWarning(target));
        }
        return new DeckCardAlternative(
                candidate.printingId(),
                candidate.card().getName(),
                candidate.owned(),
                candidate.priceUsd(),
                total(contributions),
                contributions);
    }

    private static Comparator<DeckCardAlternative> order(boolean ownedFirst) {
        var byScore =
                Comparator.comparing(DeckCardAlternative::total)
                        .reversed()
                        .thenComparing(
                                DeckCardAlternative::priceUsd,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DeckCardAlternative::name);
        return ownedFirst
                ? Comparator.comparing(DeckCardAlternative::owned).reversed().thenComparing(byScore)
                : byScore;
    }

    private static BigDecimal total(List<ScoreContribution> contributions) {
        return contributions.stream()
                .map(ScoreContribution::points)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static ScoreContribution ownedMarker() {
        return new ScoreContribution(
                RecommendationReasonCode.OWNED, BigDecimal.ZERO, Map.of("source", "collection"));
    }

    private static Optional<ScoreContribution> categoryMatch(
            AlternativeCandidate candidate, Category targetCategory) {
        var category = candidate.category();
        if (category != targetCategory) {
            return Optional.empty();
        }
        return Optional.of(
                new ScoreContribution(
                        RecommendationReasonCode.CATEGORY_NEED,
                        BigDecimal.ONE,
                        Map.of("category", category.name())));
    }

    private static ScoreContribution manaValueDistance(Card candidate, Card target) {
        var targetManaValue = manaValue(target);
        var candidateManaValue = manaValue(candidate);
        var distance = candidateManaValue.subtract(targetManaValue).abs();
        var points = MAX_MANA_VALUE_BONUS.subtract(distance).max(BigDecimal.ZERO);
        return new ScoreContribution(
                RecommendationReasonCode.MANA_VALUE_DISTANCE,
                points,
                Map.of(
                        "targetManaValue", targetManaValue.toPlainString(),
                        "candidateManaValue", candidateManaValue.toPlainString(),
                        "distance", distance.toPlainString()));
    }

    private static BigDecimal manaValue(Card card) {
        return card.getManaValue() == null ? BigDecimal.ZERO : card.getManaValue();
    }

    private static ScoreContribution commanderSynergy(CardScore score) {
        var points =
                score.synergy() == null ? BigDecimal.ZERO : BigDecimal.valueOf(score.synergy());
        var evidence = new java.util.HashMap<String, String>();
        if (score.synergy() != null) {
            evidence.put("synergy", score.synergy().toString());
        }
        if (score.inclusion() != null) {
            evidence.put("inclusion", score.inclusion().toString());
        }
        return new ScoreContribution(RecommendationReasonCode.COMMANDER_SYNERGY, points, evidence);
    }

    private static ScoreContribution price(@Nullable BigDecimal priceUsd) {
        return new ScoreContribution(
                RecommendationReasonCode.PRICE,
                BigDecimal.ZERO,
                Map.of("usd", priceUsd == null ? "unknown" : priceUsd.toPlainString()));
    }

    private static ScoreContribution comboWarning(Card target) {
        return new ScoreContribution(
                RecommendationReasonCode.COMBO,
                BigDecimal.ZERO,
                Map.of("warning", "breaks combo with " + target.getName()));
    }
}
