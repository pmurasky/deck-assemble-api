package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import org.jspecify.annotations.Nullable;

/** One spell on the stack: its controller, the card, and an optional single target. */
public record StackObject(PlayerId controller, PracticeCard card, @Nullable Target target) {

    /** Minimal cast-time target: a permanent printing id or a player. */
    public sealed interface Target {

        /** True when the target no longer exists, so the spell fizzles at resolution. */
        boolean missingFrom(Match match);

        /** Targets a permanent by printing id. */
        record PermanentTarget(long printingId) implements Target {
            @Override
            public boolean missingFrom(Match match) {
                return match.players().stream()
                        .flatMap(player -> player.battlefield().stream())
                        .noneMatch(permanent -> permanent.card().printingId() == printingId);
            }
        }

        /** Targets a player. */
        record PlayerTarget(PlayerId playerId) implements Target {
            @Override
            public boolean missingFrom(Match match) {
                return match.players().stream()
                        .noneMatch(player -> player.playerId().equals(playerId));
            }
        }
    }
}
