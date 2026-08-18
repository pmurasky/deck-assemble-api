package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommanderBracketCalculatorTest {

    @Test
    void shouldScoreBracketOneForDeckWithoutGameChangersOrCombos() {
        // Given
        List<AnalysisEntry> entries = List.of(entry(1L, "Llanowar Elves", false));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 0);

        // Then
        assertThat(bracket.level()).isEqualTo(1);
        assertThat(bracket.flaggedCards()).isEmpty();
    }

    @Test
    void shouldScoreBracketTwoForDeckWithCombosButNoGameChangers() {
        // Given
        List<AnalysisEntry> entries = List.of(entry(1L, "Llanowar Elves", false));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 2);

        // Then
        assertThat(bracket.level()).isEqualTo(2);
        assertThat(bracket.flaggedCards()).isEmpty();
    }

    @Test
    void shouldScoreBracketThreeForDeckWithAFewGameChangers() {
        // Given
        List<AnalysisEntry> entries =
                List.of(
                        entry(1L, "Rhystic Study", true),
                        entry(2L, "Cyclonic Rift", true),
                        entry(3L, "Llanowar Elves", false));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 0);

        // Then
        assertThat(bracket.level()).isEqualTo(3);
        assertThat(bracket.flaggedCards()).containsExactly("Cyclonic Rift", "Rhystic Study");
    }

    @Test
    void shouldScoreBracketFourForDeckWithSeveralGameChangers() {
        // Given
        List<AnalysisEntry> entries =
                List.of(
                        entry(1L, "Rhystic Study", true),
                        entry(2L, "Cyclonic Rift", true),
                        entry(3L, "Mana Crypt", true),
                        entry(4L, "Smothering Tithe", true));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 0);

        // Then
        assertThat(bracket.level()).isEqualTo(4);
        assertThat(bracket.flaggedCards()).hasSize(4);
    }

    @Test
    void shouldScoreBracketFourForComboHeavyDeckWithoutGameChangers() {
        // Given
        List<AnalysisEntry> entries = List.of(entry(1L, "Llanowar Elves", false));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 3);

        // Then
        assertThat(bracket.level()).isEqualTo(4);
        assertThat(bracket.flaggedCards()).isEmpty();
    }

    @Test
    void shouldScoreBracketFiveForDeckPackedWithGameChangers() {
        // Given
        List<AnalysisEntry> entries =
                List.of(
                        entry(1L, "Rhystic Study", true),
                        entry(2L, "Cyclonic Rift", true),
                        entry(3L, "Mana Crypt", true),
                        entry(4L, "Smothering Tithe", true),
                        entry(5L, "Thassa's Oracle", true),
                        entry(6L, "Demonic Tutor", true),
                        entry(7L, "Fierce Guardianship", true));

        // When
        CommanderBracket bracket = CommanderBracketCalculator.bracket(entries, 0);

        // Then
        assertThat(bracket.level()).isEqualTo(5);
        assertThat(bracket.flaggedCards()).hasSize(7);
    }

    private static AnalysisEntry entry(Long printingId, String name, boolean gameChanger) {
        return new AnalysisEntry(
                null,
                printingId,
                1,
                "OWNED",
                new CardAnalysisView(
                        printingId, name, null, null, "Creature", null, gameChanger, List.of()));
    }
}
