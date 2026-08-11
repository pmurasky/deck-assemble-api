package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Play-style quota adjustments (spec D11): shifts draft category targets per play style. Deltas net
 * to zero so the draft still balances roles across the same total.
 */
final class PlayStyleQuotas {

    private static final Map<Category, Integer> AGGRO_DELTAS =
            Map.of(
                    Category.FINISHER, 3,
                    Category.RAMP, 1,
                    Category.DRAW, -2,
                    Category.WIPE, -1,
                    Category.PROTECTION, -1);
    private static final Map<Category, Integer> CONTROL_DELTAS =
            Map.of(
                    Category.WIPE, 2,
                    Category.REMOVAL, 2,
                    Category.DRAW, 1,
                    Category.FINISHER, -3,
                    Category.PROTECTION, -2);
    private static final Map<Category, Integer> COMBO_DELTAS =
            Map.of(
                    Category.DRAW, 2,
                    Category.PROTECTION, 2,
                    Category.FINISHER, -2,
                    Category.REMOVAL, -2);
    private static final Map<Category, Integer> TRIBAL_DELTAS =
            Map.of(
                    Category.RAMP, 1,
                    Category.FINISHER, 1,
                    Category.REMOVAL, -1,
                    Category.WIPE, -1);

    private PlayStyleQuotas() {}

    static Map<Category, Integer> forStyle(@Nullable String playStyle) {
        if (playStyle == null || playStyle.isBlank()) {
            return DeckDraftPicker.QUOTAS;
        }
        return switch (playStyle.toLowerCase(Locale.ROOT)) {
            case "aggro" -> adjusted(AGGRO_DELTAS);
            case "control" -> adjusted(CONTROL_DELTAS);
            case "combo" -> adjusted(COMBO_DELTAS);
            case "tribal" -> adjusted(TRIBAL_DELTAS);
            default -> DeckDraftPicker.QUOTAS;
        };
    }

    private static Map<Category, Integer> adjusted(Map<Category, Integer> deltas) {
        var quotas = new EnumMap<Category, Integer>(DeckDraftPicker.QUOTAS);
        deltas.forEach((category, delta) -> quotas.merge(category, delta, Integer::sum));
        return Collections.unmodifiableMap(quotas);
    }
}
