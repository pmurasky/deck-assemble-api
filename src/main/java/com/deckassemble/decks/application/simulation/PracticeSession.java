package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.domain.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Mutable state of one solitaire practice session — the turn-stepped state machine: each {@link
 * #step()} performs draw (skipped on turn 1 when on the play, and once the library is exhausted),
 * land drop (the first land in hand, one per turn), then reports the in-hand spells castable by
 * mana value. Deterministic: the library was shuffled once at session start by {@link MulliganDraw}
 * from the session seed, and steps only consume it in order. Solitaire goldfishing only — no
 * opponent, no stack, no combat.
 */
final class PracticeSession {

    private final List<Long> shuffledLibrary;
    private final Map<Long, Card> cardsByPrinting;
    private final List<Long> hand;
    private final List<Card> landsInPlay = new ArrayList<>();
    private final boolean onThePlay;
    private final int mulliganCount;
    private int topOfLibrary;
    private int turn;

    PracticeSession(
            MulliganDraw.Result draw, Map<Long, Card> cardsByPrinting, boolean onThePlay) {
        this.shuffledLibrary = draw.shuffledLibrary();
        this.cardsByPrinting = cardsByPrinting;
        this.onThePlay = onThePlay;
        this.mulliganCount = draw.mulliganCount();
        this.hand =
                new ArrayList<>(
                        shuffledLibrary.subList(0, MulliganDraw.HAND_SIZE - draw.mulliganCount()));
        this.topOfLibrary = MulliganDraw.HAND_SIZE;
    }

    /** The outcome of advancing one turn. */
    record Step(
            int turn,
            @Nullable String drawnCard,
            @Nullable String landPlayed,
            int landsInPlay,
            List<String> castableSpells,
            boolean finished) {}

    Step step() {
        turn++;
        String drawnCard = draw();
        String landPlayed = playLand();
        return new Step(
                turn,
                drawnCard,
                landPlayed,
                landsInPlay.size(),
                castableSpells(),
                topOfLibrary >= shuffledLibrary.size());
    }

    private @Nullable String draw() {
        if ((turn == 1 && onThePlay) || topOfLibrary >= shuffledLibrary.size()) {
            return null;
        }
        Long printingId = shuffledLibrary.get(topOfLibrary);
        topOfLibrary++;
        hand.add(printingId);
        return nameOf(printingId);
    }

    private @Nullable String playLand() {
        for (int i = 0; i < hand.size(); i++) {
            Card card = Objects.requireNonNull(cardsByPrinting.get(hand.get(i)));
            if (DeckLibraryResolver.isLand(card)) {
                hand.remove(i);
                landsInPlay.add(card);
                return card.getName();
            }
        }
        return null;
    }

    private List<String> castableSpells() {
        List<String> names = new ArrayList<>();
        for (Long printingId : hand) {
            Card card = Objects.requireNonNull(cardsByPrinting.get(printingId));
            if (!DeckLibraryResolver.isLand(card)
                    && CastabilityCalculator.manaValueOf(card) <= landsInPlay.size()) {
                names.add(card.getName());
            }
        }
        return names;
    }

    int turn() {
        return turn;
    }

    int mulliganCount() {
        return mulliganCount;
    }

    List<String> handNames() {
        return hand.stream().map(this::nameOf).toList();
    }

    private String nameOf(Long printingId) {
        return Objects.requireNonNull(cardsByPrinting.get(printingId)).getName();
    }
}
