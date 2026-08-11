package com.deckassemble.cards.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Colored pip counts parsed from a Scryfall mana cost string. Consumed by deck scoring (color
 * requirements) and the mana-base builder (pip-weighted lands).
 */
public record ManaPips(int w, int u, int b, int r, int g) {

    public static final ManaPips ZERO = new ManaPips(0, 0, 0, 0, 0);

    private static final Pattern SYMBOL = Pattern.compile("\\{([^}]*)}");
    private static final int WHITE = 0;
    private static final int BLUE = 1;
    private static final int BLACK = 2;
    private static final int RED = 3;
    private static final int GREEN = 4;
    private static final int COLOR_COUNT = 5;

    public static ManaPips fromManaCost(@Nullable String manaCost) {
        int[] counts = new int[COLOR_COUNT];
        if (manaCost != null && !manaCost.isBlank()) {
            Matcher matcher = SYMBOL.matcher(manaCost);
            while (matcher.find()) {
                countSymbol(matcher.group(1), counts);
            }
        }
        return new ManaPips(counts[WHITE], counts[BLUE], counts[BLACK], counts[RED], counts[GREEN]);
    }

    private static void countSymbol(String symbol, int[] counts) {
        for (String part : symbol.split("/")) {
            switch (part) {
                case "W" -> counts[WHITE]++;
                case "U" -> counts[BLUE]++;
                case "B" -> counts[BLACK]++;
                case "R" -> counts[RED]++;
                case "G" -> counts[GREEN]++;
                default -> {
                    // ponytail: generic, X, and colorless pips ignored — the mana-base
                    // builder only weights colored sources; refine if Karsten validation
                    // needs them
                }
            }
        }
    }

    public int total() {
        return w + u + b + r + g;
    }

    public ManaPips plus(ManaPips other) {
        return new ManaPips(w + other.w, u + other.u, b + other.b, r + other.r, g + other.g);
    }
}
