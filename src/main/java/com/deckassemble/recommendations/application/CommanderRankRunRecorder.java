package com.deckassemble.recommendations.application;

import com.deckassemble.recommendations.domain.CommanderRankRefreshRun;
import com.deckassemble.recommendations.domain.CommanderRankRefreshRunRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommanderRankRunRecorder {

    private final CommanderRankRefreshRunRepository repository;

    public CommanderRankRunRecorder(CommanderRankRefreshRunRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long start(String triggeredBy) {
        CommanderRankRefreshRun run =
                repository.save(new CommanderRankRefreshRun(OffsetDateTime.now(), triggeredBy));
        return run.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(long runId, int cardsUpdated) {
        repository
                .findById(runId)
                .ifPresent(
                        run -> {
                            run.complete(OffsetDateTime.now(), cardsUpdated);
                            repository.save(run);
                        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(long runId, String errorSummary) {
        repository
                .findById(runId)
                .ifPresent(
                        run -> {
                            run.fail(OffsetDateTime.now(), errorSummary);
                            repository.save(run);
                        });
    }

    @Transactional(readOnly = true)
    public Optional<CommanderRankRefreshRun> latestCompleted() {
        return repository.findTopByStatusOrderByCompletedAtDesc(
                CommanderRankRefreshRun.Status.COMPLETED);
    }
}
