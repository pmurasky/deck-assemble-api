package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/** Builds the explainable score contributions attached to deck-build candidates. */
final class CandidateScoreExplainer {

    private CandidateScoreExplainer() {}

    static List<ScoreContribution> explain(
            Card card,
            Category category,
            @Nullable CardScore score,
            DeckBuildRequest request,
            boolean owned) {
        var contributions = new ArrayList<ScoreContribution>();
        contributions.add(categoryNeed(category, request));
        if (owned) {
            contributions.add(ownedMarker());
        }
        if (score != null) {
            contributions.add(commanderSynergy(score));
        }
        comboLists(score).forEach(list -> contributions.add(combo(list)));
        if (matchesPlayStyle(card, request.playStyle())) {
            contributions.add(playStyleMarker(request.playStyle()));
        }
        return List.copyOf(contributions);
    }

    static DeckCandidate withGameChangerPolicy(
            DeckCandidate candidate, int allowed, @Nullable Integer desiredPowerLevel) {
        if (!Boolean.TRUE.equals(candidate.card().getGameChanger())) {
            return candidate;
        }
        Map<String, String> evidence = new ConcurrentHashMap<>();
        evidence.put("allowedGameChangers", String.valueOf(allowed));
        if (desiredPowerLevel != null) {
            evidence.put("desiredPowerLevel", desiredPowerLevel.toString());
        }
        return candidate.withContribution(
                new ScoreContribution(
                        RecommendationReasonCode.GAME_CHANGER_POLICY, BigDecimal.ZERO, evidence));
    }

    static DeckCandidate withBudgetEvidence(
            DeckCandidate candidate, BigDecimal unit, BigDecimal cost, BigDecimal budget) {
        return candidate.withContribution(
                new ScoreContribution(
                        RecommendationReasonCode.BUDGET,
                        BigDecimal.ZERO,
                        Map.of(
                                "unitPrice", unit.toPlainString(),
                                "runningCost", cost.toPlainString(),
                                "budgetLimit", budget.toPlainString())));
    }

    private static ScoreContribution ownedMarker() {
        return new ScoreContribution(
                RecommendationReasonCode.OWNED, BigDecimal.ZERO, Map.of("source", "collection"));
    }

    private static ScoreContribution playStyleMarker(String playStyle) {
        return new ScoreContribution(
                RecommendationReasonCode.PLAY_STYLE,
                BigDecimal.ZERO,
                Map.of("playStyle", playStyle));
    }

    private static ScoreContribution categoryNeed(Category category, DeckBuildRequest request) {
        Map<String, String> evidence = new ConcurrentHashMap<>();
        evidence.put("category", category.name());
        var quota = PlayStyleQuotas.forStyle(request.playStyle()).get(category);
        if (quota != null) {
            evidence.put("quota", quota.toString());
        }
        return new ScoreContribution(
                RecommendationReasonCode.CATEGORY_NEED, BigDecimal.ZERO, evidence);
    }

    private static ScoreContribution commanderSynergy(CardScore score) {
        var points =
                score.synergy() == null ? BigDecimal.ZERO : BigDecimal.valueOf(score.synergy());
        Map<String, String> evidence = new ConcurrentHashMap<>();
        if (score.synergy() != null) {
            evidence.put("synergy", score.synergy().toString());
        }
        if (score.inclusion() != null) {
            evidence.put("inclusion", score.inclusion().toString());
        }
        return new ScoreContribution(RecommendationReasonCode.COMMANDER_SYNERGY, points, evidence);
    }

    private static List<String> comboLists(@Nullable CardScore score) {
        if (score == null) {
            return List.of();
        }
        return score.cardlists().stream()
                .filter(list -> list.toLowerCase(Locale.ROOT).contains("combo"))
                .toList();
    }

    private static ScoreContribution combo(String cardlist) {
        return new ScoreContribution(
                RecommendationReasonCode.COMBO,
                BigDecimal.ZERO,
                Map.of("edhrecCardlist", cardlist));
    }

    private static boolean matchesPlayStyle(Card card, @Nullable String playStyle) {
        if (playStyle == null || playStyle.isBlank()) {
            return false;
        }
        var needle = playStyle.toLowerCase(Locale.ROOT);
        for (CardFace face : card.getFaces()) {
            if (face.getOracleText() != null
                    && face.getOracleText().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
