package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.application.CardAnalysisView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** Pure mana curve, color demand, and average mana value calculations. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class ManaCurveCalculator {

    private static final Pattern SYMBOL = Pattern.compile("\\{([^}]*)\\}");
    private static final String COLOR_SYMBOLS = "WUBRGC";
    private static final int HIGH_CURVE_THRESHOLD = 7;

    private ManaCurveCalculator() {}

    public static Map<String, Integer> curve(List<AnalysisEntry> entries) {
        Map<String, Integer> curve = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            if (!entry.isLand()) {
                curve.merge(bucket(entry.card().manaValue()), entry.quantity(), Integer::sum);
            }
        }
        return curve;
    }

    public static Map<String, Integer> colorDemand(List<AnalysisEntry> entries) {
        Map<String, Integer> demand = new TreeMap<>();
        for (AnalysisEntry entry : entries) {
            entry.manaCosts()
                    .flatMap(ManaCurveCalculator::colorPips)
                    .forEach(color -> demand.merge(color, entry.quantity(), Integer::sum));
        }
        return demand;
    }

    public static double averageManaValue(List<AnalysisEntry> entries) {
        int count = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (AnalysisEntry entry : entries) {
            if (!entry.isLand()) {
                count += entry.quantity();
                total =
                        total.add(
                                manaValueOf(entry.card())
                                        .multiply(BigDecimal.valueOf(entry.quantity())));
            }
        }
        return count == 0 ? 0.0 : total.doubleValue() / count;
    }

    private static String bucket(@Nullable BigDecimal manaValue) {
        int value = manaValue == null ? 0 : manaValue.intValue();
        return value >= HIGH_CURVE_THRESHOLD ? HIGH_CURVE_THRESHOLD + "+" : String.valueOf(value);
    }

    private static BigDecimal manaValueOf(CardAnalysisView card) {
        return card.manaValue() == null ? BigDecimal.ZERO : card.manaValue();
    }

    private static Stream<String> colorPips(String manaCost) {
        return SYMBOL.matcher(manaCost)
                .results()
                .flatMap(match -> match.group(1).chars().mapToObj(symbol -> (char) symbol))
                .map(Character::toUpperCase)
                .filter(symbol -> COLOR_SYMBOLS.indexOf(symbol) >= 0)
                .map(String::valueOf);
    }
}
