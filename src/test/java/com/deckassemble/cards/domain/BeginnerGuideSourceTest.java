package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BeginnerGuideSourceTest {

    @Test
    void shouldHashEveryOracleTextInOrder() {
        var source =
                new BeginnerGuideSource(
                        "Spider-Man", List.of("Front text", "Back text"), List.of("Ruling one"));

        assertThat(source.oracleHash())
                .isEqualTo("318dac17bb0e276c0a13330a01c3f8a115cfcd187831daf03d48dd77f577bb0c");
    }
}
