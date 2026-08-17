package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import org.junit.jupiter.api.Test;

class CardSummaryResponseTest {

    @Test
    void shouldExposeOwnedQuantityOnlyWhenProvided() {
        Card card = new Card("oracle-id", "Sol Ring");

        assertThat(CardSummaryResponse.from(card, null, 4).ownedQuantity()).isEqualTo(4);
        assertThat(CardSummaryResponse.from(card, null).ownedQuantity()).isNull();
    }

    @Test
    void shouldTolerateDuplicateLegalityRowsLeftByConcurrentImports() {
        Card card = new Card("oracle-id", "Spider-Man");
        card.getLegalities().add(new CardLegality(card, "alchemy", "legal"));
        card.getLegalities().add(new CardLegality(card, "alchemy", "legal"));
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));

        CardSummaryResponse response = CardSummaryResponse.from(card, null);

        assertThat(response.legalities())
                .containsEntry("alchemy", "legal")
                .containsEntry("commander", "legal")
                .hasSize(2);
    }
}
