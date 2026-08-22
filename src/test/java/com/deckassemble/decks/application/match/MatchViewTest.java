package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchViewTest {

    @Test
    void shouldShowFullHandAndZonesToViewingPlayer() {
        Match match = newMatch();
        PlayerId caller = match.players().get(0).playerId();

        MatchResponse response = MatchView.forPlayer(match, caller);

        assertThat(response.you().playerId()).isEqualTo(caller);
        assertThat(response.you().hand()).hasSize(2);
        assertThat(response.you().handCount()).isEqualTo(2);
        assertThat(response.you().hand())
                .extracting(MatchResponse.CardView::name)
                .containsExactly("Caller One", "Caller Two");
        assertThat(response.you().life()).isEqualTo(40);
        assertThat(response.you().libraryCount()).isEqualTo(1);
        assertThat(response.you().commander().name()).isEqualTo("Caller Commander");
        assertThat(response.you().commanderTax()).isZero();
    }

    @Test
    void shouldHideOpponentHandButExposeCountsAndPublicZones() {
        Match match = newMatch();
        PlayerId caller = match.players().get(0).playerId();
        PlayerState opponent = match.players().get(1);
        opponent.graveyard().add(practiceCard(60L, "Dead Card"));

        MatchResponse response = MatchView.forPlayer(match, caller);

        assertThat(response.opponent().hand()).isNull();
        assertThat(response.opponent().handCount()).isEqualTo(2);
        assertThat(response.opponent().libraryCount()).isEqualTo(1);
        assertThat(response.opponent().graveyard())
                .extracting(MatchResponse.CardView::name)
                .containsExactly("Dead Card");
    }

    @Test
    void shouldHideCallerHandWhenViewingAsOpponent() {
        Match match = newMatch();
        PlayerId opponent = match.players().get(1).playerId();

        MatchResponse response = MatchView.forPlayer(match, opponent);

        assertThat(response.you().playerId()).isEqualTo(opponent);
        assertThat(response.you().hand()).hasSize(2);
        assertThat(response.opponent().hand()).isNull();
        assertThat(response.opponent().handCount()).isEqualTo(2);
    }

    @Test
    void shouldReportBattlefieldPermanentsWithCombatState() {
        Match match = newMatch();
        PlayerState caller = match.players().get(0);
        Permanent permanent = new Permanent(practiceCard(70L, "Bear"), caller.playerId(), false);
        permanent.tap();
        caller.battlefield().add(permanent);

        MatchResponse response = MatchView.forPlayer(match, caller.playerId());

        assertThat(response.you().battlefield()).hasSize(1);
        MatchResponse.PermanentView view = response.you().battlefield().get(0);
        assertThat(view.tapped()).isTrue();
        assertThat(view.commander()).isFalse();
        assertThat(view.card().name()).isEqualTo("Bear");
        assertThat(view.controller()).isEqualTo(caller.playerId());
    }

    @Test
    void shouldReportTurnStepActivePlayerAndOutcome() {
        Match match = newMatch();
        PlayerId caller = match.players().get(0).playerId();
        match.concede(caller);

        MatchResponse response = MatchView.forPlayer(match, caller);

        assertThat(response.matchId()).isEqualTo(match.id());
        assertThat(response.turnNumber()).isEqualTo(1);
        assertThat(response.step()).isEqualTo("Untap");
        assertThat(response.activePlayerId()).isEqualTo(caller);
        assertThat(response.loser()).isEqualTo(caller);
        assertThat(response.winner()).isEqualTo(match.players().get(1).playerId());
    }

    @Test
    void shouldExposeStackAndPriorityAsPublicInformation() {
        Match match = newMatch();
        PlayerId caller = match.players().get(0).playerId();
        PlayerId opponent = match.players().get(1).playerId();
        match.players().get(1).setAutoPassEnabled(true);
        match.advanceStepNow();
        match.advanceStepNow();
        match.advanceStepNow();
        match.castSpell(11L, new StackObject.Target.PlayerTarget(opponent));

        MatchResponse response = MatchView.forPlayer(match, caller);

        assertThat(response.priorityPlayerId()).isEqualTo(opponent);
        assertThat(response.stack()).hasSize(1);
        MatchResponse.StackObjectView object = response.stack().get(0);
        assertThat(object.card().name()).isEqualTo("Caller One");
        assertThat(object.controller()).isEqualTo(caller);
        assertThat(object.targetPlayerId()).isEqualTo(opponent.value());
        assertThat(object.targetPermanentId()).isNull();
        assertThat(response.you().autoPassEnabled()).isFalse();
        assertThat(response.opponent().autoPassEnabled()).isTrue();
    }

    private Match newMatch() {
        PlayerState caller =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(practiceCard(11L, "Caller One"), practiceCard(12L, "Caller Two")),
                        List.of(practiceCard(13L, "Caller Three")),
                        practiceCard(10L, "Caller Commander"));
        PlayerState opponent =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(
                                practiceCard(21L, "Opponent One"),
                                practiceCard(22L, "Opponent Two")),
                        List.of(practiceCard(23L, "Opponent Three")),
                        practiceCard(20L, "Opponent Commander"));
        return new Match(UUID.randomUUID(), caller, opponent, true);
    }

    private PracticeCard practiceCard(long printingId, String name) {
        return new PracticeCard(printingId, new Card("oracle-" + printingId, name), null);
    }
}
