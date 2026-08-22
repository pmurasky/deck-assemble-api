package com.deckassemble.decks.application.match;

import java.util.Objects;
import java.util.UUID;

/**
 * Identity of one seat in a match. Minimal for now: a random per-match id per seat, unrelated to
 * profile/database ids (in the hot-seat model the opponent seat has no authenticated profile). The
 * value-type spec is owned by the targeting issue (#46) and may evolve there.
 */
public record PlayerId(UUID value) {

    public PlayerId {
        Objects.requireNonNull(value, "value");
    }

    public static PlayerId newId() {
        return new PlayerId(UUID.randomUUID());
    }
}
