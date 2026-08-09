package com.deckassemble.decks.application.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckSimulationServiceTest {

    private static final long DECK_ID = 1L;

    @Mock private DeckRevisionService deckRevisionService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldProduceByteIdenticalOutputForTheSameSeed() {
        stubMixedLibrary(20, 20);
        DeckSimulationRequest request = request(100, 3, true, null, null, 42L);

        DeckSimulationResponse first = service().simulate(DECK_ID, request);
        DeckSimulationResponse second = service().simulate(DECK_ID, request);

        assertThat(first).isEqualTo(second);
        assertThat(first.seed()).isEqualTo(42L);
    }

    @Test
    void shouldReportCertainLandDropsAndColorAvailabilityForAnAllLandLibrary() {
        stubHomogeneousLibrary(20, landCard(1L, "Forest", "G"));
        DeckSimulationRequest request = request(100, 5, true, null, null, 7L);

        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(response.landDropProbabilityByTurn())
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(1.0));
        assertThat(response.colorAvailabilityByTurn().get("G"))
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(1.0));
        assertThat(response.colorAvailabilityByTurn().get("W"))
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
        assertThat(response.cardsSeenByTurn())
                .containsExactly(
                        Map.entry(1, 7.0),
                        Map.entry(2, 8.0),
                        Map.entry(3, 9.0),
                        Map.entry(4, 10.0),
                        Map.entry(5, 11.0));
        assertThat(response.castabilityByTurn())
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
        assertThat(response.playableSpellCountByTurn())
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
    }

    @Test
    void shouldReportZeroLandDropsAndZeroSourceColorsForAnAllSpellLibrary() {
        stubHomogeneousLibrary(20, spellCard(1L, "Bear", 2));
        DeckSimulationRequest request = request(100, 4, true, null, null, 9L);

        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(response.landDropProbabilityByTurn())
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
        for (String color : List.of("W", "U", "B", "R", "G")) {
            assertThat(response.colorAvailabilityByTurn().get(color))
                    .as("color %s", color)
                    .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
        }
        assertThat(response.castabilityByTurn())
                .allSatisfy((turn, p) -> assertThat(p).isEqualTo(0.0));
    }

    @Test
    void shouldComputeExactDeterministicStatsWhenLibraryExactlyFillsTheOpeningHand() {
        // 4 Forests (producing G) + 3 one-mana Bears = exactly 7 cards, the whole opening hand, so
        // every iteration sees the identical composition regardless of shuffle order or seed.
        List<DeckSnapshot.CardEntry> cards =
                List.of(entry(1L, 4, "MAIN_DECK"), entry(2L, 3, "MAIN_DECK"));
        stubSnapshot(cards);
        stubCatalog(
                Map.of(
                        1L, landCard(1L, "Forest", "G"),
                        2L, spellCard(2L, "Bear", 1)));
        DeckSimulationRequest request = request(100, 1, true, null, null, 3L);

        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(response.landDropProbabilityByTurn()).containsExactly(Map.entry(1, 1.0));
        assertThat(response.colorAvailabilityByTurn().get("G")).containsExactly(Map.entry(1, 1.0));
        assertThat(response.colorAvailabilityByTurn().get("W")).containsExactly(Map.entry(1, 0.0));
        assertThat(response.cardsSeenByTurn()).containsExactly(Map.entry(1, 7.0));
        assertThat(response.castabilityByTurn()).containsExactly(Map.entry(1, 1.0));
        assertThat(response.playableSpellCountByTurn()).containsExactly(Map.entry(1, 3.0));
        assertThat(response.confidence().iterations()).isEqualTo(100);
        assertThat(response.confidence().marginOfErrorPercent95()).isCloseTo(9.8, within(1e-9));
    }

    @Test
    void shouldIncludeATurnOneDrawWhenOnTheDraw() {
        stubHomogeneousLibrary(20, landCard(1L, "Forest", "G"));
        DeckSimulationRequest request = request(100, 3, false, null, null, 5L);

        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(response.cardsSeenByTurn())
                .containsExactly(Map.entry(1, 8.0), Map.entry(2, 9.0), Map.entry(3, 10.0));
    }

    @Test
    void shouldAcceptLondonMulliganStrategyDuringSimulation() {
        stubMixedLibrary(20, 20);
        DeckSimulationRequest request = request(100, 3, true, 3, 7, 11L);

        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(response.landDropProbabilityByTurn().values())
                .allSatisfy(p -> assertThat(p).isBetween(0.0, 1.0));
    }

    @Test
    void shouldCompleteMaximumWorkloadWithinAReasonableTime() {
        stubHomogeneousLibrary(20, landCard(1L, "Forest", "G"));
        DeckSimulationRequest request = request(100_000, 10, false, null, null, 21L);

        long start = System.nanoTime();
        DeckSimulationResponse response = service().simulate(DECK_ID, request);

        assertThat(System.nanoTime() - start).isLessThan(Duration.ofSeconds(5).toNanos());
        assertThat(response.iterations()).isEqualTo(100_000);
    }

    private void stubMixedLibrary(int landCount, int spellCount) {
        List<DeckSnapshot.CardEntry> cards =
                List.of(entry(1L, landCount, "MAIN_DECK"), entry(2L, spellCount, "MAIN_DECK"));
        stubSnapshot(cards);
        stubCatalog(
                Map.of(
                        1L, landCard(1L, "Forest", "G"),
                        2L, spellCard(2L, "Bear", 2)));
    }

    private void stubHomogeneousLibrary(int quantity, Card card) {
        List<DeckSnapshot.CardEntry> cards = List.of(entry(1L, quantity, "MAIN_DECK"));
        stubSnapshot(cards);
        stubCatalog(Map.of(1L, card));
    }

    private void stubSnapshot(List<DeckSnapshot.CardEntry> cards) {
        lenient().when(deckRevisionService.snapshotAt(DECK_ID, 1)).thenReturn(snapshot(cards));
    }

    private void stubCatalog(Map<Long, Card> catalog) {
        lenient().when(cardCatalogService.getCardsByPrintingIds(any())).thenReturn(catalog);
    }

    private static DeckSnapshot.CardEntry entry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    private static Card landCard(long id, String name, String color) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Basic Land — " + name);
        card.setOracleText("{T}: Add {" + color + "}.");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static Card spellCard(long id, String name, int manaValue) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Creature — Bear");
        card.setOracleText("");
        card.setManaValue(BigDecimal.valueOf(manaValue));
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static DeckSnapshot snapshot(List<DeckSnapshot.CardEntry> cards) {
        return new DeckSnapshot(
                "Deck",
                "COMMANDER",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                cards,
                List.of(),
                List.of());
    }

    private static DeckSimulationRequest request(
            int iterations,
            int turns,
            boolean onThePlay,
            @Nullable Integer minimumLands,
            @Nullable Integer maximumLands,
            @Nullable Long seed) {
        MulliganStrategy strategy =
                minimumLands == null ? MulliganStrategy.NONE : MulliganStrategy.LONDON_LAND_RANGE;
        return new DeckSimulationRequest(
                1, iterations, turns, onThePlay, strategy, minimumLands, maximumLands, seed);
    }

    private DeckSimulationService service() {
        return new DeckSimulationService(deckRevisionService, cardCatalogService);
    }
}
