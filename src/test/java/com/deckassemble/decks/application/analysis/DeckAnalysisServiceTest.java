package com.deckassemble.decks.application.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckComboResponse;
import com.deckassemble.decks.application.DeckComboService;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckAnalysisServiceTest {

    @Mock private DeckService deckService;
    @Mock private DeckCardService deckCardService;
    @Mock private DeckComboService deckComboService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CardPriceService cardPriceService;

    @Test
    void shouldReturnEmptyAnalysisForEmptyDeck() {
        // Given a deck with no cards
        stubLegality();
        when(deckCardService.listCards(1L)).thenReturn(List.of());
        when(cardCatalogService.getAnalysisViewsByPrintingIds(List.of())).thenReturn(Map.of());
        when(cardPriceService.latestPrices(List.of())).thenReturn(Map.of());
        when(deckComboService.getCombos(1L)).thenReturn(new DeckComboResponse(true, List.of()));

        // When
        DeckAnalysisResponse analysis = service().analyze(1L);

        // Then every section is present but empty
        assertThat(analysis.manaCurve()).isEmpty();
        assertThat(analysis.typeDistribution()).isEmpty();
        assertThat(analysis.colorDemand()).isEmpty();
        assertThat(analysis.colorProduction()).isEmpty();
        assertThat(analysis.landCount()).isZero();
        assertThat(analysis.averageManaValue()).isEqualTo(0.0);
        assertThat(analysis.ownershipBreakdown()).isEmpty();
        assertThat(analysis.valueByCurrency()).isEmpty();
        assertThat(analysis.missingCostByCurrency()).isEmpty();
        assertThat(analysis.unpricedCardCount()).isZero();
        assertThat(analysis.functionalCategories()).isEmpty();
        assertThat(analysis.tokenProducers()).isEmpty();
        assertThat(analysis.gameChangers()).isEmpty();
        assertThat(analysis.legality().legal()).isFalse();
        assertThat(analysis.combos().available()).isTrue();
        assertThat(analysis.combos().count()).isZero();
    }

    @Test
    void shouldComposeAnalysisAcrossCollaborators() {
        // Given a deck with an owned spell, a wishlist land, and a proxied game changer
        stubLegality();
        when(deckCardService.listCards(1L))
                .thenReturn(
                        List.of(
                                deckCard(10L, 2, "MAIN_DECK", "OWNED"),
                                deckCard(11L, 4, "MAIN_DECK", "WISHLIST"),
                                deckCard(12L, 1, "MAIN_DECK", "PROXY")));
        when(cardCatalogService.getAnalysisViewsByPrintingIds(List.of(10L, 11L, 12L)))
                .thenReturn(
                        Map.of(
                                10L, view(10L, "Lightning Bolt", "{R}", "1", "Instant", "Deals 3 damage.", false),
                                11L, view(11L, "Forest", null, "0", "Basic Land — Forest", "{T}: Add {G}.", false),
                                12L, view(12L, "Sol Ring", "{1}", "1", "Artifact", "{T}: Add {C}{C}.", true)));
        when(cardPriceService.latestPrices(List.of(10L, 11L, 12L)))
                .thenReturn(
                        Map.of(
                                10L, new CardPrice(usd("2.00"), null, null, null),
                                12L, new CardPrice(usd("5.00"), null, null, null)));
        when(deckComboService.getCombos(1L))
                .thenReturn(
                        new DeckComboResponse(
                                true,
                                List.of(
                                        new SpellbookCombo(
                                                "c1", List.of("A", "B"), List.of(), "d", ""))));

        // When
        DeckAnalysisResponse analysis = service().analyze(1L);

        // Then composition, value, and summaries are all populated
        assertThat(analysis.manaCurve()).containsExactly(Map.entry("1", 3));
        assertThat(analysis.typeDistribution())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("INSTANT", 2, "LAND", 4, "ARTIFACT", 1));
        assertThat(analysis.colorDemand()).containsExactly(Map.entry("R", 2));
        assertThat(analysis.colorProduction())
                .containsExactlyInAnyOrderEntriesOf(Map.of("G", 4, "C", 1));
        assertThat(analysis.landCount()).isEqualTo(4);
        assertThat(analysis.averageManaValue()).isEqualTo(1.0);
        assertThat(analysis.ownershipBreakdown())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("OWNED", 2, "WISHLIST", 4, "PROXY", 1));
        assertThat(analysis.valueByCurrency()).containsExactly(Map.entry("usd", usd("9.00")));
        assertThat(analysis.missingCostByCurrency())
                .containsExactly(Map.entry("usd", usd("5.00")));
        assertThat(analysis.unpricedCardCount()).isEqualTo(4);
        assertThat(analysis.functionalCategories())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("LAND", 4, "RAMP", 1, "SYNERGY", 2));
        assertThat(analysis.gameChangers()).containsExactly("Sol Ring");
        assertThat(analysis.legality().legal()).isFalse();
        assertThat(analysis.combos().count()).isEqualTo(1);
    }

    @Test
    void shouldExcludeSideboardCompanionAndMaybeBoard() {
        // Given cards spread across play and non-play sections
        stubLegality();
        when(deckCardService.listCards(1L))
                .thenReturn(
                        List.of(
                                deckCard(10L, 1, "COMMANDER", "OWNED"),
                                deckCard(11L, 1, "SIDEBOARD", "OWNED"),
                                deckCard(12L, 1, "COMPANION", "OWNED"),
                                deckCard(13L, 1, "MAYBE_BOARD", "OWNED")));
        when(cardCatalogService.getAnalysisViewsByPrintingIds(List.of(10L, 11L, 12L, 13L)))
                .thenReturn(
                        Map.of(
                                10L, view(10L, "Commander", "{3}", "3", "Legendary Creature — Human", "", false),
                                11L, view(11L, "Side", "{2}", "2", "Instant", "", false),
                                12L, view(12L, "Companion", "{4}", "4", "Creature — Beast", "", false),
                                13L, view(13L, "Maybe", "{5}", "5", "Sorcery", "", false)));
        when(cardPriceService.latestPrices(List.of(10L))).thenReturn(Map.of());
        when(deckComboService.getCombos(1L)).thenReturn(new DeckComboResponse(true, List.of()));

        // When
        DeckAnalysisResponse analysis = service().analyze(1L);

        // Then only commander and main deck shape the analysis
        assertThat(analysis.manaCurve()).containsExactly(Map.entry("3", 1));
        assertThat(analysis.ownershipBreakdown()).containsExactly(Map.entry("OWNED", 1));
        assertThat(analysis.unpricedCardCount()).isEqualTo(1);
    }

    private DeckAnalysisService service() {
        return new DeckAnalysisService(
                deckService, deckCardService, deckComboService, cardCatalogService,
                cardPriceService);
    }

    private void stubLegality() {
        when(deckService.legality(1L))
                .thenReturn(
                        new DeckLegalityResponse(
                                false,
                                List.of(
                                        new DeckLegalityResponse.Violation(
                                                "COMMANDER_REQUIRED", "No commander"))));
    }

    private static DeckCardResponse deckCard(
            Long printingId, int quantity, String section, String ownership) {
        return new DeckCardResponse(1L, printingId, quantity, section, ownership, null);
    }

    private static CardAnalysisView view(
            Long printingId,
            String name,
            String manaCost,
            String manaValue,
            String typeLine,
            String oracleText,
            boolean gameChanger) {
        return new CardAnalysisView(
                printingId,
                name,
                null,
                new BigDecimal(manaValue),
                typeLine,
                null,
                gameChanger,
                List.of(new CardAnalysisView.Face(manaCost, typeLine, oracleText)));
    }

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }
}
