package com.deckassemble.decks.application.analysis;

import com.deckassemble.cards.domain.CardPrice;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;

/** Pure deck value and missing-cost calculations over latest price snapshots. */
// Justified: method-local maps, never shared across threads.
@SuppressWarnings("PMD.UseConcurrentHashMap")
public final class DeckValueCalculator {

    private static final String OWNED = "OWNED";

    private DeckValueCalculator() {}

    public static DeckValue value(List<AnalysisEntry> entries, Map<Long, CardPrice> prices) {
        Map<String, BigDecimal> value = new TreeMap<>();
        Map<String, BigDecimal> missing = new TreeMap<>();
        int unpriced = 0;
        for (AnalysisEntry entry : entries) {
            CardPrice price = prices.get(entry.printingId());
            if (price == null || isEmpty(price)) {
                unpriced += entry.quantity();
                continue;
            }
            accumulate(value, price, entry.quantity());
            if (!OWNED.equals(entry.ownershipStatus())) {
                accumulate(missing, price, entry.quantity());
            }
        }
        return new DeckValue(value, missing, unpriced);
    }

    private static void accumulate(Map<String, BigDecimal> totals, CardPrice price, int quantity) {
        add(totals, "usd", price.usd(), quantity);
        add(totals, "usdFoil", price.usdFoil(), quantity);
        add(totals, "eur", price.eur(), quantity);
        add(totals, "tix", price.tix(), quantity);
    }

    private static void add(
            Map<String, BigDecimal> totals,
            String currency,
            @Nullable BigDecimal amount,
            int quantity) {
        if (amount != null) {
            totals.merge(currency, amount.multiply(BigDecimal.valueOf(quantity)), BigDecimal::add);
        }
    }

    private static boolean isEmpty(CardPrice price) {
        return price.usd() == null
                && price.usdFoil() == null
                && price.eur() == null
                && price.tix() == null;
    }

    public record DeckValue(
            Map<String, BigDecimal> valueByCurrency,
            Map<String, BigDecimal> missingCostByCurrency,
            int unpricedCount) {}
}
