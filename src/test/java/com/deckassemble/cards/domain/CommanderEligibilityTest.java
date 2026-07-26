package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommanderEligibilityTest {

    @Test
    void shouldBeEligibleForLegendaryCreature() {
        Card card = cardWithFace("Legendary Creature - Human Wizard", null);

        assertThat(CommanderEligibility.isEligible(card)).isTrue();
    }

    @Test
    void shouldBeEligibleForCanBeYourCommanderText() {
        Card card = cardWithFace("Legendary Planeswalker", "Ajani can be your commander.");

        assertThat(CommanderEligibility.isEligible(card)).isTrue();
    }

    @Test
    void shouldBeEligibleWhenSecondFaceIsLegendaryCreature() {
        Card card = new Card("oracle-1", "DFC");
        card.getFaces().add(face(card, 0, "Enchantment", null));
        card.getFaces().add(face(card, 1, "Legendary Creature - Spirit", null));

        assertThat(CommanderEligibility.isEligible(card)).isTrue();
    }

    @Test
    void shouldNotBeEligibleForNonLegendaryCreature() {
        Card card = cardWithFace("Creature - Goblin", null);

        assertThat(CommanderEligibility.isEligible(card)).isFalse();
    }

    @Test
    void shouldNotBeEligibleForLegendaryNonCreature() {
        Card card = cardWithFace("Legendary Artifact", null);

        assertThat(CommanderEligibility.isEligible(card)).isFalse();
    }

    @Test
    void shouldNotBeEligibleWhenFaceAttributesAreNull() {
        Card card = cardWithFace(null, null);

        assertThat(CommanderEligibility.isEligible(card)).isFalse();
    }

    private Card cardWithFace(String typeLine, String oracleText) {
        Card card = new Card("oracle-1", "Card");
        card.getFaces().add(face(card, 0, typeLine, oracleText));
        return card;
    }

    private CardFace face(Card card, int order, String typeLine, String oracleText) {
        CardFace face = new CardFace(card, order, card.getName());
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        return face;
    }
}
