package com.deckassemble.decks.application.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DeckSampleHandServiceTest {

    private static final long DECK_ID = 1L;

    @Mock private DeckRevisionService deckRevisionService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldProduceByteIdenticalOutputForTheSameSeed() {
        List<DeckSnapshot.CardEntry> cards = nonlandCards(1, 10);
        stubSnapshot(null, null, cards);
        stubCatalog(cardsFor(cards, "Creature"));
        DeckSampleHandRequest request = request(3, MulliganStrategy.NONE, null, null, 42L);

        DeckSampleHandResponse first = service().generate(DECK_ID, request);
        DeckSampleHandResponse second = service().generate(DECK_ID, request);

        assertThat(first).isEqualTo(second);
        assertThat(first.seed()).isEqualTo(42L);
    }

    @Test
    void shouldRespectDeckCardQuantities() {
        List<DeckSnapshot.CardEntry> cards =
                List.of(entry(100L, 4, "MAIN_DECK"), entry(200L, 3, "MAIN_DECK"));
        stubSnapshot(null, null, cards);
        stubCatalog(
                Map.of(
                        100L, card(100L, "Forest", "Basic Land — Forest"),
                        200L, card(200L, "Bear", "Creature — Bear")));
        DeckSampleHandRequest request = request(1, MulliganStrategy.NONE, null, null, 7L);

        DeckSampleHandResponse response = service().generate(DECK_ID, request);

        List<Long> drawnPrintingIds =
                response.hands().get(0).cards().stream()
                        .map(DeckSampleHandResponse.DrawnCard::cardPrintingId)
                        .toList();
        assertThat(drawnPrintingIds).hasSize(7);
        assertThat(drawnPrintingIds.stream().filter(id -> id == 100L).count()).isEqualTo(4);
        assertThat(drawnPrintingIds.stream().filter(id -> id == 200L).count()).isEqualTo(3);
    }

    @Test
    void shouldExcludeCommanderSectionCardFromLibrary() {
        List<DeckSnapshot.CardEntry> cards = new ArrayList<>(nonlandCards(1, 7));
        cards.add(entry(900L, 1, "COMMANDER"));
        stubSnapshot(500L, null, cards);
        Map<Long, Card> catalog =
                withExtra(
                        cardsFor(nonlandCards(1, 7), "Creature"),
                        900L,
                        card(500L, "Commander Card", "Legendary Creature"));
        stubCatalog(catalog);
        DeckSampleHandRequest request = request(1, MulliganStrategy.NONE, null, null, 1L);

        DeckSampleHandResponse response = service().generate(DECK_ID, request);

        List<Long> drawnPrintingIds =
                response.hands().get(0).cards().stream()
                        .map(DeckSampleHandResponse.DrawnCard::cardPrintingId)
                        .toList();
        assertThat(drawnPrintingIds)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L)
                .doesNotContain(900L);
    }

    @Test
    void shouldExcludeCommanderByCardIdEvenWhenRowIsMiscategorizedAsMainDeck() {
        List<DeckSnapshot.CardEntry> cards = new ArrayList<>(nonlandCards(1, 7));
        cards.add(entry(900L, 1, "MAIN_DECK"));
        stubSnapshot(500L, null, cards);
        Map<Long, Card> catalog =
                withExtra(
                        cardsFor(nonlandCards(1, 7), "Creature"),
                        900L,
                        card(500L, "Commander Card", "Legendary Creature"));
        stubCatalog(catalog);
        DeckSampleHandRequest request = request(1, MulliganStrategy.NONE, null, null, 1L);

        DeckSampleHandResponse response = service().generate(DECK_ID, request);

        List<Long> drawnPrintingIds =
                response.hands().get(0).cards().stream()
                        .map(DeckSampleHandResponse.DrawnCard::cardPrintingId)
                        .toList();
        assertThat(drawnPrintingIds).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L, 7L);
    }

    @Test
    void shouldBottomOneCardPerMulliganAndCapAtThreeWhenLandRangeIsUnsatisfiable() {
        // 3 lands out of 10 cards: a 7-card hand can never contain 7 lands, so every attempt
        // fails and the mulligan loop is forced to hit its cap.
        List<DeckSnapshot.CardEntry> lands = List.of(entry(1L, 3, "MAIN_DECK"));
        List<DeckSnapshot.CardEntry> spells = nonlandCards(2, 8);
        List<DeckSnapshot.CardEntry> cards =
                Stream.concat(lands.stream(), spells.stream()).toList();
        stubSnapshot(null, null, cards);
        Map<Long, Card> catalog =
                withExtra(
                        cardsFor(spells, "Creature"),
                        1L,
                        card(1L, "Forest", "Basic Land — Forest"));
        stubCatalog(catalog);
        DeckSampleHandRequest request = request(1, MulliganStrategy.LONDON_LAND_RANGE, 7, 7, 1L);

        DeckSampleHandResponse response = service().generate(DECK_ID, request);

        DeckSampleHandResponse.Hand hand = response.hands().get(0);
        assertThat(hand.mulliganCount()).isEqualTo(3);
        assertThat(hand.cards()).hasSize(4);
    }

    @Test
    void shouldNotMulliganWhenFirstDrawAlreadySatisfiesLandRange() {
        List<DeckSnapshot.CardEntry> cards = nonlandCards(1, 10);
        stubSnapshot(null, null, cards);
        stubCatalog(cardsFor(cards, "Creature"));
        DeckSampleHandRequest request = request(1, MulliganStrategy.LONDON_LAND_RANGE, 0, 7, 5L);

        DeckSampleHandResponse response = service().generate(DECK_ID, request);

        DeckSampleHandResponse.Hand hand = response.hands().get(0);
        assertThat(hand.mulliganCount()).isEqualTo(0);
        assertThat(hand.cards()).hasSize(7);
    }

    @Test
    void shouldRejectDeckWithFewerThanSevenLibraryCards() {
        List<DeckSnapshot.CardEntry> cards = nonlandCards(1, 5);
        stubSnapshot(null, null, cards);
        DeckSampleHandRequest request = request(1, MulliganStrategy.NONE, null, null, 1L);

        assertThatThrownBy(() -> service().generate(DECK_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectLondonStrategyWithoutLandBounds() {
        DeckSampleHandRequest request =
                request(1, MulliganStrategy.LONDON_LAND_RANGE, null, null, 1L);

        assertThatThrownBy(() -> service().generate(DECK_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private void stubSnapshot(
            @Nullable Long commanderCardId,
            @Nullable Long secondaryCommanderCardId,
            List<DeckSnapshot.CardEntry> cards) {
        lenient()
                .when(deckRevisionService.snapshotAt(DECK_ID, 1))
                .thenReturn(snapshot(commanderCardId, secondaryCommanderCardId, cards));
    }

    private void stubCatalog(Map<Long, Card> catalog) {
        lenient().when(cardCatalogService.getCardsByPrintingIds(any())).thenReturn(catalog);
    }

    private static Map<Long, Card> withExtra(Map<Long, Card> base, long printingId, Card extra) {
        return Stream.concat(base.entrySet().stream(), Stream.of(Map.entry(printingId, extra)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map<Long, Card> cardsFor(List<DeckSnapshot.CardEntry> entries, String typeLine) {
        return entries.stream()
                .collect(
                        Collectors.toMap(
                                DeckSnapshot.CardEntry::cardPrintingId,
                                entry ->
                                        card(
                                                entry.cardPrintingId(),
                                                "Card " + entry.cardPrintingId(),
                                                typeLine),
                                (first, second) -> first));
    }

    private static List<DeckSnapshot.CardEntry> nonlandCards(
            long fromPrintingId, long toPrintingId) {
        return LongStream.rangeClosed(fromPrintingId, toPrintingId)
                .mapToObj(id -> entry(id, 1, "MAIN_DECK"))
                .toList();
    }

    private static DeckSnapshot.CardEntry entry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    private static Card card(long id, String name, String typeLine) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine(typeLine);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static DeckSnapshot snapshot(
            @Nullable Long commanderCardId,
            @Nullable Long secondaryCommanderCardId,
            List<DeckSnapshot.CardEntry> cards) {
        return new DeckSnapshot(
                "Deck",
                "COMMANDER",
                null,
                commanderCardId,
                secondaryCommanderCardId,
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

    private static DeckSampleHandRequest request(
            int handCount,
            MulliganStrategy strategy,
            @Nullable Integer minimumLands,
            @Nullable Integer maximumLands,
            @Nullable Long seed) {
        return new DeckSampleHandRequest(1, handCount, strategy, minimumLands, maximumLands, seed);
    }

    private DeckSampleHandService service() {
        return new DeckSampleHandService(deckRevisionService, cardCatalogService);
    }
}
