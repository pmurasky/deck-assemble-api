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
        assertThat(ManaCurveCalculator.colorDemand(entries)).containsExactly(Map.entry("U", 2));
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

    @Test
    void shouldTreatNullManaValueAsZero() {
        // Given a non-land card without a recorded mana value
        CardAnalysisView noValue =
                new CardAnalysisView(
                        1L,
                        "Ancestral Recall",
                        "{U}",
                        null,
                        "Instant",
                        null,
                        false,
                        List.of(new CardAnalysisView.Face("{U}", "Instant", "Draw three cards.")));
        List<AnalysisEntry> entries = List.of(entry(1, noValue));

        // When / Then it lands in the zero bucket and does not skew the average
        assertThat(ManaCurveCalculator.curve(entries)).containsExactly(Map.entry("0", 1));
        assertThat(ManaCurveCalculator.averageManaValue(entries)).isEqualTo(0.0);
    }

    @Test
    void shouldRecommendZeroLandsForEmptyDeck() {
        // Given an empty deck
        // When / Then
        assertThat(ManaCurveCalculator.recommendedLandCount(List.of())).isZero();
    }

    @Test
    void shouldRecommendBaselineRatioForLowCurve() {
        // Given a 100-card deck with a low curve (average mana value 2.0)
        List<AnalysisEntry> entries =
                List.of(
                        entry(64, card("Bear", "{1}{G}", "2", "Creature — Bear", "")),
                        entry(36, card("Forest", null, "0", "Basic Land — Forest", "")));

        // When / Then the baseline 35% ratio applies
        assertThat(ManaCurveCalculator.recommendedLandCount(entries)).isEqualTo(35);
    }

    @Test
    void shouldRecommendMoreLandsForHighCurve() {
        // Given a 100-card deck with a steep curve (average mana value 4.0)
        List<AnalysisEntry> entries =
                List.of(
                        entry(60, card("Dragon", "{2}{R}{R}", "4", "Creature — Dragon", "")),
                        entry(40, card("Mountain", null, "0", "Basic Land — Mountain", "")));

        // When / Then the ratio rises above the baseline
        assertThat(ManaCurveCalculator.recommendedLandCount(entries)).isEqualTo(41);
    }

    @Test
    void shouldCapRecommendationForExtremeCurve() {
        // Given a 100-card deck of only haymakers (average mana value far above baseline)
        List<AnalysisEntry> entries =
                List.of(
                        entry(63, card("Big One", "{10}", "10", "Sorcery", "")),
                        entry(37, card("Forest", null, "0", "Basic Land — Forest", "")));

        // When / Then the recommendation is capped at 45% of deck size
        assertThat(ManaCurveCalculator.recommendedLandCount(entries)).isEqualTo(45);
    }

    private static AnalysisEntry entry(int quantity, CardAnalysisView card) {
        return new AnalysisEntry(1L, 1L, quantity, "OWNED", card);
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
