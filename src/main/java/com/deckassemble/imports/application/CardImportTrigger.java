package com.deckassemble.imports.application;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Component;

// ponytail: single background thread serializes imports; swap for a job queue if concurrency
// matters
@Component
public class CardImportTrigger {

    private final CardImportService cardImportService;
    private final ImportRunRecorder runRecorder;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CardImportTrigger(CardImportService cardImportService, ImportRunRecorder runRecorder) {
        this.cardImportService = cardImportService;
        this.runRecorder = runRecorder;
    }

    public long trigger(String query, String subject) {
        long runId = runRecorder.start(query, subject);
        executor.execute(() -> cardImportService.importQuery(runId, query));
        return runId;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
