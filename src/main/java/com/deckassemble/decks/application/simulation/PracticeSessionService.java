package com.deckassemble.decks.application.simulation;

import com.deckassemble.cards.application.CardCatalogService;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Runs solitaire turn-stepped practice sessions: {@link #start} draws a seeded opening hand (same
 * library resolution and mulligan mechanics as {@link DeckSimulationService}), {@link #step}
 * advances one turn of draw / land-drop / cast at a time, and {@link #reset} returns the session to
 * its opening hand using the original seed. Sessions are kept in memory, keyed by a random id — a
 * practice session is ephemeral goldfishing state, not deck data.
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

    public PracticeSessionResponse step(long deckId, UUID sessionId) {
        SessionEntry entry = entry(deckId, sessionId);
        PracticeSession.Step step = entry.session().step();
        return new PracticeSessionResponse(
                sessionId,
                entry.seed(),
                step.turn(),
                entry.session().mulliganCount(),
                entry.session().handNames(),
                step.drawnCard(),
                step.landPlayed(),
                step.landsInPlay(),
                step.castableSpells(),
                step.finished());
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
        Map<Long, Card> cardsByPrinting =
                cardCatalogService.getCardsByPrintingIds(
                        DeckLibraryResolver.printingIdsOf(entries));
        List<Long> library = DeckLibraryResolver.expandLibrary(entries, cardsByPrinting, snapshot);
        validateLibrarySize(library);
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(seed);
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        return new PracticeSession(draw, cardsByPrinting, request.onThePlay());
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

    private static PracticeSessionResponse started(
            UUID sessionId, long seed, PracticeSession session) {
        return new PracticeSessionResponse(
                sessionId,
                seed,
                session.turn(),
                session.mulliganCount(),
                session.handNames(),
                null,
                null,
                0,
                List.of(),
                false);
    }

    private record SessionEntry(
            long deckId, PracticeSessionRequest request, long seed, PracticeSession session) {}
}
