package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CardFunctionalCategoryTest {

    @Test
    void shouldAssignMultipleCategoriesToAFlexibleCard() {
        // Given a card that both draws and removes
        String text = "draw a card. destroy target creature.";

        // When categorizing with overlap allowed
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll("", text, null);

        // Then both roles are recognized
        assertThat(categories)
                .containsExactlyInAnyOrder(
                        CardFunctionalCategory.DRAW, CardFunctionalCategory.REMOVAL);
    }

    @Test
    void shouldDeriveCategoriesFromOracleTags() {
        // Given tagger labels on an otherwise neutral card
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll("", "", "removal,protects-creature");

        // Then tags drive the classification
        assertThat(categories)
                .containsExactlyInAnyOrder(
                        CardFunctionalCategory.REMOVAL, CardFunctionalCategory.PROTECTION);
    }

    @Test
    void shouldMapSweeperTagsToWipe() {
        assertThat(CardFunctionalCategory.categorizeAll("", "", "sweeper"))
                .containsExactly(CardFunctionalCategory.WIPE);
        assertThat(CardFunctionalCategory.categorizeAll("", "", "board-reset"))
                .containsExactly(CardFunctionalCategory.WIPE);
    }

    @Test
    void shouldMapCounterspellToInteraction() {
        // Given a counterspell tag
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll("", "", "counterspell");

        // Then it counts toward the interaction package
        assertThat(categories).containsExactly(CardFunctionalCategory.REMOVAL);
    }

    @Test
    void shouldDetectFinisherFromWinText() {
        // Given an explicit win condition in the rules text
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll(
                        "creature", "at the beginning of your upkeep, you win the game.", null);

        // Then the card is a finisher
        assertThat(categories).containsExactly(CardFunctionalCategory.FINISHER);
    }

    @Test
    void shouldIgnoreMechanicalCycleTags() {
        // Given a cycle tag that mentions protection but is not a protection effect
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll("", "", "cycle-lea-circle-protection");

        // Then it falls back to synergy
        assertThat(categories).containsExactly(CardFunctionalCategory.SYNERGY);
    }

    @Test
    void shouldFallBackToSynergyWhenNothingMatches() {
        assertThat(CardFunctionalCategory.categorizeAll("creature", "", null))
                .containsExactly(CardFunctionalCategory.SYNERGY);
    }

    @Test
    void shouldKeepSingleLabelPriorityForLegacyCallers() {
        // Given a card matching both draw and removal markers
        String text = "draw a card. destroy target creature.";

        // When categorizing through the single-label contract
        CardFunctionalCategory category = CardFunctionalCategory.categorize("", text);

        // Then the legacy priority order is preserved
        assertThat(category).isEqualTo(CardFunctionalCategory.DRAW);
    }

    @Test
    void shouldClassifyLandFromTypeLine() {
        assertThat(CardFunctionalCategory.categorizeAll("land", "", null))
                .containsExactly(CardFunctionalCategory.LAND);
    }

    @Test
    void shouldReturnEnumSetForDeterministicIteration() {
        // Given a multi-label card
        Set<CardFunctionalCategory> categories =
                CardFunctionalCategory.categorizeAll("", "draw a card. destroy target x.", null);

        // Then callers can iterate in enum order
        assertThat(categories).isInstanceOf(EnumSet.class);
    }
}
