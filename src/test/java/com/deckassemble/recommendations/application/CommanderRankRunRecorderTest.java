package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.recommendations.domain.CommanderRankRefreshRun;
import com.deckassemble.recommendations.domain.CommanderRankRefreshRunRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommanderRankRunRecorderTest {

    @Mock
    private CommanderRankRefreshRunRepository repository;

    @Test
    void shouldStartRunAndReturnId() {
        when(repository.save(any(CommanderRankRefreshRun.class)))
                .thenAnswer(invocation -> {
                    CommanderRankRefreshRun run = invocation.getArgument(0);
                    ReflectionTestUtils.setField(run, "id", 42L);
                    return run;
                });

        long id = recorder().start("scheduled");

        assertThat(id).isEqualTo(42L);
    }

    @Test
    void shouldRecordCardsUpdatedAndCompleteRun() {
        CommanderRankRefreshRun run = newRun();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        recorder().complete(1L, 357);

        assertThat(run.getStatus()).isEqualTo(CommanderRankRefreshRun.Status.COMPLETED);
        assertThat(run.getCardsUpdated()).isEqualTo(357);
        assertThat(run.getCompletedAt()).isNotNull();
        verify(repository).save(run);
    }

    @Test
    void shouldIgnoreCompleteWhenRunMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        recorder().complete(1L, 10);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailRunWithTruncatedErrorSummary() {
        CommanderRankRefreshRun run = newRun();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        recorder().fail(1L, "x".repeat(3000));

        assertThat(run.getStatus()).isEqualTo(CommanderRankRefreshRun.Status.FAILED);
        assertThat(run.getErrorSummary()).hasSize(2000);
        verify(repository).save(run);
    }

    @Test
    void shouldReturnLatestCompleted() {
        CommanderRankRefreshRun run = newRun();
        when(repository.findTopByStatusOrderByCompletedAtDesc(CommanderRankRefreshRun.Status.COMPLETED))
                .thenReturn(Optional.of(run));

        assertThat(recorder().latestCompleted()).contains(run);
    }

    private CommanderRankRunRecorder recorder() {
        return new CommanderRankRunRecorder(repository);
    }

    private CommanderRankRefreshRun newRun() {
        return new CommanderRankRefreshRun(OffsetDateTime.now(), "scheduled");
    }
}
