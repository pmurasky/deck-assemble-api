package com.deckassemble.decks.api.match;

import com.deckassemble.decks.application.match.Match;
import com.deckassemble.decks.application.match.MatchActionRequest;
import com.deckassemble.decks.application.match.MatchRequest;
import com.deckassemble.decks.application.match.MatchResponse;
import com.deckassemble.decks.application.match.MatchService;
import com.deckassemble.shared.security.CurrentProfile;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Two-player Commander match endpoints. */
@RestController
@RequestMapping("/matches")
public class MatchController {

    private final MatchService matchService;
    private final CurrentProfile currentProfile;

    public MatchController(MatchService matchService, CurrentProfile currentProfile) {
        this.matchService = matchService;
        this.currentProfile = currentProfile;
    }

    /** Starts a match for the authenticated caller. */
    @PostMapping
    public MatchResponse start(@Valid @RequestBody MatchRequest request) {
        long callerProfileId = currentProfile.requireProfile().getId();
        Match match = matchService.start(request, callerProfileId);
        return matchService.view(match.id(), callerProfileId);
    }

    /** The caller's hidden-info view of the match. */
    @GetMapping("/{matchId}")
    public MatchResponse view(@PathVariable UUID matchId) {
        return matchService.view(matchId, currentProfile.requireProfile().getId());
    }

    /** Applies a player action and returns the caller's updated view. */
    @PostMapping("/{matchId}/actions")
    public MatchResponse act(
            @PathVariable UUID matchId, @Valid @RequestBody MatchActionRequest request) {
        long callerProfileId = currentProfile.requireProfile().getId();
        apply(matchId, callerProfileId, request);
        return matchService.view(matchId, callerProfileId);
    }

    private void apply(UUID matchId, long callerProfileId, MatchActionRequest request) {
        switch (request.type()) {
            case PLAY_LAND -> matchService.playLand(matchId, callerProfileId, printingId(request));
            case CAST_SPELL ->
                    matchService.castSpell(matchId, callerProfileId, printingId(request), null);
            case PASS_PRIORITY -> matchService.passPriority(matchId, callerProfileId);
            case DECLARE_ATTACKERS ->
                    matchService.declareAttackers(
                            matchId, callerProfileId, orEmpty(request.attackerIds()));
            case DECLARE_BLOCKERS ->
                    matchService.declareBlockers(
                            matchId, callerProfileId, orEmpty(request.blockerAssignments()));
            case CONCEDE -> matchService.concede(matchId, callerProfileId);
            default -> throw new IllegalStateException("unexpected action: " + request.type());
        }
    }

    private long printingId(MatchActionRequest request) {
        if (request.printingId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "printingId is required for " + request.type());
        }
        return request.printingId();
    }

    private static List<Long> orEmpty(@Nullable List<Long> ids) {
        return ids == null ? List.of() : ids;
    }

    private static Map<Long, Long> orEmpty(@Nullable Map<Long, Long> assignments) {
        return assignments == null ? Map.of() : assignments;
    }
}
