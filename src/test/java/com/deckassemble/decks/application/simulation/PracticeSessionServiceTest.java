package com.deckassemble.decks.application.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PracticeSessionServiceTest {

    private static final long DECK_ID = 1L;

    @Mock private DeckRevisionService deckRevisionService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldPlayNonLandFromHandWithoutManaEnforcement() {
        stubHomogeneousLibrary(8, spellCard(1L, "Bear", 7));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 41L));

        PracticeSessionResponse response = service().playCard(DECK_ID, started.sessionId(), 1L);

        assertThat(response.hand()).hasSize(6);
        assertThat(response.battlefield())
                .singleElement()
                .satisfies(
                        permanent -> {
                            assertThat(permanent.printingId()).isEqualTo(1L);
                            assertThat(permanent.card().name()).isEqualTo("Bear");
                            assertThat(permanent.tapped()).isFalse();
                        });
    }

    @Test
    void shouldRejectSecondLandPlayedInTheSameTurn() {
        stubSnapshot(List.of(entry(1L, 1, "MAIN_DECK"), entry(2L, 6, "MAIN_DECK")));
        stubCatalog(
                Map.of(
                        1L, landCard(1L, "Forest", "G"),
                        2L, landCard(2L, "Island", "U")));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 41L));
        service().playCard(DECK_ID, started.sessionId(), 1L);

        assertThatThrownBy(() -> service().playCard(DECK_ID, started.sessionId(), 2L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldToggleBattlefieldPermanentTwice() {
        stubHomogeneousLibrary(8, spellCard(1L, "Bear", 2));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 41L));
        service().playCard(DECK_ID, started.sessionId(), 1L);

        PracticeSessionResponse tapped = service().toggleTap(DECK_ID, started.sessionId(), 1L);
        PracticeSessionResponse untapped = service().toggleTap(DECK_ID, started.sessionId(), 1L);

        assertThat(tapped.battlefield())
                .singleElement()
                .extracting(permanent -> permanent.tapped())
                .isEqualTo(true);
        assertThat(untapped.battlefield())
                .singleElement()
                .extracting(permanent -> permanent.tapped())
                .isEqualTo(false);
    }

    @Test
    void shouldAdvanceTurnUntapBattlefieldAndResetLandPlay() {
        stubSnapshot(List.of(entry(1L, 1, "MAIN_DECK"), entry(2L, 6, "MAIN_DECK")));
        stubCatalog(
                Map.of(
                        1L, landCard(1L, "Forest", "G"),
                        2L, landCard(2L, "Island", "U")));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 41L));
        PracticeSessionResponse played = service().playCard(DECK_ID, started.sessionId(), 1L);
        PracticeSessionResponse tapped = service().toggleTap(DECK_ID, started.sessionId(), 1L);

        PracticeSessionResponse advanced = service().nextTurn(DECK_ID, started.sessionId());
        PracticeSessionResponse secondLand = service().playCard(DECK_ID, started.sessionId(), 2L);

        assertThat(started.landsInPlay()).isZero();
        assertThat(started.landPlayedThisTurn()).isFalse();
        assertThat(played.landsInPlay()).isEqualTo(1);
        assertThat(played.landPlayedThisTurn()).isTrue();
        assertThat(tapped.landsInPlay()).isZero();
        assertThat(tapped.landPlayedThisTurn()).isTrue();
        assertThat(advanced.turn()).isEqualTo(1);
        assertThat(advanced.drawnCard()).isNull();
        assertThat(advanced.battlefield())
                .singleElement()
                .extracting(permanent -> permanent.tapped())
                .isEqualTo(false);
        assertThat(advanced.landsInPlay()).isEqualTo(1);
        assertThat(advanced.landPlayedThisTurn()).isFalse();
        assertThat(secondLand.battlefield()).hasSize(2);
        assertThat(secondLand.landsInPlay()).isEqualTo(2);
        assertThat(secondLand.landPlayedThisTurn()).isTrue();
    }

    @Test
    void shouldDrawFinalCardAndFinishOnNextTurn() {
        stubHomogeneousLibrary(8, spellCard(1L, "Bear", 2));
        PracticeSessionResponse started = service().start(DECK_ID, request(false, 5L));

        PracticeSessionResponse advanced = service().nextTurn(DECK_ID, started.sessionId());

        assertThat(advanced.turn()).isEqualTo(1);
        assertThat(advanced.drawnCard())
                .extracting(PracticeSessionResponse.CardView::name)
                .isEqualTo("Bear");
        assertThat(advanced.hand()).hasSize(8);
        assertThat(advanced.finished()).isTrue();
    }

    @Test
    void shouldKeepFinishedAfterPlayerActionWhenLibraryIsExhausted() {
        stubHomogeneousLibrary(8, spellCard(1L, "Bear", 2));
        PracticeSessionResponse started = service().start(DECK_ID, request(false, 5L));
        service().nextTurn(DECK_ID, started.sessionId());

        PracticeSessionResponse played = service().playCard(DECK_ID, started.sessionId(), 1L);

        assertThat(played.finished()).isTrue();
    }

    @Test
    void shouldEnrichCardsInHandBattlefieldAndDrawnCard() {
        Card bear = spellCard(1L, "Bear", 7);
        bear.setManaCost("{5}{G}{G}");
        bear.setOracleText("Vigilance");
        stubSnapshot(List.of(entry(1L, 8, "MAIN_DECK")));
        stubPracticeCatalog(
                Map.of(1L, new PracticeCard(1L, bear, "https://images.example/bear.jpg")));
        PracticeSessionResponse started = service().start(DECK_ID, request(false, 5L));

        PracticeSessionResponse.CardView handCard = started.hand().getFirst();
        PracticeSessionResponse played = service().playCard(DECK_ID, started.sessionId(), 1L);
        PracticeSessionResponse.CardView battlefieldCard = played.battlefield().getFirst().card();
        PracticeSessionResponse.CardView drawnCard =
                Objects.requireNonNull(
                        service().nextTurn(DECK_ID, started.sessionId()).drawnCard());

        assertEnrichedBear(handCard);
        assertEnrichedBear(battlefieldCard);
        assertEnrichedBear(drawnCard);
    }

    @Test
    void shouldReportCastableSpellsOnlyWhileLandsAreUntapped() {
        stubSnapshot(List.of(entry(1L, 4, "MAIN_DECK"), entry(2L, 3, "MAIN_DECK")));
        stubCatalog(Map.of(1L, landCard(1L, "Forest", "G"), 2L, spellCard(2L, "Bear", 1)));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 3L));

        PracticeSessionResponse played = service().playCard(DECK_ID, started.sessionId(), 1L);
        PracticeSessionResponse tapped = service().toggleTap(DECK_ID, started.sessionId(), 1L);

        assertThat(played.castableSpells())
                .extracting(PracticeSessionResponse.CardView::name)
                .containsExactly("Bear", "Bear", "Bear");
        assertThat(tapped.castableSpells()).isEmpty();
    }

    @Test
    void shouldProduceIdenticalTurnSequenceForTheSameSeed() {
        stubMixedLibrary(20, 20);
        PracticeSessionResponse first = service().start(DECK_ID, request(true, 42L));
        PracticeSessionResponse second = service().start(DECK_ID, request(true, 42L));

        assertThat(second).usingRecursiveComparison().ignoringFields("sessionId").isEqualTo(first);
        for (int i = 0; i < 5; i++) {
            PracticeSessionResponse firstTurn = service().nextTurn(DECK_ID, first.sessionId());
            PracticeSessionResponse secondTurn = service().nextTurn(DECK_ID, second.sessionId());
            assertThat(secondTurn)
                    .usingRecursiveComparison()
                    .ignoringFields("sessionId")
                    .isEqualTo(firstTurn);
        }
    }

    @Test
    void shouldResetSessionToTheSameOpeningHand() {
        stubMixedLibrary(20, 20);
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 42L));
        service().nextTurn(DECK_ID, started.sessionId());
        service().nextTurn(DECK_ID, started.sessionId());

        PracticeSessionResponse reset = service().reset(DECK_ID, started.sessionId());

        assertThat(reset.turn()).isZero();
        assertThat(reset.sessionId()).isEqualTo(started.sessionId());
        assertThat(reset).usingRecursiveComparison().ignoringFields("sessionId").isEqualTo(started);
    }

    @Test
    void shouldRejectNextTurnForUnknownSession() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service().nextTurn(DECK_ID, unknown))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("practice session");
    }

    @Test
    void shouldRejectLibrarySmallerThanTheOpeningHand() {
        stubHomogeneousLibrary(5, spellCard(1L, "Bear", 2));

        assertThatThrownBy(() -> service().start(DECK_ID, request(true, 1L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("library");
    }

    private static void assertEnrichedBear(PracticeSessionResponse.CardView card) {
        assertThat(card.printingId()).isEqualTo(1L);
        assertThat(card.name()).isEqualTo("Bear");
        assertThat(card.imageUrl()).isEqualTo("https://images.example/bear.jpg");
        assertThat(card.manaCost()).isEqualTo("{5}{G}{G}");
        assertThat(card.typeLine()).isEqualTo("Creature — Bear");
        assertThat(card.oracleText()).isEqualTo("Vigilance");
    }

    private void stubMixedLibrary(int landCount, int spellCount) {
        stubSnapshot(
                List.of(entry(1L, landCount, "MAIN_DECK"), entry(2L, spellCount, "MAIN_DECK")));
        stubCatalog(Map.of(1L, landCard(1L, "Forest", "G"), 2L, spellCard(2L, "Bear", 2)));
    }

    private void stubHomogeneousLibrary(int quantity, Card card) {
        stubSnapshot(List.of(entry(1L, quantity, "MAIN_DECK")));
        stubCatalog(Map.of(1L, card));
    }

    private void stubSnapshot(List<DeckSnapshot.CardEntry> cards) {
        lenient().when(deckRevisionService.snapshotAt(DECK_ID, 1)).thenReturn(snapshot(cards));
    }

    private void stubCatalog(Map<Long, Card> catalog) {
        stubPracticeCatalog(
                catalog.entrySet().stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry ->
                                                new PracticeCard(
                                                        entry.getKey(), entry.getValue(), null))));
    }

    private void stubPracticeCatalog(Map<Long, PracticeCard> catalog) {
        lenient().when(cardCatalogService.getPracticeCardsByPrintingIds(any())).thenReturn(catalog);
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

    private static PracticeSessionRequest request(boolean onThePlay, @Nullable Long seed) {
        return new PracticeSessionRequest(1, onThePlay, MulliganStrategy.NONE, null, null, seed);
    }

    private @Nullable PracticeSessionService service;

    private PracticeSessionService service() {
        if (service == null) {
            service = new PracticeSessionService(deckRevisionService, cardCatalogService);
        }
        return service;
    }
}
