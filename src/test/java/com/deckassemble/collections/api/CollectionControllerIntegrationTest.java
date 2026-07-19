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
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.infrastructure.MagicSetRepository;
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
        mockMvc
            .perform(post("/collections").with(jwt().jwt(jwt -> jwt.subject(subject)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Marvel\",\"defaultCollection\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Marvel"))
            .andReturn();
    long collectionId = idFrom(result);

    mockMvc.perform(patch("/collections/{collectionId}", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Updated Marvel\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Marvel"));

    mockMvc.perform(delete("/collections/{collectionId}", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldMergeQuantitiesForTheSamePrinting() throws Exception {
    String subject = "auth0|collection-cards";
    long collectionId = createCollection(subject);
    long printingId = createPrinting("colprint");

    String request = "{\"cardPrintingId\":%d,\"regularQuantity\":1,\"foilQuantity\":2}".formatted(printingId);
    mockMvc.perform(post("/collections/{collectionId}/cards", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/collections/{collectionId}/cards", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))).contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/collections/{collectionId}/cards", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].regularQuantity").value(2))
        .andExpect(jsonPath("$[0].foilQuantity").value(4));
  }

  @Test
  void shouldHideAnotherUsersCollection() throws Exception {
    long collectionId = createCollection("auth0|collection-private");

    mockMvc.perform(get("/collections/{collectionId}", collectionId)
            .with(jwt().jwt(jwt -> jwt.subject("auth0|collection-other"))))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COLLECTION_NOT_FOUND"));
  }

  private long createCollection(String subject) throws Exception {
    MvcResult result = mockMvc.perform(post("/collections").with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Collection\"}"))
        .andExpect(status().isCreated()).andReturn();
    return idFrom(result);
  }

  private long idFrom(MvcResult result) {
    String location = result.getResponse().getHeader("Location");
    return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
  }

  private long createPrinting(String identifier) {
    Card card = cardRepository.save(new Card("oracle-" + identifier, "Collection Card"));
    MagicSet set = magicSetRepository.save(new MagicSet("set-" + identifier, identifier, "Collection Set"));
    return cardPrintingRepository.save(new CardPrinting(card, set, "printing-" + identifier)).getId();
  }
}
