package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.domain.Card;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Shared London-mulligan draw logic for {@link DeckSampleHandService} and {@link
 * DeckSimulationService}: fully shuffles a library and, for {@link
 * MulliganStrategy#LONDON_LAND_RANGE}, redraws until the opening 7 cards' land count falls within
 * the requested range (or a small attempt cap is hit). Returns the *whole* shuffled library, not
 * just the opening 7, so a caller that needs more than a static hand — e.g. per-turn draws in a
 * Monte Carlo simulation — can keep reading from where the opening hand left off, using the same
 * shuffle and the same random-number sequence.
 */
final class MulliganDraw {

    static final int HAND_SIZE = 7;

    // ponytail: cap London mulligans at 3 (bottoming down to a 4-card hand) instead of looping
    // until the land range is satisfied or the hand is mulliganed away entirely. Bounds worst-case
    // runtime against a land range a pathological library (e.g. all lands, or none) can never
    // satisfy; the loop keeps the last drawn hand once the cap is hit. Raise if playtesters need
    // deeper mulligans modeled.
    private static final int MAX_MULLIGANS = 3;

    private MulliganDraw() {}

    /** The full shuffled library for one drawn game, plus how many London mulligans it took. */
    record Result(List<Long> shuffledLibrary, int mulliganCount) {}

    static void validateLandRange(MulliganRequest request) {
        if (request.mulliganStrategy() != MulliganStrategy.LONDON_LAND_RANGE) {
            return;
        }
        if (!hasValidLandRange(request)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minimumLands and maximumLands must both be set between 0 and "
                            + HAND_SIZE
                            + " (minimumLands <= maximumLands) for LONDON_LAND_RANGE.");
        }
    }

    private static boolean hasValidLandRange(MulliganRequest request) {
        Integer min = request.minimumLands();
        Integer max = request.maximumLands();
        return min != null && max != null && min >= 0 && max <= HAND_SIZE && min <= max;
    }

    static Result draw(
            List<Long> library,
            Map<Long, Card> cardsByPrinting,
            RandomGenerator random,
            MulliganRequest request) {
        if (request.mulliganStrategy() != MulliganStrategy.LONDON_LAND_RANGE) {
            return new Result(shuffle(library, random), 0);
        }
        return drawWithLondonMulligan(
                library,
                cardsByPrinting,
                random,
                Objects.requireNonNull(request.minimumLands()),
                Objects.requireNonNull(request.maximumLands()));
    }

    private static Result drawWithLondonMulligan(
            List<Long> library,
            Map<Long, Card> cardsByPrinting,
            RandomGenerator random,
            int minimumLands,
            int maximumLands) {
        List<Long> shuffled;
        int mulliganCount = 0;
        while (true) {
            shuffled = shuffle(library, random);
            long lands =
                    shuffled.subList(0, HAND_SIZE).stream()
                            .filter(id -> DeckLibraryResolver.isLand(cardsByPrinting.get(id)))
                            .count();
            boolean withinRange = lands >= minimumLands && lands <= maximumLands;
            if (withinRange || mulliganCount >= MAX_MULLIGANS) {
                break;
            }
            mulliganCount++;
        }
        return new Result(shuffled, mulliganCount);
    }

    private static List<Long> shuffle(List<Long> library, RandomGenerator random) {
        List<Long> shuffled = new ArrayList<>(library);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            Collections.swap(shuffled, i, random.nextInt(i + 1));
        }
        return shuffled;
    }
}
