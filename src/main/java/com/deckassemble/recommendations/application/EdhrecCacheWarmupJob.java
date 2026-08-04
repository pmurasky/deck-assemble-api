package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CommanderEligibility;
import com.deckassemble.collections.application.CollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Warms the EDHREC commander cache for every commander present in any user collection, so
 * recommendation requests hit warm cache. The 7-day cache TTL in {@link EdhrecCommanderService}
 * means only stale entries trigger network calls.
 */
@Component
public class EdhrecCacheWarmupJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(EdhrecCacheWarmupJob.class);

    private final CollectionService collectionService;
    private final CardCatalogService cardCatalogService;
    private final EdhrecCommanderService edhrecCommanderService;

    public EdhrecCacheWarmupJob(
            CollectionService collectionService,
            CardCatalogService cardCatalogService,
            EdhrecCommanderService edhrecCommanderService) {
        this.collectionService = collectionService;
        this.cardCatalogService = cardCatalogService;
        this.edhrecCommanderService = edhrecCommanderService;
    }

    /** Refreshes stale EDHREC payloads for owned commanders daily at 6:15. */
    @Scheduled(cron = "0 15 6 * * *")
    public void warmCommanderCache() {
        var printingIds = collectionService.getAllOwnedPrintingIds();
        if (printingIds.isEmpty()) {
            return;
        }
        cardCatalogService.getCardsByPrintingIds(printingIds).values().stream()
                .filter(card -> Boolean.TRUE.equals(card.getActive()))
                .filter(CommanderEligibility::isEligible)
                .forEach(this::warm);
        LOGGER.info("EDHREC commander cache warmup complete");
    }

    private void warm(Card commander) {
        try {
            edhrecCommanderService.getCommanderData(
                    commander.getScryfallOracleId(), commander.getName());
        } catch (RestClientException exception) {
            LOGGER.warn("EDHREC warmup failed for commander {}", commander.getName(), exception);
        }
    }
}
