package com.deckassemble.decks.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.infrastructure.CardLegalityRepository;
import com.deckassemble.cards.infrastructure.MagicSetRepository;
import org.junit.jupiter.api.Test;
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

  @Test
  void shouldCreateUpdateDuplicateArchiveAndDeleteDeck() throws Exception {
    String subject = "auth0|deck-owner";
    long commanderCardId = cardRepository.save(new Card("oracle-commander", "Commander")).getId();
    MvcResult result = mockMvc
        .perform(post("/decks").with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Spider-Verse Commander\",\"formatCode\":\"COMMANDER\","
                + "\"commanderCardId\":" + commanderCardId + ",\"desiredPowerLevel\":5}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Spider-Verse Commander"))
        .andExpect(jsonPath("$.commanderName").value("Commander"))
        .andExpect(jsonPath("$.cardCount").value(0))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andReturn();
    long deckId = idFrom(result);

    mockMvc.perform(patch("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Updated Commander\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated Commander"));

    mockMvc.perform(post("/decks/{deckId}/duplicate", deckId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Updated Commander (Copy)"));

    mockMvc.perform(post("/decks/{deckId}/archive", deckId)
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));

    mockMvc.perform(delete("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isNoContent());
  }

  @Test
  void shouldManageDeckCardsWithSectionsAndMerging() throws Exception {
    String subject = "auth0|deck-cards";
    long deckId = createDeck(subject);
    long printingId = createPrinting("deckprint");

    String request =
        "{\"cardPrintingId\":%d,\"quantity\":1,\"deckSection\":\"MAIN_DECK\"}".formatted(printingId);
    mockMvc.perform(post("/decks/{deckId}/cards", deckId).with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated());
    mockMvc.perform(post("/decks/{deckId}/cards", deckId).with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/decks/{deckId}/cards", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk()).andExpect(jsonPath("$[0].quantity").value(2))
        .andExpect(jsonPath("$[0].deckSection").value("MAIN_DECK"))
        .andExpect(jsonPath("$[0].card.name").value("Deck Card"));

    mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk()).andExpect(jsonPath("$.cardCount").value(2));
  }

  @Test
  void shouldHideAnotherUsersDeck() throws Exception {
    long deckId = createDeck("auth0|deck-private");

    mockMvc.perform(get("/decks/{deckId}", deckId)
            .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-other"))))
        .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
  }

  @Test
  void shouldReportMissingCommanderWithoutRejectingDraft() throws Exception {
    String subject = "auth0|legality-draft";
    long deckId = createDeck(subject);

    mockMvc.perform(get("/decks/{deckId}/legality", deckId)
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
    MvcResult result = mockMvc.perform(post("/decks").with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\",\"commanderCardId\":"
                + first + ",\"secondaryCommanderCardId\":" + second + "}"))
        .andExpect(status().isCreated()).andReturn();

    mockMvc.perform(get("/decks/{deckId}/legality", idFrom(result))
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.violations[*].code", hasItem("COMMANDER_PAIR_INVALID")));
  }

  @Test
  void shouldReportColorSingletonLegalityAndDeckSizeViolations() throws Exception {
    String subject = "auth0|legality-rules";
    long commanderId = createLegalCommander("white", "White Commander", "");
    MvcResult result = mockMvc.perform(post("/decks").with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\",\"commanderCardId\":"
                + commanderId + "}"))
        .andExpect(status().isCreated()).andReturn();
    long printingId = createIllegalBluePrinting();

    mockMvc.perform(post("/decks/{deckId}/cards", idFrom(result))
            .with(jwt().jwt(jwt -> jwt.subject(subject))).contentType(MediaType.APPLICATION_JSON)
            .content("{\"cardPrintingId\":" + printingId + ",\"quantity\":2}"))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/decks/{deckId}/legality", idFrom(result))
            .with(jwt().jwt(jwt -> jwt.subject(subject))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.legal").value(false))
        .andExpect(jsonPath("$.violations[*].code", hasItem("COLOR_IDENTITY_VIOLATION")))
        .andExpect(jsonPath("$.violations[*].code", hasItem("SINGLETON_VIOLATION")))
        .andExpect(jsonPath("$.violations[*].code", hasItem("COMMANDER_LEGALITY_INVALID")))
        .andExpect(jsonPath("$.violations[*].code", hasItem("DECK_SIZE_INVALID")));
  }

  private long createDeck(String subject) throws Exception {
    MvcResult result = mockMvc
        .perform(post("/decks").with(jwt().jwt(jwt -> jwt.subject(subject)))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\"}"))
        .andExpect(status().isCreated()).andReturn();
    return idFrom(result);
  }

  private long idFrom(MvcResult result) {
    String location = result.getResponse().getHeader("Location");
    return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
  }

  private long createPrinting(String identifier) {
    Card card = cardRepository.save(new Card("oracle-" + identifier, "Deck Card"));
    MagicSet set = magicSetRepository.save(new MagicSet("set-" + identifier, identifier, "Deck Set"));
    return cardPrintingRepository.save(new CardPrinting(card, set, "printing-" + identifier)).getId();
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
