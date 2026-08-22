package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchPriorityTest {

    @Test
    void shouldStartWithActivePlayerHoldingPriority() {
        Match match = newMatch();

        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.activePlayer().playerId());
    }

    @Test
    void shouldPassPriorityToOpponentOnSinglePass() {
        Match match = newMatch();
        PlayerId active = match.activePlayer().playerId();

        match.stackResolver().passPriority(match, active);

        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.players().get(1).playerId());
    }

    @Test
    void shouldRejectPassFromPlayerWithoutPriority() {
        Match match = newMatch();
        PlayerId notHolding = match.players().get(1).playerId();

        assertThatThrownBy(() -> match.stackResolver().passPriority(match, notHolding))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not have priority");
    }

    @Test
    void shouldAdvanceStepWhenBothPassWithEmptyStack() {
        Match match = newMatch();
        PlayerId active = match.activePlayer().playerId();

        match.stackResolver().passPriority(match, active);
        match.stackResolver().passPriority(match, match.players().get(1).playerId());

        assertThat(match.step()).isEqualTo(new TurnStep.Upkeep());
        assertThat(match.stackResolver().priorityHolder()).isEqualTo(active);
    }

    @Test
    void shouldResolveTopSpellWhenBothPassWithStackOnIt() {
        Match match = newMatch();
        PlayerId active = match.activePlayer().playerId();
        PracticeCard bolt = practiceCard(50L, "Bolt", "Instant");
        match.stackResolver().push(new StackObject(active, bolt, null));

        match.stackResolver().passPriority(match, active);
        match.stackResolver().passPriority(match, match.players().get(1).playerId());

        assertThat(match.stackResolver().stack()).isEmpty();
        assertThat(match.players().get(0).graveyard())
                .extracting(PracticeCard::card)
                .extracting(Card::getName)
                .containsExactly("Bolt");
        assertThat(match.stackResolver().priorityHolder()).isEqualTo(active);
    }

    @Test
    void shouldPutCreatureOnBattlefieldWhenItResolves() {
        Match match = newMatch();
        PlayerState active = match.players().get(0);
        PracticeCard bear = practiceCard(51L, "Bear", "Creature — Bear");
        match.stackResolver().push(new StackObject(active.playerId(), bear, null));

        match.stackResolver().passPriority(match, active.playerId());
        match.stackResolver().passPriority(match, match.players().get(1).playerId());

        assertThat(active.battlefield()).hasSize(1);
        assertThat(active.battlefield().get(0).card()).isEqualTo(bear);
        assertThat(active.battlefield().get(0).tapped()).isFalse();
    }

    @Test
    void shouldFizzleWhenTargetMissingAtResolution() {
        Match match = newMatch();
        PlayerId active = match.activePlayer().playerId();
        PracticeCard bolt = practiceCard(52L, "Bolt", "Instant");
        StackObject.Target target = new StackObject.Target.PermanentTarget(99L);
        match.stackResolver().push(new StackObject(active, bolt, target));

        match.stackResolver().passPriority(match, active);
        match.stackResolver().passPriority(match, match.players().get(1).playerId());

        assertThat(match.stackResolver().stack()).isEmpty();
        assertThat(match.players().get(0).battlefield()).isEmpty();
        assertThat(match.players().get(0).graveyard()).hasSize(1);
    }

    @Test
    void shouldAdvanceIntoNewTurnWhenBothPassThroughCleanup() {
        Match match = newMatch();

        for (int step = 0; step < 12; step++) {
            match.stackResolver().passPriority(match, match.stackResolver().priorityHolder());
            match.stackResolver().passPriority(match, match.stackResolver().priorityHolder());
        }

        assertThat(match.turnNumber()).isEqualTo(2);
        assertThat(match.activePlayer().playerId()).isEqualTo(match.players().get(1).playerId());
        assertThat(match.stackResolver().priorityHolder())
                .isEqualTo(match.players().get(1).playerId());
    }

    private Match newMatch() {
        PlayerState caller =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(practiceCard(11L, "Caller One", "Instant")),
                        List.of(practiceCard(12L, "Caller Two", "Instant")),
                        practiceCard(10L, "Caller Commander", "Creature — Legend"));
        PlayerState opponent =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(practiceCard(21L, "Opponent One", "Instant")),
                        List.of(practiceCard(22L, "Opponent Two", "Instant")),
                        practiceCard(20L, "Opponent Commander", "Creature — Legend"));
        return new Match(UUID.randomUUID(), caller, opponent, true);
    }

    private PracticeCard practiceCard(long printingId, String name, String typeLine) {
        Card card = new Card("oracle-" + printingId, name);
        card.setTypeLine(typeLine);
        return new PracticeCard(printingId, card, null);
    }
}
