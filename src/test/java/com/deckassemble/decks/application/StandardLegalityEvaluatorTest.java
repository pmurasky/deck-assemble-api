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
class StandardLegalityEvaluatorTest {

    @Mock private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldReportLegalForValidStandardDeck() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "Instant", "common", "legal")), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void shouldFlagDeckBelowMinimumSize() {
        // Given
        List<DeckCard> deckCards =
                List.of(deckCard(1, printing(1, card("oracle-1", "Card 1", "Instant", "common", "legal")), 10));

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("DECK_SIZE_INVALID");
    }

    @Test
    void shouldFlagMoreThanFourCopiesOfNonBasicCard() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        deckCards.add(deckCard(1, printing(1, card("oracle-1", "Sol Ring", "Artifact", "uncommon", "legal")), 5));
        for (int i = 2; i <= 13; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "Instant", "common", "legal")), 4));
        }
        deckCards.add(deckCard(14, printing(14, card("oracle-14", "Card 14", "Instant", "common", "legal")), 3));

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("COPY_LIMIT_VIOLATION");
    }

    @Test
    void shouldIgnoreBasicLandsForCopyLimit() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        deckCards.add(deckCard(1, printing(1, basicLand("oracle-1", "Forest", "legal")), 24));
        for (int i = 2; i <= 11; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "Instant", "common", "legal")), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void shouldFlagCardNotLegalInStandard() {
        // Given
        List<DeckCard> deckCards = new ArrayList<>();
        deckCards.add(deckCard(1, printing(1, card("oracle-1", "Banned Card", "Instant", "rare", "banned")), 4));
        for (int i = 2; i <= 15; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "Instant", "common", "legal")), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("STANDARD_LEGALITY_INVALID");
    }

    @Test
    void shouldFlagCardWithUnknownStandardLegality() {
        // Given
        Card unknown = mock(Card.class);
        when(unknown.getName()).thenReturn("Mystery Card");
        when(unknown.getScryfallOracleId()).thenReturn("oracle-x");
        when(unknown.getTypeLine()).thenReturn("Instant");
        when(unknown.getLegalities()).thenReturn(List.of());
        List<DeckCard> deckCards = new ArrayList<>();
        deckCards.add(deckCard(1, printing(1, unknown), 4));
        for (int i = 2; i <= 15; i++) {
            deckCards.add(deckCard(i, printing(i, card("oracle-" + i, "Card " + i, "Instant", "common", "legal")), 4));
        }

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("STANDARD_LEGALITY_UNKNOWN");
    }

    @Test
    void shouldFlagMissingCardPrinting() {
        // Given
        when(cardPrintingRepository.findById(99L)).thenReturn(Optional.empty());
        List<DeckCard> deckCards = List.of(new DeckCard(1L, 99L, 1, DeckCard.Section.MAIN_DECK));

        // When
        DeckLegalityResponse result = evaluator().evaluate(deck(), deckCards);

        // Then
        assertThat(result.legal()).isFalse();
        assertThat(codes(result)).contains("CARD_NOT_FOUND");
    }

    private StandardLegalityEvaluator evaluator() {
        return new StandardLegalityEvaluator(cardPrintingRepository);
    }

    private Deck deck() {
        return new Deck(1L, "Standard Deck", "STANDARD");
    }

    private DeckCard deckCard(long printingId, CardPrinting printing, int quantity) {
        when(cardPrintingRepository.findById(printingId)).thenReturn(Optional.of(printing));
        return new DeckCard(1L, printingId, quantity, DeckCard.Section.MAIN_DECK);
    }

    private CardPrinting printing(long id, Card card) {
        CardPrinting printing = mock(CardPrinting.class);
        when(printing.getCard()).thenReturn(card);
        return printing;
    }

    private Card card(String oracleId, String name, String typeLine, String rarity, String status) {
        CardLegality legality = mock(CardLegality.class);
        when(legality.getFormatCode()).thenReturn("standard");
        when(legality.getLegalityStatus()).thenReturn(status);
        Card card = mock(Card.class);
        when(card.getName()).thenReturn(name);
        when(card.getScryfallOracleId()).thenReturn(oracleId);
        when(card.getTypeLine()).thenReturn(typeLine);
        when(card.getLegalities()).thenReturn(List.of(legality));
        return card;
    }

    private Card basicLand(String oracleId, String name, String status) {
        CardLegality legality = mock(CardLegality.class);
        when(legality.getFormatCode()).thenReturn("standard");
        when(legality.getLegalityStatus()).thenReturn(status);
        Card card = mock(Card.class);
        when(card.getScryfallOracleId()).thenReturn(oracleId);
        when(card.getTypeLine()).thenReturn("Basic Land — " + name);
        when(card.getLegalities()).thenReturn(List.of(legality));
        return card;
    }

    private List<String> codes(DeckLegalityResponse result) {
        return result.violations().stream().map(DeckLegalityResponse.Violation::code).toList();
    }
}
