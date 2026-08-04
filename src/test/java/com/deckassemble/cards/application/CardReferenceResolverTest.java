package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CardReferenceResolverTest {

    private static final long CARD_ID = 1L;
    private static final long PRINTING_ID = 2L;
    private static final long SECOND_PRINTING_ID = 3L;
    private static final UUID SCRYFALL_ID = UUID.fromString("03fcf7d4-8a1b-4e2f-89f1-12c840e27721");
    private static final UUID SECOND_SCRYFALL_ID =
            UUID.fromString("3efb3cb0-6bd8-4da1-8d76-c992f62c6281");
    private static final UUID UNKNOWN_SCRYFALL_ID =
            UUID.fromString("9e16456a-71ee-4db5-9796-66f3e799a94c");

    @Mock private CardRepository cardRepository;
    @Mock private CardPrintingRepository printingRepository;
    @InjectMocks private CardReferenceResolver resolver;

    @Test
    void shouldResolveExactScryfallId() {
        var printing = printing("Lightning Bolt", "lea", "161");
        when(printingRepository.findByScryfallCardId(SCRYFALL_ID.toString()))
                .thenReturn(Optional.of(printing));

        var result = resolver.resolve(new CardReference(SCRYFALL_ID, "wrong", "bad", "0"));

        assertThat(result).isEqualTo(new CardReferenceResolution.Matched(CARD_ID, PRINTING_ID));
    }

    @Test
    void shouldResolveExactNameSetAndCollectorNumber() {
        var printing = printing("Lightning Bolt", "lea", "161");
        when(printingRepository
                        .findByCardNameIgnoreCaseAndMagicSetSetCodeIgnoreCaseAndCollectorNumberIgnoreCase(
                                "lightning bolt", "LEA", "161"))
                .thenReturn(List.of(printing));

        var result = resolver.resolve(new CardReference(null, "lightning bolt", "LEA", "161"));

        assertThat(result).isEqualTo(new CardReferenceResolution.Matched(CARD_ID, PRINTING_ID));
    }

    @Test
    void shouldReturnUnmatchedForUnknownExactPrintingReference() {
        when(printingRepository
                        .findByCardNameIgnoreCaseAndMagicSetSetCodeIgnoreCaseAndCollectorNumberIgnoreCase(
                                "lightning bolt", "LEA", "999"))
                .thenReturn(List.of());

        var result = resolver.resolve(new CardReference(null, "lightning bolt", "LEA", "999"));

        assertThat(result).isEqualTo(new CardReferenceResolution.Unmatched());
    }

    @Test
    void shouldFallBackToExactPrintingReferenceWhenScryfallIdIsUnknown() {
        var printing = printing("Lightning Bolt", "lea", "161");
        when(printingRepository.findByScryfallCardId(UNKNOWN_SCRYFALL_ID.toString()))
                .thenReturn(Optional.empty());
        when(printingRepository
                        .findByCardNameIgnoreCaseAndMagicSetSetCodeIgnoreCaseAndCollectorNumberIgnoreCase(
                                "lightning bolt", "LEA", "161"))
                .thenReturn(List.of(printing));

        var result =
                resolver.resolve(
                        new CardReference(UNKNOWN_SCRYFALL_ID, "lightning bolt", "LEA", "161"));

        assertThat(result).isEqualTo(new CardReferenceResolution.Matched(CARD_ID, PRINTING_ID));
    }

    @Test
    void shouldReturnAmbiguousForNameOnlyWithMultiplePrintings() {
        var first = printing(PRINTING_ID, SCRYFALL_ID, "Lightning Bolt", "lea", "161");
        var second =
                printing(SECOND_PRINTING_ID, SECOND_SCRYFALL_ID, "Lightning Bolt", "2ed", "157");
        when(cardRepository.findByNameIgnoreCase("lightning bolt"))
                .thenReturn(List.of(first.getCard()));
        when(printingRepository.findByCardIdOrderByReleasedAtDesc(1L))
                .thenReturn(List.of(first, second));

        var result = resolver.resolve(new CardReference(null, "lightning bolt", null, null));

        assertThat(result)
                .isEqualTo(
                        new CardReferenceResolution.Ambiguous(
                                List.of(PRINTING_ID, SECOND_PRINTING_ID)));
    }

    @Test
    void shouldReturnUnmatchedForNameOnlyWithOnePrinting() {
        var printing = printing("Lightning Bolt", "lea", "161");
        when(cardRepository.findByNameIgnoreCase("lightning bolt"))
                .thenReturn(List.of(printing.getCard()));
        when(printingRepository.findByCardIdOrderByReleasedAtDesc(1L))
                .thenReturn(List.of(printing));

        var result = resolver.resolve(new CardReference(null, "lightning bolt", null, null));

        assertThat(result).isEqualTo(new CardReferenceResolution.Unmatched());
    }

    @Test
    void shouldReturnUnmatchedForEmptyReference() {
        var result = resolver.resolve(new CardReference(null, null, null, null));

        assertThat(result).isEqualTo(new CardReferenceResolution.Unmatched());
        verifyNoInteractions(cardRepository, printingRepository);
    }

    private static CardPrinting printing(String name, String setCode, String collectorNumber) {
        return printing(PRINTING_ID, SCRYFALL_ID, name, setCode, collectorNumber);
    }

    private static CardPrinting printing(
            long printingId, UUID scryfallId, String name, String setCode, String collectorNumber) {
        var card = new Card("d9e6aaf9-9f9b-4fe3-8c80-643f82096db4", name);
        ReflectionTestUtils.setField(card, "id", CARD_ID);
        var set = new MagicSet("set-id", setCode, "Test Set");
        var printing = new CardPrinting(card, set, scryfallId.toString());
        ReflectionTestUtils.setField(printing, "id", printingId);
        printing.setCollectorNumber(collectorNumber);
        return printing;
    }
}
