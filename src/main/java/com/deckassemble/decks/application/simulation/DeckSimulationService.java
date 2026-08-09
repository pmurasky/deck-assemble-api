package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs a Monte Carlo consistency simulation over a deck revision's snapshot: {@code iterations}
 * independent seeded games, each drawing an opening hand (mulligan mechanics shared with {@link
 * DeckSampleHandService} via {@link MulliganDraw}) and {@code turns} of subsequent draws, then
 * aggregates land-drop, color-availability, cards-seen, and castability statistics across every
 * game. The library and card facts are resolved once and reused for every iteration — only the
 * shuffle changes. Statistical goldfishing only — no card-text execution, no stack/priority/combat
 * logic.
 */
@Service
// Justified: method-local/instance maps below are never shared across threads (one
// DeckSimulationService.simulate() call runs single-threaded, same as ManaCurveCalculator).
@SuppressWarnings("PMD.UseConcurrentHashMap")
public class DeckSimulationService {

    private static final int HAND_SIZE = MulliganDraw.HAND_SIZE;
    private static final List<String> COLORS = List.of("W", "U", "B", "R", "G");

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    public DeckSimulationService(
            DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    public DeckSimulationResponse simulate(long deckId, DeckSimulationRequest request) {
        MulliganDraw.validateLandRange(request);
        ResolvedLibrary resolved = resolveLibrary(deckId, request);
        long seed =
                request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(seed);

        Accumulators accumulators = new Accumulators(request.turns());
        for (int i = 0; i < request.iterations(); i++) {
            simulateGame(
                    resolved.library(), resolved.cardsByPrinting(), random, request, accumulators);
        }
        return accumulators.toResponse(seed, request.iterations());
    }

    private ResolvedLibrary resolveLibrary(long deckId, DeckSimulationRequest request) {
        DeckSnapshot snapshot = deckRevisionService.snapshotAt(deckId, request.revision());
        List<DeckSnapshot.CardEntry> mainDeckEntries =
                DeckLibraryResolver.mainDeckEntries(snapshot);
        Map<Long, Card> cardsByPrinting =
                cardCatalogService.getCardsByPrintingIds(
                        DeckLibraryResolver.printingIdsOf(mainDeckEntries));
        List<Long> library =
                DeckLibraryResolver.expandLibrary(mainDeckEntries, cardsByPrinting, snapshot);
        int maxDraws = request.onThePlay() ? request.turns() - 1 : request.turns();
        validateLibrarySize(library, maxDraws);
        return new ResolvedLibrary(library, cardsByPrinting);
    }

    /** The expanded library and resolved card facts a simulation run repeatedly draws from. */
    private record ResolvedLibrary(List<Long> library, Map<Long, Card> cardsByPrinting) {}

    private static void validateLibrarySize(List<Long> library, int maxDraws) {
        int required = HAND_SIZE + maxDraws;
        if (library.size() < required) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Deck library has only "
                            + library.size()
                            + " card(s) after excluding commander(s); at least "
                            + required
                            + " are required to simulate "
                            + maxDraws
                            + " turn(s) of draws on top of a "
                            + HAND_SIZE
                            + "-card opening hand.");
        }
    }

    private static void simulateGame(
            List<Long> library,
            Map<Long, Card> cardsByPrinting,
            RandomGenerator random,
            DeckSimulationRequest request,
            Accumulators accumulators) {
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        List<Long> shuffled = draw.shuffledLibrary();
        List<Long> keptHand = shuffled.subList(0, HAND_SIZE - draw.mulliganCount());
        List<Long> topOfLibrary = shuffled.subList(HAND_SIZE, shuffled.size());

        GameState state = new GameState();
        for (int turn = 1; turn <= request.turns(); turn++) {
            for (Long printingId :
                    newCardsAtTurn(turn, keptHand, topOfLibrary, request.onThePlay())) {
                state.reveal(Objects.requireNonNull(cardsByPrinting.get(printingId)));
            }
            accumulators.record(turn, state);
        }
    }

    private static List<Long> newCardsAtTurn(
            int turn, List<Long> keptHand, List<Long> topOfLibrary, boolean onThePlay) {
        int drawsBefore = drawsThroughTurn(turn - 1, onThePlay);
        int drawsThrough = drawsThroughTurn(turn, onThePlay);
        List<Long> draws = topOfLibrary.subList(drawsBefore, drawsThrough);
        if (turn > 1) {
            return draws;
        }
        List<Long> firstTurn = new ArrayList<>(keptHand);
        firstTurn.addAll(draws);
        return firstTurn;
    }

    private static int drawsThroughTurn(int turn, boolean onThePlay) {
        if (turn <= 0) {
            return 0;
        }
        return onThePlay ? turn - 1 : turn;
    }

    /** One simulated game's running state as cards are revealed turn by turn. */
    private static final class GameState {

        private static final int MAX_TRACKED_MANA_VALUE = 10;

        private final int[] manaValueHistogram = new int[MAX_TRACKED_MANA_VALUE + 1];
        private final List<Card> landsInDrawOrder = new ArrayList<>();
        private final Set<String> colorsInPlay = new TreeSet<>();
        private int cumulativeLands;
        private int totalCardsSeen;
        private int landsMergedIntoPlay;

        void reveal(Card card) {
            totalCardsSeen++;
            if (DeckLibraryResolver.isLand(card)) {
                cumulativeLands++;
                landsInDrawOrder.add(card);
            } else {
                int bucket =
                        Math.min(CastabilityCalculator.manaValueOf(card), MAX_TRACKED_MANA_VALUE);
                manaValueHistogram[bucket]++;
            }
        }

        /** Merges colors from lands assumed in play by {@code turn} into {@link #colorsInPlay}. */
        void mergeColorsInPlay(int turn) {
            int target = LandDropCalculator.landsInPlay(cumulativeLands, turn);
            while (landsMergedIntoPlay < target) {
                colorsInPlay.addAll(
                        ColorAvailabilityCalculator.producedColors(
                                landsInDrawOrder.get(landsMergedIntoPlay)));
                landsMergedIntoPlay++;
            }
        }

        int castableSpellCount(int landsInPlay) {
            int count = 0;
            for (int mv = 0; mv <= Math.min(landsInPlay, MAX_TRACKED_MANA_VALUE); mv++) {
                count += manaValueHistogram[mv];
            }
            return count;
        }

        int totalSpellsSeen() {
            return totalCardsSeen - cumulativeLands;
        }

        int cumulativeLands() {
            return cumulativeLands;
        }

        int totalCardsSeen() {
            return totalCardsSeen;
        }

        Set<String> colorsInPlay() {
            return colorsInPlay;
        }
    }

    /** Running per-turn sums across every simulated game, divided into a response at the end. */
    private static final class Accumulators {

        private final int turns;
        private final long[] landDropOnCurveCount;
        private final Map<String, long[]> colorAvailableCount = new LinkedHashMap<>();
        private final double[] cardsSeenSum;
        private final double[] castableCountSum;
        private final double[] castableFractionSum;

        Accumulators(int turns) {
            this.turns = turns;
            this.landDropOnCurveCount = new long[turns];
            this.cardsSeenSum = new double[turns];
            this.castableCountSum = new double[turns];
            this.castableFractionSum = new double[turns];
            COLORS.forEach(color -> colorAvailableCount.put(color, new long[turns]));
        }

        void record(int turn, GameState state) {
            int index = turn - 1;
            if (LandDropCalculator.onCurve(state.cumulativeLands(), turn)) {
                landDropOnCurveCount[index]++;
            }
            recordColorsInPlay(turn, index, state);
            recordCastability(turn, index, state);
            cardsSeenSum[index] += state.totalCardsSeen();
        }

        private void recordColorsInPlay(int turn, int index, GameState state) {
            state.mergeColorsInPlay(turn);
            for (String color : COLORS) {
                if (state.colorsInPlay().contains(color)) {
                    Objects.requireNonNull(colorAvailableCount.get(color))[index]++;
                }
            }
        }

        private void recordCastability(int turn, int index, GameState state) {
            int landsInPlay = LandDropCalculator.landsInPlay(state.cumulativeLands(), turn);
            int castable = state.castableSpellCount(landsInPlay);
            castableCountSum[index] += castable;
            int totalSpells = state.totalSpellsSeen();
            castableFractionSum[index] += totalSpells == 0 ? 0.0 : (double) castable / totalSpells;
        }

        DeckSimulationResponse toResponse(long seed, int iterations) {
            return new DeckSimulationResponse(
                    seed,
                    iterations,
                    turns,
                    byTurn(landDropOnCurveCount, iterations),
                    colorAvailabilityByTurn(iterations),
                    byTurn(cardsSeenSum, iterations),
                    byTurn(castableFractionSum, iterations),
                    byTurn(castableCountSum, iterations),
                    DeckSimulationResponse.ConfidenceMetadata.of(iterations));
        }

        private Map<String, Map<Integer, Double>> colorAvailabilityByTurn(int iterations) {
            Map<String, Map<Integer, Double>> byColor = new LinkedHashMap<>();
            colorAvailableCount.forEach(
                    (color, counts) -> byColor.put(color, byTurn(counts, iterations)));
            return byColor;
        }

        private Map<Integer, Double> byTurn(long[] counts, int iterations) {
            Map<Integer, Double> result = new LinkedHashMap<>();
            for (int i = 0; i < turns; i++) {
                result.put(i + 1, (double) counts[i] / iterations);
            }
            return result;
        }

        private Map<Integer, Double> byTurn(double[] sums, int iterations) {
            Map<Integer, Double> result = new LinkedHashMap<>();
            for (int i = 0; i < turns; i++) {
                result.put(i + 1, sums[i] / iterations);
            }
            return result;
        }
    }
}
