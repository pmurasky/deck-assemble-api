package com.deckassemble.cards.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardImportFaceTest {

    @Test
    void shouldTolerateNullColorsWhenConvertingToCardFace() {
        // Given
        var card = new Card("oracle-id", "Brazen Borrower // Petty Theft");
        var source =
                new CardImportFace(
                        "Brazen Borrower",
                        "{1}{U}",
                        "Creature",
                        null,
                        "2",
                        "2",
                        null,
                        null,
                        "front");

        // When
        CardFace face = source.toCardFace(card, 0);

        // Then
        assertThat(face.getColors()).isNull();
    }
}
