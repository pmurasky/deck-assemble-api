package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.domain.ManaColorParser;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure color production and land counting from oracle text. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class ManaProductionCalculator {

    private ManaProductionCalculator() {}

    public static Map<String, Integer> production(List<AnalysisEntry> entries) {
        Map<String, Integer> production = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            for (String color : ManaColorParser.producedColors(entry.allOracleText())) {
                production.merge(color, entry.quantity(), Integer::sum);
            }
        }
        return production;
    }

    public static int landCount(List<AnalysisEntry> entries) {
        return entries.stream()
                .filter(AnalysisEntry::isLand)
                .mapToInt(AnalysisEntry::quantity)
                .sum();
    }
}
