package com.deckassemble.administration.api;

import com.deckassemble.recommendations.application.CommanderRankRunRecorder;
import com.deckassemble.recommendations.application.CommanderRankService;
import com.deckassemble.recommendations.application.RefreshOutcome;
import com.deckassemble.recommendations.domain.CommanderRankRefreshRun;
import com.deckassemble.shared.security.CurrentUser;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/commander-ranks")
public class CommanderRankAdminController {

    private final CommanderRankService commanderRankService;
    private final CommanderRankRunRecorder runRecorder;
    private final CurrentUser currentUser;

    public CommanderRankAdminController(
            CommanderRankService commanderRankService,
            CommanderRankRunRecorder runRecorder,
            CurrentUser currentUser) {
        this.commanderRankService = commanderRankService;
        this.runRecorder = runRecorder;
        this.currentUser = currentUser;
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefreshOutcome> refresh() {
        RefreshOutcome outcome =
                commanderRankService.refreshNow(currentUser.subject().orElse("system"));
        if (!outcome.success()) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(outcome);
        }
        return ResponseEntity.ok(outcome);
    }

    @GetMapping("/latest")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefreshRunResponse> latest() {
        return runRecorder
                .latestCompleted()
                .map(run -> ResponseEntity.ok(RefreshRunResponse.from(run)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record RefreshRunResponse(
            long id,
            String status,
            int cardsUpdated,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String triggeredBy) {

        static RefreshRunResponse from(CommanderRankRefreshRun run) {
            return new RefreshRunResponse(
                    run.getId(),
                    run.getStatus().name(),
                    run.getCardsUpdated(),
                    run.getStartedAt(),
                    run.getCompletedAt(),
                    run.getTriggeredBy());
        }
    }
}
