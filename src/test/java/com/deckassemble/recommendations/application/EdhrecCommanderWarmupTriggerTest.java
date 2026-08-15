package com.deckassemble.recommendations.application;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class EdhrecCommanderWarmupTriggerTest {

    private static final long VERIFY_TIMEOUT_MS = 5000;
    private static final String ORACLE_ID = "oracle-1";
    private static final String COMMANDER_NAME = "Atraxa, Praetors' Voice";

    @Mock private EdhrecCommanderService commanderService;

    @Test
    void shouldWarmCommanderCacheInBackground() {
        EdhrecCommanderWarmupTrigger trigger = new EdhrecCommanderWarmupTrigger(commanderService);

        trigger.warmInBackground(ORACLE_ID, COMMANDER_NAME);

        verify(commanderService, timeout(VERIFY_TIMEOUT_MS)).getCommanderData(ORACLE_ID, COMMANDER_NAME);
        trigger.shutdown();
    }

    @Test
    void shouldSwallowRestClientExceptionFromWarmup() {
        doThrow(new RestClientException("boom"))
                .when(commanderService)
                .getCommanderData(ORACLE_ID, COMMANDER_NAME);
        EdhrecCommanderWarmupTrigger trigger = new EdhrecCommanderWarmupTrigger(commanderService);

        trigger.warmInBackground(ORACLE_ID, COMMANDER_NAME);

        verify(commanderService, timeout(VERIFY_TIMEOUT_MS)).getCommanderData(ORACLE_ID, COMMANDER_NAME);
        trigger.shutdown();
    }
}
