package com.deckassemble.imports.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// ponytail: dev-only trigger until Auth0 admin tokens exist; gated off unless
// deckassemble.dev-import.query is set, remove once the admin endpoint is exercised for real
@Component
@ConditionalOnProperty("deckassemble.dev-import.query")
public class DevCardImportRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DevCardImportRunner.class);

    private final CardImportService cardImportService;
    private final String query;

    public DevCardImportRunner(
            CardImportService cardImportService,
            @Value("${deckassemble.dev-import.query}") String query) {
        this.cardImportService = cardImportService;
        this.query = query;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOG.info("Starting dev card import for query '{}'", query);
        var result = cardImportService.importQuery(query);
        LOG.info(
                "Dev card import completed: read={}, created={}, updated={}, failed={}",
                result.recordsRead(),
                result.recordsCreated(),
                result.recordsUpdated(),
                result.recordsFailed());
    }
}
