package com.deckassemble.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardImportFace;
import com.deckassemble.cards.domain.CardImportImages;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingFace;
import com.deckassemble.cards.domain.CardPrintingFaceRepository;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.CardSearchPage;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.shared.security.CurrentUser;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
    @Mock private CardPrintingFaceRepository cardPrintingFaceRepository;
    @Mock private ImportRunRecorder runRecorder;
    @Mock private CurrentUser currentUser;

    @Test
    void shouldImportAValidScryfallCard() {
        CardImportData source =
                new CardImportData(
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
                        "Grimm Fate",
                        new CardImportImages("small", "normal", "large"),
                        List.of(
                                new CardImportFace(
                                        "Spider-Man",
                                        "{1}{R}",
                                        "Legendary Creature",
                                        "Web-slinging",
                                        "2",
                                        "3",
                                        null,
                                        List.of("R"),
                                        "front"),
                                new CardImportFace(
                                        "Spider-Back",
                                        null,
                                        "Legendary Creature",
                                        "Back-face text",
                                        "3",
                                        "2",
                                        null,
                                        List.of("R"),
                                        "back")),
                        null,
                        false,
                        false,
                        false,
                        false,
                        "en",
                        Map.of("commander", "legal"),
                        true);
        URI nextPage = URI.create("https://api.scryfall.com/cards/search?page=2");
        when(scryfallClient.searchCards("set:mar"))
                .thenReturn(new CardSearchPage(List.of(source), true, nextPage));
        when(scryfallClient.searchCards(nextPage))
                .thenReturn(new CardSearchPage(List.of(source), false, null));
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
                                cardPrintingFaceRepository,
                                runRecorder,
                                currentUser)
                        .importQuery("set:mar");

        assertThat(result).isEqualTo(new ImportResult(7L, 2, 2, 0, 0));
        verify(runRecorder).complete(7L, 2, 2, 0, 0);
        ArgumentCaptor<CardPrinting> printing = ArgumentCaptor.forClass(CardPrinting.class);
        verify(cardPrintingRepository, org.mockito.Mockito.times(2)).save(printing.capture());
        assertThat(printing.getAllValues())
                .allSatisfy(value -> assertThat(value.getImageUriNormal()).isEqualTo("normal"));
        assertThat(printing.getAllValues())
                .allSatisfy(value -> assertThat(value.getFlavorName()).isEqualTo("Grimm Fate"));
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
        assertThat(cards.getAllValues())
                .allSatisfy(card -> assertThat(card.getGameChanger()).isTrue());
        assertThat(cards.getAllValues())
                .allSatisfy(
                        card ->
                                assertThat(card.getFaces())
                                        .extracting(
                                                face -> face.getName() + ":" + face.getOracleText())
                                        .containsExactly(
                                                "Spider-Man:Web-slinging",
                                                "Spider-Back:Back-face text"));
        ArgumentCaptor<Iterable<CardPrintingFace>> faces = ArgumentCaptor.forClass(Iterable.class);
        verify(cardPrintingFaceRepository, org.mockito.Mockito.times(2)).saveAll(faces.capture());
        assertThat(faces.getAllValues())
                .allSatisfy(
                        values ->
                                assertThat(values)
                                        .extracting(CardPrintingFace::getImageUri)
                                        .containsExactly("front", "back"));
    }

    @Test
    void shouldMergeLegalitiesInPlaceWhenReimportingAnExistingCard() {
        CardImportData first = importData(Map.of("commander", "legal"));
        CardImportData second = importData(Map.of("commander", "banned", "legacy", "legal"));
        when(scryfallClient.searchCards("set:mar"))
                .thenReturn(new CardSearchPage(List.of(first), false, null))
                .thenReturn(new CardSearchPage(List.of(second), false, null));
        AtomicReference<Card> savedCard = new AtomicReference<>();
        when(cardRepository.findByScryfallOracleId("oracle-id"))
                .thenReturn(Optional.empty())
                .thenAnswer(invocation -> Optional.of(savedCard.get()));
        when(cardRepository.save(any(Card.class)))
                .thenAnswer(
                        invocation -> {
                            savedCard.set(invocation.getArgument(0));
                            return invocation.getArgument(0);
                        });
        when(magicSetRepository.findBySetCode("mar")).thenReturn(Optional.empty());
        when(magicSetRepository.save(any(MagicSet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPrintingRepository.findByScryfallCardId("printing-id"))
                .thenReturn(Optional.empty());
        when(cardPrintingRepository.save(any(CardPrinting.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(runRecorder.start("set:mar", "admin-sub")).thenReturn(7L);
        when(currentUser.subject()).thenReturn(Optional.of("admin-sub"));
        CardImportService service =
                new CardImportService(
                        scryfallClient,
                        cardRepository,
                        magicSetRepository,
                        cardPrintingRepository,
                        cardPrintingFaceRepository,
                        runRecorder,
                        currentUser);

        service.importQuery("set:mar");
        var commanderLegality =
                savedCard.get().getLegalities().stream()
                        .filter(legality -> legality.getFormatCode().equals("commander"))
                        .findFirst()
                        .orElseThrow();

        service.importQuery("set:mar");

        assertThat(savedCard.get().getLegalities())
                .extracting(
                        legality -> legality.getFormatCode() + ":" + legality.getLegalityStatus())
                .containsExactlyInAnyOrder("commander:banned", "legacy:legal");
        assertThat(savedCard.get().getLegalities())
                .as("re-import must update the existing legality row, not insert a duplicate")
                .contains(commanderLegality);
    }

    private CardImportData importData(Map<String, String> legalities) {
        return new CardImportData(
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
                new CardImportImages("small", "normal", "large"),
                List.of(
                        new CardImportFace(
                                "Spider-Man",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                "front")),
                null,
                false,
                false,
                false,
                false,
                "en",
                legalities,
                true);
    }
}
