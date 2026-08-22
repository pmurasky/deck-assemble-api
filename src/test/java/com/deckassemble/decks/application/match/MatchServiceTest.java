package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
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

    private static Card landCard(long id, String name) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Land");
        card.setOracleText("");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static Card instantCard(long id, String name) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Instant");
        card.setOracleText("");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private void advanceSteps(Match match, int count) {
        for (int i = 0; i < count; i++) {
            service().advanceStep(match.id(), CALLER_PROFILE_ID);
        }
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

    @Test
    void shouldPlayLandDuringMainStep() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, landCard(1L, "Forest"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3); // Upkeep, Draw (skipped on turn 1), FirstMain

        Match result = service().playLand(match.id(), CALLER_PROFILE_ID, 1L);

        assertThat(result).isSameAs(match);
        assertThat(you.hand()).hasSize(6);
        assertThat(you.battlefield()).hasSize(1);
        assertThat(you.landPlayedThisTurn()).isTrue();
    }

    @Test
    void shouldRejectSecondLandInSameTurn() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, landCard(1L, "Forest"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        service().playLand(match.id(), CALLER_PROFILE_ID, 1L);

        assertBadRequest(
                () -> service().playLand(match.id(), CALLER_PROFILE_ID, 1L),
                "already played a land this turn");
    }

    @Test
    void shouldRejectLandOutsideMainStep() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, landCard(1L, "Forest"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);

        assertBadRequest(
                () -> service().playLand(match.id(), CALLER_PROFILE_ID, 1L), "main step");
    }

    @Test
    void shouldCastCreatureOntoBattlefield() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);

        assertThat(you.hand()).hasSize(6);
        assertThat(you.battlefield()).hasSize(1);
        assertThat(you.battlefield().getFirst().tapped()).isFalse();
        assertThat(you.graveyard()).isEmpty();
    }

    @Test
    void shouldSendInstantToGraveyardAfterResolving() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, instantCard(1L, "Shock"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);

        assertThat(you.hand()).hasSize(6);
        assertThat(you.battlefield()).isEmpty();
        assertThat(you.graveyard()).hasSize(1);
    }

    @Test
    void shouldCastCommanderFromCommandZoneAndIncreaseTax() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 10L);

        assertThat(you.hand()).hasSize(7);
        assertThat(you.commanderTax()).isEqualTo(2);
        assertThat(you.commanderInCommandZone()).isFalse();
        assertThat(you.battlefield()).hasSize(1);
        assertThat(you.battlefield().getFirst().commander()).isTrue();

        assertBadRequest(
                () -> service().castSpell(match.id(), CALLER_PROFILE_ID, 10L),
                "card is not in hand");
    }

    @Test
    void shouldRejectActionFromNonParticipant() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);

        assertThatThrownBy(() -> service().advanceStep(match.id(), 999L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void shouldRejectActionsAfterMatchEnds() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);

        service().concede(match.id(), CALLER_PROFILE_ID);

        assertThat(match.loser()).isEqualTo(you.playerId());
        assertThat(match.winner()).isEqualTo(opponent.playerId());
        assertBadRequest(() -> service().advanceStep(match.id(), CALLER_PROFILE_ID), "match is over");
        assertBadRequest(
                () -> service().castSpell(match.id(), CALLER_PROFILE_ID, 1L), "match is over");
    }

    @Test
    void shouldSwitchActivePlayerAndDrawOnNewTurn() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);

        advanceSteps(match, 12); // full first turn: back to Untap of turn 2

        assertThat(match.turnNumber()).isEqualTo(2);
        assertThat(match.step()).isInstanceOf(TurnStep.Untap.class);
        assertThat(match.activePlayer()).isSameAs(opponent);
        assertThat(you.hand()).hasSize(7); // on the play: turn-1 Draw skipped

        advanceSteps(match, 2); // Upkeep, Draw

        assertThat(opponent.hand()).hasSize(8);
        assertThat(opponent.library()).hasSize(1);
    }

    @Test
    void shouldDealUnblockedDamageToDefendingPlayer() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);
        advanceSteps(match, 2); // BeginCombat, DeclareAttackers

        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));

        assertThat(you.battlefield().getFirst().tapped()).isTrue();

        advanceSteps(match, 1); // DeclareBlockers
        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of());

        assertThat(opponent.life()).isEqualTo(38);
        assertThat(opponent.commanderDamageReceived()).isEmpty();
        assertThat(match.loser()).isNull();
    }

    @Test
    void shouldLoseByCommanderDamageAt21() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(10L, creatureCard(100L, "Your Commander", "21", "5"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 10L);
        advanceSteps(match, 2);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(10L));
        advanceSteps(match, 1);

        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of());

        assertThat(opponent.life()).isEqualTo(19);
        assertThat(opponent.commanderDamageReceived()).containsEntry(you.playerId(), 21);
        assertThat(match.loser()).isEqualTo(opponent.playerId());
        assertThat(match.winner()).isEqualTo(you.playerId());
    }

    @Test
    void shouldLoseByCombatDamageAtZeroLife() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, creatureCard(1L, "Bear", "41", "2"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);
        advanceSteps(match, 2);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));
        advanceSteps(match, 1);

        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of());

        assertThat(match.loser()).isEqualTo(opponent.playerId());
    }

    @Test
    void shouldTradeCreaturesWhenBlocked() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L); // Bear 2/2
        advanceSteps(match, 12); // opponent's FirstMain (turn 2)
        service().castSpell(match.id(), CALLER_PROFILE_ID, 2L); // Elite 3/3
        advanceSteps(match, 14); // your DeclareAttackers (turn 3)
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));
        advanceSteps(match, 1);

        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of(2L, 1L));

        assertThat(you.battlefield()).isEmpty();
        assertThat(you.graveyard()).hasSize(1);
        assertThat(opponent.battlefield()).hasSize(1);
        assertThat(opponent.graveyard()).isEmpty();
    }

    @Test
    void shouldKillBlockerAndSurvive() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, creatureCard(1L, "Bear", "4", "4"));
        catalog.put(2L, creatureCard(2L, "Elite", "2", "2"));
        stubMatchDecks(9, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);
        advanceSteps(match, 12);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 2L);
        advanceSteps(match, 14);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));
        advanceSteps(match, 1);

        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of(2L, 1L));

        assertThat(you.battlefield()).hasSize(1);
        assertThat(opponent.battlefield()).isEmpty();
        assertThat(opponent.graveyard()).hasSize(1);
    }

    @Test
    void shouldRejectDeclareAttackersOutsideDeclareAttackersStep() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 3); // FirstMain

        assertBadRequest(
                () -> service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L)),
                "declare attackers");
    }

    @Test
    void shouldRejectAttackerNotOnBattlefield() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 5); // DeclareAttackers

        assertBadRequest(
                () -> service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L)),
                "attacker is not on the battlefield");
    }

    @Test
    void shouldRejectBlockerNotOnBattlefield() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L);
        advanceSteps(match, 2);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));
        advanceSteps(match, 1);

        assertBadRequest(
                () -> service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of(99L, 1L)),
                "blocker is not on the battlefield");
    }

    private void assertBadRequest(ThrowingCallable action, String reason) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode())
                                    .isEqualTo(HttpStatus.BAD_REQUEST);
                            assertThat(exception.getReason()).contains(reason);
                        });
    }

    private @Nullable MatchService service;

    private MatchService service() {
        if (service == null) {
            service = new MatchService(deckRevisionService, cardCatalogService);
        }
        return service;
    }
}
