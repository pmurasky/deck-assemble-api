package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.cards.infrastructure.scryfall.ScryfallClient;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCardFace;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallImageUris;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallList;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.shared.security.CurrentUser;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardImportServiceTest {

    @Mock private ScryfallClient scryfallClient;
    @Mock private CardRepository cardRepository;
    @Mock private MagicSetRepository magicSetRepository;
    @Mock private CardPrintingRepository cardPrintingRepository;
    @Mock private ImportRunRecorder runRecorder;
    @Mock private CurrentUser currentUser;

    @Test
    void shouldImportAValidScryfallCard() {
        ScryfallCard source =
                new ScryfallCard(
                        "printing-id",
                        "oracle-id",
                        "Spider-Man",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        false,
                        "set-id",
                        "mar",
                        "Marvel",
                        "1",
                        "rare",
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        false,
                        false,
                        "en",
                        List.of(
                                new ScryfallCardFace(
                                        "Spider-Man",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        List.of(),
                                        new ScryfallImageUris("small", "normal", "large"))),
                        Map.of("commander", "legal"));
        URI nextPage = URI.create("https://api.scryfall.com/cards/search?page=2");
        when(scryfallClient.searchCards("set:mar"))
                .thenReturn(new ScryfallList<>(List.of(source), true, nextPage));
        when(scryfallClient.searchCards(nextPage))
                .thenReturn(new ScryfallList<>(List.of(source), false, null));
        when(cardRepository.findByScryfallOracleId("oracle-id")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(magicSetRepository.findBySetCode("mar")).thenReturn(Optional.empty());
        when(magicSetRepository.save(any(MagicSet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPrintingRepository.findByScryfallCardId("printing-id"))
                .thenReturn(Optional.empty());
        when(cardPrintingRepository.save(any(CardPrinting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(runRecorder.start("set:mar", "admin-sub")).thenReturn(7L);
        when(currentUser.subject()).thenReturn(Optional.of("admin-sub"));

        ImportResult result =
                new CardImportService(
                                scryfallClient,
                                cardRepository,
                                magicSetRepository,
                                cardPrintingRepository,
                                runRecorder,
                                currentUser)
                        .importQuery("set:mar");

        assertThat(result).isEqualTo(new ImportResult(7L, 2, 2, 0, 0));
        verify(runRecorder).complete(7L, 2, 2, 0, 0);
        ArgumentCaptor<CardPrinting> printing = ArgumentCaptor.forClass(CardPrinting.class);
        verify(cardPrintingRepository, org.mockito.Mockito.times(2)).save(printing.capture());
        assertThat(printing.getAllValues())
                .allSatisfy(value -> assertThat(value.getImageUriNormal()).isEqualTo("normal"));
        ArgumentCaptor<Card> cards = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository, org.mockito.Mockito.times(2)).save(cards.capture());
        assertThat(cards.getAllValues())
                .allSatisfy(
                        card ->
                                assertThat(card.getLegalities())
                                        .singleElement()
                                        .satisfies(
                                                legality -> {
                                                    assertThat(legality.getFormatCode())
                                                            .isEqualTo("commander");
                                                    assertThat(legality.getLegalityStatus())
                                                            .isEqualTo("legal");
                                                }));
    }
}
