package com.deckassemble.imports.application;

import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.infrastructure.CardImportRunRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportRunRecorder {

  private final CardImportRunRepository repository;

  public ImportRunRecorder(CardImportRunRepository repository) {
    this.repository = repository;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public long start(String query, String createdBy) {
    return repository.save(new CardImportRun("SCRYFALL", query, OffsetDateTime.now(), createdBy)).getId();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(long runId, int read, int created, int updated, int skipped) {
    find(runId).ifPresent(run -> {
      run.finish(read, created, updated, skipped, OffsetDateTime.now());
      repository.save(run);
    });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(long runId, String errorSummary) {
    find(runId).ifPresent(run -> {
      run.fail(OffsetDateTime.now(), errorSummary);
      repository.save(run);
    });
  }

  private Optional<CardImportRun> find(long runId) {
    return repository.findById(runId);
  }
}
