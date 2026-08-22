package com.deckassemble.decks.application.match;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A two-player Commander match: the two seats, whose turn it is, the current step, and the loser. */
public final class Match {

    private final UUID id;
    private final List<PlayerState> players;
    private int activePlayerIndex;
    private int turnNumber = 1;
    private TurnStep step = new TurnStep.Untap();
    // The player on the play skips their first Draw step; cleared once that step has passed.
    private boolean initialDrawSkipPending = true;
    private @Nullable PlayerId loser;

    public Match(UUID id, PlayerState first, PlayerState second, boolean firstOnThePlay) {
        this.id = id;
        this.players = List.of(first, second);
        this.activePlayerIndex = firstOnThePlay ? 0 : 1;
    }

    public PlayerState player(PlayerId playerId) {
        return players.stream()
                .filter(player -> player.playerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("player is not part of this match"));
    }

    public PlayerState opponentOf(PlayerId playerId) {
        PlayerState player = player(playerId);
        return players.getFirst() == player ? players.get(1) : players.getFirst();
    }

    public void markLoser(PlayerId playerId) {
        loser = player(playerId).playerId();
    }

    public UUID id() {
        return id;
    }

    public List<PlayerState> players() {
        return players;
    }

    public PlayerState activePlayer() {
        return players.get(activePlayerIndex);
    }

    public int activePlayerIndex() {
        return activePlayerIndex;
    }

    void setActivePlayerIndex(int activePlayerIndex) {
        this.activePlayerIndex = activePlayerIndex;
    }

    public int turnNumber() {
        return turnNumber;
    }

    void incrementTurnNumber() {
        turnNumber++;
    }

    public TurnStep step() {
        return step;
    }

    void setStep(TurnStep step) {
        this.step = step;
    }

    public boolean initialDrawSkipPending() {
        return initialDrawSkipPending;
    }

    void clearInitialDrawSkip() {
        initialDrawSkipPending = false;
    }

    public @Nullable PlayerId loser() {
        return loser;
    }
}
