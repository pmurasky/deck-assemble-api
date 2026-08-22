package com.deckassemble.decks.application.match;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** One player action submitted to a match. */
public record MatchActionRequest(
        @NotNull MatchActionType type,
        @Nullable Long printingId,
        @Nullable List<Long> attackerIds,
        @Nullable Map<Long, Long> blockerAssignments,
        @Nullable Long targetPermanentId,
        @Nullable UUID targetPlayerId,
        @Nullable Boolean autoPassEnabled) {

    /** The action kinds a player can submit. */
    public enum MatchActionType {
        PLAY_LAND,
        CAST_SPELL,
        PASS_PRIORITY,
        SET_AUTO_PASS,
        DECLARE_ATTACKERS,
        DECLARE_BLOCKERS,
        CONCEDE
    }
}
