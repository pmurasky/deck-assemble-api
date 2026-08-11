package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.Card;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManaBaseCheckTest {

    @Test
    void shouldReportNoShortfallWhenSourcesMeetRequirement() {
        // Given
        var deck = new ArrayList<Card>();
        deck.addAll(lands("({T}: Add {U}.)", 20));
        deck.add(spell("{1}{U}", 2));

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.requiredSources()).isEqualTo(Map.of("U", 17));
        assertThat(check.actualSources()).isEqualTo(Map.of("U", 20));
        assertThat(check.shortfalls()).isEmpty();
    }

    @Test
    void shouldReportShortfallWhenSourcesAreInsufficient() {
        // Given
        var deck = new ArrayList<Card>();
        deck.addAll(lands("({T}: Add {U}.)", 10));
        deck.add(spell("{U}{U}", 2));

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.shortfalls()).isEqualTo(Map.of("U", 15));
    }

    @Test
    void shouldCountAnyColorLandForEveryColor() {
        // Given
        var deck = lands("({T}: Add one mana of any color.)", 1);

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.actualSources()).isEqualTo(Map.of("W", 1, "U", 1, "B", 1, "R", 1, "G", 1));
    }

    @Test
    void shouldIgnoreColorlessSpells() {
        // Given
        var deck = List.of(spell("{3}", 3));

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.requiredSources()).isEmpty();
    }

    @Test
    void shouldIgnoreLandsWithoutAddClause() {
        // Given
        var deck = lands("{T}, Sacrifice this land: Search your library for a basic land card.", 1);

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.actualSources()).isEmpty();
    }

    @Test
    void shouldTakeHardestCardAsRequirementPerColor() {
        // Given
        var deck = new ArrayList<Card>();
        deck.add(spell("{W}{W}", 2));
        deck.add(spell("{4}{W}", 5));

        // When
        var check = ManaBaseCheck.evaluate(deck);

        // Then
        assertThat(check.requiredSources()).isEqualTo(Map.of("W", 25));
    }

    private static List<Card> lands(String oracleText, int count) {
        var lands = new ArrayList<Card>();
        for (var i = 0; i < count; i++) {
            var land = new Card("oracle-land", "Test Land");
            land.setTypeLine("Basic Land");
            land.setOracleText(oracleText);
            lands.add(land);
        }
        return lands;
    }

    private static Card spell(String manaCost, int manaValue) {
        var spell = new Card("oracle-spell", "Test Spell");
        spell.setTypeLine("Instant");
        spell.setManaCost(manaCost);
        spell.setManaValue(BigDecimal.valueOf(manaValue));
        return spell;
    }
}
