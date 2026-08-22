package com.deckassemble.decks.application.match;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** One player action submitted to a match. */
public record MatchActionRequest(
        @NotNull MatchActionType type,
        @Nullable Long printingId,
        @Nullable List<Long> attackerIds,
        @Nullable Map<Long, Long> blockerAssignments) {

    /** The action kinds a player can submit. */
    public enum MatchActionType {
        PLAY_LAND,
        CAST_SPELL,
        ADVANCE_STEP,
        DECLARE_ATTACKERS,
        DECLARE_BLOCKERS,
        CONCEDE
    }
}
