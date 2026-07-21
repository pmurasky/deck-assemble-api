package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.infrastructure.MagicSetRepository;
import com.deckassemble.cards.infrastructure.scryfall.ScryfallClient;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallCard;
import com.deckassemble.cards.infrastructure.scryfall.dto.ScryfallImageUris;
import com.deckassemble.imports.application.ImportRunRecorder;
import com.deckassemble.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardImportService {

    private final ScryfallClient scryfallClient;
    private final CardRepository cardRepository;
    private final MagicSetRepository magicSetRepository;
    private final CardPrintingRepository cardPrintingRepository;
    private final ImportRunRecorder runRecorder;
    private final CurrentUser currentUser;

    // Suppressed: six collaborators is what this orchestration service needs; Spring injects them.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CardImportService(
            ScryfallClient scryfallClient,
            CardRepository cardRepository,
            MagicSetRepository magicSetRepository,
            CardPrintingRepository cardPrintingRepository,
            ImportRunRecorder runRecorder,
            CurrentUser currentUser) {
        this.scryfallClient = scryfallClient;
        this.cardRepository = cardRepository;
        this.magicSetRepository = magicSetRepository;
        this.cardPrintingRepository = cardPrintingRepository;
        this.runRecorder = runRecorder;
        this.currentUser = currentUser;
    }

    @Transactional
    public ImportResult importQuery(String query) {
        long runId = runRecorder.start(query, currentUser.subject().orElse("system"));
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
            runRecorder.fail(runId, exception.getMessage());
            throw exception;
        }
    }

    private void importPage(List<ScryfallCard> cards, Counters counters) {
        cards.forEach(card -> counters.add(importCard(card)));
    }

    private URI nextPage(URI nextPage) {
        if (nextPage == null) {
            throw new IllegalStateException(
                    "Scryfall response marked additional pages without a next page URL");
        }
        return nextPage;
    }

    private Outcome importCard(ScryfallCard source) {
        if (source.id() == null
                || source.oracleId() == null
                || source.setId() == null
                || source.set() == null) {
            return Outcome.SKIPPED;
        }
        Card card =
                cardRepository
                        .findByScryfallOracleId(source.oracleId())
                        .orElseGet(() -> new Card(source.oracleId(), source.name()));
        applyCardDetails(card, source);
        card = cardRepository.save(card);
        return savePrinting(card, resolveSet(source), source);
    }

    private MagicSet resolveSet(ScryfallCard source) {
        return magicSetRepository
                .findBySetCode(source.set())
                .orElseGet(
                        () ->
                                magicSetRepository.save(
                                        new MagicSet(
                                                source.setId(), source.set(), source.setName())));
    }

    private void applyCardDetails(Card card, ScryfallCard source) {
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
        replaceLegalities(card, source);
    }

    private void replaceLegalities(Card card, ScryfallCard source) {
        card.getLegalities().clear();
        if (source.legalities() == null) {
            return;
        }
        source.legalities()
                .forEach(
                        (format, status) ->
                                card.getLegalities().add(new CardLegality(card, format, status)));
    }

    private String join(List<String> values) {
        return values == null ? null : String.join(",", values);
    }

    private Outcome savePrinting(Card card, MagicSet set, ScryfallCard source) {
        var existing = cardPrintingRepository.findByScryfallCardId(source.id());
        CardPrinting printing = existing.orElseGet(() -> new CardPrinting(card, set, source.id()));
        printing.setCollectorNumber(source.collectorNumber());
        printing.setRarity(source.rarity());
        printing.setArtist(source.artist());
        printing.setFlavorText(source.flavorText());
        printing.setReleasedAt(source.releasedAt());
        printing.setFoilAvailable(source.foil());
        printing.setNonfoilAvailable(source.nonfoil());
        printing.setPromo(source.promo());
        printing.setDigital(source.digital());
        printing.setLanguage(source.lang());
        applyImageUris(printing, source);
        cardPrintingRepository.save(printing);
        return existing.isPresent() ? Outcome.UPDATED : Outcome.CREATED;
    }

    private void applyImageUris(CardPrinting printing, ScryfallCard source) {
        ScryfallImageUris imageUris = imageUris(source);
        if (imageUris == null) {
            return;
        }
        printing.setImageUriSmall(imageUris.small());
        printing.setImageUriNormal(imageUris.normal());
        printing.setImageUriLarge(imageUris.large());
    }

    private ScryfallImageUris imageUris(ScryfallCard source) {
        if (source.imageUris() != null) {
            return source.imageUris();
        }
        if (source.cardFaces() == null) {
            return null;
        }
        return source.cardFaces().stream()
                .map(face -> face.imageUris())
                .filter(uri -> uri != null)
                .findFirst()
                .orElse(null);
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
