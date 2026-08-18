package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardExportView;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.exporting.DeckExportFormat;
import com.deckassemble.decks.application.exporting.DeckExporter;
import com.deckassemble.decks.domain.DeckCard;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckExportControllerTest {

    @Mock private DeckService deckService;
    @Mock private DeckCardService deckCardService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private DeckExporter exporter;
    private DeckExportController controller;

    @BeforeEach
    void setUp() {
        when(exporter.format()).thenReturn(DeckExportFormat.GENERIC_CSV);
        controller =
                new DeckExportController(
                        deckService, deckCardService, cardCatalogService, List.of(exporter));
    }

    @Test
    void shouldMapReadModelAndBuildSafeAttachment() {
        DeckResponse deck = mock(DeckResponse.class);
        DeckCardResponse card = mock(DeckCardResponse.class);
        when(deck.name()).thenReturn("../ My Deck");
        when(deckService.getById(7L)).thenReturn(deck);
        when(card.cardPrintingId()).thenReturn(9L);
        when(card.quantity()).thenReturn(2);
        when(card.deckSection()).thenReturn(DeckCard.Section.MAIN_DECK.name());
        when(deckCardService.listCards(7L)).thenReturn(List.of(card));
        when(cardCatalogService.getExportViewsByPrintingIds(List.of(9L)))
                .thenReturn(Map.of(9L, exportView()));
        when(exporter.export(anyList())).thenReturn("exported");

        var response = controller.export(7L, DeckExportFormat.GENERIC_CSV);

        var cards = ArgumentCaptor.forClass(List.class);
        verify(exporter).export(cards.capture());
        assertThat(cards.getValue()).containsExactly(exportCard());
        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("My-Deck-generic.csv");
        assertThat(response.getBody()).isEqualTo("exported".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldUseFallbackFilenameForUnsafeDeckName() {
        DeckResponse deck = mock(DeckResponse.class);
        when(deck.name()).thenReturn("../");
        when(deckService.getById(7L)).thenReturn(deck);
        when(deckCardService.listCards(7L)).thenReturn(List.of());
        when(cardCatalogService.getExportViewsByPrintingIds(List.of())).thenReturn(Map.of());
        when(exporter.export(List.of())).thenReturn("");

        var response = controller.export(7L, DeckExportFormat.GENERIC_CSV);

        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("deck-generic.csv");
    }

    @Test
    void shouldRejectMissingPrintingReadModel() {
        DeckResponse deck = mock(DeckResponse.class);
        DeckCardResponse card = mock(DeckCardResponse.class);
        when(deckService.getById(7L)).thenReturn(deck);
        when(card.cardPrintingId()).thenReturn(9L);
        when(deckCardService.listCards(7L)).thenReturn(List.of(card));
        when(cardCatalogService.getExportViewsByPrintingIds(List.of(9L))).thenReturn(Map.of());

        assertThatThrownBy(() -> controller.export(7L, DeckExportFormat.GENERIC_CSV))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Deck references a missing card printing");
    }

    @Test
    void shouldReturnOnlyUnownedCardsInProxySheet() {
        // Given
        DeckCardResponse owned = mock(DeckCardResponse.class);
        when(owned.deckSection()).thenReturn(DeckCard.Section.MAIN_DECK.name());
        when(owned.ownershipStatus()).thenReturn(DeckCard.OwnershipStatus.OWNED.name());
        DeckCardResponse proxy = mock(DeckCardResponse.class);
        when(proxy.cardPrintingId()).thenReturn(10L);
        when(proxy.quantity()).thenReturn(2);
        when(proxy.deckSection()).thenReturn(DeckCard.Section.MAIN_DECK.name());
        when(proxy.ownershipStatus()).thenReturn(DeckCard.OwnershipStatus.PROXY.name());
        when(deckCardService.listCards(7L)).thenReturn(List.of(owned, proxy));
        when(cardCatalogService.getExportViewsByPrintingIds(List.of(10L)))
                .thenReturn(
                        Map.of(
                                10L,
                                new CardExportView(
                                        10L,
                                        "Unowned Card",
                                        null,
                                        new CardExportView.PrintingReference(
                                                "TST", "8", "scryfall-8"),
                                        "https://img.example/8.png")));

        // When
        ProxySheetResponse response = controller.proxySheet(7L);

        // Then
        assertThat(response.cards())
                .containsExactly(
                        new ProxySheetResponse.ProxySheetCard(
                                "Unowned Card", "https://img.example/8.png", 2));
    }

    private static CardExportView exportView() {
        return new CardExportView(
                9L,
                "Canonical Name",
                "Flavor Name",
                new CardExportView.PrintingReference("TST", "7", "scryfall-id"),
                null);
    }

    private static DeckExporter.ExportCard exportCard() {
        return new DeckExporter.ExportCard(
                DeckCard.Section.MAIN_DECK,
                2,
                "Flavor Name",
                new DeckExporter.PrintingReference("TST", "7", "scryfall-id"));
    }
}
