package com.deckassemble.decks.api.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class DeckSimulationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;
    @Autowired private DeckRevisionRepository deckRevisionRepository;

    @Test
    void shouldGenerateRequestedNumberOfSevenCardHandsAndEchoTheSeed() throws Exception {
        String subject = "auth0|sim-basic";
        long deckId = createDeck(subject, "Sample Hand Deck");
        addSevenCards(subject, deckId, "basic");
        int revision = currentRevision(deckId);

        mockMvc.perform(
                        post("/decks/{deckId}/sample-hands", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"revision":%d,"handCount":2,"mulliganStrategy":"NONE","seed":123}
                                        """
                                                .formatted(revision)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(123))
                .andExpect(jsonPath("$.hands.length()").value(2))
                .andExpect(jsonPath("$.hands[0].cards.length()").value(7))
                .andExpect(jsonPath("$.hands[0].mulliganCount").value(0));
    }

    @Test
    void shouldGenerateAndReturnASeedWhenNoneIsSupplied() throws Exception {
        String subject = "auth0|sim-noseed";
        long deckId = createDeck(subject, "No Seed Deck");
        addSevenCards(subject, deckId, "noseed");
        int revision = currentRevision(deckId);

        MvcResult result =
                mockMvc.perform(
                                post("/decks/{deckId}/sample-hands", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"revision":%d,"handCount":1,"mulliganStrategy":"NONE"}
                                                """
                                                        .formatted(revision)))
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(result.getResponse().getContentAsString()).contains("\"seed\"");
    }

    @Test
    void shouldReturnSameHandsForTheSameSeedAcrossSeparateRequests() throws Exception {
        String subject = "auth0|sim-repeat";
        long deckId = createDeck(subject, "Repeat Deck");
        addSevenCards(subject, deckId, "repeat");
        int revision = currentRevision(deckId);
        String body =
                """
                {"revision":%d,"handCount":3,"mulliganStrategy":"NONE","seed":999}
                """
                        .formatted(revision);

        MvcResult first =
                mockMvc.perform(
                                post("/decks/{deckId}/sample-hands", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn();
        MvcResult second =
                mockMvc.perform(
                                post("/decks/{deckId}/sample-hands", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn();

        assertThat(first.getResponse().getContentAsString())
                .isEqualTo(second.getResponse().getContentAsString());
    }

    @Test
    void shouldReturn400WhenLibraryHasFewerThanSevenCards() throws Exception {
        String subject = "auth0|sim-toosmall";
        long deckId = createDeck(subject, "Tiny Deck");
        addCard(subject, deckId, createPrinting("toosmall-1"));
        addCard(subject, deckId, createPrinting("toosmall-2"));
        int revision = currentRevision(deckId);

        mockMvc.perform(
                        post("/decks/{deckId}/sample-hands", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"revision":%d,"handCount":1,"mulliganStrategy":"NONE"}
                                        """
                                                .formatted(revision)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenLondonStrategyIsMissingLandBounds() throws Exception {
        String subject = "auth0|sim-missingbounds";
        long deckId = createDeck(subject, "Bounds Deck");
        addSevenCards(subject, deckId, "bounds");
        int revision = currentRevision(deckId);

        mockMvc.perform(
                        post("/decks/{deckId}/sample-hands", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"revision":%d,"handCount":1,"mulliganStrategy":"LONDON_LAND_RANGE","seed":1}
                                        """
                                                .formatted(revision)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForNonexistentRevision() throws Exception {
        String subject = "auth0|sim-badrevision";
        long deckId = createDeck(subject, "Deck");
        addSevenCards(subject, deckId, "badrev");

        mockMvc.perform(
                        post("/decks/{deckId}/sample-hands", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"revision":999,"handCount":1,"mulliganStrategy":"NONE"}
                                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForForeignProfile() throws Exception {
        String owner = "auth0|sim-owner";
        String foreign = "auth0|sim-foreign";
        long deckId = createDeck(owner, "Owned Deck");
        addSevenCards(owner, deckId, "foreign");
        int revision = currentRevision(deckId);

        mockMvc.perform(
                        post("/decks/{deckId}/sample-hands", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(foreign)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"revision":%d,"handCount":1,"mulliganStrategy":"NONE"}
                                        """
                                                .formatted(revision)))
                .andExpect(status().isNotFound());
    }

    private int currentRevision(long deckId) {
        return deckRevisionRepository
                .findFirstByDeckIdOrderByRevisionNumberDesc(deckId)
                .orElseThrow()
                .getRevisionNumber();
    }

    private void addSevenCards(String subject, long deckId, String prefix) throws Exception {
        for (int i = 0; i < 7; i++) {
            addCard(subject, deckId, createPrinting(prefix + "-" + i));
        }
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

    private void addCard(String subject, long deckId, long printingId) throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"cardPrintingId\":%d,\"quantity\":1}"
                                                .formatted(printingId)))
                .andExpect(status().isCreated());
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long createPrinting(String identifier) {
        Card card = cardRepository.save(new Card("oracle-" + identifier, "Card " + identifier));
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-" + identifier, identifier, "Deck Set"));
        return printingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }
}
