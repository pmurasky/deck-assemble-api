package com.deckassemble.decks.application.match;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A two-player Commander match: zones, life totals, commander damage, and the turn structure.
 * Hidden information is enforced by the view layer, not here.
 */
public final class Match {

    private final UUID id;
    private final List<PlayerState> players;
    private final CombatResolver combat = new CombatResolver();
    private final StackResolver stackResolver;
    private int activePlayerIndex;
    private int turnNumber = 1;
    private TurnStep step = new TurnStep.Untap();
    // The player on the play skips their first Draw step.
    private boolean initialDrawSkipPending = true;
    @Nullable private PlayerId loser;

    public Match(UUID id, PlayerState first, PlayerState second, boolean firstOnThePlay) {
        this.id = id;
        this.players = List.of(first, second);
        this.activePlayerIndex = firstOnThePlay ? 0 : 1;
        this.stackResolver = new StackResolver(activePlayer().playerId());
    }

    public PlayerState player(PlayerId playerId) {
        return players.stream()
                .filter(player -> player.playerId().equals(playerId))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("player is not part of this match"));
    }

    public PlayerState opponentOf(PlayerId playerId) {
        PlayerState player = player(playerId);
        return players.getFirst().equals(player) ? players.get(1) : players.getFirst();
    }

    public void markLoser(PlayerId playerId) {
        player(playerId);
        loser = playerId;
    }

    /** Plays a land from the active player's hand onto their battlefield. */
    public void playLand(long printingId) {
        requireInProgress();
        requireMainStep();
        MainPhaseActions.playLand(activePlayer(), printingId);
    }

    /**
     * Casts a spell for the player holding priority: commanders from the command zone (adding tax),
     * other cards from hand. The spell goes on the stack and resolves once both players pass
     * priority in succession.
     */
    public void castSpell(long printingId, StackObject.@Nullable Target target) {
        requireInProgress();
        SpellCasting.castSpell(
                this, stackResolver, stackResolver.priorityHolder(), printingId, target);
    }

    /** Advances to the next step, beginning a new turn for the opponent after Cleanup. */
    void advanceStepNow() {
        requireInProgress();
        TurnStep next = step.next();
        if (next instanceof TurnStep.Untap) {
            turnNumber++;
            activePlayerIndex = 1 - activePlayerIndex;
            PlayerState active = activePlayer();
            active.untapAll();
            active.setLandPlayedThisTurn(false);
            combat.reset();
        }
        step = next;
        if (next instanceof TurnStep.Draw) {
            drawForActivePlayer();
        }
        stackResolver.resetPriority(activePlayer().playerId());
    }

    /** Draws for the active player, skipping the first draw of the player on the play. */
    public void drawForActivePlayer() {
        if (initialDrawSkipPending) {
            initialDrawSkipPending = false;
            return;
        }
        PlayerState player = activePlayer();
        if (player.library().isEmpty()) {
            markLoser(player.playerId());
            return;
        }
        player.hand().add(player.library().removeFirst());
    }

    /** The given seat concedes; the match is over. */
    public void concede(PlayerId seat) {
        markLoser(seat);
    }

    /** The active player declares attackers (printing ids); they tap and wait for blocks. */
    public void declareAttackers(List<Long> printingIds) {
        requireInProgress();
        requireStep(new TurnStep.DeclareAttackers(), "declare attackers");
        combat.declareAttackers(activePlayer(), printingIds);
    }

    /**
     * The defending player declares blockers (blocker printing id to attacker printing id) and
     * combat damage resolves immediately with auto-assigned damage order.
     */
    public void declareBlockers(Map<Long, Long> blockerToAttacker) {
        requireInProgress();
        requireStep(new TurnStep.DeclareBlockers(), "declare blockers");
        combat.declareBlockers(
                activePlayer(), opponentOf(activePlayer().playerId()), blockerToAttacker, this);
    }

    private void requireStep(TurnStep required, String action) {
        if (!step.equals(required)) {
            throw new IllegalArgumentException(
                    "cannot " + action + " during the " + step.stepName() + " step");
        }
    }

    private void requireInProgress() {
        if (loser != null) {
            throw new IllegalArgumentException("match is over");
        }
    }

    private void requireMainStep() {
        if (!(step instanceof TurnStep.FirstMain || step instanceof TurnStep.SecondMain)) {
            throw new IllegalArgumentException("action is only legal during a main step");
        }
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

    public int turnNumber() {
        return turnNumber;
    }

    public TurnStep step() {
        return step;
    }

    @Nullable public PlayerId loser() {
        return loser;
    }

    StackResolver stackResolver() {
        return stackResolver;
    }
}
