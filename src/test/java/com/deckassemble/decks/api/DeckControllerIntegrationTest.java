package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardLegalityRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.decks.application.exporting.DeckExportFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class DeckControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardLegalityRepository cardLegalityRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;

    @Autowired
    private com.deckassemble.cards.domain.CardPriceSnapshotRepository cardPriceSnapshotRepository;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private com.deckassemble.recommendations.domain.CommanderSpellbookClient
            commanderSpellbookClient;

    @Test
    void shouldCreateUpdateDuplicateArchiveAndDeleteDeck() throws Exception {
        String subject = "auth0|deck-owner";
        long commanderCardId =
                cardRepository.save(new Card("oracle-commander", "Commander")).getId();
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Spider-Verse Commander\",\"formatCode\":\"COMMANDER\","
                                                        + "\"commanderCardId\":"
                                                        + commanderCardId
                                                        + ",\"desiredPowerLevel\":5}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value("Spider-Verse Commander"))
                        .andExpect(jsonPath("$.commanderName").value("Commander"))
                        .andExpect(jsonPath("$.cardCount").value(0))
                        .andExpect(jsonPath("$.status").value("DRAFT"))
                        .andReturn();
        long deckId = idFrom(result);

        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Commander\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Commander"));

        mockMvc.perform(
                        post("/decks/{deckId}/duplicate", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Updated Commander (Copy)"));

        mockMvc.perform(
                        post("/decks/{deckId}/archive", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(
                        delete("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldManageDeckCardsWithSectionsAndMerging() throws Exception {
        String subject = "auth0|deck-cards";
        long deckId = createDeck(subject);
        long printingId = createPrinting("deckprint");

        String request =
                "{\"cardPrintingId\":%d,\"quantity\":1,\"deckSection\":\"MAIN_DECK\"}"
                        .formatted(printingId);
        mockMvc.perform(
                        post("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(2))
                .andExpect(jsonPath("$[0].deckSection").value("MAIN_DECK"))
                .andExpect(jsonPath("$[0].card.name").value("Deck Card"));

        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardCount").value(2));
    }

    @Test
    void shouldHideAnotherUsersDeck() throws Exception {
        long deckId = createDeck("auth0|deck-private");

        mockMvc.perform(
                        get("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-other"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    @Test
    void shouldExportDeterministicAttachmentWithExactPrintingFields() throws Exception {
        String subject = "auth0|deck-export";
        long deckId = createDeck(subject, "Unsafe ../ Deck");
        Card card = cardRepository.save(new Card("oracle-export", "Original Card Name"));
        MagicSet set = magicSetRepository.save(new MagicSet("set-export", "TST", "Test Set"));
        CardPrinting printing = new CardPrinting(card, set, "00000000-0000-0000-0000-000000000099");
        printing.setCollectorNumber("007");
        printing.setFlavorName("Flavor Name");
        long printingId = cardPrintingRepository.save(printing).getId();
        addCard(subject, deckId, printingId, 2);

        MvcResult first = export(subject, deckId, "GENERIC_CSV");
        MvcResult second = export(subject, deckId, "GENERIC_CSV");

        String expected =
                "quantity,name,set,collector_number,section,scryfall_id\n"
                        + "2,Flavor Name,TST,007,main,00000000-0000-0000-0000-000000000099\n";
        assertThat(first.getResponse().getContentAsString()).isEqualTo(expected);
        assertThat(second.getResponse().getContentAsByteArray())
                .containsExactly(first.getResponse().getContentAsByteArray());
    }

    @Test
    void shouldHideAnotherUsersDeckExport() throws Exception {
        long deckId = createDeck("auth0|deck-export-private");

        mockMvc.perform(
                        get("/decks/{deckId}/exports", deckId)
                                .param("format", "ARENA_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-export-other"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    @Test
    void shouldRejectUnsupportedDeckExportFormat() throws Exception {
        long deckId = createDeck("auth0|deck-export-format");

        mockMvc.perform(
                        get("/decks/{deckId}/exports", deckId)
                                .param("format", "UNKNOWN")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-export-format"))))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(DeckExportFormat.class)
    void shouldServeEveryDeckExportFormat(DeckExportFormat format) throws Exception {
        String subject = "auth0|deck-export-" + format;
        long deckId = createDeck(subject);

        mockMvc.perform(
                        get("/decks/{deckId}/exports", deckId)
                                .param("format", format.name())
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(format.mediaType()))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        containsString(format.filenameSuffix())));
    }

    @Test
    void shouldReportMissingCommanderWithoutRejectingDraft() throws Exception {
        String subject = "auth0|legality-draft";
        long deckId = createDeck(subject);

        mockMvc.perform(
                        get("/decks/{deckId}/legality", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legal").value(false))
                .andExpect(jsonPath("$.violations[0].code").value("COMMANDER_REQUIRED"));
    }

    @Test
    void shouldRejectUnmatchedPartnerWithCommanders() throws Exception {
        String subject = "auth0|legality-pair";
        long first = createLegalCommander("partner-first", "First", "Partner with Alice");
        long second = createLegalCommander("partner-second", "Second", "Partner with Bob");
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\",\"commanderCardId\":"
                                                        + first
                                                        + ",\"secondaryCommanderCardId\":"
                                                        + second
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn();

        mockMvc.perform(
                        get("/decks/{deckId}/legality", idFrom(result))
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations[*].code", hasItem("COMMANDER_PAIR_INVALID")));
    }

    @Test
    void shouldReportColorSingletonLegalityAndDeckSizeViolations() throws Exception {
        String subject = "auth0|legality-rules";
        long commanderId = createLegalCommander("white", "White Commander", "");
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\",\"commanderCardId\":"
                                                        + commanderId
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        long printingId = createIllegalBluePrinting();

        mockMvc.perform(
                        post("/decks/{deckId}/cards", idFrom(result))
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"cardPrintingId\":" + printingId + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/decks/{deckId}/legality", idFrom(result))
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legal").value(false))
                .andExpect(jsonPath("$.violations[*].code", hasItem("COLOR_IDENTITY_VIOLATION")))
                .andExpect(jsonPath("$.violations[*].code", hasItem("SINGLETON_VIOLATION")))
                .andExpect(jsonPath("$.violations[*].code", hasItem("COMMANDER_LEGALITY_INVALID")))
                .andExpect(jsonPath("$.violations[*].code", hasItem("DECK_SIZE_INVALID")));
    }

    @Test
    void shouldHydrateCommanderInDeckAndCardsResponses() throws Exception {
        String subject = "auth0|commander-hydration";
        Card commander = new Card("oracle-hydrated-commander", "Hydrated Commander");
        commander.setTypeLine("Legendary Creature — Human");
        commander.setColorIdentity("W");
        commander = cardRepository.save(commander);
        cardLegalityRepository.save(new CardLegality(commander, "commander", "legal"));
        MagicSet set = magicSetRepository.save(new MagicSet("set-hyd", "hyd", "Hyd Set"));
        long printingId =
                cardPrintingRepository
                        .save(new CardPrinting(commander, set, "printing-hyd"))
                        .getId();
        long commanderCardId = commander.getId();

        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\",\"commanderCardId\":"
                                                        + commanderCardId
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.commander.name").value("Hydrated Commander"))
                        .andReturn();
        long deckId = idFrom(result);

        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commander.name").value("Hydrated Commander"))
                .andExpect(jsonPath("$.commander.printingId").value((int) printingId));

        mockMvc.perform(
                        get("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].deckSection").value("COMMANDER"))
                .andExpect(jsonPath("$[0].quantity").value(1))
                .andExpect(jsonPath("$[0].cardPrintingId").value((int) printingId))
                .andExpect(jsonPath("$[0].card.name").value("Hydrated Commander"));
    }

    @Test
    void shouldReturnAnalysisForOwnedDeck() throws Exception {
        // Given a deck with spells, lands, a game changer, and one priced printing
        String subject = "auth0|deck-analysis";
        long deckId = createDeck(subject);
        long boltPrinting =
                createAnalysisPrinting(
                        "Lightning Bolt",
                        "{R}",
                        "1",
                        "Sorcery",
                        "Lightning Bolt deals 3 damage to any target.");
        long forestPrinting =
                createAnalysisPrinting("Forest", null, "0", "Basic Land — Forest", "{T}: Add {G}.");
        long ringPrinting =
                createAnalysisPrinting("Sol Ring", "{1}", "1", "Artifact", "{T}: Add {C}{C}.");
        markGameChanger(ringPrinting);
        addCard(subject, deckId, boltPrinting, 2);
        addCard(subject, deckId, forestPrinting, 4);
        addCard(subject, deckId, ringPrinting, 1);
        cardPriceSnapshotRepository.save(
                new com.deckassemble.cards.domain.CardPriceSnapshot(
                        boltPrinting,
                        new com.deckassemble.cards.domain.CardPrice(
                                new java.math.BigDecimal("2.00"), null,
                                new java.math.BigDecimal("1.50"), null),
                        java.time.Instant.now()));
        org.mockito.Mockito.when(
                        commanderSpellbookClient.findCombos(org.mockito.ArgumentMatchers.any()))
                .thenReturn(
                        java.util.List.of(
                                new com.deckassemble.recommendations.domain.SpellbookCombo(
                                        "c1",
                                        java.util.List.of("A", "B"),
                                        java.util.List.of(),
                                        "desc",
                                        "")));

        // When / Then the analysis reconciles composition, value, legality, and combos
        mockMvc.perform(
                        get("/decks/{deckId}/analysis", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manaCurve['1']").value(3))
                .andExpect(jsonPath("$.typeDistribution.SORCERY").value(2))
                .andExpect(jsonPath("$.typeDistribution.LAND").value(4))
                .andExpect(jsonPath("$.colorDemand.R").value(2))
                .andExpect(jsonPath("$.colorProduction.G").value(4))
                .andExpect(jsonPath("$.colorProduction.C").value(1))
                .andExpect(jsonPath("$.landCount").value(4))
                .andExpect(jsonPath("$.averageManaValue").value(1.0))
                .andExpect(jsonPath("$.ownershipBreakdown.WISHLIST").value(7))
                .andExpect(jsonPath("$.valueByCurrency.usd").value(4.0))
                .andExpect(jsonPath("$.missingCostByCurrency.usd").value(4.0))
                .andExpect(jsonPath("$.unpricedCardCount").value(5))
                .andExpect(jsonPath("$.functionalCategories.LAND").value(4))
                .andExpect(jsonPath("$.functionalCategories.RAMP").value(1))
                .andExpect(jsonPath("$.gameChangers[0]").value("Sol Ring"))
                .andExpect(jsonPath("$.legality.legal").value(false))
                .andExpect(jsonPath("$.combos.available").value(true))
                .andExpect(jsonPath("$.combos.count").value(1));
    }

    @Test
    void shouldHideAnotherUsersDeckAnalysis() throws Exception {
        long deckId = createDeck("auth0|deck-analysis-private");

        mockMvc.perform(
                        get("/decks/{deckId}/analysis", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-analysis-other"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    private long createDeck(String subject) throws Exception {
        return createDeck(subject, "Deck");
    }

    private long createDeck(String subject, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"%s\",\"formatCode\":\"COMMANDER\"}"
                                                        .formatted(name)))
                        .andExpect(status().isCreated())
                        .andReturn();
        return idFrom(result);
    }

    private void addCard(String subject, long deckId, long printingId, int quantity)
            throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"cardPrintingId\":%d,\"quantity\":%d}"
                                                .formatted(printingId, quantity)))
                .andExpect(status().isCreated());
    }

    private MvcResult export(String subject, long deckId, String format) throws Exception {
        return mockMvc.perform(
                        get("/decks/{deckId}/exports", deckId)
                                .param("format", format)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(
                        header().string(
                                        "Content-Disposition",
                                        "attachment; filename=\"Unsafe-Deck-generic.csv\""))
                .andReturn();
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long createPrinting(String identifier) {
        Card card = cardRepository.save(new Card("oracle-" + identifier, "Deck Card"));
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-" + identifier, identifier, "Deck Set"));
        return cardPrintingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }

    private long createAnalysisPrinting(
            String name, String manaCost, String manaValue, String typeLine, String oracleText) {
        String identifier = name.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        String setCode = identifier.substring(0, Math.min(identifier.length(), 10));
        Card card = new Card("oracle-" + identifier, name);
        card.setManaCost(manaCost);
        card.setManaValue(new java.math.BigDecimal(manaValue));
        card.setTypeLine(typeLine);
        card.setOracleText(oracleText);
        var face = new com.deckassemble.cards.domain.CardFace(card, 0, name);
        face.setManaCost(manaCost);
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        card.getFaces().add(face);
        card = cardRepository.save(card);
        MagicSet set = magicSetRepository.save(new MagicSet("set-" + identifier, setCode, "S"));
        return cardPrintingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }

    private void markGameChanger(long printingId) {
        CardPrinting printing = cardPrintingRepository.findById(printingId).orElseThrow();
        printing.getCard().setGameChanger(true);
        cardRepository.save(printing.getCard());
    }

    private long createLegalCommander(String identifier, String name, String oracleText) {
        Card card = new Card("oracle-" + identifier, name);
        card.setTypeLine("Legendary Creature — Human");
        card.setOracleText(oracleText);
        card.setColorIdentity("W");
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        return card.getId();
    }

    private long createIllegalBluePrinting() {
        Card card = new Card("oracle-blue", "Blue Card");
        card.setColorIdentity("U");
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "banned"));
        MagicSet set = magicSetRepository.save(new MagicSet("set-blue", "blue", "Blue Set"));
        return cardPrintingRepository.save(new CardPrinting(card, set, "printing-blue")).getId();
    }
}
