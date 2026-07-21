package com.deckassemble.administration.api;

import com.deckassemble.imports.application.CardImportService;
import com.deckassemble.imports.application.ImportResult;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.imports.domain.CardImportRun;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/card-imports")
public class CardImportController {

    private final CardImportService cardImportService;
    private final ImportRunRecorder importRunRecorder;

    public CardImportController(
            CardImportService cardImportService, ImportRunRecorder importRunRecorder) {
        this.cardImportService = cardImportService;
        this.importRunRecorder = importRunRecorder;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ImportResult importCards(@RequestParam @NotBlank String query) {
        return cardImportService.importQuery(query);
    }

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
