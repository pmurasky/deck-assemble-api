package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.PracticeCard;
import org.jspecify.annotations.Nullable;

/**
 * A card on a player's battlefield: the card itself, its controller, tapped state, whether it is
 * that player's commander, and its combat stats parsed to concrete integers.
 */
public final class Permanent {

    private final PracticeCard card;
    private final PlayerId controller;
    private final boolean commander;
    private final int power;
    private final int toughness;
    private boolean tapped;

    public Permanent(PracticeCard card, PlayerId controller, boolean commander) {
        this.card = card;
        this.controller = controller;
        this.commander = commander;
        this.power = parseStat(card, "power", card.card().getPower());
        this.toughness = parseStat(card, "toughness", card.card().getToughness());
    }

    /** Fails fast when a deck card's power/toughness is not numeric (checked at match start). */
    public static void validateParseable(PracticeCard card) {
        parseStat(card, "power", card.card().getPower());
        parseStat(card, "toughness", card.card().getToughness());
    }

    // Scryfall ships non-numeric stats ("*", "1+*"); match combat needs concrete ints, so a card
    // with a non-numeric stat rejects the whole deck at start (the parse-or-reject policy #47
    // builds on). Null stats are normal for non-creature cards and read as 0.
    private static int parseStat(PracticeCard card, String stat, @Nullable String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Card '"
                            + card.card().getName()
                            + "' has non-numeric "
                            + stat
                            + " '"
                            + value
                            + "' and cannot be used in a match");
        }
    }

    public PracticeCard card() {
        return card;
    }

    public PlayerId controller() {
        return controller;
    }

    public boolean commander() {
        return commander;
    }

    public int power() {
        return power;
    }

    public int toughness() {
        return toughness;
    }

    public boolean tapped() {
        return tapped;
    }

    public void tap() {
        tapped = true;
    }

    public void untap() {
        tapped = false;
    }
}
