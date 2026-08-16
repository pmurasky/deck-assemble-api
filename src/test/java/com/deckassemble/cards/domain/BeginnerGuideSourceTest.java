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

    @Test
    void shouldCreateSourceFromCardFacesInOrder() {
        var card = new Card("oracle-id", "Spider-Man");
        card.getFaces().add(face(card, 0, "Front text"));
        card.getFaces().add(face(card, 1, "Back text"));

        var source = BeginnerGuideSource.fromCard(card, List.of("Ruling one"));

        assertThat(source)
                .isEqualTo(
                        new BeginnerGuideSource(
                                "Spider-Man",
                                List.of("Front text", "Back text"),
                                List.of("Ruling one")));
    }

    @Test
    void shouldCreateSourceFromCardOracleTextWhenFacesAreAbsent() {
        var card = new Card("oracle-id", "Spider-Man");
        card.setOracleText("Oracle text");

        var source = BeginnerGuideSource.fromCard(card, List.of());

        assertThat(source.oracleTexts()).containsExactly("Oracle text");
    }

    private static CardFace face(Card card, int order, String oracleText) {
        var face = new CardFace(card, order, "Face");
        face.setOracleText(oracleText);
        return face;
    }
}
