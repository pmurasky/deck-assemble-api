package com.deckassemble.collections.api.physical;

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
import com.deckassemble.collections.domain.physical.StorageLocation;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PhysicalCollectionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private StorageLocationRepository storageLocationRepository;

    @Test
    void shouldManageLocationsAndPhysicalMetadataForOwner() throws Exception {
        String subject = "auth0|physical-owner";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("physical"));
        String locationId = createLocation(subject, "Binder", null);

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(locationId, "NEAR_MINT", "12.30", "USD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.condition").value("NEAR_MINT"))
                .andExpect(jsonPath("$.finish").value("FOIL"))
                .andExpect(jsonPath("$.purchasePrice").value(12.30))
                .andExpect(jsonPath("$.purchaseCurrency").value("USD"));

        mockMvc.perform(
                        get("/collections/{collectionId}/cards/physical", collectionId)
                                .param("locationId", locationId)
                                .param("condition", "NEAR_MINT")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].collectionCardId").value(collectionCardId));
    }

    @Test
    void shouldRejectInvalidConditionEnum() throws Exception {
        String subject = "auth0|physical-invalid-enum";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("bad-enum"));

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(null, "MINTY", "1.00", "USD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidPriceScale() throws Exception {
        String subject = "auth0|physical-invalid-price";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("bad-price"));

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(null, "NEAR_MINT", "1.999", "USD")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidCurrency() throws Exception {
        String subject = "auth0|physical-invalid-currency";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("bad-cur"));

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(null, "NEAR_MINT", "1.00", "US1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHideAnotherUsersLocationAndMetadata() throws Exception {
        String owner = "auth0|physical-private-owner";
        String other = "auth0|physical-private-other";
        long collectionId = createCollection(owner);
        long collectionCardId = addCard(owner, collectionId, createPrinting("private"));
        String locationId = createLocation(owner, "Private Box", null);

        mockMvc.perform(get("/collection-locations").with(jwt().jwt(jwt -> jwt.subject(other))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(other)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(locationId, "NEAR_MINT", "1.00", "USD")))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectLocationCyclesAndDeletingLocationWithCards() throws Exception {
        String subject = "auth0|physical-tree";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("tree"));
        String rootId = createLocation(subject, "Root", null);
        String childId = createLocation(subject, "Child", rootId);
        String grandchildId = createLocation(subject, "Grandchild", childId);

        mockMvc.perform(
                        patch("/collection-locations/{id}", rootId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Root\",\"parentId\":\"" + childId + "\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(grandchildId, "LIGHTLY_PLAYED", "2.00", "EUR")))
                .andExpect(status().isOk());
        mockMvc.perform(
                        delete("/collection-locations/{id}", rootId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDeletingCyclicLocationSubtreeWithCards() throws Exception {
        String subject = "auth0|physical-cyclic-delete";
        long collectionId = createCollection(subject);
        long collectionCardId = addCard(subject, collectionId, createPrinting("cycle"));
        String rootId = createLocation(subject, "Cycle Root", null);
        String childId = createLocation(subject, "Cycle Child", rootId);

        StorageLocation root =
                storageLocationRepository.findById(UUID.fromString(rootId)).orElseThrow();
        root.update("Cycle Root", UUID.fromString(childId));
        storageLocationRepository.saveAndFlush(root);

        mockMvc.perform(
                        patch(
                                        "/collections/{collectionId}/cards/{cardId}/physical",
                                        collectionId,
                                        collectionCardId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(metadata(childId, "LIGHTLY_PLAYED", "2.00", "EUR")))
                .andExpect(status().isOk());

        mockMvc.perform(
                        delete("/collection-locations/{id}", rootId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isConflict());
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
        return idFromLocation(result);
    }

    private long addCard(String subject, long collectionId, long printingId) throws Exception {
        String body =
                "{\"cardPrintingId\":%d,\"regularQuantity\":1,\"foilQuantity\":0}"
                        .formatted(printingId);
        MvcResult result =
                mockMvc.perform(
                                post("/collections/{collectionId}/cards", collectionId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isCreated())
                        .andReturn();
        return idFromLocation(result);
    }

    private String createLocation(String subject, String name, String parentId) throws Exception {
        String parent = parentId == null ? "null" : "\"" + parentId + "\"";
        MvcResult result =
                mockMvc.perform(
                                post("/collection-locations")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\""
                                                        + name
                                                        + "\",\"parentId\":"
                                                        + parent
                                                        + "}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String metadata(String locationId, String condition, String price, String currency) {
        String location = locationId == null ? "null" : "\"" + locationId + "\"";
        return "{\"condition\":\""
                + condition
                + "\",\"language\":\"en\","
                + "\"finish\":\"FOIL\",\"purchasePrice\":"
                + price
                + ","
                + "\"purchaseCurrency\":\""
                + currency
                + "\","
                + "\"purchaseDate\":\"2024-01-02\",\"notes\":\"opened\","
                + "\"storageLocationId\":"
                + location
                + "}";
    }

    private long idFromLocation(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long createPrinting(String identifier) {
        Card card = cardRepository.save(new Card("oracle-" + identifier, "Physical Card"));
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet("set-" + identifier, identifier, "Physical Set"));
        return cardPrintingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }
}
