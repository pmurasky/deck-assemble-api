package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest extends MatchServiceTestSupport {

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

    @Test
    void shouldAutoPassForNonActivePlayerWithEmptyStack() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        match.players().get(1).setAutoPassEnabled(true);

        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(match.step()).isEqualTo(new TurnStep.BeginCombat());
        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.activePlayer().playerId());
    }

    @Test
    void shouldNotAutoPassForTheActivePlayer() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        match.players().get(0).setAutoPassEnabled(true);
        match.players().get(1).setAutoPassEnabled(true);

        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(match.step()).isEqualTo(new TurnStep.BeginCombat());
        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.players().get(0).playerId());
    }

    @Test
    void shouldNotAutoPassWhileStackIsNonEmpty() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, instantCard(1L, "Shock"));
        stubMatchDecks(9, catalog);
        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        match.players().get(1).setAutoPassEnabled(true);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L, null);

        assertThat(match.stackResolver().stack()).hasSize(1);
        assertThat(match.step()).isInstanceOf(TurnStep.FirstMain.class);
        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.players().get(1).playerId());
    }

    @Test
    void shouldThrowWhenCascadeLimitIsExceeded() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        match.players().get(1).setAutoPassEnabled(true);
        match.stackResolver().cascadeLimit(0);

        assertThatThrownBy(() -> service().passPriority(match.id(), CALLER_PROFILE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cascade limit");
    }

    @Test
    void shouldToggleAutoPassForCallersSeat() {
        stubMatchDecks(9, defaultCatalog());
        Match match = service().start(request(true, 41L), CALLER_PROFILE_ID);

        service().setAutoPass(match.id(), CALLER_PROFILE_ID, true);

        assertThat(match.players().get(0).autoPassEnabled()).isTrue();
        assertThat(match.players().get(1).autoPassEnabled()).isFalse();
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

        assertBadRequest(() -> service().playLand(match.id(), CALLER_PROFILE_ID, 1L), "main step");
    }

    @Test
    void shouldCastCreatureOntoBattlefield() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3);

        castAndResolve(match, 1L);

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

        castAndResolve(match, 1L);

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

        castAndResolve(match, 10L);

        assertThat(you.hand()).hasSize(7);
        assertThat(you.commanderTax()).isEqualTo(2);
        assertThat(you.commanderInCommandZone()).isFalse();
        assertThat(you.battlefield()).hasSize(1);
        assertThat(you.battlefield().getFirst().commander()).isTrue();

        assertBadRequest(
                () -> service().castSpell(match.id(), CALLER_PROFILE_ID, 10L, null),
                "card is not in hand");
    }

    @Test
    void shouldRejectActionFromNonParticipant() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);

        assertThatThrownBy(() -> service().passPriority(match.id(), 999L))
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
        assertThat(match.opponentOf(match.loser()).playerId()).isEqualTo(opponent.playerId());
        assertBadRequest(
                () -> service().passPriority(match.id(), CALLER_PROFILE_ID), "match is over");
        assertBadRequest(
                () -> service().castSpell(match.id(), CALLER_PROFILE_ID, 1L, null),
                "match is over");
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
        castAndResolve(match, 1L);
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
        castAndResolve(match, 10L);
        advanceSteps(match, 2);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(10L));
        advanceSteps(match, 1);

        service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of());

        assertThat(opponent.life()).isEqualTo(19);
        assertThat(opponent.commanderDamageReceived()).containsEntry(you.playerId(), 21);
        assertThat(match.loser()).isEqualTo(opponent.playerId());
        assertThat(match.opponentOf(match.loser()).playerId()).isEqualTo(you.playerId());
    }

    @Test
    void shouldLoseByCombatDamageAtZeroLife() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, creatureCard(1L, "Bear", "41", "2"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);
        castAndResolve(match, 1L);
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
        castAndResolve(match, 1L); // Bear 2/2
        advanceSteps(match, 12); // opponent's FirstMain (turn 2)
        castAndResolve(match, 2L); // Elite 3/3
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
        castAndResolve(match, 1L);
        advanceSteps(match, 12);
        castAndResolve(match, 2L);
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
        castAndResolve(match, 1L);
        advanceSteps(match, 2);
        service().declareAttackers(match.id(), CALLER_PROFILE_ID, List.of(1L));
        advanceSteps(match, 1);

        assertBadRequest(
                () -> service().declareBlockers(match.id(), CALLER_PROFILE_ID, Map.of(99L, 1L)),
                "blocker is not on the battlefield");
    }

    @Test
    void shouldKeepSpellOnStackUntilBothPlayersPass() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        advanceSteps(match, 3);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L, null);

        assertThat(match.stackResolver().stack()).hasSize(1);
        assertThat(you.battlefield()).isEmpty();
        assertThat(you.hand()).hasSize(6);

        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(match.stackResolver().stack()).hasSize(1);
        assertThat(you.battlefield()).isEmpty();

        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(match.stackResolver().stack()).isEmpty();
        assertThat(you.battlefield()).hasSize(1);
    }

    @Test
    void shouldResolveTopOfStackBeforeEarlierSpells() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, instantCard(1L, "Shock"));
        catalog.put(2L, instantCard(2L, "Bolt"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        PlayerState opponent = match.players().get(1);
        advanceSteps(match, 3);

        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L, null);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 2L, null);
        service().passPriority(match.id(), CALLER_PROFILE_ID);
        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(opponent.graveyard())
                .extracting(card -> card.card().getName())
                .containsExactly("Bolt");
        assertThat(you.graveyard()).isEmpty();
        assertThat(match.stackResolver().stack()).hasSize(1);

        service().passPriority(match.id(), CALLER_PROFILE_ID);
        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(you.graveyard())
                .extracting(card -> card.card().getName())
                .containsExactly("Shock");
        assertThat(match.stackResolver().stack()).isEmpty();
    }

    @Test
    void shouldRejectSorcerySpeedCastWithNonEmptyStack() {
        Map<Long, Card> catalog = defaultCatalog();
        catalog.put(1L, instantCard(1L, "Shock"));
        stubMatchDecks(8, catalog);
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);
        service().castSpell(match.id(), CALLER_PROFILE_ID, 1L, null);

        assertBadRequest(
                () -> service().castSpell(match.id(), CALLER_PROFILE_ID, 2L, null),
                "sorcery speed");
    }

    @Test
    void shouldRejectCastWithMissingTarget() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        advanceSteps(match, 3);

        assertBadRequest(
                () ->
                        service()
                                .castSpell(
                                        match.id(),
                                        CALLER_PROFILE_ID,
                                        1L,
                                        new StackObject.Target.PermanentTarget(99L)),
                "target does not exist");
    }

    @Test
    void shouldFizzleWhenTargetLeavesBeforeResolution() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState you = match.players().getFirst();
        you.battlefield()
                .add(
                        new Permanent(
                                new PracticeCard(99L, creatureCard(99L, "Target", "2", "2"), null),
                                you.playerId(),
                                false));
        advanceSteps(match, 3);
        service()
                .castSpell(
                        match.id(),
                        CALLER_PROFILE_ID,
                        1L,
                        new StackObject.Target.PermanentTarget(99L));

        you.battlefield().clear();
        service().passPriority(match.id(), CALLER_PROFILE_ID);
        service().passPriority(match.id(), CALLER_PROFILE_ID);

        assertThat(match.stackResolver().stack()).isEmpty();
        assertThat(you.battlefield()).isEmpty();
        assertThat(you.graveyard()).hasSize(1);
    }

    @Test
    void shouldRejectCastWhenPlayerDoesNotHavePriority() {
        stubMatchDecks(8, defaultCatalog());
        Match match = service().start(request(true, 42L), CALLER_PROFILE_ID);
        PlayerState opponent = match.players().get(1);

        assertThatThrownBy(
                        () ->
                                SpellCasting.castSpell(
                                        match,
                                        match.stackResolver(),
                                        opponent.playerId(),
                                        1L,
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not have priority");
    }
}
