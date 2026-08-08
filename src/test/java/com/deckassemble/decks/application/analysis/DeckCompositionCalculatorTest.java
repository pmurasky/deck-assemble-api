package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.recommendations.application.CardCategorizer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DeckCompositionCalculatorTest {

    @Test
    void shouldReturnEmptyCompositionForEmptyDeck() {
        // Given an empty deck
        // When / Then
        assertThat(DeckCompositionCalculator.typeDistribution(List.of())).isEmpty();
        assertThat(DeckCompositionCalculator.functionalCategories(List.of(), Map.of())).isEmpty();
        assertThat(DeckCompositionCalculator.tokenProducers(List.of())).isEmpty();
        assertThat(DeckCompositionCalculator.gameChangers(List.of())).isEmpty();
    }

    @Test
    void shouldDistributeTypesWithQuantities() {
        // Given cards covering several types
        List<AnalysisEntry> entries =
                List.of(
                        entry(2, card("Bear", "Creature — Bear", "")),
                        entry(1, card("Bolt", "Instant", "")),
                        entry(1, card("Golem", "Artifact Creature — Golem", "")),
                        entry(3, card("Forest", "Basic Land — Forest", "")));

        // When / Then multi-type cards count in each matching bucket
        assertThat(DeckCompositionCalculator.typeDistribution(entries))
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of("CREATURE", 3, "INSTANT", 1, "ARTIFACT", 1, "LAND", 3));
    }

    @Test
    void shouldCategorizeFunctionalRoles() {
        // Given a ramp spell, a draw spell, a wipe, removal, a land, and a synergy piece
        List<AnalysisEntry> entries =
                List.of(
                        entry(
                                1,
                                card(
                                        "Rampant Growth",
                                        "Sorcery",
                                        "Search your library for a basic land card.")),
                        entry(2, card("Opt", "Instant", "Draw a card.")),
                        entry(1, card("Wrath", "Sorcery", "Destroy all creatures.")),
                        entry(1, card("Swords", "Instant", "Exile target creature.")),
                        entry(4, card("Forest", "Basic Land — Forest", "{T}: Add {G}.")),
                        entry(1, card("Bear", "Creature — Bear", "")));

        // When / Then
        assertThat(DeckCompositionCalculator.functionalCategories(entries, Map.of()))
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of(
                                "RAMP", 1, "DRAW", 2, "WIPE", 1, "REMOVAL", 1, "LAND", 4, "SYNERGY",
                                1));
    }

    @Test
    void shouldPreferExplicitCategoryAssignmentOverInferredPresentationCategory() {
        // Given a synergy piece the user has explicitly filed under a custom category, and an
        // unassigned removal spell left to the inferred bucket
        AnalysisEntry assigned = entry(20L, 1, card("Bear", "Creature — Bear", ""));
        AnalysisEntry unassigned =
                entry(21L, 1, card("Swords", "Instant", "Exile target creature."));
        Map<Long, String> explicitCategoryNames = Map.of(20L, "Combo Pieces");

        // When
        Map<String, Integer> categories =
                DeckCompositionCalculator.functionalCategories(
                        List.of(assigned, unassigned), explicitCategoryNames);

        // Then the assigned card shows the user's category and the rest fall back to inferred
        assertThat(categories)
                .containsExactlyInAnyOrderEntriesOf(Map.of("Combo Pieces", 1, "REMOVAL", 1));
        // And the canonical categorizer itself never sees the override: raw classification of
        // the same card is still SYNERGY, unaffected by the user's presentation choice.
        assertThat(
                        CardCategorizer.categorizeText(
                                assigned.allTypeLines(), assigned.allOracleText()))
                .isEqualTo(CardCategorizer.Category.SYNERGY);
    }

    @Test
    void shouldFallBackToInferredCategoryForCardsWithNoDeckCardId() {
        // Given a synthesized commander row (no persisted DeckCard, so no deckCardId) that can
        // never have an explicit assignment, alongside a non-empty override map for other cards
        AnalysisEntry synthesizedCommander =
                new AnalysisEntry(null, 1L, 1, "OWNED", card("Bear", "Creature — Bear", ""));
        Map<Long, String> explicitCategoryNames = Map.of(20L, "Combo Pieces");

        // When / Then the lookup tolerates the missing id instead of throwing
        assertThat(
                        DeckCompositionCalculator.functionalCategories(
                                List.of(synthesizedCommander), explicitCategoryNames))
                .containsExactly(Map.entry("SYNERGY", 1));
    }

    @Test
    void shouldListTokenProducers() {
        // Given a token creator and a plain creature
        List<AnalysisEntry> entries =
                List.of(
                        entry(
                                1,
                                card(
                                        "Raise the Alarm",
                                        "Instant",
                                        "Create two 1/1 white Soldier creature tokens.")),
                        entry(1, card("Bear", "Creature — Bear", "")));

        // When / Then
        assertThat(DeckCompositionCalculator.tokenProducers(entries))
                .containsExactly("Raise the Alarm");
    }

    @Test
    void shouldListGameChangers() {
        // Given a game changer and a regular card
        List<AnalysisEntry> entries =
                List.of(
                        entry(1, gameChanger("Sol Ring")),
                        entry(1, card("Bear", "Creature — Bear", "")));

        // When / Then
        assertThat(DeckCompositionCalculator.gameChangers(entries)).containsExactly("Sol Ring");
    }

    @Test
    void shouldBucketUnrecognizedTypesAsOther() {
        // Given a card whose type matches no standard bucket
        List<AnalysisEntry> entries = List.of(entry(1, card("Conspiracy", "Conspiracy", "")));

        // When / Then
        assertThat(DeckCompositionCalculator.typeDistribution(entries))
                .containsExactly(java.util.Map.entry("OTHER", 1));
    }

    @Test
    void shouldNotListCreatorsThatMakeNoTokens() {
        // Given a card whose oracle text creates something other than a token
        List<AnalysisEntry> entries =
                List.of(entry(1, card("Clone Crafter", "Creature — Wizard", "Create a copy.")));

        // When / Then
        assertThat(DeckCompositionCalculator.tokenProducers(entries)).isEmpty();
    }

    private static AnalysisEntry entry(int quantity, CardAnalysisView card) {
        return entry(1L, quantity, card);
    }

    private static AnalysisEntry entry(long deckCardId, int quantity, CardAnalysisView card) {
        return new AnalysisEntry(deckCardId, 1L, quantity, "OWNED", card);
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

    private static CardAnalysisView gameChanger(String name) {
        return new CardAnalysisView(
                1L, name, null, BigDecimal.ZERO, "Artifact", null, true, List.of());
    }
}
