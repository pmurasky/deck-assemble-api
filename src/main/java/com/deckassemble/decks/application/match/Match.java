package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.decks.application.simulation.DeckLibraryResolver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A two-player Commander match: the two seats, whose turn it is, the current step, and the legal
 * moves each seat can take. Damage, triggers, and real priority arrive in later issues; this class
 * models sorcery-speed plays and the turn cycle only.
 */
public final class Match {

    private final UUID id;
    private final List<PlayerState> players;
    private final List<Permanent> pendingAttackers = new ArrayList<>();
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

    /** Plays a land from the active player's hand: main step only, one per turn. */
    public void playLand(long printingId) {
        requireInProgress();
        requireMainStep();
        PlayerState player = activePlayer();
        if (player.landPlayedThisTurn()) {
            throw new IllegalArgumentException("already played a land this turn");
        }
        PracticeCard card = findInHand(player, printingId);
        if (!DeckLibraryResolver.isLand(card.card())) {
            throw new IllegalArgumentException("card is not a land: " + card.card().getName());
        }
        player.hand().remove(card);
        player.battlefield().add(new Permanent(card, player.playerId(), false));
        player.setLandPlayedThisTurn(true);
    }

    /**
     * Casts a spell from the active player's hand (or their commander from the command zone,
     * paying the commander tax) at sorcery speed. It resolves immediately: creatures and other
     * permanents enter the battlefield, instants and sorceries go to the graveyard. No mana costs
     * are modeled yet.
     */
    public void castSpell(long printingId) {
        requireInProgress();
        requireMainStep();
        PlayerState player = activePlayer();
        if (player.commanderInCommandZone() && player.commander().printingId() == printingId) {
            castCommanderFromCommandZone(player);
            return;
        }
        PracticeCard card = findInHand(player, printingId);
        if (DeckLibraryResolver.isLand(card.card())) {
            throw new IllegalArgumentException(
                    "lands are played, not cast: " + card.card().getName());
        }
        player.hand().remove(card);
        resolveSpell(player, card);
    }

    private void castCommanderFromCommandZone(PlayerState player) {
        player.incrementCommanderTax();
        player.setCommanderInCommandZone(false);
        player.battlefield().add(new Permanent(player.commander(), player.playerId(), true));
    }

    private void resolveSpell(PlayerState player, PracticeCard card) {
        String typeLine = card.card().getTypeLine();
        if (typeLine != null && (typeLine.contains("Instant") || typeLine.contains("Sorcery"))) {
            player.graveyard().add(card);
        } else {
            player.battlefield().add(new Permanent(card, player.playerId(), false));
        }
    }

    /** Moves to the next step, starting a new turn (untap, switch active player, draw) on wrap. */
    public void advanceStep() {
        requireInProgress();
        TurnStep next = step.next();
        if (next instanceof TurnStep.Untap) {
            beginNewTurn();
        }
        step = next;
        if (next instanceof TurnStep.Draw) {
            drawForActivePlayer();
        }
    }

    private void beginNewTurn() {
        turnNumber++;
        activePlayerIndex = 1 - activePlayerIndex;
        PlayerState active = activePlayer();
        active.untapAll();
        active.setLandPlayedThisTurn(false);
        pendingAttackers.clear();
    }

    private void drawForActivePlayer() {
        if (initialDrawSkipPending) {
            initialDrawSkipPending = false;
            return;
        }
        PlayerState active = activePlayer();
        if (active.library().isEmpty()) {
            markLoser(active.playerId());
            return;
        }
        active.hand().add(active.library().removeFirst());
    }

    /** The given seat concedes; the match is over. */
    public void concede(PlayerId seat) {
        requireInProgress();
        markLoser(seat);
    }

    /**
     * Declares the active player's attackers: each must be one of their battlefield permanents and
     * taps. Damage assignment order and keywords arrive in #47.
     */
    public void declareAttackers(List<Long> printingIds) {
        requireInProgress();
        requireStep(new TurnStep.DeclareAttackers(), "declare attackers");
        PlayerState attacker = activePlayer();
        for (long printingId : printingIds) {
            Permanent creature = findAttacker(attacker, printingId);
            creature.tap();
            pendingAttackers.add(creature);
        }
    }

