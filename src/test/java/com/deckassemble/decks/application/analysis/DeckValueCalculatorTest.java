package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.cards.domain.CardPrice;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeckValueCalculatorTest {

    @Test
    void shouldReturnZeroValueForEmptyDeck() {
        // Given an empty deck
        // When
        DeckValueCalculator.DeckValue value = DeckValueCalculator.value(List.of(), Map.of());

        // Then
        assertThat(value.valueByCurrency()).isEmpty();
        assertThat(value.missingCostByCurrency()).isEmpty();
        assertThat(value.unpricedCount()).isZero();
    }

    @Test
    void shouldSumValueByCurrencyWithQuantities() {
        // Given three copies of a card priced in usd and eur
        List<AnalysisEntry> entries = List.of(entry(1L, 3, "OWNED"));
        Map<Long, CardPrice> prices =
                Map.of(1L, new CardPrice(usd("2.00"), null, usd("1.50"), null));

        // When
        DeckValueCalculator.DeckValue value = DeckValueCalculator.value(entries, prices);

        // Then
        assertThat(value.valueByCurrency())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("usd", usd("6.00"), "eur", usd("4.50")));
        assertThat(value.unpricedCount()).isZero();
    }

    @Test
    void shouldTolerateMissingPrices() {
        // Given cards without any price snapshot
        List<AnalysisEntry> entries = List.of(entry(1L, 2, "OWNED"), entry(2L, 1, "OWNED"));
        Map<Long, CardPrice> prices = Map.of(2L, new CardPrice(usd("1.00"), null, null, null));

        // When
        DeckValueCalculator.DeckValue value = DeckValueCalculator.value(entries, prices);

        // Then unpriced cards are counted but excluded from totals
        assertThat(value.valueByCurrency()).containsExactly(Map.entry("usd", usd("1.00")));
        assertThat(value.unpricedCount()).isEqualTo(2);
    }

    @Test
    void shouldCountOnlyNonOwnedCardsTowardMissingCost() {
        // Given an owned card, a wishlist card, and a proxy, all priced
        List<AnalysisEntry> entries =
                List.of(
                        entry(1L, 1, "OWNED"),
                        entry(2L, 2, "WISHLIST"),
                        entry(3L, 1, "PROXY"));
        Map<Long, CardPrice> prices =
                Map.of(
                        1L, new CardPrice(usd("10.00"), null, null, null),
                        2L, new CardPrice(usd("5.00"), null, null, null),
                        3L, new CardPrice(usd("3.00"), null, null, null));

        // When
        DeckValueCalculator.DeckValue value = DeckValueCalculator.value(entries, prices);

        // Then total covers everything; missing cost covers only wishlist and proxy
        assertThat(value.valueByCurrency()).containsExactly(Map.entry("usd", usd("23.00")));
        assertThat(value.missingCostByCurrency())
                .containsExactly(Map.entry("usd", usd("13.00")));
    }

    private static AnalysisEntry entry(Long printingId, int quantity, String ownership) {
        return new AnalysisEntry(
                printingId,
                quantity,
                ownership,
                new CardAnalysisView(
                        printingId, "Card", null, BigDecimal.ZERO, "Instant", null, false,
                        List.of()));
    }

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }
}
