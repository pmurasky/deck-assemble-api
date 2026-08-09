package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.ArrayList;
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
 * redraws via the London mulligan rule until a hand's land count falls within the requested range
 * (draw/mulligan mechanics live in {@link MulliganDraw}, shared with {@link
 * DeckSimulationService}). Statistical goldfishing only — no card-text execution.
 */
@Service
public class DeckSampleHandService {

    static final int HAND_SIZE = MulliganDraw.HAND_SIZE;

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    public DeckSampleHandService(
            DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    public DeckSampleHandResponse generate(long deckId, DeckSampleHandRequest request) {
        MulliganDraw.validateLandRange(request);
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
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        int keep = HAND_SIZE - draw.mulliganCount();
        return toHand(
                draw.shuffledLibrary().subList(0, keep), cardsByPrinting, draw.mulliganCount());
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
