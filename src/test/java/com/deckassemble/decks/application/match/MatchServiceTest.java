package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.simulation.MulliganStrategy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class MatchServiceTest {

    private static final long YOUR_DECK_ID = 1L;
    private static final long OPPONENT_DECK_ID = 2L;
    private static final long CALLER_PROFILE_ID = 42L;
    private static final long YOUR_COMMANDER_CARD_ID = 100L;
    private static final long OPPONENT_COMMANDER_CARD_ID = 200L;

    @Mock private DeckRevisionService deckRevisionService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldStartMatchWithBothPlayersReady() {
        stubMatchDecks(8, defaultCatalog());

        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);

        assertThat(match.players()).hasSize(2);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        assertThat(you.hand()).hasSize(7);
        assertThat(you.library()).hasSize(1);
        assertThat(you.life()).isEqualTo(PlayerState.STARTING_LIFE);
        assertThat(you.commanderTax()).isZero();
        assertThat(you.commander().card().getName()).isEqualTo("Your Commander");
        assertThat(opponent.hand()).hasSize(7);
        assertThat(opponent.commander().card().getName()).isEqualTo("Opponent Commander");
        assertThat(match.activePlayer()).isSameAs(you);
        assertThat(match.turnNumber()).isEqualTo(1);
        assertThat(match.step()).isInstanceOf(TurnStep.Untap.class);
    }

    @Test
    void shouldStartWithOpponentActiveWhenCallerIsNotOnThePlay() {
        stubMatchDecks(8, defaultCatalog());

        Match match = service().start(request(false, 41L), CALLER_PROFILE_ID);

        assertThat(match.activePlayer()).isSameAs(match.players().get(1));
    }

    @Test
    void shouldRejectDeckWithNonNumericPowerAtStart() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, creatureCard(1L, "Bear", "*", "2"));
        stubMatchDecks(8, catalog);

        assertThatThrownBy(() -> service().start(request(true, 41L), CALLER_PROFILE_ID))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void shouldRejectLibrarySmallerThanTheOpeningHand() {
        stubMatchDecks(5, defaultCatalog());

        assertThatThrownBy(() -> service().start(request(true, 41L), CALLER_PROFILE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("library");
    }

    @Test
    void shouldRejectStartWhenCommanderIsMissingFromTheDeck() {
        stubMatchDecks(8, defaultCatalog(), 999L, OPPONENT_COMMANDER_CARD_ID);

        assertThatThrownBy(() -> service().start(request(true, 41L), CALLER_PROFILE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("commander");
    }

    private void stubMatchDecks(int mainDeckQuantity, Map<Long, Card> catalog) {
        stubMatchDecks(
                mainDeckQuantity, catalog, YOUR_COMMANDER_CARD_ID, OPPONENT_COMMANDER_CARD_ID);
    }

    private void stubMatchDecks(
            int mainDeckQuantity,
            Map<Long, Card> catalog,
            long yourCommanderCardId,
            long opponentCommanderCardId) {
        lenient()
                .when(deckRevisionService.snapshotAt(YOUR_DECK_ID, 1))
                .thenReturn(
                        snapshot(
                                List.of(
                                        entry(1L, mainDeckQuantity, "MAIN_DECK"),
                                        entry(10L, 1, "COMMANDER")),
                                yourCommanderCardId));
        lenient()
                .when(deckRevisionService.snapshotAtForSharedAccess(OPPONENT_DECK_ID, 2))
                .thenReturn(
                        snapshot(
                                List.of(
                                        entry(2L, mainDeckQuantity, "MAIN_DECK"),
                                        entry(20L, 1, "COMMANDER")),
                                opponentCommanderCardId));
        lenient()
                .when(cardCatalogService.getPracticeCardsByPrintingIds(any()))
                .thenReturn(practiceCatalog(catalog));
    }

    private static Map<Long, Card> defaultCatalog() {
        Map<Long, Card> catalog = new HashMap<>();
        catalog.put(1L, creatureCard(1L, "Bear", "2", "2"));
        catalog.put(10L, creatureCard(YOUR_COMMANDER_CARD_ID, "Your Commander", "5", "5"));
        catalog.put(2L, creatureCard(2L, "Elite", "3", "3"));
        catalog.put(20L, creatureCard(OPPONENT_COMMANDER_CARD_ID, "Opponent Commander", "4", "4"));
        return catalog;
    }

    private static Map<Long, PracticeCard> practiceCatalog(Map<Long, Card> catalog) {
        return catalog.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new PracticeCard(entry.getKey(), entry.getValue(), null)));
    }

    private static DeckSnapshot.CardEntry entry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    private static Card creatureCard(long id, String name, String power, String toughness) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Creature — Bear");
        card.setOracleText("");
        card.setPower(power);
        card.setToughness(toughness);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static DeckSnapshot snapshot(
            List<DeckSnapshot.CardEntry> cards, long commanderCardId) {
        return new DeckSnapshot(
                "Deck",
                "COMMANDER",
                null,
                commanderCardId,
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

    private static MatchRequest request(boolean callerOnThePlay, @Nullable Long seed) {
        return new MatchRequest(
                YOUR_DECK_ID,
                1,
                OPPONENT_DECK_ID,
                2,
                MulliganStrategy.NONE,
                null,
                null,
                seed,
                callerOnThePlay);
    }

    private @Nullable MatchService service;

    private MatchService service() {
        if (service == null) {
            service = new MatchService(deckRevisionService, cardCatalogService);
        }
        return service;
    }
}
