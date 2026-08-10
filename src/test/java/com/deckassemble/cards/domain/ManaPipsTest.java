package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ManaPipsTest {

    @Test
    void shouldCountColoredPipsAndIgnoreGeneric() {
        // Given a cost with generic and double-blue
        ManaPips pips = ManaPips.fromManaCost("{2}{U}{U}");

        // Then only colored pips are counted
        assertThat(pips.u()).isEqualTo(2);
        assertThat(pips.total()).isEqualTo(2);
    }

    @Test
    void shouldCountHybridIntoBothColors() {
        // Given a hybrid pip
        ManaPips pips = ManaPips.fromManaCost("{W/U}{G}");

        // Then it contributes to both colors
        assertThat(pips.w()).isEqualTo(1);
        assertThat(pips.u()).isEqualTo(1);
        assertThat(pips.g()).isEqualTo(1);
    }

    @Test
    void shouldCountPhyrexianAsItsColor() {
        assertThat(ManaPips.fromManaCost("{W/P}").w()).isEqualTo(1);
    }

    @Test
    void shouldCountAcrossDoubleFacedCosts() {
        // Given an MDFC cost string
        ManaPips pips = ManaPips.fromManaCost("{1}{R} // {2}{G}");

        // Then both faces contribute
        assertThat(pips.r()).isEqualTo(1);
        assertThat(pips.g()).isEqualTo(1);
    }

    @Test
    void shouldIgnoreXAndColorless() {
        assertThat(ManaPips.fromManaCost("{X}{B}{C}").total()).isEqualTo(1);
    }

    @Test
    void shouldReturnZeroForNullOrEmptyCost() {
        assertThat(ManaPips.fromManaCost(null).total()).isZero();
        assertThat(ManaPips.fromManaCost("").total()).isZero();
    }
}
