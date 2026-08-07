package com.deckassemble.decks.api.comparison;

import com.deckassemble.decks.application.comparison.DeckComparisonService.CardChange;
import com.deckassemble.decks.application.comparison.DeckComparisonService.ComboDelta;
import com.deckassemble.decks.application.comparison.DeckComparisonService.Comparison;
import com.deckassemble.decks.application.comparison.DeckComparisonService.LegalityDelta;
import com.deckassemble.decks.application.comparison.DeckComparisonService.QuantityChange;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** API view of a deck comparison: composition changes and per-metric deltas (other minus base). */
public record DeckComparisonResponse(
        List<CardChange> added,
        List<CardChange> removed,
        List<QuantityChange> quantityChanged,
        Map<String, Integer> ownershipDelta,
        Map<String, BigDecimal> valueDeltaByCurrency,
        Map<String, BigDecimal> missingCostDeltaByCurrency,
        Map<String, Integer> curveDelta,
        Map<String, Integer> categoryDelta,
        LegalityDelta legality,
        List<String> gameChangersAdded,
        List<String> gameChangersRemoved,
        ComboDelta combos) {

    public static DeckComparisonResponse from(Comparison comparison) {
        return new DeckComparisonResponse(
                comparison.added(),
                comparison.removed(),
                comparison.quantityChanged(),
                comparison.ownershipDelta(),
                comparison.valueDeltaByCurrency(),
                comparison.missingCostDeltaByCurrency(),
                comparison.curveDelta(),
                comparison.categoryDelta(),
                comparison.legality(),
                comparison.gameChangersAdded(),
                comparison.gameChangersRemoved(),
                comparison.combos());
    }
}
