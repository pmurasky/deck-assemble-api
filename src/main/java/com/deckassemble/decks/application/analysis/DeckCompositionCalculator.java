package com.deckassemble.decks.application.analysis;

import com.deckassemble.recommendations.application.CardCategorizer;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Pure type distribution, functional category, token, and game changer rollups. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class DeckCompositionCalculator {

    private static final List<String> TYPE_BUCKETS =
            List.of(
                    "CREATURE",
                    "INSTANT",
                    "SORCERY",
                    "ARTIFACT",
                    "ENCHANTMENT",
                    "PLANESWALKER",
                    "LAND");

    private DeckCompositionCalculator() {}

    public static Map<String, Integer> typeDistribution(List<AnalysisEntry> entries) {
        Map<String, Integer> distribution = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            typesOf(entry)
                    .forEach(type -> distribution.merge(type, entry.quantity(), Integer::sum));
        }
        return distribution;
    }

    public static Map<String, Integer> functionalCategories(List<AnalysisEntry> entries) {
        Map<String, Integer> categories = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            Category category =
                    CardCategorizer.categorizeText(entry.allTypeLines(), entry.allOracleText());
            categories.merge(category.name(), entry.quantity(), Integer::sum);
        }
        return categories;
    }

    public static List<String> tokenProducers(List<AnalysisEntry> entries) {
        return entries.stream()
                .filter(entry -> createsTokens(entry.allOracleText()))
                .map(entry -> entry.card().name())
                .distinct()
                .sorted()
                .toList();
    }

    public static List<String> gameChangers(List<AnalysisEntry> entries) {
        return entries.stream()
                .filter(entry -> entry.card().gameChanger())
                .map(entry -> entry.card().name())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<String> typesOf(AnalysisEntry entry) {
        String typeLine = entry.primaryTypeLine().toLowerCase(Locale.ROOT);
        List<String> matches =
                TYPE_BUCKETS.stream()
                        .filter(type -> typeLine.contains(type.toLowerCase(Locale.ROOT)))
                        .toList();
        return matches.isEmpty() ? List.of("OTHER") : matches;
    }

    private static boolean createsTokens(String oracleText) {
        return oracleText.contains("creat") && oracleText.contains("token");
    }
}
