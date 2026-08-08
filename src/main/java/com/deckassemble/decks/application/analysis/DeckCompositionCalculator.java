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

    /**
     * Groups entries by presentation category: a card explicitly filed by the user into a deck
     * category (see {@code explicitCategoryNames}, keyed by deck card id) shows that category's
     * name; every other card falls back to its inferred functional {@link Category}. The override
     * never reaches {@link CardCategorizer} itself, so canonical categorization (and anything built
     * on it, e.g. recommendation quotas) is unaffected.
     */
    public static Map<String, Integer> functionalCategories(
            List<AnalysisEntry> entries, Map<Long, String> explicitCategoryNames) {
        Map<String, Integer> categories = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            String categoryName = presentationCategory(entry, explicitCategoryNames);
            categories.merge(categoryName, entry.quantity(), Integer::sum);
        }
        return categories;
    }

    private static String presentationCategory(
            AnalysisEntry entry, Map<Long, String> explicitCategoryNames) {
        // Synthesized rows (e.g. a commander with no persisted DeckCard, see
        // DeckCardService#synthesizedCommander) have no deckCardId and thus can never carry an
        // explicit assignment; skip the lookup rather than querying a null key, which the JDK's
        // immutable Map.of()-family maps reject even for a plain get().
        Long deckCardId = entry.deckCardId();
        String explicit = deckCardId == null ? null : explicitCategoryNames.get(deckCardId);
        if (explicit != null) {
            return explicit;
        }
        Category category =
                CardCategorizer.categorizeText(entry.allTypeLines(), entry.allOracleText());
        return category.name();
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
