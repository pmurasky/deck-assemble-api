package com.deckassemble.imports.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardImportFace;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.ScryfallClient;
import com.deckassemble.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CardImportService.class);

    private final ScryfallClient scryfallClient;
    private final CardImportCardStore cardStore;
    private final CardPrintingImporter cardPrintingImporter;
    private final ImportRunRecorder runRecorder;
    private final CurrentUser currentUser;

    public CardImportService(
            ScryfallClient scryfallClient,
            CardImportCardStore cardStore,
            CardPrintingImporter cardPrintingImporter,
            ImportRunRecorder runRecorder,
            CurrentUser currentUser) {
        this.scryfallClient = scryfallClient;
        this.cardStore = cardStore;
        this.cardPrintingImporter = cardPrintingImporter;
        this.runRecorder = runRecorder;
        this.currentUser = currentUser;
    }

    @Transactional
    public ImportResult importQuery(String query) {
        long runId = runRecorder.start(query, currentUser.subject().orElse("system"));
        return importQuery(runId, query);
    }

    @Transactional
    public ImportResult importQuery(long runId, String query) {
        var counters = new Counters();
        try {
            var page = scryfallClient.searchCards(query);
            importPage(page.data(), counters);
            while (page.hasMore()) {
                page = scryfallClient.searchCards(nextPage(page.nextPage()));
                importPage(page.data(), counters);
            }
            runRecorder.complete(
                    runId, counters.read, counters.created, counters.updated, counters.skipped);
            return counters.result(runId);
        } catch (RuntimeException exception) {
            LOGGER.error("Card import failed for query '{}'", query, exception);
            runRecorder.fail(runId, String.valueOf(exception.getMessage()));
            throw exception;
        }
    }

    private void importPage(List<CardImportData> cards, Counters counters) {
        cards.forEach(card -> counters.add(importCard(card)));
    }

    private URI nextPage(URI nextPage) {
        if (nextPage == null) {
            throw new IllegalStateException(
                    "Scryfall response marked additional pages without a next page URL");
        }
        return nextPage;
    }

    private Outcome importCard(CardImportData source) {
        if (source.id() == null
                || source.oracleId() == null
                || source.setId() == null
                || source.set() == null) {
            return Outcome.SKIPPED;
        }
        Card card =
                cardStore
                        .findByScryfallOracleId(source.oracleId())
                        .orElseGet(() -> new Card(source.oracleId(), source.name()));
        applyCardDetails(card, source);
        card = cardStore.save(card);
        return cardPrintingImporter.importPrinting(card, source)
                ? Outcome.UPDATED
                : Outcome.CREATED;
    }

    private void applyCardDetails(Card card, CardImportData source) {
        card.setManaCost(source.manaCost());
        card.setManaValue(source.cmc() == null ? null : BigDecimal.valueOf(source.cmc()));
        card.setTypeLine(source.typeLine());
        card.setOracleText(source.oracleText());
        card.setPower(source.power());
        card.setToughness(source.toughness());
        card.setLoyalty(source.loyalty());
        card.setColors(join(source.colors()));
        card.setColorIdentity(join(source.colorIdentity()));
        card.setKeywords(join(source.keywords()));
        card.setLayout(source.layout());
        card.setReserved(source.reserved());
        card.setGameChanger(Boolean.TRUE.equals(source.gameChanger()));
        replaceCardFaces(card, source.faces());
        replaceLegalities(card, source);
    }

    private void replaceCardFaces(Card card, List<CardImportFace> sourceFaces) {
        card.getFaces().clear();
        for (int faceOrder = 0; faceOrder < sourceFaces.size(); faceOrder++) {
            card.getFaces().add(sourceFaces.get(faceOrder).toCardFace(card, faceOrder));
        }
    }

    private void replaceLegalities(Card card, CardImportData source) {
        var legalities = card.getLegalities();
        if (source.legalities() == null) {
            legalities.clear();
            return;
        }
        legalities.removeIf(legality -> !source.legalities().containsKey(legality.getFormatCode()));
        source.legalities()
                .forEach(
                        (format, status) ->
                                legalities.stream()
                                        .filter(legality -> legality.getFormatCode().equals(format))
                                        .findFirst()
                                        .ifPresentOrElse(
                                                legality -> legality.updateStatus(status),
                                                () ->
                                                        legalities.add(
                                                                new CardLegality(
                                                                        card, format, status))));
    }

    private @Nullable String join(@Nullable List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    private enum Outcome {
        CREATED,
        UPDATED,
        SKIPPED
    }

    private static final class Counters {
        private int read;
        private int created;
        private int updated;
        private int skipped;

        private void add(Outcome outcome) {
            read++;
            switch (outcome) {
                case CREATED -> created++;
                case UPDATED -> updated++;
                case SKIPPED -> skipped++;
                default -> throw new IllegalStateException("Unexpected outcome: " + outcome);
            }
        }

        private ImportResult result(long runId) {
            return new ImportResult(runId, read, created, updated, skipped);
        }
    }
}
