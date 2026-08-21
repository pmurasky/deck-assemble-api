package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** Mutable player-directed state of one solitaire practice session. */
final class PracticeSession {

    private final List<Long> shuffledLibrary;
    private final Map<Long, PracticeCard> cardsByPrinting;
    private final List<Long> hand;
    private final List<Permanent> battlefield = new ArrayList<>();
    private final boolean onThePlay;
    private final int mulliganCount;
    private boolean landPlayedThisTurn;
    private int topOfLibrary;
    private int turn;

    PracticeSession(
            MulliganDraw.Result draw, Map<Long, PracticeCard> cardsByPrinting, boolean onThePlay) {
        this.shuffledLibrary = draw.shuffledLibrary();
        this.cardsByPrinting = cardsByPrinting;
        this.onThePlay = onThePlay;
        this.mulliganCount = draw.mulliganCount();
        this.hand =
                new ArrayList<>(
                        shuffledLibrary.subList(0, MulliganDraw.HAND_SIZE - draw.mulliganCount()));
        this.topOfLibrary = MulliganDraw.HAND_SIZE;
    }

    void playCard(long printingId) {
        Card card = card(printingId);
        if (DeckLibraryResolver.isLand(card) && landPlayedThisTurn) {
            throw new IllegalArgumentException("Only one land may be played per turn.");
        }
        if (!hand.remove(printingId)) {
            throw new IllegalArgumentException("Card is not in this practice session's hand.");
        }
        battlefield.add(new Permanent(printingId, false));
        landPlayedThisTurn = landPlayedThisTurn || DeckLibraryResolver.isLand(card);
    }

    void toggleTap(long printingId) {
        for (int index = 0; index < battlefield.size(); index++) {
            Permanent permanent = battlefield.get(index);
            if (permanent.printingId() == printingId) {
                battlefield.set(index, new Permanent(printingId, !permanent.tapped()));
                return;
            }
        }
        throw new IllegalArgumentException("Card is not on this practice session's battlefield.");
    }

    Turn nextTurn() {
        turn++;
        battlefield.replaceAll(permanent -> new Permanent(permanent.printingId(), false));
        landPlayedThisTurn = false;
        PracticeCard drawnCard = draw();
        return new Turn(turn, drawnCard, finished());
    }

    private @Nullable PracticeCard draw() {
        if ((turn == 1 && onThePlay) || topOfLibrary >= shuffledLibrary.size()) {
            return null;
        }
        Long printingId = shuffledLibrary.get(topOfLibrary);
        topOfLibrary++;
        hand.add(printingId);
        return practiceCard(printingId);
    }

    List<PracticeCard> castableSpells() {
        int untappedLands = untappedLands();
        return hand.stream()
                .map(this::practiceCard)
                .filter(card -> isCastable(card, untappedLands))
                .toList();
    }

    private int untappedLands() {
        int count = 0;
        for (Permanent permanent : battlefield) {
            if (!permanent.tapped() && DeckLibraryResolver.isLand(card(permanent.printingId()))) {
                count++;
            }
        }
        return count;
    }

    private boolean isCastable(PracticeCard practiceCard, int untappedLands) {
        Card card = practiceCard.card();
        return !DeckLibraryResolver.isLand(card)
                && CastabilityCalculator.manaValueOf(card) <= untappedLands;
    }

    boolean finished() {
        return topOfLibrary >= shuffledLibrary.size();
    }

    int turn() {
        return turn;
    }

    int mulliganCount() {
        return mulliganCount;
    }

    boolean landPlayedThisTurn() {
        return landPlayedThisTurn;
    }

    int landsInPlay() {
        return untappedLands();
    }

    List<PracticeCard> hand() {
        return hand.stream().map(this::practiceCard).toList();
    }

    List<Permanent> battlefield() {
        return List.copyOf(battlefield);
    }

    Card card(long printingId) {
        return practiceCard(printingId).card();
    }

    PracticeCard practiceCard(long printingId) {
        return Objects.requireNonNull(cardsByPrinting.get(printingId));
    }

    record Permanent(long printingId, boolean tapped) {}

    record Turn(int turn, @Nullable PracticeCard drawnCard, boolean finished) {}
}
