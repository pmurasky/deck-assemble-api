package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Read model of a match tailored to one player's perspective. */
public record MatchResponse(
        UUID matchId,
        int turnNumber,
        String step,
        PlayerId activePlayerId,
        PlayerView you,
        PlayerView opponent,
        @Nullable PlayerId winner,
        @Nullable PlayerId loser,
        List<StackObjectView> stack,
        PlayerId priorityPlayerId) {

    /** Public card data as exposed to a player. */
    public record CardView(
            long printingId,
            String name,
            @Nullable String imageUrl,
            @Nullable String manaCost,
            @Nullable String typeLine,
            @Nullable String oracleText) {

        static CardView of(PracticeCard card) {
            return new CardView(
                    card.printingId(),
                    card.card().getName(),
                    card.imageUrl(),
                    card.card().getManaCost(),
                    card.card().getTypeLine(),
                    card.card().getOracleText());
        }
    }

    /** Public battlefield permanent data. */
    public record PermanentView(
            long printingId,
            CardView card,
            PlayerId controller,
            boolean commander,
            boolean tapped,
            int power,
            int toughness) {

        static PermanentView of(Permanent permanent) {
            return new PermanentView(
                    permanent.card().printingId(),
                    CardView.of(permanent.card()),
                    permanent.controller(),
                    permanent.commander(),
                    permanent.tapped(),
                    permanent.power(),
                    permanent.toughness());
        }
    }

    /** One spell on the stack; the stack is a public zone. */
    public record StackObjectView(
            CardView card,
            PlayerId controller,
            @Nullable Long targetPermanentId,
            @Nullable UUID targetPlayerId) {}

    /** Per-player zone view; hand contents are only populated for the viewing player. */
    public record PlayerView(
            PlayerId playerId,
            int life,
            int handCount,
            int libraryCount,
            @Nullable List<CardView> hand,
            List<PermanentView> battlefield,
            List<CardView> graveyard,
            List<CardView> exile,
            CardView commander,
            int commanderTax,
            Map<PlayerId, Integer> commanderDamageReceived,
            boolean landPlayedThisTurn,
            boolean autoPassEnabled) {}
}
