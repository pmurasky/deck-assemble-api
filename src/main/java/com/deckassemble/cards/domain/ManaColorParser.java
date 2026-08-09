package com.deckassemble.cards.domain;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Pure "which WUBRG colors can this oracle text produce" parsing: finds every {@code add} clause
 * (mirroring how Magic templates mana abilities) and extracts either an explicit set of mana
 * symbols or, for text like "add one mana of any color," every color. Shared by {@code
 * decks.application.analysis.ManaProductionCalculator} (deck-level mana-production analytics) and
 * {@code decks.application.simulation.ColorAvailabilityCalculator} (Monte Carlo color-availability
 * stats) so both packages read the identical regex/parsing logic instead of drifting apart —
 * relocated here (following {@link CardFunctionalCategory}'s precedent) since neither {@code
 * analysis} nor {@code simulation} may depend on the other. Case-insensitive; callers may pass raw
 * (non-lowercased) oracle text.
 */
public final class ManaColorParser {

    private static final Pattern ADD_CLAUSE = Pattern.compile("add\\b[^.\\n]*");
    private static final Pattern SYMBOL = Pattern.compile("\\{([^}]*)\\}");
    private static final String COLOR_SYMBOLS = "WUBRGC";
    private static final Set<String> ALL_COLORS = Set.of("W", "U", "B", "R", "G");

    private ManaColorParser() {}

    public static Set<String> producedColors(String oracleText) {
        if (oracleText == null) {
            return Set.of();
        }
        Set<String> colors = new TreeSet<>();
        ADD_CLAUSE
                .matcher(oracleText.toLowerCase(Locale.ROOT))
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
