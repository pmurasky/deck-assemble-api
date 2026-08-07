package com.deckassemble.decks.application.comparison;

import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse.ComboSummary;
import com.deckassemble.decks.application.comparison.DeckComparisonService.ComboDelta;
import com.deckassemble.decks.application.comparison.DeckComparisonService.LegalityDelta;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Computes per-metric deltas (other minus base); zero-delta keys are dropped. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
final class DeckComparisonCalculator {

    private DeckComparisonCalculator() {}

    static Map<String, Integer> intDelta(Map<String, Integer> base, Map<String, Integer> other) {
        Map<String, Integer> delta = new TreeMap<>();
        base.forEach((key, value) -> delta.put(key, -value));
        other.forEach((key, value) -> delta.merge(key, value, Integer::sum));
        delta.values().removeIf(value -> value == 0);
        return delta;
    }

    static Map<String, BigDecimal> decimalDelta(
            Map<String, BigDecimal> base, Map<String, BigDecimal> other) {
        Map<String, BigDecimal> delta = new TreeMap<>();
        base.forEach((key, value) -> delta.put(key, value.negate()));
        other.forEach((key, value) -> delta.merge(key, value, BigDecimal::add));
        delta.values().removeIf(value -> value.compareTo(BigDecimal.ZERO) == 0);
        return delta;
    }

    static LegalityDelta legalityDelta(DeckLegalityResponse base, DeckLegalityResponse other) {
        List<String> baseCodes =
                base.violations().stream().map(DeckLegalityResponse.Violation::code).toList();
        List<String> otherCodes =
                other.violations().stream().map(DeckLegalityResponse.Violation::code).toList();
        return new LegalityDelta(
                base.legal(),
                other.legal(),
                addedItems(baseCodes, otherCodes),
                removedItems(baseCodes, otherCodes));
    }

    static ComboDelta comboDelta(ComboSummary base, ComboSummary other) {
        List<String> baseIds = base.combos().stream().map(SpellbookCombo::id).toList();
        List<String> otherIds = other.combos().stream().map(SpellbookCombo::id).toList();
        return new ComboDelta(
                base.count(),
                other.count(),
                addedItems(baseIds, otherIds),
                removedItems(baseIds, otherIds));
    }

    static List<String> addedItems(List<String> base, List<String> other) {
        return other.stream().filter(item -> !base.contains(item)).distinct().sorted().toList();
    }

    static List<String> removedItems(List<String> base, List<String> other) {
        return addedItems(other, base);
    }
}
