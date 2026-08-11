package com.deckassemble.collections.application.trading;

import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.trading.TradeMatchService.TradeMatchItemView;
import com.deckassemble.collections.application.trading.TradeMatchService.ValueDeltaView;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
class TradeValueDeltaCalculator {

    private final CardPriceService priceService;

    TradeValueDeltaCalculator(CardPriceService priceService) {
        this.priceService = priceService;
    }

    ValueTotals totals(long leftListId, List<TradeMatchItemView> matches) {
        var prices = priceService.latestPrices(printingIds(matches));
        Map<String, DirectionalTotals> totals = new ConcurrentHashMap<>();
        int unpriced = 0;
        for (TradeMatchItemView match : matches) {
            CardPrice price = prices.get(match.matchedCollectionCardPrintingId());
            if (price == null || isEmpty(price)) {
                unpriced += match.quantity();
            } else {
                addPrice(leftListId, totals, match, price);
            }
        }
        return new ValueTotals(valueViews(totals), unpriced);
    }

    private void addPrice(
            long leftListId,
            Map<String, DirectionalTotals> totals,
            TradeMatchItemView match,
            CardPrice price) {
        add(leftListId, totals, "usd", price.usd(), match);
        add(leftListId, totals, "usdFoil", price.usdFoil(), match);
        add(leftListId, totals, "eur", price.eur(), match);
        add(leftListId, totals, "tix", price.tix(), match);
    }

    private void add(
            long leftListId,
            Map<String, DirectionalTotals> totals,
            String currency,
            @Nullable BigDecimal amount,
            TradeMatchItemView match) {
        if (amount == null) {
            return;
        }
        BigDecimal total = amount.multiply(BigDecimal.valueOf(match.quantity()));
        totals.computeIfAbsent(currency, ignored -> new DirectionalTotals())
                .add(leftListId, match, total);
    }

    private List<ValueDeltaView> valueViews(Map<String, DirectionalTotals> totals) {
        return totals.entrySet().stream()
                .map(
                        entry ->
                                new ValueDeltaView(
                                        entry.getKey(),
                                        entry.getValue().leftToRight(),
                                        entry.getValue().rightToLeft()))
                .toList();
    }

    private Collection<Long> printingIds(List<TradeMatchItemView> matches) {
        return matches.stream()
                .map(TradeMatchItemView::matchedCollectionCardPrintingId)
                .distinct()
                .toList();
    }

    private boolean isEmpty(CardPrice price) {
        return price.usd() == null
                && price.usdFoil() == null
                && price.eur() == null
                && price.tix() == null;
    }

    record ValueTotals(List<ValueDeltaView> deltas, int unpriced) {}

    private static final class DirectionalTotals {
        private BigDecimal leftToRight = BigDecimal.ZERO;
        private BigDecimal rightToLeft = BigDecimal.ZERO;

        void add(long leftListId, TradeMatchItemView match, BigDecimal amount) {
            if (match.fromListId().equals(leftListId)) {
                leftToRight = leftToRight.add(amount);
            } else {
                rightToLeft = rightToLeft.add(amount);
            }
        }

        BigDecimal leftToRight() {
            return leftToRight;
        }

        BigDecimal rightToLeft() {
            return rightToLeft;
        }
    }
}
