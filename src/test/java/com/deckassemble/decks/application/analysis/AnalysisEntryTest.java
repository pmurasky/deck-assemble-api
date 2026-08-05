package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.CardAnalysisView;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisEntryTest {

    @Test
    void shouldFallBackToCardLevelManaCostWhenFacesHaveNone() {
        // Given a card with no face mana costs
        AnalysisEntry entry =
                new AnalysisEntry(
                        1L,
                        1,
                        "OWNED",
                        new CardAnalysisView(
                                1L,
                                "Bolt",
                                "{R}",
                                BigDecimal.ONE,
                                "Instant",
                                null,
                                false,
                                List.of(new CardAnalysisView.Face(null, "Instant", ""))));

        // When / Then the card-level cost is used
        assertThat(entry.manaCosts()).containsExactly("{R}");
    }

    @Test
    void shouldFallBackToFaceTypeLineWhenCardLevelMissing() {
        // Given a card with no card-level type line
        AnalysisEntry entry =
                new AnalysisEntry(
                        1L,
                        1,
                        "OWNED",
                        new CardAnalysisView(
                                1L,
                                "Split",
                                null,
                                BigDecimal.ONE,
                                null,
                                null,
                                false,
                                List.of(new CardAnalysisView.Face("{1}", "Instant", ""))));

        // When / Then the first face type line is used
        assertThat(entry.primaryTypeLine()).isEqualTo("Instant");
    }

    @Test
    void shouldSkipFacesWithoutTypeLineWhenResolvingPrimaryType() {
        // Given a card whose first face lacks a type line
        AnalysisEntry entry =
                new AnalysisEntry(
                        1L,
                        1,
                        "OWNED",
                        new CardAnalysisView(
                                1L,
                                "Split",
                                null,
                                BigDecimal.ONE,
                                null,
                                null,
                                false,
                                List.of(
                                        new CardAnalysisView.Face(null, null, null),
                                        new CardAnalysisView.Face(null, "Sorcery", ""))));

        // When / Then the first face with a type line wins
        assertThat(entry.primaryTypeLine()).isEqualTo("Sorcery");
    }

    @Test
    void shouldReturnEmptyTypeLineWhenNoTypeKnown() {
        // Given a card with no type information at all
        AnalysisEntry entry =
                new AnalysisEntry(
                        1L,
                        1,
                        "OWNED",
                        new CardAnalysisView(
                                1L, "Blank", null, null, null, null, false, List.of()));

        // When / Then
        assertThat(entry.primaryTypeLine()).isEmpty();
        assertThat(entry.manaCosts()).isEmpty();
    }
}
