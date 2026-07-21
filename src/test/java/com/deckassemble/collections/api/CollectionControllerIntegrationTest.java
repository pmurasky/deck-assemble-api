package com.deckassemble.collections.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CollectionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldCreateUpdateAndDeleteTheCurrentUsersCollection() throws Exception {
        String subject = "auth0|collection-owner";
        MvcResult result =
                mockMvc.perform(
                                post("/collections")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Marvel\",\"defaultCollection\":true}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value("Marvel"))
                        .andReturn();
        long collectionId = idFrom(result);

        mockMvc.perform(
                        patch("/collections/{collectionId}", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Updated Marvel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Marvel"));

        mockMvc.perform(
                        delete("/collections/{collectionId}", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldMergeQuantitiesForTheSamePrinting() throws Exception {
        String subject = "auth0|collection-cards";
        long collectionId = createCollection(subject);
        long printingId = createPrinting("colprint");

        String request =
                "{\"cardPrintingId\":%d,\"regularQuantity\":1,\"foilQuantity\":2}"
                        .formatted(printingId);
        mockMvc.perform(
                        post("/collections/{collectionId}/cards", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/collections/{collectionId}/cards", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/collections/{collectionId}/cards", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].regularQuantity").value(2))
                .andExpect(jsonPath("$[0].foilQuantity").value(4));
    }

    @Test
    void shouldReturnChosenPrintingSummaryForCollectionCards() throws Exception {
        String subject = "auth0|collection-summary";
        long collectionId = createCollection(subject);
        long printingId = createPrintingWithLaterSibling("summary");

        MvcResult result = assertCreatedCardSummary(subject, collectionId, printingId);
        assertUpdatedCardSummary(subject, collectionId, idFrom(result));
        assertListedCardSummary(subject, collectionId);
    }

    @Test
    void shouldHideAnotherUsersCollection() throws Exception {
        long collectionId = createCollection("auth0|collection-private");

        mockMvc.perform(
                        get("/collections/{collectionId}", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|collection-other"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COLLECTION_NOT_FOUND"));
    }

    private long createCollection(String subject) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/collections")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"Collection\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        return idFrom(result);
    }

    private MvcResult assertCreatedCardSummary(String subject, long collectionId, long printingId)
            throws Exception {
        String request =
                "{\"cardPrintingId\":%d,\"regularQuantity\":1,\"foilQuantity\":0}"
                        .formatted(printingId);
        return mockMvc.perform(
                        post("/collections/{collectionId}/cards", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.card.name").value("Chosen Card"))
                .andExpect(jsonPath("$.card.imageUrl").value("https://img.example/chosen.jpg"))
                .andExpect(jsonPath("$.card.setCode").value("chs"))
                .andReturn();
    }

    private void assertUpdatedCardSummary(String subject, long collectionId, long cardId)
            throws Exception {
        mockMvc.perform(
                        patch("/collections/{collectionId}/cards/{cardId}", collectionId, cardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"regularQuantity\":3,\"foilQuantity\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.card.setName").value("Chosen Set"))
                .andExpect(jsonPath("$.card.rarity").value("rare"));
    }

    private void assertListedCardSummary(String subject, long collectionId) throws Exception {
        mockMvc.perform(
                        get("/collections/{collectionId}/cards", collectionId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].card.oracleId").value("oracle-summary"))
                .andExpect(jsonPath("$[0].card.manaCost").value("{1}{W}"))
                .andExpect(jsonPath("$[0].card.manaValue").value(2.0))
                .andExpect(jsonPath("$[0].card.colors").value("W"))
                .andExpect(jsonPath("$[0].card.colorIdentity").value("W"))
                .andExpect(jsonPath("$[0].card.typeLine").value("Creature — Human"))
                .andExpect(jsonPath("$[0].card.power").value("2"))
                .andExpect(jsonPath("$[0].card.toughness").value("2"));
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long createPrinting(String identifier) {
        Card card = cardRepository.save(new Card("oracle-" + identifier, "Collection Card"));
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet("set-" + identifier, identifier, "Collection Set"));
        return cardPrintingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }

    private long createPrintingWithLaterSibling(String identifier) {
        Card card = new Card("oracle-" + identifier, "Chosen Card");
        card.setManaCost("{1}{W}");
        card.setManaValue(BigDecimal.valueOf(2.0));
        card.setColors("W");
        card.setColorIdentity("W");
        card.setTypeLine("Creature — Human");
        card.setPower("2");
        card.setToughness("2");
        card = cardRepository.save(card);
        long chosenId =
                savePrinting(card, "chs", "Chosen Set", "chosen", LocalDate.parse("2020-01-01"));
        savePrinting(card, "lat", "Later Set", "later", LocalDate.parse("2024-01-01"));
        return chosenId;
    }

    private long savePrinting(
            Card card, String setCode, String setName, String identifier, LocalDate releasedAt) {
        MagicSet set = magicSetRepository.save(new MagicSet("set-" + identifier, setCode, setName));
        CardPrinting printing = new CardPrinting(card, set, "printing-" + identifier);
        printing.setReleasedAt(releasedAt);
        printing.setImageUriNormal("https://img.example/" + identifier + ".jpg");
        printing.setRarity("rare");
        return cardPrintingRepository.save(printing).getId();
    }
}
