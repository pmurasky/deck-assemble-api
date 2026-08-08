package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardLegalityRepository;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPriceSnapshot;
import com.deckassemble.cards.domain.CardPriceSnapshotRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.recommendations.domain.EdhrecClient;
import com.deckassemble.users.domain.ProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class DeckUpgradePlansIntegrationTest extends AbstractIntegrationTest {

    private static final String UPG_PROXY_PAYLOAD =
            """
            {"container":{"json_dict":{"cardlists":[
              {"header":"Card Advantage","cardviews":[
                {"name":"Upg Owned Draw","synergy":0.9,"inclusion":100},
                {"name":"Upg Off Color","synergy":0.99,"inclusion":300}
              ]}]}}}
            """;

    private static final String UPG_BUDGET_PAYLOAD =
            """
            {"container":{"json_dict":{"cardlists":[
              {"header":"Card Advantage","cardviews":[
                {"name":"Upg Pricey Draw","synergy":0.95,"inclusion":200},
                {"name":"Upg Cheap Draw","synergy":0.9,"inclusion":100}
              ]}]}}}
            """;

    private static final String UPG_GAPS_PAYLOAD =
            """
            {"container":{"json_dict":{"cardlists":[
              {"header":"Ramp","cardviews":[
                {"name":"Upg Ramp Rock","synergy":0.9,"inclusion":100}
              ]}]}}}
            """;

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardLegalityRepository cardLegalityRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private CardPriceSnapshotRepository cardPriceSnapshotRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private CardCollectionRepository cardCollectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;
    @Autowired private DeckCardRepository deckCardRepository;

    @MockitoBean private EdhrecClient edhrecClient;

    @Test
    void shouldGenerateProxyReplacementPlanWithoutMutatingDeck() throws Exception {
        String subject = "auth0|deck-upg-proxies";
        long deckId = createDeckWithCommander(subject, createUpgradeCommander("Upg Cmd Proxies"));
        long proxyPrinting = createDrawPrinting("Upg Proxy Draw");
        long ownedPrinting = createDrawPrinting("Upg Owned Draw");
        createOffColorPrinting("Upg Off Color");
        ownPrinting(subject, ownedPrinting);
        long deckCardId = addCardAndReturnId(subject, deckId, proxyPrinting);
        markProxy(deckId, deckCardId);
        Mockito.when(edhrecClient.fetchCommanderData("upg-cmd-proxies"))
                .thenReturn(UPG_PROXY_PAYLOAD);
        String cardsBefore = getCardsJson(subject, deckId);

        String planBody =
                postPlan(subject, deckId, "{\"objective\":\"REPLACE_PROXIES_WITH_OWNED\"}")
                        .andExpect(jsonPath("$.objective").value("REPLACE_PROXIES_WITH_OWNED"))
                        .andExpect(jsonPath("$.substitutions.length()").value(1))
                        .andExpect(
                                jsonPath("$.substitutions[0].removedName").value("Upg Proxy Draw"))
                        .andExpect(
                                jsonPath("$.substitutions[0].removedOwnershipStatus")
                                        .value("PROXY"))
                        .andExpect(jsonPath("$.substitutions[0].addedName").value("Upg Owned Draw"))
                        .andExpect(jsonPath("$.substitutions[0].addedOwned").value(true))
                        .andExpect(jsonPath("$.substitutions[0].cost").value(0))
                        .andExpect(jsonPath("$.before.ownershipBreakdown.PROXY").value(1))
                        .andExpect(jsonPath("$.after.ownershipBreakdown.PROXY").doesNotExist())
                        .andExpect(jsonPath("$.after.ownershipBreakdown.OWNED").value(1))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String planAgain =
                postPlan(subject, deckId, "{\"objective\":\"REPLACE_PROXIES_WITH_OWNED\"}")
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(planAgain).isEqualTo(planBody);
        assertThat(getCardsJson(subject, deckId)).isEqualTo(cardsBefore);
    }

    @Test
    void shouldReturnEmptyUpgradePlanWhenNothingToReplace() throws Exception {
        String subject = "auth0|deck-upg-none";
        long deckId = createDeck(subject, "Upg None Deck");
        addCard(subject, deckId, createDrawPrinting("Upg Settled Draw"), 1);

        postPlan(subject, deckId, "{\"objective\":\"REPLACE_PROXIES_WITH_OWNED\"}")
                .andExpect(jsonPath("$.substitutions").isEmpty())
                .andExpect(jsonPath("$.before.ownershipBreakdown.WISHLIST").value(1))
                .andExpect(jsonPath("$.after.ownershipBreakdown.WISHLIST").value(1));
    }

    @Test
    void shouldImproveUnderBudgetCeiling() throws Exception {
        String subject = "auth0|deck-upg-budget";
        long deckId = createDeckWithCommander(subject, createUpgradeCommander("Upg Cmd Budget"));
        addCard(subject, deckId, createDrawPrinting("Upg Budget Draw"), 1);
        long priceyPrinting = createDrawPrinting("Upg Pricey Draw");
        long cheapPrinting = createDrawPrinting("Upg Cheap Draw");
        snapshotPrice(priceyPrinting, "10.00");
        snapshotPrice(cheapPrinting, "2.00");
        Mockito.when(edhrecClient.fetchCommanderData("upg-cmd-budget"))
                .thenReturn(UPG_BUDGET_PAYLOAD);

        postPlan(
                        subject,
                        deckId,
                        "{\"objective\":\"IMPROVE_UNDER_BUDGET\",\"budget\":5,"
                                + "\"currency\":\"usd\",\"maxChanges\":3}")
                .andExpect(jsonPath("$.substitutions.length()").value(1))
                .andExpect(jsonPath("$.substitutions[0].removedName").value("Upg Budget Draw"))
                .andExpect(jsonPath("$.substitutions[0].addedName").value("Upg Cheap Draw"))
                .andExpect(jsonPath("$.substitutions[0].cost").value(2.0))
                .andExpect(jsonPath("$.substitutions[0].reasons").isArray());
    }

    @Test
    void shouldCloseCategoryGapWithMatchingAlternative() throws Exception {
        String subject = "auth0|deck-upg-gaps";
        long deckId = createDeckWithCommander(subject, createUpgradeCommander("Upg Cmd Gaps"));
        addCard(
                subject,
                deckId,
                createUpgradePrinting(
                        "Upg Filler", "Artifact", "Whenever a creature enters, you gain 1 life."),
                1);
        createUpgradePrinting("Upg Ramp Rock", "Artifact", "{T}: Add {C}.");
        Mockito.when(edhrecClient.fetchCommanderData("upg-cmd-gaps")).thenReturn(UPG_GAPS_PAYLOAD);

        postPlan(subject, deckId, "{\"objective\":\"CLOSE_CATEGORY_GAPS\"}")
                .andExpect(jsonPath("$.substitutions.length()").value(1))
                .andExpect(jsonPath("$.substitutions[0].removedName").value("Upg Filler"))
                .andExpect(jsonPath("$.substitutions[0].addedName").value("Upg Ramp Rock"))
                .andExpect(jsonPath("$.before.functionalCategories.RAMP").doesNotExist())
                .andExpect(jsonPath("$.after.functionalCategories.RAMP").value(1))
                .andExpect(jsonPath("$.after.functionalCategories.SYNERGY").value(1));
    }

    @Test
    void shouldHideUpgradePlansForForeignDeck() throws Exception {
        long deckId = createDeck("auth0|deck-upg-private", "Upg Private Deck");

        mockMvc.perform(
                        post("/decks/{deckId}/upgrade-plans", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-upg-other")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"objective\":\"REPLACE_PROXIES_WITH_OWNED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    @Test
    void shouldRejectInvalidUpgradePlanRequests() throws Exception {
        String subject = "auth0|deck-upg-invalid";
        long deckId = createDeck(subject, "Upg Invalid Deck");

        assertUpgradePlanRejected(subject, deckId, "{}");
        assertUpgradePlanRejected(
                subject, deckId, "{\"objective\":\"IMPROVE_UNDER_BUDGET\",\"budget\":-5}");
        assertUpgradePlanRejected(
                subject, deckId, "{\"objective\":\"IMPROVE_UNDER_BUDGET\",\"currency\":\"btc\"}");
        assertUpgradePlanRejected(
                subject, deckId, "{\"objective\":\"IMPROVE_UNDER_BUDGET\",\"maxChanges\":0}");
        assertUpgradePlanRejected(
                subject, deckId, "{\"objective\":\"IMPROVE_UNDER_BUDGET\",\"maxChanges\":51}");
    }

    private ResultActions postPlan(String subject, long deckId, String body) throws Exception {
        return mockMvc.perform(
                        post("/decks/{deckId}/upgrade-plans", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
    }

    private void assertUpgradePlanRejected(String subject, long deckId, String body)
            throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/upgrade-plans", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    private String getCardsJson(String subject, long deckId) throws Exception {
        return mockMvc.perform(
                        get("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void markProxy(long deckId, long deckCardId) {
        var deckCard = deckCardRepository.findByIdAndDeckId(deckCardId, deckId).orElseThrow();
        deckCard.setOwnershipStatus(DeckCard.OwnershipStatus.PROXY);
        deckCardRepository.save(deckCard);
    }

    private void snapshotPrice(long printingId, String usd) {
        cardPriceSnapshotRepository.save(
                new CardPriceSnapshot(
                        printingId,
                        new CardPrice(new BigDecimal(usd), null, null, null),
                        Instant.now()));
    }

    private long createUpgradeCommander(String name) {
        String identifier = identifierOf(name);
        Card card = new Card("oracle-" + identifier, name);
        card.setColorIdentity("W");
        card.setTypeLine("Legendary Creature — Human");
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        MagicSet set = saveSet(identifier, "Upg");
        cardPrintingRepository.save(new CardPrinting(card, set, "printing-" + identifier));
        return card.getId();
    }

    private long createDrawPrinting(String name) {
        return createUpgradePrinting(name, "Sorcery", "Draw a card.");
    }

    private long createOffColorPrinting(String name) {
        String identifier = identifierOf(name);
        Card card = new Card("oracle-" + identifier, name);
        card.setColorIdentity("U");
        card.setTypeLine("Sorcery");
        card.setOracleText("Draw a card.");
        card.setManaValue(new BigDecimal("2"));
        var face = new CardFace(card, 0, name);
        face.setTypeLine("Sorcery");
        face.setOracleText("Draw a card.");
        card.getFaces().add(face);
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        return cardPrintingRepository
                .save(new CardPrinting(card, saveSet(identifier, "Upg"), "printing-" + identifier))
                .getId();
    }

    private long createUpgradePrinting(String name, String typeLine, String oracleText) {
        String identifier = identifierOf(name);
        Card card = new Card("oracle-" + identifier, name);
        card.setColorIdentity("W");
        card.setTypeLine(typeLine);
        card.setOracleText(oracleText);
        card.setManaValue(new BigDecimal("2"));
        var face = new CardFace(card, 0, name);
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        card.getFaces().add(face);
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        return cardPrintingRepository
                .save(new CardPrinting(card, saveSet(identifier, "Upg"), "printing-" + identifier))
                .getId();
    }

    private MagicSet saveSet(String identifier, String setName) {
        String setCode = identifier.substring(0, Math.min(identifier.length(), 10));
        return magicSetRepository.save(new MagicSet("set-" + identifier, setCode, setName));
    }

    private String identifierOf(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '-');
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

    private long createDeckWithCommander(String subject, long commanderCardId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Upg Deck\",\"formatCode\":\"COMMANDER\","
                                                        + "\"commanderCardId\":"
                                                        + commanderCardId
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        return idFrom(result);
    }

    private long addCardAndReturnId(String subject, long deckId, long printingId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks/{deckId}/cards", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"cardPrintingId\":" + printingId + "}"))
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

    private void ownPrinting(String subject, long printingId) {
        var profile = profileRepository.findByAuthProviderSubject(subject).orElseThrow();
        var collection =
                cardCollectionRepository.save(
                        new CardCollection(profile.getId(), "Upg Collection", "", true));
        collectionCardRepository.save(new CollectionCard(collection.getId(), printingId, 1, 0));
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
