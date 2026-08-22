package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StackObjectTest {

    @Test
    void shouldReportPermanentTargetMissingWhenNotOnAnyBattlefield() {
        Match match = newMatch();
        StackObject.Target target = new StackObject.Target.PermanentTarget(99L);

        assertThat(target.missingFrom(match)).isTrue();
    }

    @Test
    void shouldReportPermanentTargetPresentWhenOnBattlefield() {
        Match match = newMatch();
        PlayerState caller = match.players().get(0);
        caller.battlefield()
                .add(new Permanent(practiceCard(99L, "Bear"), caller.playerId(), false));
        StackObject.Target target = new StackObject.Target.PermanentTarget(99L);

        assertThat(target.missingFrom(match)).isFalse();
    }

    @Test
    void shouldReportPlayerTargetMissingWhenNotInMatch() {
        Match match = newMatch();
        StackObject.Target target = new StackObject.Target.PlayerTarget(PlayerId.newId());

        assertThat(target.missingFrom(match)).isTrue();
    }

    @Test
    void shouldReportPlayerTargetPresentWhenInMatch() {
        Match match = newMatch();
        PlayerId opponent = match.players().get(1).playerId();
        StackObject.Target target = new StackObject.Target.PlayerTarget(opponent);

        assertThat(target.missingFrom(match)).isFalse();
    }

    private Match newMatch() {
        PlayerState caller =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(practiceCard(11L, "Caller One")),
                        List.of(practiceCard(12L, "Caller Two")),
                        practiceCard(10L, "Caller Commander"));
        PlayerState opponent =
                new PlayerState(
                        PlayerId.newId(),
                        List.of(practiceCard(21L, "Opponent One")),
                        List.of(practiceCard(22L, "Opponent Two")),
                        practiceCard(20L, "Opponent Commander"));
        return new Match(UUID.randomUUID(), caller, opponent, true);
    }

    private PracticeCard practiceCard(long printingId, String name) {
        return new PracticeCard(printingId, new Card("oracle-" + printingId, name), null);
    }
}
