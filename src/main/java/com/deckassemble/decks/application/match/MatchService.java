package com.deckassemble.decks.application.match;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.simulation.MulliganDraw;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Runs two-player Commander matches in memory, mirroring PracticeSessionService. */
@Service
public class MatchService {

    // ponytail: in-memory match store, a database table can replace it when matches need to outlive the process
    private final Map<UUID, MatchEntry> matches = new ConcurrentHashMap<>();

    private final MatchDeckFactory deckFactory;

    public MatchService(DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckFactory = new MatchDeckFactory(deckRevisionService, cardCatalogService);
    }

    /** Creates a match between the caller's deck and another deck; both opening hands are dealt. */
    public Match start(MatchRequest request, long callerProfileId) {
        MulliganDraw.validateLandRange(request);
        long seed =
                request.seed() != null
                        ? request.seed()
                        : ThreadLocalRandom.current().nextLong();
        RandomGenerator random = RandomGeneratorFactory.getDefault().create(seed);
        DeckSnapshot yourSnapshot =
                deckFactory.snapshotAt(request.yourDeckId(), request.yourRevision());
        DeckSnapshot opponentSnapshot =
                deckFactory.snapshotAtForSharedAccess(
                        request.opponentDeckId(), request.opponentRevision());
        PlayerState you = deckFactory.buildPlayer(yourSnapshot, request, random);
        PlayerState opponent = deckFactory.buildPlayer(opponentSnapshot, request, random);
        Match match = new Match(UUID.randomUUID(), you, opponent, request.callerOnThePlay());
        matches.put(match.id(), new MatchEntry(match, callerProfileId, you.playerId()));
        return match;
    }

    MatchEntry entry(UUID matchId) {
        MatchEntry entry = matches.get(matchId);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "match not found: " + matchId);
        }
        return entry;
    }

    /** Returns the caller's hidden-info view of the match; 404 unknown, 403 non-participant. */
    public MatchResponse view(UUID matchId, long callerProfileId) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return MatchView.forPlayer(entry.match(), entry.callerSeat());
    }

    /** Plays a land for the active seat; the caller must be the match's participant. */
    public Match playLand(UUID matchId, long callerProfileId, long printingId) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().playLand(printingId));
    }

    /** Casts a spell for the seat holding priority; the caller must be the match's participant. */
    public Match castSpell(
            UUID matchId,
            long callerProfileId,
            long printingId,
            StackObject.@Nullable Target target) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().castSpell(printingId, target));
    }

    /** Passes priority for the seat currently holding it; both passes resolve or advance. */
    public Match passPriority(UUID matchId, long callerProfileId) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().stackResolver().passPriorityForHolder(entry.match()));
    }

    /** The caller's own seat concedes; the match is over. */
    public Match concede(UUID matchId, long callerProfileId) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().concede(entry.callerSeat()));
    }

    /** The active player's declared attackers (printing ids) tap and wait for blocks. */
    public Match declareAttackers(UUID matchId, long callerProfileId, List<Long> printingIds) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().declareAttackers(printingIds));
    }

    /**
     * The defending player declares blockers (blocker printing id -&gt; attacker printing id) and
     * combat damage resolves immediately with auto-assigned damage order.
     */
    public Match declareBlockers(
            UUID matchId, long callerProfileId, Map<Long, Long> blockerAssignments) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().declareBlockers(blockerAssignments));
    }

    private MatchEntry authorizedEntry(UUID matchId, long callerProfileId) {
        MatchEntry entry = entry(matchId);
        if (entry.callerProfileId() != callerProfileId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "caller is not a participant of this match");
        }
        return entry;
    }

    private Match applyAction(MatchEntry entry, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return entry.match();
    }

    /** One live match plus the profile allowed to drive it (hot-seat for both sides). */
    public record MatchEntry(Match match, long callerProfileId, PlayerId callerSeat) {}
}
