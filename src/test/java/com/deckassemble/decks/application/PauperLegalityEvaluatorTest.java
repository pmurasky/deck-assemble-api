package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PauperLegalityEvaluatorTest {

    @Mock private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldReportLegalForCommonOnlyPauperDeck() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "legal"), "common"), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void shouldFlagNonCommonPrinting() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        deckCards.add(deckCard(1, printing(1, card("oracle-1", "Fancy Card", "legal"), "rare"), 4));
        for (int i = 2; i <= 15; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "legal"), "common"), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("PAUPER_RARITY_VIOLATION");
    }

    @Test
    void shouldApplyConstructedRulesForPauper() {
        // Given
        List<DeckCard> deckCards =
                List.of(deckCard(1, printing(1, card("oracle-1", "Card 1", "legal"), "common"), 5));

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("DECK_SIZE_INVALID", "COPY_LIMIT_VIOLATION");
    }

    private PauperLegalityEvaluator evaluator() {
        return new PauperLegalityEvaluator(cardPrintingRepository);
    }

    private Deck deck() {
        return new Deck(1L, "Pauper Deck", "PAUPER");
    }

    private DeckCard deckCard(long printingId, CardPrinting printing, int quantity) {
        when(cardPrintingRepository.findById(printingId)).thenReturn(Optional.of(printing));
        return new DeckCard(1L, printingId, quantity, DeckCard.Section.MAIN_DECK);
    }

    private CardPrinting printing(long id, Card card, String rarity) {
        CardPrinting printing = mock(CardPrinting.class);
        when(printing.getCard()).thenReturn(card);
        when(printing.getRarity()).thenReturn(rarity);
        return printing;
    }

    private Card card(String oracleId, String name, String status) {
        CardLegality legality = mock(CardLegality.class);
        when(legality.getFormatCode()).thenReturn("pauper");
        when(legality.getLegalityStatus()).thenReturn(status);
        Card card = mock(Card.class);
        when(card.getName()).thenReturn(name);
        when(card.getScryfallOracleId()).thenReturn(oracleId);
        when(card.getTypeLine()).thenReturn("Instant");
        when(card.getLegalities()).thenReturn(List.of(legality));
        return card;
    }

    private List<String> codes(DeckLegalityResponse result) {
        return result.violations().stream().map(DeckLegalityResponse.Violation::code).toList();
    }
}
