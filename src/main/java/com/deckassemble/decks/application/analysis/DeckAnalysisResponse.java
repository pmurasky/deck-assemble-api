package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckComboResponse;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Full deck analysis: composition, mana, ownership, value, legality, and combos. */
// Justified: method-local map, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public record DeckAnalysisResponse(
        Map<String, Integer> manaCurve,
        Map<String, Integer> typeDistribution,
        Map<String, Integer> colorDemand,
        Map<String, Integer> colorProduction,
        int landCount,
        double averageManaValue,
        Map<String, Integer> ownershipBreakdown,
        Map<String, BigDecimal> valueByCurrency,
        Map<String, BigDecimal> missingCostByCurrency,
        int unpricedCardCount,
        Map<String, Integer> functionalCategories,
        List<String> tokenProducers,
        List<String> gameChangers,
        DeckLegalityResponse legality,
        ComboSummary combos) {

    // Suppressed: a 15-field record factory is one mapping per line; splitting harms readability.
    @SuppressWarnings("checkstyle:MethodLength")
    static DeckAnalysisResponse from(
            List<AnalysisEntry> entries,
            Map<Long, CardPrice> prices,
            DeckLegalityResponse legality,
            DeckComboResponse combos) {
        DeckValueCalculator.DeckValue value = DeckValueCalculator.value(entries, prices);
        return new DeckAnalysisResponse(
                ManaCurveCalculator.curve(entries),
                DeckCompositionCalculator.typeDistribution(entries),
                ManaCurveCalculator.colorDemand(entries),
                ManaProductionCalculator.production(entries),
                ManaProductionCalculator.landCount(entries),
                ManaCurveCalculator.averageManaValue(entries),
                ownershipBreakdown(entries),
                value.valueByCurrency(),
                value.missingCostByCurrency(),
                value.unpricedCount(),
                DeckCompositionCalculator.functionalCategories(entries),
                DeckCompositionCalculator.tokenProducers(entries),
                DeckCompositionCalculator.gameChangers(entries),
                legality,
                new ComboSummary(combos.available(), combos.combos().size(), combos.combos()));
    }

    private static Map<String, Integer> ownershipBreakdown(List<AnalysisEntry> entries) {
        Map<String, Integer> breakdown = new TreeMap<>();
        entries.forEach(
                entry -> breakdown.merge(entry.ownershipStatus(), entry.quantity(), Integer::sum));
        return breakdown;
    }

    public record ComboSummary(boolean available, int count, List<SpellbookCombo> combos) {}
}
