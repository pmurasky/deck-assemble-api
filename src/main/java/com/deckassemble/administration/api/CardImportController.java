package com.deckassemble.administration.api;

import com.deckassemble.imports.application.CardImportTrigger;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.domain.CardSeries;
import com.deckassemble.shared.security.CurrentUser;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
            @RequestParam(required = false) String query,
            @RequestParam(required = false) List<String> series) {
        String effectiveQuery = resolveQuery(query, series);
        long runId =
                cardImportTrigger.trigger(effectiveQuery, currentUser.subject().orElse("system"));
        return ResponseEntity.accepted().body(new ImportAcceptedResponse(runId));
    }

    private String resolveQuery(String query, List<String> seriesKeys) {
        boolean hasQuery = query != null && !query.isBlank();
        boolean hasSeries = seriesKeys != null && !seriesKeys.isEmpty();
        if (hasQuery == hasSeries) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Provide exactly one of 'query' or 'series'");
        }
        if (hasQuery) {
            return query;
        }
        return CardSeries.toQueryFragment(seriesKeys.stream().map(this::parseSeries).toList());
    }

    private CardSeries parseSeries(String key) {
        return CardSeries.fromKey(key)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST, "Unknown card series: " + key));
    }

    public record ImportAcceptedResponse(long runId) {}

    @GetMapping("/series")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CardSeriesResponse> cardSeries() {
        return Arrays.stream(CardSeries.values()).map(CardSeriesResponse::from).toList();
    }

    public record CardSeriesResponse(String key, String label) {

        static CardSeriesResponse from(CardSeries series) {
            return new CardSeriesResponse(series.name(), series.label());
        }
    }

    @PostMapping("/oracle-tags")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ImportAcceptedResponse> importOracleTags() {
        long runId =
                cardImportTrigger.triggerOracleTagImport(currentUser.subject().orElse("system"));
        return ResponseEntity.accepted().body(new ImportAcceptedResponse(runId));
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
            @Nullable String errorSummary,
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
                    run.getErrorSummary(),
                    run.getStartedAt(),
                    run.getCompletedAt());
        }
    }
}