    /**
     * Declares blockers (blocker printing -> attacker printing) for the defending player and
     * resolves combat damage immediately: each attacker assigns lethal to its blockers in order
     * (excess is lost without trample), blockers deal their power back, unblocked attackers hit
     * the defender, and destroyed creatures go to the graveyard.
     */
    public void declareBlockers(Map<Long, Long> blockerToAttacker) {
        requireInProgress();
        requireStep(new TurnStep.DeclareBlockers(), "declare blockers");
        PlayerState defender = opponentOf(activePlayer().playerId());
        validateBlockers(defender, blockerToAttacker);
        resolveCombatDamage(defender, blockerToAttacker);
        pendingAttackers.clear();
        markDefeatedPlayers();
    }

    private Permanent findAttacker(PlayerState attacker, long printingId) {
        return attacker.battlefield().stream()
                .filter(permanent -> permanent.card().printingId() == printingId)
                .filter(permanent -> !pendingAttackers.contains(permanent))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("attacker is not on the battlefield"));
    }

    private void validateBlockers(PlayerState defender, Map<Long, Long> blockerToAttacker) {
        for (Map.Entry<Long, Long> assignment : blockerToAttacker.entrySet()) {
            findOnBattlefield(defender, assignment.getKey(), "blocker is not on the battlefield");
            boolean attacking =
                    pendingAttackers.stream()
                            .anyMatch(
                                    attacker ->
                                            attacker.card().printingId()
                                                    == assignment.getValue());
            if (!attacking) {
                throw new IllegalArgumentException("block target is not attacking");
            }
        }
    }

    private void resolveCombatDamage(PlayerState defender, Map<Long, Long> blockerToAttacker) {
        Map<Permanent, Integer> damageMarked = new HashMap<>();
        for (Permanent attacker : pendingAttackers) {
            List<Permanent> blockers = blockersFor(defender, attacker, blockerToAttacker);
            if (blockers.isEmpty()) {
                defender.takeCombatDamage(
                        attacker.power(), attacker.commander() ? attacker.controller() : null);
            } else {
                assignDamage(attacker, blockers, damageMarked);
            }
        }
        buryDestroyedPermanents(damageMarked);
    }

    private List<Permanent> blockersFor(
            PlayerState defender, Permanent attacker, Map<Long, Long> blockerToAttacker) {
        return blockerToAttacker.entrySet().stream()
                .filter(assignment -> assignment.getValue() == attacker.card().printingId())
                .map(
                        assignment ->
                                findOnBattlefield(
                                        defender, assignment.getKey(), "blocker not found"))
                .toList();
    }

    private void assignDamage(
            Permanent attacker, List<Permanent> blockers, Map<Permanent, Integer> damageMarked) {
        int remaining = attacker.power();
        for (Permanent blocker : blockers) {
            damageMarked.merge(attacker, blocker.power(), Integer::sum);
            if (remaining <= 0) {
                continue;
            }
            int assigned = Math.min(blocker.toughness(), remaining);
            damageMarked.merge(blocker, assigned, Integer::sum);
            remaining -= assigned;
        }
    }

    private void buryDestroyedPermanents(Map<Permanent, Integer> damageMarked) {
        for (PlayerState player : players) {
            List<Permanent> destroyed =
                    player.battlefield().stream()
                            .filter(
                                    permanent ->
                                            damageMarked.getOrDefault(permanent, 0)
                                                    >= permanent.toughness())
                            .toList();
            player.battlefield().removeAll(destroyed);
            destroyed.forEach(permanent -> player.graveyard().add(permanent.card()));
        }
    }

    private Permanent findOnBattlefield(PlayerState player, long printingId, String error) {
        return player.battlefield().stream()
                .filter(permanent -> permanent.card().printingId() == printingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(error));
    }

    private void markDefeatedPlayers() {
        for (PlayerState player : players) {
            if (loser == null && player.isDefeated()) {
                markLoser(player.playerId());
            }
        }
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

    private static PracticeCard findInHand(PlayerState player, long printingId) {
        return player.hand().stream()
                .filter(card -> card.printingId() == printingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("card is not in hand"));
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

    public int turnNumber() {
        return turnNumber;
    }

    public TurnStep step() {
        return step;
    }

    public boolean initialDrawSkipPending() {
        return initialDrawSkipPending;
    }

    public @Nullable PlayerId loser() {
        return loser;
    }

    public @Nullable PlayerId winner() {
        return loser == null ? null : opponentOf(loser).playerId();
    }
}
