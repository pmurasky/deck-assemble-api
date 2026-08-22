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
 * Runs in-memory two-player Commander matches. Mirrors {@code PracticeSessionService}: matches live
 * in a process-local map keyed by match id, and the caller's deck library and opening hand are
 * built through the shared mulligan/library helpers.
 */
@Service
public class MatchService {

    // ponytail: in-memory store, mirroring PracticeSessionService; swap for persistence if matches
    // ever need to survive a restart.
    private final Map<UUID, MatchEntry> matches = new ConcurrentHashMap<>();

    private final DeckRevisionService deckRevisionService;
    private final CardCatalogService cardCatalogService;

    public MatchService(
            DeckRevisionService deckRevisionService, CardCatalogService cardCatalogService) {
        this.deckRevisionService = deckRevisionService;
        this.cardCatalogService = cardCatalogService;
    }

    /**
     * A stored match plus the profile allowed to drive it (hot-seat: the caller pilots both seats).
     */
    public record MatchEntry(Match match, long callerProfileId, PlayerId callerSeat) {}

    public Match start(MatchRequest request, long callerProfileId) {
        MulliganDraw.validateLandRange(request);
        DeckSnapshot yourSnapshot =
                deckRevisionService.snapshotAt(request.yourDeckId(), request.yourRevision());
        DeckSnapshot opponentSnapshot =
                deckRevisionService.snapshotAtForSharedAccess(
                        request.opponentDeckId(), request.opponentRevision());
        RandomGenerator random = randomFor(request);
        PlayerId callerSeat = PlayerId.newId();
        PlayerState you = buildPlayer(yourSnapshot, request, random, callerSeat);
        PlayerState opponent = buildPlayer(opponentSnapshot, request, random, PlayerId.newId());
        Match match = new Match(UUID.randomUUID(), you, opponent, request.callerOnThePlay());
        matches.put(match.id(), new MatchEntry(match, callerProfileId, callerSeat));
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

    /**
     * Casts a spell for the seat holding priority, with an optional single target; the caller
     * must be the match's participant.
     */
    public Match castSpell(
            UUID matchId,
            long callerProfileId,
            long printingId,
            StackObject.@Nullable Target target) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().castSpell(printingId, target));
    }

    /** Passes priority for the seat that currently holds it; the caller must be a participant. */
    public Match passPriority(UUID matchId, long callerProfileId) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(
                entry,
                () -> {
                    Match match = entry.match();
                    match.stackResolver().passPriorityForHolder(match);
                });
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
     * The defending player declares blockers (blocker printing id -> attacker printing id) and
     * combat damage resolves immediately with auto-assigned damage order.
     */
    public Match declareBlockers(
            UUID matchId, long callerProfileId, Map<Long, Long> blockerToAttacker) {
        MatchEntry entry = authorizedEntry(matchId, callerProfileId);
        return applyAction(entry, () -> entry.match().declareBlockers(blockerToAttacker));
    }

    private MatchEntry authorizedEntry(UUID matchId, long callerProfileId) {
        MatchEntry entry = entry(matchId);
        if (entry.callerProfileId() != callerProfileId) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "caller is not a participant in match " + matchId);
        }
        return entry;
    }

    private static Match applyAction(MatchEntry entry, Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
        return entry.match();
    }

    private PlayerState buildPlayer(
            DeckSnapshot snapshot, MulliganRequest request, RandomGenerator random, PlayerId seat) {
        Map<Long, PracticeCard> catalog = loadCatalog(snapshot);
        Map<Long, Card> cardsByPrinting = toCards(catalog);
        List<Long> library =
                DeckLibraryResolver.expandLibrary(
                        DeckLibraryResolver.mainDeckEntries(snapshot), cardsByPrinting, snapshot);
        validateLibrary(library, catalog);
        PracticeCard commander = resolveCommander(snapshot, catalog);
        MulliganDraw.Result draw = MulliganDraw.draw(library, cardsByPrinting, random, request);
        List<PracticeCard> all = mapToCards(draw.shuffledLibrary(), catalog);
        return new PlayerState(
                seat,
                all.subList(0, MulliganDraw.HAND_SIZE),
                all.subList(MulliganDraw.HAND_SIZE, all.size()),
                commander);
    }

    private Map<Long, PracticeCard> loadCatalog(DeckSnapshot snapshot) {
        // All sections (not just MAIN_DECK) so commander printings resolve too.
        return cardCatalogService.getPracticeCardsByPrintingIds(
                DeckLibraryResolver.printingIdsOf(snapshot.cards()));
    }

    private static Map<Long, Card> toCards(Map<Long, PracticeCard> catalog) {
        return catalog.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().card()));
    }

    private static void validateLibrary(List<Long> library, Map<Long, PracticeCard> catalog) {
        if (library.size() < MulliganDraw.HAND_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "library has fewer cards than the opening hand");
        }
        catalog.values().forEach(MatchService::validateStats);
    }

    private static void validateStats(PracticeCard card) {
        try {
            Permanent.validateParseable(card);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    // ponytail: only the primary commander is modeled; partner/background pairs are out of scope
    // for the initial match engine.
    private static PracticeCard resolveCommander(
            DeckSnapshot snapshot, Map<Long, PracticeCard> catalog) {
        return snapshot.cards().stream()
                .filter(entry -> isCommander(entry, snapshot, catalog))
                .findFirst()
                .map(entry -> catalog.get(entry.cardPrintingId()))
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "commander not found among the deck's printings"));
    }

    private static boolean isCommander(
            DeckSnapshot.CardEntry entry, DeckSnapshot snapshot, Map<Long, PracticeCard> catalog) {
        PracticeCard card = catalog.get(entry.cardPrintingId());
        return card != null && card.card().getId().equals(snapshot.commanderCardId());
    }

    private static List<PracticeCard> mapToCards(
            List<Long> printingIds, Map<Long, PracticeCard> catalog) {
        return printingIds.stream().map(catalog::get).collect(Collectors.toList());
    }

    private static RandomGenerator randomFor(MatchRequest request) {
        long seed =
                request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextLong();
        return RandomGeneratorFactory.getDefault().create(seed);
    }
}
