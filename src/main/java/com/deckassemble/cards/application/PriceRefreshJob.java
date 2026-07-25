package com.deckassemble.cards.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PriceRefreshJob {

    private final CardPriceService cardPriceService;

    public PriceRefreshJob(CardPriceService cardPriceService) {
        this.cardPriceService = cardPriceService;
    }

    /** Refreshes prices daily for printings we already track (spec D8). */
    @Scheduled(cron = "0 0 6 * * *")
    public void refreshTrackedPrices() {
        cardPriceService.refreshPrices(cardPriceService.trackedPrintingIds());
    }
}
