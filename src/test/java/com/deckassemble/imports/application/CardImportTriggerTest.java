package com.deckassemble.imports.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardImportTriggerTest {

    private static final long VERIFY_TIMEOUT_MS = 5000;

    @Mock private CardImportService cardImportService;
    @Mock private ImportRunRecorder runRecorder;

    @Test
    void shouldStartRunImmediatelyAndImportInBackground() {
        when(runRecorder.start("set:mar", "admin-sub")).thenReturn(7L);
        CardImportTrigger trigger = new CardImportTrigger(cardImportService, runRecorder);

        long runId = trigger.trigger("set:mar", "admin-sub");

        assertThat(runId).isEqualTo(7L);
        verify(cardImportService, timeout(VERIFY_TIMEOUT_MS)).importQuery(7L, "set:mar");
        trigger.shutdown();
    }
}
