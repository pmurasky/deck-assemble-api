package com.deckassemble.recommendations.application;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

// ponytail: single background thread serializes warmups; swap for a job queue if concurrency matters
@Component
public class EdhrecCommanderWarmupTrigger {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdhrecCommanderWarmupTrigger.class);

    private final EdhrecCommanderService commanderService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public EdhrecCommanderWarmupTrigger(EdhrecCommanderService commanderService) {
        this.commanderService = commanderService;
    }

    public void warmInBackground(String commanderOracleId, String commanderName) {
        executor.execute(() -> warm(commanderOracleId, commanderName));
    }

    private void warm(String commanderOracleId, String commanderName) {
        try {
            commanderService.getCommanderData(commanderOracleId, commanderName);
        } catch (RestClientException exception) {
            LOGGER.warn("EDHREC warmup failed for commander {}", commanderName, exception);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
