package com.deckassemble.administration.api;

import com.deckassemble.imports.application.CardImportTrigger;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.shared.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/card-imports")
public class CardImportController {

    private final CardImportTrigger cardImportTrigger;
    private final ImportRunRecorder importRunRecorder;
    private final CurrentUser currentUser;

    public CardImportController(
            CardImportTrigger cardImportTrigger,
            ImportRunRecorder importRunRecorder,
            CurrentUser currentUser) {
        this.cardImportTrigger = cardImportTrigger;
        this.importRunRecorder = importRunRecorder;
        this.currentUser = currentUser;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportAcceptedResponse> importCards(
            @RequestParam @NotBlank String query) {
        long runId = cardImportTrigger.trigger(query, currentUser.subject().orElse("system"));
        return ResponseEntity.accepted().body(new ImportAcceptedResponse(runId));
    }

    public record ImportAcceptedResponse(long runId) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ImportRunHistoryResponse> history() {
        return importRunRecorder.history().stream().map(ImportRunHistoryResponse::from).toList();
    }

    public record ImportRunHistoryResponse(
            long id,
            String provider,
            String query,
            String status,
            int recordsRead,
            int recordsCreated,
            int recordsUpdated,
            int recordsFailed,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt) {

        static ImportRunHistoryResponse from(CardImportRun run) {
            return new ImportRunHistoryResponse(
                    run.getId(),
                    run.getProvider(),
                    run.getQuery(),
                    run.getStatus().name(),
                    run.getRecordsRead(),
                    run.getRecordsCreated(),
                    run.getRecordsUpdated(),
                    run.getRecordsFailed(),
                    run.getStartedAt(),
                    run.getCompletedAt());
        }
    }
}
