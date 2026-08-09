package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Draws deterministic, seeded sample opening hands from a deck revision's snapshot: shuffles the
 * revision's main-deck library (commander(s) excluded — they live in the command zone, not the
 * library; see {@link DeckLibraryResolver}) and, for {@link MulliganStrategy#LONDON_LAND_RANGE},
 * redraws via the London mulligan rule until a hand's land count falls within the requested range.
 * Statistical goldfishing only — no card-text execution.
 */
@Service
public class DeckSampleHandService {

    static final int HAND_SIZE = 7;

    // ponytail: cap London mulligans at 3 (bottoming down to a 4-card hand) instead of looping
    // until the land range is satisfied or the hand is mulliganed away entirely. Bounds worst-case
    // runtime against a land range a pathological library (e.g. all lands, or none) can never
    // satisfy; the loop keeps the last drawn hand once the cap is hit. Raise if playtesters need
    // deeper mulligans modeled.
    private static final int MAX_MULLIGANS = 3;

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    public DeckSampleHandService(
            DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    public DeckSampleHandResponse generate(long deckId, DeckSampleHandRequest request) {
        validateLandRange(request);
        DeckSnapshot snapshot = deckRevisionService.snapshotAt(deckId, request.revision());
        List<DeckSnapshot.CardEntry> mainDeckEntries =
                DeckLibraryResolver.mainDeckEntries(snapshot);
        Map<Long, Card> cardsByPrinting =
                cardCatalogService.getCardsByPrintingIds(
                        DeckLibraryResolver.printingIdsOf(mainDeckEntries));
        List<Long> library =
                DeckLibraryResolver.expandLibrary(mainDeckEntries, cardsByPrinting, snapshot);
        validateLibrarySize(library);
        long seed =
                request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(seed);
        List<DeckSampleHandResponse.Hand> hands = new ArrayList<>();
        for (int i = 0; i < request.handCount(); i++) {
            hands.add(drawHand(library, cardsByPrinting, random, request));
        }
        return new DeckSampleHandResponse(seed, hands);
    }

    private static void validateLandRange(DeckSampleHandRequest request) {
        if (request.mulliganStrategy() == MulliganStrategy.LONDON_LAND_RANGE
                && !hasValidLandRange(request)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minimumLands and maximumLands must both be set between 0 and "
                            + HAND_SIZE
                            + " (minimumLands <= maximumLands) for LONDON_LAND_RANGE.");
        }
    }

    private static boolean hasValidLandRange(DeckSampleHandRequest request) {
        Integer min = request.minimumLands();
        Integer max = request.maximumLands();
        return min != null && max != null && min >= 0 && max <= HAND_SIZE && min <= max;
    }

    private static void validateLibrarySize(List<Long> library) {
        if (library.size() < HAND_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Deck library has only "
                            + library.size()
                            + " card(s) after excluding commander(s); at least "
                            + HAND_SIZE
                            + " are required to generate a sample hand.");
        }
    }

    private DeckSampleHandResponse.Hand drawHand(
            List<Long> library,
            Map<Long, Card> cardsByPrinting,
            RandomGenerator random,
            DeckSampleHandRequest request) {
        if (request.mulliganStrategy() != MulliganStrategy.LONDON_LAND_RANGE) {
            return toHand(shuffleAndDraw(library, random), cardsByPrinting, 0);
        }
        return drawWithLondonMulligan(library, cardsByPrinting, random, request);
    }

    private DeckSampleHandResponse.Hand drawWithLondonMulligan(
            List<Long> library,
            Map<Long, Card> cardsByPrinting,
            RandomGenerator random,
            DeckSampleHandRequest request) {
        int minimumLands = Objects.requireNonNull(request.minimumLands());
        int maximumLands = Objects.requireNonNull(request.maximumLands());
        List<Long> drawn;
        int mulliganCount = 0;
        while (true) {
            drawn = shuffleAndDraw(library, random);
            long lands =
                    drawn.stream()
                            .filter(id -> DeckLibraryResolver.isLand(cardsByPrinting.get(id)))
                            .count();
            boolean withinRange = lands >= minimumLands && lands <= maximumLands;
            if (withinRange || mulliganCount >= MAX_MULLIGANS) {
                break;
            }
            mulliganCount++;
        }
        int keep = HAND_SIZE - mulliganCount;
        return toHand(drawn.subList(0, keep), cardsByPrinting, mulliganCount);
    }

    private static List<Long> shuffleAndDraw(List<Long> library, RandomGenerator random) {
        List<Long> shuffled = new ArrayList<>(library);
        shuffle(shuffled, random);
        return new ArrayList<>(shuffled.subList(0, HAND_SIZE));
    }

    private static void shuffle(List<Long> deck, RandomGenerator random) {
        for (int i = deck.size() - 1; i > 0; i--) {
            Collections.swap(deck, i, random.nextInt(i + 1));
        }
    }

    private static DeckSampleHandResponse.Hand toHand(
            List<Long> printingIds, Map<Long, Card> cardsByPrinting, int mulliganCount) {
        List<DeckSampleHandResponse.DrawnCard> cards =
                printingIds.stream().map(id -> drawnCard(id, cardsByPrinting)).toList();
        return new DeckSampleHandResponse.Hand(mulliganCount, cards);
    }

    private static DeckSampleHandResponse.DrawnCard drawnCard(
            Long printingId, Map<Long, Card> cardsByPrinting) {
        Card card = Objects.requireNonNull(cardsByPrinting.get(printingId));
        return new DeckSampleHandResponse.DrawnCard(printingId, card.getName());
    }
}
