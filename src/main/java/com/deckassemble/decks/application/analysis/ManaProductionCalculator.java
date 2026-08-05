package com.deckassemble.decks.application.analysis;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Pure color production and land counting from oracle text. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class ManaProductionCalculator {

    private static final Pattern ADD_CLAUSE = Pattern.compile("add\\b[^.\\n]*");
    private static final Pattern SYMBOL = Pattern.compile("\\{([^}]*)\\}");
    private static final String COLOR_SYMBOLS = "WUBRGC";
    private static final Set<String> ALL_COLORS = Set.of("W", "U", "B", "R", "G");

    private ManaProductionCalculator() {}

    public static Map<String, Integer> production(List<AnalysisEntry> entries) {
        Map<String, Integer> production = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            for (String color : producedColors(entry.allOracleText())) {
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

    private static Set<String> producedColors(String oracleText) {
        Set<String> colors = new TreeSet<>();
        ADD_CLAUSE
                .matcher(oracleText)
                .results()
                .forEach(clause -> colors.addAll(clauseColors(clause.group())));
        return colors;
    }

    private static Set<String> clauseColors(String clause) {
        if (clause.contains("any color") || clause.contains("any one color")) {
            return ALL_COLORS;
        }
        Set<String> colors = new TreeSet<>();
        SYMBOL.matcher(clause)
                .results()
                .flatMap(match -> match.group(1).chars().mapToObj(symbol -> (char) symbol))
                .map(Character::toUpperCase)
                .filter(symbol -> COLOR_SYMBOLS.indexOf(symbol) >= 0)
                .forEach(symbol -> colors.add(String.valueOf(symbol)));
        return colors;
    }
}
