package com.deckassemble.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.domain.CardImportRunRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ImportRunRecorderTest {

    @Mock private CardImportRunRepository repository;

    private ImportRunRecorder recorder() {
        return new ImportRunRecorder(repository);
    }

    private CardImportRun newRun() {
        return new CardImportRun("SCRYFALL", "goblins", OffsetDateTime.now(), "admin");
    }

    @Test
    void shouldStartRunAndReturnId() {
        when(repository.save(any(CardImportRun.class)))
                .thenAnswer(
                        invocation -> {
                            CardImportRun run = invocation.getArgument(0);
                            ReflectionTestUtils.setField(run, "id", 42L);
                            return run;
                        });

        assertThat(recorder().start("goblins", "admin")).isEqualTo(42L);
    }

    @Test
    void shouldRecordCountsAndCompleteRun() {
        CardImportRun run = newRun();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        recorder().complete(1L, 5, 3, 2, 0);

        assertThat(run.getRecordsRead()).isEqualTo(5);
        assertThat(run.getRecordsCreated()).isEqualTo(3);
        assertThat(run.getRecordsUpdated()).isEqualTo(2);
        assertThat(run.getRecordsFailed()).isZero();
        assertThat(run.getStatus()).isEqualTo(CardImportRun.Status.COMPLETED);
        verify(repository).save(run);
    }

    @Test
    void shouldMarkCompletedWithErrorsWhenSkippedExceedsReadRemainder() {
        CardImportRun run = newRun();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        recorder().complete(1L, 5, 2, 1, 4);

        assertThat(run.getRecordsFailed()).isEqualTo(4);
        assertThat(run.getStatus()).isEqualTo(CardImportRun.Status.COMPLETED_WITH_ERRORS);
        verify(repository).save(run);
    }

    @Test
    void shouldIgnoreCompleteWhenRunMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        recorder().complete(99L, 1, 1, 0, 0);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldFailRunWithTruncatedErrorSummary() {
        CardImportRun run = newRun();
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        recorder().fail(1L, "x".repeat(3000));

        assertThat(run.getStatus()).isEqualTo(CardImportRun.Status.FAILED);
        assertThat(run.getErrorSummary()).hasSize(2000);
        verify(repository).save(run);
    }

    @Test
    void shouldReturnHistory() {
        List<CardImportRun> runs = List.of(newRun());
        when(repository.findTop20ByOrderByStartedAtDesc()).thenReturn(runs);

        assertThat(recorder().history()).isEqualTo(runs);
    }

    @Test
    void shouldReturnLatestCompleted() {
        CardImportRun run = newRun();
        when(repository.findTopByStatusOrderByCompletedAtDesc(CardImportRun.Status.COMPLETED))
                .thenReturn(Optional.of(run));

        assertThat(recorder().latestCompleted()).contains(run);
    }
}
