package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs solitaire practice sessions: {@link #start} draws a seeded opening hand, player actions
 * mutate the board, and {@link #reset} returns the session to its opening hand using the original
 * seed. Sessions are kept in memory, keyed by a random id.
 */
@Service
public class PracticeSessionService {

    // ponytail: in-memory session store; sessions vanish on restart and are not shared across
    // instances. Move to a datastore if practice sessions ever need to survive a deploy or run
    // behind more than one replica.
    private final Map<UUID, SessionEntry> sessions = new ConcurrentHashMap<>();

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    public PracticeSessionService(
            DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    public PracticeSessionResponse start(long deckId, PracticeSessionRequest request) {
        long seed =
                request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
        PracticeSession session = newSession(deckId, request, seed);
        UUID sessionId = UUID.randomUUID();
        sessions.put(sessionId, new SessionEntry(deckId, request, seed, session));
        return started(sessionId, seed, session);
    }

    public PracticeSessionResponse playCard(long deckId, UUID sessionId, long printingId) {
        SessionEntry entry = entry(deckId, sessionId);
        applyPlayerAction(() -> entry.session().playCard(printingId));
        return current(sessionId, entry);
    }

    public PracticeSessionResponse toggleTap(long deckId, UUID sessionId, long printingId) {
        SessionEntry entry = entry(deckId, sessionId);
        applyPlayerAction(() -> entry.session().toggleTap(printingId));
        return current(sessionId, entry);
    }

    public PracticeSessionResponse nextTurn(long deckId, UUID sessionId) {
        SessionEntry entry = entry(deckId, sessionId);
        PracticeSession.Turn turn = entry.session().nextTurn();
        return current(sessionId, entry, turn.drawnCard(), turn.finished());
    }

    public PracticeSessionResponse reset(long deckId, UUID sessionId) {
        SessionEntry entry = entry(deckId, sessionId);
        PracticeSession session = newSession(deckId, entry.request(), entry.seed());
        sessions.put(sessionId, new SessionEntry(deckId, entry.request(), entry.seed(), session));
        return started(sessionId, entry.seed(), session);
    }

    private PracticeSession newSession(long deckId, PracticeSessionRequest request, long seed) {
        MulliganDraw.validateLandRange(request);
        DeckSnapshot snapshot = deckRevisionService.snapshotAt(deckId, request.revision());
        List<DeckSnapshot.CardEntry> entries = DeckLibraryResolver.mainDeckEntries(snapshot);
        Map<Long, PracticeCard> practiceCards =
                cardCatalogService.getPracticeCardsByPrintingIds(
                        DeckLibraryResolver.printingIdsOf(entries));
        Map<Long, Card> cardsByPrinting = cardsByPrinting(practiceCards);
        List<Long> library = DeckLibraryResolver.expandLibrary(entries, cardsByPrinting, snapshot);
        validateLibrarySize(library);
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(seed);
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        return new PracticeSession(draw, practiceCards, request.onThePlay());
    }

    private static Map<Long, Card> cardsByPrinting(Map<Long, PracticeCard> practiceCards) {
        return practiceCards.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().card()));
    }

    private void validateLibrarySize(List<Long> library) {
        if (library.size() < MulliganDraw.HAND_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Deck library has only "
                            + library.size()
                            + " card(s) after excluding commander(s); at least "
                            + MulliganDraw.HAND_SIZE
                            + " are required for an opening hand.");
        }
    }

    private SessionEntry entry(long deckId, UUID sessionId) {
        SessionEntry entry = sessions.get(sessionId);
        if (entry == null || entry.deckId() != deckId) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "No practice session " + sessionId + " for this deck.");
        }
        return entry;
    }

    private static void applyPlayerAction(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private static PracticeSessionResponse current(UUID sessionId, SessionEntry entry) {
        return current(sessionId, entry, null, entry.session().finished());
    }

    private static PracticeSessionResponse current(
            UUID sessionId,
            SessionEntry entry,
            @Nullable PracticeCard drawnCard,
            boolean finished) {
        PracticeSession session = entry.session();
        return new PracticeSessionResponse(
                sessionId,
                entry.seed(),
                session.turn(),
                session.mulliganCount(),
                cardViews(session.hand()),
                drawnCard == null ? null : cardView(drawnCard),
                cardViews(session.castableSpells()),
                finished,
                permanentViews(session));
    }

    private static List<PracticeSessionResponse.CardView> cardViews(List<PracticeCard> cards) {
        return cards.stream().map(PracticeSessionService::cardView).toList();
    }

    private static List<PracticeSessionResponse.PermanentView> permanentViews(
            PracticeSession session) {
        return session.battlefield().stream()
                .map(permanent -> permanentView(session, permanent))
                .toList();
    }

    private static PracticeSessionResponse.PermanentView permanentView(
            PracticeSession session, PracticeSession.Permanent permanent) {
        return new PracticeSessionResponse.PermanentView(
                permanent.printingId(),
                cardView(session.practiceCard(permanent.printingId())),
                permanent.tapped());
    }

    private static PracticeSessionResponse.CardView cardView(PracticeCard card) {
        return new PracticeSessionResponse.CardView(
                card.printingId(),
                card.card().getName(),
                card.imageUrl(),
                card.card().getManaCost(),
                card.card().getTypeLine(),
                card.card().getOracleText());
    }

    private static PracticeSessionResponse started(
            UUID sessionId, long seed, PracticeSession session) {
        return new PracticeSessionResponse(
                sessionId,
                seed,
                session.turn(),
                session.mulliganCount(),
                cardViews(session.hand()),
                null,
                List.of(),
                false,
                List.of());
    }

    private record SessionEntry(
            long deckId, PracticeSessionRequest request, long seed, PracticeSession session) {}
}
