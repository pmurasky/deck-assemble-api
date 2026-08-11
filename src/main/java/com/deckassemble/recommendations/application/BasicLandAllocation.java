package com.deckassemble.recommendations.application;

import com.deckassemble.cards.domain.ManaPips;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Allocates basic-land slots across the commanders' color identity proportional to the deck's
 * colored pip demand, using largest-remainder rounding so the counts sum exactly.
 */
final class BasicLandAllocation {

    private static final List<String> COLOR_ORDER = List.of("W", "U", "B", "R", "G");

    private BasicLandAllocation() {}

    static Map<String, Integer> byPips(Set<String> identity, ManaPips pips, int basicsNeeded) {
        if (basicsNeeded <= 0) {
            return Map.of();
        }
        var colors = COLOR_ORDER.stream().filter(identity::contains).toList();
        return largestRemainder(weights(colors, pips), basicsNeeded);
    }

    private static Map<String, Integer> weights(List<String> colors, ManaPips pips) {
        var weights = new LinkedHashMap<String, Integer>();
        colors.forEach(color -> weights.put(color, pipsOf(color, pips)));
        if (weights.values().stream().allMatch(weight -> weight == 0)) {
            // ponytail: colorless or zero-pip decks fall back to an even split
            weights.replaceAll((color, weight) -> 1);
        }
        return weights;
    }

    private static int pipsOf(String color, ManaPips pips) {
        return switch (color) {
            case "W" -> pips.w();
            case "U" -> pips.u();
            case "B" -> pips.b();
            case "R" -> pips.r();
            default -> pips.g();
        };
    }

    private static Map<String, Integer> largestRemainder(
            Map<String, Integer> weights, int basicsNeeded) {
        var total = weights.values().stream().mapToInt(Integer::intValue).sum();
        var result = new LinkedHashMap<String, Integer>();
        var fractions = new LinkedHashMap<String, Double>();
        var assigned = 0;
        for (var entry : weights.entrySet()) {
            var exact = entry.getValue() * (double) basicsNeeded / total;
            result.put(entry.getKey(), (int) exact);
            fractions.put(entry.getKey(), exact - (int) exact);
            assigned += (int) exact;
        }
        fractions.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(basicsNeeded - assigned)
                .forEach(entry -> result.merge(entry.getKey(), 1, Integer::sum));
        result.values().removeIf(count -> count == 0);
        return result;
    }
}
