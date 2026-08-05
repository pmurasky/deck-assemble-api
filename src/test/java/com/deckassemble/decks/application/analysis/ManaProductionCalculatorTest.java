package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManaProductionCalculatorTest {

    @Test
    void shouldReturnZeroForEmptyDeck() {
        // Given an empty deck
        // When / Then
        assertThat(ManaProductionCalculator.production(List.of())).isEmpty();
        assertThat(ManaProductionCalculator.landCount(List.of())).isZero();
    }

    @Test
    void shouldCountLandsWithQuantities() {
        // Given forests and islands
        List<AnalysisEntry> entries =
                List.of(
                        entry(4, card("Forest", "Basic Land — Forest", "{T}: Add {G}.")),
                        entry(2, card("Island", "Basic Land — Island", "{T}: Add {U}.")));

        // When / Then
        assertThat(ManaProductionCalculator.landCount(entries)).isEqualTo(6);
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("G", 4, "U", 2));
    }

    @Test
    void shouldParseMultipleColorsFromOneCard() {
        // Given a dual land producing two colors
        List<AnalysisEntry> entries =
                List.of(
                        entry(
                                1,
                                card(
                                        "Breeding Pool",
                                        "Land — Forest Island",
                                        "{T}: Add {G} or {U}.")));

        // When / Then each color counts once per source
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("G", 1, "U", 1));
    }

    @Test
    void shouldTreatAnyColorAsAllFiveColors() {
        // Given a command tower
        List<AnalysisEntry> entries =
                List.of(
                        entry(
                                1,
                                card(
                                        "Command Tower",
                                        "Land",
                                        "{T}: Add one mana of any color in your commander's color identity.")));

        // When / Then
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of("W", 1, "U", 1, "B", 1, "R", 1, "G", 1));
    }

    @Test
    void shouldCountProductionFromSplitCardFaces() {
        // Given a split card whose halves produce different colors
        CardAnalysisView split =
                new CardAnalysisView(
                        10L,
                        "Spring // Mind",
                        null,
                        new BigDecimal("6"),
                        "Sorcery // Sorcery",
                        "GU",
                        false,
                        List.of(
                                new CardAnalysisView.Face(
                                        "{4}{G}{G}", "Sorcery", "Search for lands. Add {G}."),
                                new CardAnalysisView.Face(
                                        "{2}{U}", "Sorcery", "{T}: Add {U}. Draw.")));

        // When / Then
        assertThat(ManaProductionCalculator.production(List.of(entry(1, split))))
                .containsExactlyInAnyOrderEntriesOf(java.util.Map.of("G", 1, "U", 1));
    }

    @Test
    void shouldCountColorlessProduction() {
        // Given a sol ring producing colorless mana
        List<AnalysisEntry> entries =
                List.of(entry(1, card("Sol Ring", "Artifact", "{T}: Add {C}{C}.")));

        // When / Then
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactly(java.util.Map.entry("C", 1));
    }

    @Test
    void shouldTreatAnyOneColorAsAllFiveColors() {
        // Given a card that adds one mana of any one color
        List<AnalysisEntry> entries =
                List.of(
                        entry(
                                1,
                                card(
                                        "Birds of Paradise",
                                        "Creature — Bird",
                                        "{T}: Add one mana of any one color.")));

        // When / Then
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of("W", 1, "U", 1, "B", 1, "R", 1, "G", 1));
    }

    @Test
    void shouldIgnoreGenericSymbolsInAddClauses() {
        // Given a card adding generic and colored mana
        List<AnalysisEntry> entries =
                List.of(entry(1, card("Mana Bloom", "Artifact", "{T}: Add {2}{G}.")));

        // When / Then only the colored symbol counts
        assertThat(ManaProductionCalculator.production(entries))
                .containsExactly(java.util.Map.entry("G", 1));
    }

    private static AnalysisEntry entry(int quantity, CardAnalysisView card) {
        return new AnalysisEntry(1L, quantity, "OWNED", card);
    }

    private static CardAnalysisView card(String name, String typeLine, String oracleText) {
        return new CardAnalysisView(
                1L,
                name,
                null,
                BigDecimal.ZERO,
                typeLine,
                null,
                false,
                List.of(new CardAnalysisView.Face(null, typeLine, oracleText)));
    }
}
