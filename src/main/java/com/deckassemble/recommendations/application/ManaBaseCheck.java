package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.ManaPips;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Karsten-style mana-base validation: derives per-color source requirements from the deck's
 * hardest-to-cast cards (~90% on-curve probability, 99-card table) and compares them against the
 * sources the deck's lands produce. Consumed by deck-level scoring.
 */
public record ManaBaseCheck(
        Map<String, Integer> requiredSources, Map<String, Integer> actualSources) {

    private static final List<String> COLOR_ORDER = List.of("W", "U", "B", "R", "G");
    private static final Pattern ADD_CLAUSE = Pattern.compile("Add ([^.]*)");
    private static final Pattern COLOR_SYMBOL = Pattern.compile("\\{([WUBRG])(?:/[WUBRG])*}");
    private static final String ANY_COLOR = "any color";
    private static final String LAND_TYPE = "Land";
    // ponytail: Karsten's commander offset (~1.35 sources) rounded down to 1
    private static final int COMMANDER_OFFSET = 1;
    private static final int MAX_PIPS = 3;
    private static final int MAX_MV = 5;
    // Karsten 99-card table (~90% on-curve): sources required per (pips, mana value)
    private static final int ONE_PIP_MV1 = 19;
    private static final int ONE_PIP_MV2 = 18;
    private static final int ONE_PIP_MV3 = 16;
    private static final int ONE_PIP_MV4 = 15;
    private static final int ONE_PIP_MV5 = 14;
    private static final int TWO_PIP_MV2 = 26;
    private static final int TWO_PIP_MV3 = 23;
    private static final int TWO_PIP_MV4 = 22;
    private static final int TWO_PIP_MV5 = 20;
    private static final int THREE_PIP_MV3 = 28;
    private static final int THREE_PIP_MV4 = 26;
    private static final int THREE_PIP_MV5 = 23;
    private static final int[][] REQUIRED_TABLE = {
        {0, 0, 0, 0, 0, 0},
        {0, ONE_PIP_MV1, ONE_PIP_MV2, ONE_PIP_MV3, ONE_PIP_MV4, ONE_PIP_MV5},
        {0, 0, TWO_PIP_MV2, TWO_PIP_MV3, TWO_PIP_MV4, TWO_PIP_MV5},
        {0, 0, 0, THREE_PIP_MV3, THREE_PIP_MV4, THREE_PIP_MV5},
    };

    public Map<String, Integer> shortfalls() {
        var shortfalls = new LinkedHashMap<String, Integer>();
        requiredSources.forEach(
                (color, required) -> {
                    var shortfall = required - actualSources.getOrDefault(color, 0);
                    if (shortfall > 0) {
                        shortfalls.put(color, shortfall);
                    }
                });
        return shortfalls;
    }

    public static ManaBaseCheck evaluate(List<Card> deck) {
        var required = new LinkedHashMap<String, Integer>();
        var actual = new LinkedHashMap<String, Integer>();
        for (var card : deck) {
            if (isLand(card)) {
                countProducedSources(card, actual);
            } else {
                raiseRequirements(card, required);
            }
        }
        return new ManaBaseCheck(required, actual);
    }

    private static boolean isLand(Card card) {
        return card.getTypeLine() != null && card.getTypeLine().contains(LAND_TYPE);
    }

    private static void raiseRequirements(Card card, Map<String, Integer> required) {
        if (card.getManaValue() == null) {
            return;
        }
        var pips = ManaPips.fromManaCost(card.getManaCost());
        for (var color : COLOR_ORDER) {
            var count = pips.forColor(color);
            if (count > 0) {
                var requirement =
                        Math.max(
                                0,
                                requiredSources(card.getManaValue().intValue(), count)
                                        - COMMANDER_OFFSET);
                required.merge(color, requirement, Math::max);
            }
        }
    }

    private static void countProducedSources(Card card, Map<String, Integer> actual) {
        if (card.getOracleText() == null) {
            return;
        }
        var clauses = ADD_CLAUSE.matcher(card.getOracleText());
        while (clauses.find()) {
            var clause = clauses.group(1);
            if (clause.contains(ANY_COLOR)) {
                COLOR_ORDER.forEach(color -> actual.merge(color, 1, Integer::sum));
            } else {
                var symbols = COLOR_SYMBOL.matcher(clause);
                while (symbols.find()) {
                    actual.merge(symbols.group(1), 1, Integer::sum);
                }
            }
        }
        // ponytail: fetchlands and non-Add activated abilities produce nothing here;
        // refine when deck scoring needs finer source accounting
    }

    static int requiredSources(int manaValue, int pips) {
        var row = REQUIRED_TABLE[Math.min(pips, MAX_PIPS)];
        return row[Math.min(Math.max(manaValue, 0), MAX_MV)];
    }
}
