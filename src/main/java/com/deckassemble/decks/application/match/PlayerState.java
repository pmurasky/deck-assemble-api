package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;

/**
 * One seat's full game state: life, zones (hand, library, battlefield, graveyard, exile, command),
 * commander tax, per-opponent commander damage, and the one-land-per-turn flag.
 */
public final class PlayerState {

    public static final int STARTING_LIFE = 40;
    public static final int COMMANDER_DAMAGE_LIMIT = 21;

    private final PlayerId playerId;
    private final List<PracticeCard> hand;
    private final List<PracticeCard> library;
    private final List<Permanent> battlefield = new ArrayList<>();
    private final List<PracticeCard> graveyard = new ArrayList<>();
    private final List<PracticeCard> exile = new ArrayList<>();
    private final PracticeCard commander;
    private final Map<PlayerId, Integer> commanderDamageReceived = new ConcurrentHashMap<>();
    private int life = STARTING_LIFE;
    private int commanderTax;
    private boolean landPlayedThisTurn;
    private boolean commanderInCommandZone = true;

    public PlayerState(
            PlayerId playerId,
            List<PracticeCard> hand,
            List<PracticeCard> library,
            PracticeCard commander) {
        this.playerId = playerId;
        this.hand = new ArrayList<>(hand);
        this.library = new ArrayList<>(library);
        this.commander = commander;
    }

    /** Applies combat damage to this player, tracking commander-sourced damage separately. */
    public void takeCombatDamage(int amount, @Nullable PlayerId commanderOwner) {
        life -= amount;
        if (commanderOwner != null) {
            commanderDamageReceived.merge(commanderOwner, amount, Integer::sum);
        }
    }

    /** A player loses at 0 life or after taking 21+ combat damage from one commander. */
    public boolean isDefeated() {
        return life <= 0
                || commanderDamageReceived.values().stream()
                        .anyMatch(damage -> damage >= COMMANDER_DAMAGE_LIMIT);
    }

    /** Finds a card in hand by printing id, or throws. */
    public PracticeCard requireInHand(long printingId) {
        return hand.stream()
                .filter(card -> card.printingId() == printingId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("card is not in hand"));
    }

    public PlayerId playerId() {
        return playerId;
    }

    public int life() {
        return life;
    }

    public List<PracticeCard> hand() {
        return hand;
    }

    public List<PracticeCard> library() {
        return library;
    }

    public List<Permanent> battlefield() {
        return battlefield;
    }

    public List<PracticeCard> graveyard() {
        return graveyard;
    }

    public List<PracticeCard> exile() {
        return exile;
    }

    public PracticeCard commander() {
        return commander;
    }

    public int commanderTax() {
        return commanderTax;
    }

    public void incrementCommanderTax() {
        commanderTax += 2;
    }

    public Map<PlayerId, Integer> commanderDamageReceived() {
        return commanderDamageReceived;
    }

    public boolean landPlayedThisTurn() {
        return landPlayedThisTurn;
    }

    public void setLandPlayedThisTurn(boolean landPlayedThisTurn) {
        this.landPlayedThisTurn = landPlayedThisTurn;
    }

    public boolean commanderInCommandZone() {
        return commanderInCommandZone;
    }

    public void setCommanderInCommandZone(boolean commanderInCommandZone) {
        this.commanderInCommandZone = commanderInCommandZone;
    }

    /** Untaps every permanent on this player's battlefield. */
    public void untapAll() {
        battlefield.forEach(Permanent::untap);
    }
}
