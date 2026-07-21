package com.deckassemble.imports.api;

import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.imports.domain.CardImportRun;
import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/card-imports")
public class ImportStatusController {

    private final ImportRunRecorder importRunRecorder;

    public ImportStatusController(ImportRunRecorder importRunRecorder) {
        this.importRunRecorder = importRunRecorder;
    }

    @GetMapping("/latest")
    public ResponseEntity<ImportRunResponse> latest() {
        return importRunRecorder
                .latestCompleted()
                .map(ImportRunResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record ImportRunResponse(
            long id, String provider, String query, int recordsRead, OffsetDateTime completedAt) {

        static ImportRunResponse from(CardImportRun run) {
            return new ImportRunResponse(
                    run.getId(),
                    run.getProvider(),
                    run.getQuery(),
                    run.getRecordsRead(),
                    run.getCompletedAt());
        }
    }
}
