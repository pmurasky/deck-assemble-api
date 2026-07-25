package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPriceSnapshot;
import com.deckassemble.cards.domain.CardPriceSnapshotRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class CardPriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardPriceService.class);

    private final CardPrintingRepository cardPrintingRepository;
    private final CardPriceSnapshotRepository snapshotRepository;
    private final ScryfallClient scryfallClient;

    public CardPriceService(
            CardPrintingRepository cardPrintingRepository,
            CardPriceSnapshotRepository snapshotRepository,
            ScryfallClient scryfallClient) {
        this.cardPrintingRepository = cardPrintingRepository;
        this.snapshotRepository = snapshotRepository;
        this.scryfallClient = scryfallClient;
    }

    @Transactional
    public int refreshPrices(Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return 0;
        }
        Map<Long, String> scryfallIds =
                cardPrintingRepository.findAllById(cardPrintingIds).stream()
                        .collect(
                                Collectors.toMap(
                                        CardPrinting::getId, CardPrinting::getScryfallCardId));
        var fetchedAt = Instant.now();
        var refreshed = 0;
        for (var entry : scryfallIds.entrySet()) {
            if (refreshOne(entry, fetchedAt)) {
                refreshed++;
            }
        }
        return refreshed;
    }

    private boolean refreshOne(Map.Entry<Long, String> entry, Instant fetchedAt) {
        try {
            CardPrice price = scryfallClient.getCardPrice(entry.getValue());
            snapshotRepository.save(new CardPriceSnapshot(entry.getKey(), price, fetchedAt));
            return true;
        } catch (RestClientException exception) {
            LOGGER.warn("Failed to refresh price for printing {}", entry.getKey(), exception);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, CardPrice> latestPrices(Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return Map.of();
        }
        return snapshotRepository.findLatestByCardPrintingIds(cardPrintingIds).stream()
                .collect(
                        Collectors.toMap(
                                CardPriceSnapshot::getCardPrintingId, CardPriceSnapshot::toPrice));
    }

    @Transactional(readOnly = true)
    public Set<Long> trackedPrintingIds() {
        return snapshotRepository.findTrackedPrintingIds();
    }
}
