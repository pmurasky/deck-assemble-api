package com.deckassemble.cards.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Colored pip counts parsed from a Scryfall mana cost string. Consumed by deck scoring (color
 * requirements) and the mana-base builder (pip-weighted lands).
 */
public record ManaPips(int w, int u, int b, int r, int g) {

    private static final Pattern SYMBOL = Pattern.compile("\\{([^}]*)}");

    public static ManaPips fromManaCost(@Nullable String manaCost) {
        if (manaCost == null || manaCost.isBlank()) {
            return new ManaPips(0, 0, 0, 0, 0);
        }
        int w = 0;
        int u = 0;
        int b = 0;
        int r = 0;
        int g = 0;
        Matcher matcher = SYMBOL.matcher(manaCost);
        while (matcher.find()) {
            for (String part : matcher.group(1).split("/")) {
                switch (part) {
                    case "W" -> w++;
                    case "U" -> u++;
                    case "B" -> b++;
                    case "R" -> r++;
                    case "G" -> g++;
                    default -> {
                        // ponytail: generic, X, and colorless pips ignored — the mana-base
                        // builder only weights colored sources; refine if Karsten validation
                        // needs them
                    }
                }
            }
        }
        return new ManaPips(w, u, b, r, g);
    }

    public int total() {
        return w + u + b + r + g;
    }
}
