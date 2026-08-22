package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.simulation.DeckLibraryResolver;
import com.deckassemble.decks.application.simulation.MulliganDraw;
import com.deckassemble.decks.application.simulation.MulliganRequest;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Builds a match-ready player from a deck snapshot: catalog, library, mulligan, commander. */
final class MatchDeckFactory {

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    MatchDeckFactory(DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    DeckSnapshot snapshotAt(long deckId, int revision) {
        return deckRevisionService.snapshotAt(deckId, revision);
    }

    DeckSnapshot snapshotAtForSharedAccess(long deckId, int revision) {
        return deckRevisionService.snapshotAtForSharedAccess(deckId, revision);
    }

    PlayerState buildPlayer(DeckSnapshot snapshot, MulliganRequest request, RandomGenerator random) {
        Map<Long, PracticeCard> catalog = loadCatalog(snapshot);
        Map<Long, Card> cardsByPrinting = toCards(catalog);
        List<Long> library =
                DeckLibraryResolver.expandLibrary(
                        DeckLibraryResolver.mainDeckEntries(snapshot), cardsByPrinting, snapshot);
        validateLibrary(library, catalog);
        PracticeCard commander = resolveCommander(snapshot, catalog);
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        List<PracticeCard> deck = mapToCards(draw.shuffledLibrary(), catalog);
        return new PlayerState(
                PlayerId.newId(),
                deck.subList(0, MulliganDraw.HAND_SIZE),
                deck.subList(MulliganDraw.HAND_SIZE, deck.size()),
                commander);
    }

    private Map<Long, PracticeCard> loadCatalog(DeckSnapshot snapshot) {
        List<Long> printingIds =
                snapshot.cards().stream()
                        .map(DeckSnapshot.CardEntry::cardPrintingId)
                        .distinct()
                        .toList();
        return cardCatalogService.getPracticeCardsByPrintingIds(printingIds);
    }

    private Map<Long, Card> toCards(Map<Long, PracticeCard> catalog) {
        return catalog.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().card()));
    }

    private void validateLibrary(List<Long> library, Map<Long, PracticeCard> catalog) {
        if (library.size() < MulliganDraw.HAND_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "deck library must contain at least " + MulliganDraw.HAND_SIZE + " cards");
        }
        validateStats(catalog);
    }

    private void validateStats(Map<Long, PracticeCard> catalog) {
        try {
            catalog.values().forEach(Permanent::validateParseable);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private PracticeCard resolveCommander(DeckSnapshot snapshot, Map<Long, PracticeCard> catalog) {
        return snapshot.cards().stream()
                .filter(cardEntry -> isCommander(snapshot, catalog, cardEntry))
                .findFirst()
                .map(cardEntry -> catalog.get(cardEntry.cardPrintingId()))
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "commander card not found in deck"));
    }

    private boolean isCommander(
            DeckSnapshot snapshot, Map<Long, PracticeCard> catalog, DeckSnapshot.CardEntry entry) {
        PracticeCard practiceCard = catalog.get(entry.cardPrintingId());
        return practiceCard != null
                && snapshot.commanderCardId() != null
                && snapshot.commanderCardId().equals(practiceCard.card().getId());
    }

    private List<PracticeCard> mapToCards(List<Long> library, Map<Long, PracticeCard> catalog) {
        return library.stream().map(catalog::get).toList();
    }
}
