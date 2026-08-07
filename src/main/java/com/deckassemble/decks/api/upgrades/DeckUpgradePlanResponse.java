package com.deckassemble.decks.api.upgrades;

import com.deckassemble.decks.api.alternatives.DeckCardAlternativeReason;
import com.deckassemble.decks.application.upgrades.DeckUpgradeService.DeckUpgradePlan;
import com.deckassemble.decks.application.upgrades.DeckUpgradeService.Substitution;
import com.deckassemble.decks.application.upgrades.DeckUpgradeService.UpgradeMetrics;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** A bounded upgrade plan proposal: reasoned substitutions plus before/after metrics. */
public record DeckUpgradePlanResponse(
        DeckUpgradeObjective objective,
        String currency,
        @Nullable BigDecimal budget,
        int maxChanges,
        List<UpgradeSubstitutionResponse> substitutions,
        UpgradeMetricsResponse before,
        UpgradeMetricsResponse after) {

    public static DeckUpgradePlanResponse from(DeckUpgradePlan plan) {
        return new DeckUpgradePlanResponse(
                DeckUpgradeObjective.valueOf(plan.objective().name()),
                plan.currency(),
                plan.budget(),
                plan.maxChanges(),
                plan.substitutions().stream().map(UpgradeSubstitutionResponse::from).toList(),
                UpgradeMetricsResponse.from(plan.before()),
                UpgradeMetricsResponse.from(plan.after()));
    }

    /** One proposed swap of a deck card for a ranked alternative, with ranking reasons. */
    public record UpgradeSubstitutionResponse(
            long deckCardId,
            long removedPrintingId,
            String removedName,
            String removedOwnershipStatus,
            int quantity,
            long addedPrintingId,
            String addedName,
            boolean addedOwned,
            @Nullable BigDecimal cost,
            List<DeckCardAlternativeReason> reasons) {

        static UpgradeSubstitutionResponse from(Substitution substitution) {
            return new UpgradeSubstitutionResponse(
                    substitution.deckCardId(),
                    substitution.removedPrintingId(),
                    substitution.removedName(),
                    substitution.removedOwnershipStatus(),
                    substitution.quantity(),
                    substitution.addedPrintingId(),
                    substitution.addedName(),
                    substitution.addedOwned(),
                    substitution.cost(),
                    substitution.reasons().stream()
                            .map(
                                    contribution ->
                                            new DeckCardAlternativeReason(
                                                    contribution.code(),
                                                    contribution.points(),
                                                    contribution.evidence()))
                            .toList());
        }
    }

    /** Ownership, value, missing cost, and category counts for one deck state. */
    public record UpgradeMetricsResponse(
            Map<String, Integer> ownershipBreakdown,
            Map<String, BigDecimal> valueByCurrency,
            Map<String, BigDecimal> missingCostByCurrency,
            Map<String, Integer> functionalCategories,
            boolean legal) {

        static UpgradeMetricsResponse from(UpgradeMetrics metrics) {
            return new UpgradeMetricsResponse(
                    metrics.ownershipBreakdown(),
                    metrics.valueByCurrency(),
                    metrics.missingCostByCurrency(),
                    metrics.functionalCategories(),
                    metrics.legal());
        }
    }
}
