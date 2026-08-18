package com.deckassemble.decks.application.comparison;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
import com.deckassemble.decks.application.comparison.DeckComparisonService.CardChange;
import com.deckassemble.decks.application.comparison.DeckComparisonService.Comparison;
import com.deckassemble.decks.application.comparison.DeckComparisonService.QuantityChange;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckComparisonServiceTest {

    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckCardService deckCardService;
    @Mock private DeckAnalysisService deckAnalysisService;

    @Test
    void shouldTreatSameCardInDifferentPrintingsAsUnchanged() {
        // Given the same card identity in different printings at equal quantity
        stubAccess();
        when(deckCardService.listCards(1L))
                .thenReturn(List.of(deckCard(10L, 2, "MAIN_DECK", "oracle-shared", "Cmp Shared")));
        when(deckCardService.listCards(2L))
                .thenReturn(List.of(deckCard(20L, 2, "MAIN_DECK", "oracle-shared", "Cmp Shared")));
        stubAnalyses(emptyAnalysis(), emptyAnalysis());

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then card-identity equivalence wins over exact printing
        assertThat(comparison.added()).isEmpty();
        assertThat(comparison.removed()).isEmpty();
        assertThat(comparison.quantityChanged()).isEmpty();
        assertThat(comparison.ownershipDelta()).isEmpty();
    }

    @Test
    void shouldReportAddedRemovedAndQuantityChangedCards() {
        // Given decks sharing one card at different quantities with unique cards on each side
        stubAccess();
        when(deckCardService.listCards(1L))
                .thenReturn(
                        List.of(
                                deckCard(10L, 2, "MAIN_DECK", "oracle-alpha", "Cmp Alpha"),
                                deckCard(11L, 1, "MAIN_DECK", "oracle-beta", "Cmp Beta")));
        when(deckCardService.listCards(2L))
                .thenReturn(
                        List.of(
                                deckCard(20L, 5, "MAIN_DECK", "oracle-alpha", "Cmp Alpha"),
                                deckCard(21L, 3, "MAIN_DECK", "oracle-gamma", "Cmp Gamma")));
        stubAnalyses(emptyAnalysis(), emptyAnalysis());

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then added, removed, and quantity-changed cards reconcile by identity
        assertThat(comparison.added()).containsExactly(new CardChange("Cmp Gamma", 3));
        assertThat(comparison.removed()).containsExactly(new CardChange("Cmp Beta", 1));
        assertThat(comparison.quantityChanged())
                .containsExactly(new QuantityChange("Cmp Alpha", 2, 5));
    }

    @Test
    void shouldMergeQuantitiesAcrossPrintingsOfOneIdentity() {
        // Given one deck splitting a card across two printings while the other uses one
        stubAccess();
        when(deckCardService.listCards(1L))
                .thenReturn(
                        List.of(
                                deckCard(10L, 1, "MAIN_DECK", "oracle-shared", "Cmp Shared"),
                                deckCard(11L, 2, "MAIN_DECK", "oracle-shared", "Cmp Shared")));
        when(deckCardService.listCards(2L))
                .thenReturn(List.of(deckCard(20L, 3, "MAIN_DECK", "oracle-shared", "Cmp Shared")));
        stubAnalyses(emptyAnalysis(), emptyAnalysis());

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then quantities merge by identity and no change is reported
        assertThat(comparison.quantityChanged()).isEmpty();
        assertThat(comparison.added()).isEmpty();
        assertThat(comparison.removed()).isEmpty();
    }

    @Test
    void shouldFallBackToPrintingIdentityWhenCardSummaryMissing() {
        // Given deck cards whose catalog summary no longer resolves
        stubAccess();
        when(deckCardService.listCards(1L))
                .thenReturn(List.of(unresolvedCard(10L, 1), unresolvedCard(11L, 1)));
        when(deckCardService.listCards(2L))
                .thenReturn(List.of(unresolvedCard(10L, 2), unresolvedCard(12L, 1)));
        stubAnalyses(emptyAnalysis(), emptyAnalysis());

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then unresolved cards compare by printing id
        assertThat(comparison.added()).hasSize(1);
        assertThat(comparison.removed()).hasSize(1);
        assertThat(comparison.quantityChanged()).hasSize(1);
        assertThat(comparison.quantityChanged().getFirst().fromQuantity()).isEqualTo(1);
        assertThat(comparison.quantityChanged().getFirst().toQuantity()).isEqualTo(2);
    }

    @Test
    void shouldCompareOnlyCommanderAndMainDeck() {
        // Given cards outside the playable sections plus a commander quantity change
        stubAccess();
        when(deckCardService.listCards(1L))
                .thenReturn(
                        List.of(
                                deckCard(10L, 1, "SIDEBOARD", "oracle-side", "Cmp Side"),
                                deckCard(11L, 1, "COMPANION", "oracle-comp", "Cmp Companion"),
                                deckCard(12L, 1, "MAYBE_BOARD", "oracle-maybe", "Cmp Maybe"),
                                deckCard(13L, 1, "COMMANDER", "oracle-cmd", "Cmp Commander")));
        when(deckCardService.listCards(2L))
                .thenReturn(List.of(deckCard(20L, 2, "COMMANDER", "oracle-cmd", "Cmp Commander")));
        stubAnalyses(emptyAnalysis(), emptyAnalysis());

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then only the commander is compared; sideboard, companion, and maybe are excluded
        assertThat(comparison.added()).isEmpty();
        assertThat(comparison.removed()).isEmpty();
        assertThat(comparison.quantityChanged())
                .containsExactly(new QuantityChange("Cmp Commander", 1, 2));
    }

    @Test
    void shouldComputeMetricDeltasAsOtherMinusBase() {
        // Given analyses with differing ownership, value, missing cost, curve, and categories
        stubAccess();
        stubCards(List.of(), List.of());
        DeckAnalysisResponse base =
                analysis(
                        Map.of("OWNED", 2, "WISHLIST", 1, "PROXY", 4),
                        Map.of("usd", usd("10.00"), "eur", usd("9.00"), "tix", usd("5.00")),
                        Map.of("usd", usd("4.00")),
                        Map.of("1", 2, "2", 1),
                        Map.of("LAND", 4, "RAMP", 2));
        DeckAnalysisResponse other =
                analysis(
                        Map.of("OWNED", 5, "PROXY", 4),
                        Map.of("usd", usd("25.00"), "tix", usd("5.00")),
                        Map.of("usd", usd("1.00")),
                        Map.of("1", 3, "2", 1, "3", 2),
                        Map.of("LAND", 2, "RAMP", 2, "DRAW", 1));
        stubAnalyses(base, other);

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then deltas are other minus base with zero-delta keys removed
        assertThat(comparison.ownershipDelta())
                .containsExactlyInAnyOrderEntriesOf(Map.of("OWNED", 3, "WISHLIST", -1));
        assertThat(comparison.valueDeltaByCurrency())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of("usd", usd("15.00"), "eur", usd("-9.00")));
        assertThat(comparison.missingCostDeltaByCurrency())
                .containsExactly(Map.entry("usd", usd("-3.00")));
        assertThat(comparison.curveDelta())
                .containsExactlyInAnyOrderEntriesOf(Map.of("1", 1, "3", 2));
        assertThat(comparison.categoryDelta())
                .containsExactlyInAnyOrderEntriesOf(Map.of("LAND", -2, "DRAW", 1));
    }

    @Test
    void shouldComputeLegalityGameChangerAndComboDeltas() {
        // Given analyses with differing legality, game changers, and combos
        stubAccess();
        stubCards(List.of(), List.of());
        DeckAnalysisResponse base =
                analysis(
                        new DeckLegalityResponse(
                                true, List.of(new DeckLegalityResponse.Violation("V1", "one"))),
                        List.of("Alpha", "Shared"),
                        new DeckAnalysisResponse.ComboSummary(
                                true,
                                1,
                                List.of(new SpellbookCombo("c1", List.of(), List.of(), "d", ""))));
        DeckAnalysisResponse other =
                analysis(
                        new DeckLegalityResponse(
                                false,
                                List.of(
                                        new DeckLegalityResponse.Violation("V1", "one"),
                                        new DeckLegalityResponse.Violation("V2", "two"))),
                        List.of("Beta", "Shared"),
                        new DeckAnalysisResponse.ComboSummary(
                                true,
                                2,
                                List.of(
                                        new SpellbookCombo("c1", List.of(), List.of(), "d", ""),
                                        new SpellbookCombo("c2", List.of(), List.of(), "d", ""))));
        stubAnalyses(base, other);

        // When
        Comparison comparison = service().compare(1L, 2L);

        // Then legality, game changer, and combo deltas reconcile
        assertThat(comparison.legality().baseLegal()).isTrue();
        assertThat(comparison.legality().otherLegal()).isFalse();
        assertThat(comparison.legality().addedViolations()).containsExactly("V2");
        assertThat(comparison.legality().removedViolations()).isEmpty();
        assertThat(comparison.gameChangersAdded()).containsExactly("Beta");
        assertThat(comparison.gameChangersRemoved()).containsExactly("Alpha");
        assertThat(comparison.combos().baseCount()).isEqualTo(1);
        assertThat(comparison.combos().otherCount()).isEqualTo(2);
        assertThat(comparison.combos().addedComboIds()).containsExactly("c2");
        assertThat(comparison.combos().removedComboIds()).isEmpty();
    }

    @Test
    void shouldEnforceAccessOnBothDecks() {
        // Given the current user owns the base deck but not the other deck
        Deck deck = mock(Deck.class);
        when(deckAccessGuard.owned(1L)).thenReturn(deck);
        when(deckAccessGuard.owned(2L)).thenThrow(new DeckNotFoundException());

        // When / Then the comparison is rejected before any data is loaded
        assertThatThrownBy(() -> service().compare(1L, 2L))
                .isInstanceOf(DeckNotFoundException.class);
        verify(deckCardService, never()).listCards(anyLong());
        verify(deckAnalysisService, never()).analyze(anyLong());
    }

    private DeckComparisonService service() {
        return new DeckComparisonService(deckAccessGuard, deckCardService, deckAnalysisService);
    }

    private void stubAccess() {
        Deck deck = mock(Deck.class);
        when(deckAccessGuard.owned(1L)).thenReturn(deck);
        when(deckAccessGuard.owned(2L)).thenReturn(deck);
    }

    private void stubCards(List<DeckCardResponse> base, List<DeckCardResponse> other) {
        when(deckCardService.listCards(1L)).thenReturn(base);
        when(deckCardService.listCards(2L)).thenReturn(other);
    }

    private void stubAnalyses(DeckAnalysisResponse base, DeckAnalysisResponse other) {
        when(deckAnalysisService.analyze(1L)).thenReturn(base);
        when(deckAnalysisService.analyze(2L)).thenReturn(other);
    }

    private static DeckAnalysisResponse emptyAnalysis() {
        return analysis(
                new DeckLegalityResponse(true, List.of()),
                List.of(),
                new DeckAnalysisResponse.ComboSummary(true, 0, List.of()));
    }

    private static DeckAnalysisResponse analysis(
            DeckLegalityResponse legality,
            List<String> gameChangers,
            DeckAnalysisResponse.ComboSummary combos) {
        return new DeckAnalysisResponse(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0,
                0.0,
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                Map.of(),
                List.of(),
                gameChangers,
                legality,
                combos);
    }

    private static DeckAnalysisResponse analysis(
            Map<String, Integer> ownership,
            Map<String, BigDecimal> value,
            Map<String, BigDecimal> missingCost,
            Map<String, Integer> curve,
            Map<String, Integer> categories) {
        return new DeckAnalysisResponse(
                curve,
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0,
                0.0,
                ownership,
                value,
                missingCost,
                0,
                categories,
                List.of(),
                List.of(),
                new DeckLegalityResponse(true, List.of()),
                new DeckAnalysisResponse.ComboSummary(true, 0, List.of()));
    }

    private static DeckCardResponse deckCard(
            long printingId, int quantity, String section, String oracleId, String name) {
        return new DeckCardResponse(
                1L, printingId, quantity, section, "OWNED", summary(oracleId, name));
    }

    private static DeckCardResponse unresolvedCard(long printingId, int quantity) {
        return new DeckCardResponse(1L, printingId, quantity, "MAIN_DECK", "OWNED", null);
    }

    // Suppressed: a 19-field summary fixture is one placeholder per line; splitting harms clarity.
    @SuppressWarnings("checkstyle:MethodLength")
    private static CardSummaryResponse summary(String oracleId, String name) {
        return new CardSummaryResponse(
                1L, oracleId, name, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, Map.of(), List.of(), null);
    }

    private static BigDecimal usd(String amount) {
        return new BigDecimal(amount);
    }
}
