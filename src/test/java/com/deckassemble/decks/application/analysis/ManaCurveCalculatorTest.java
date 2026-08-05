package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManaCurveCalculatorTest {

    @Test
    void shouldReturnEmptyCurveForEmptyDeck() {
        // Given an empty deck
        List<AnalysisEntry> entries = List.of();

        // When / Then
        assertThat(ManaCurveCalculator.curve(entries)).isEmpty();
        assertThat(ManaCurveCalculator.colorDemand(entries)).isEmpty();
        assertThat(ManaCurveCalculator.averageManaValue(entries)).isEqualTo(0.0);
    }

    @Test
    void shouldBucketCurveByManaValueExcludingLands() {
        // Given non-land spells and a land
        List<AnalysisEntry> entries =
                List.of(
                        entry(1, card("Bolt", "{R}", "1", "Instant", "")),
                        entry(1, card("Divination", "{2}{U}", "3", "Sorcery", "")),
                        entry(4, card("Forest", null, "0", "Basic Land — Forest", "")));

        // When
        Map<String, Integer> curve = ManaCurveCalculator.curve(entries);

        // Then lands are excluded and quantities weight the buckets
        assertThat(curve).containsExactly(Map.entry("1", 1), Map.entry("3", 1));
    }

    @Test
    void shouldCountXCostAsItsPrintedManaValue() {
        // Given a card with an X cost ({X}{U}{U} has mana value 2)
        List<AnalysisEntry> entries =
                List.of(entry(1, card("Blue Sun's Zenith", "{X}{U}{U}", "2", "Instant", "")));

        // When / Then X contributes nothing beyond the printed mana value
        assertThat(ManaCurveCalculator.curve(entries)).containsExactly(Map.entry("2", 1));
        assertThat(ManaCurveCalculator.colorDemand(entries))
                .containsExactly(Map.entry("U", 2));
    }

    @Test
    void shouldGroupHighManaValuesIntoSevenPlusBucket() {
        // Given cards with mana values 7 and 10
        List<AnalysisEntry> entries =
                List.of(
                        entry(1, card("Big One", "{7}", "7", "Sorcery", "")),
                        entry(1, card("Bigger One", "{10}", "10", "Sorcery", "")));

        // When / Then
        assertThat(ManaCurveCalculator.curve(entries)).containsExactly(Map.entry("7+", 2));
    }

    @Test
    void shouldWeightCurveByQuantity() {
        // Given three copies of a two-mana spell
        List<AnalysisEntry> entries =
                List.of(entry(3, card("Counterspell", "{U}{U}", "2", "Instant", "")));

        // When / Then
        assertThat(ManaCurveCalculator.curve(entries)).containsExactly(Map.entry("2", 3));
    }

    @Test
    void shouldDemandColorsFromBothFacesOfSplitCards() {
        // Given a split card with red and blue halves
        CardAnalysisView split =
                new CardAnalysisView(
                        10L,
                        "Fire // Ice",
                        null,
                        new BigDecimal("4"),
                        "Instant // Instant",
                        "UR",
                        false,
                        List.of(
                                new CardAnalysisView.Face("{1}{R}", "Instant", "Deal 2 damage."),
                                new CardAnalysisView.Face("{1}{U}", "Instant", "Tap and draw.")));

        // When / Then both halves contribute pips, weighted by quantity
        assertThat(ManaCurveCalculator.colorDemand(List.of(entry(2, split))))
                .containsExactly(Map.entry("R", 2), Map.entry("U", 2));
    }

    @Test
    void shouldComputeAverageManaValueExcludingLands() {
        // Given two spells and a land
        List<AnalysisEntry> entries =
                List.of(
                        entry(1, card("Bolt", "{R}", "1", "Instant", "")),
                        entry(1, card("Divination", "{2}{U}", "3", "Sorcery", "")),
                        entry(4, card("Forest", null, "0", "Basic Land — Forest", "")));

        // When / Then lands do not drag the average down
        assertThat(ManaCurveCalculator.averageManaValue(entries)).isEqualTo(2.0);
    }

    private static AnalysisEntry entry(int quantity, CardAnalysisView card) {
        return new AnalysisEntry(1L, quantity, "OWNED", card);
    }

    private static CardAnalysisView card(
            String name, String manaCost, String manaValue, String typeLine, String oracleText) {
        return new CardAnalysisView(
                1L,
                name,
                manaCost,
                new BigDecimal(manaValue),
                typeLine,
                null,
                false,
                List.of(new CardAnalysisView.Face(manaCost, typeLine, oracleText)));
    }
}
