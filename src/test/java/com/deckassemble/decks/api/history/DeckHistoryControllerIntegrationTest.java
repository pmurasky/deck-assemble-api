package com.deckassemble.decks.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class DeckHistoryControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;
    @Autowired private DeckRevisionRepository deckRevisionRepository;

    @Test
    void shouldListRevisionsMostRecentFirstWithPagination() throws Exception {
        String subject = "auth0|history-list";
        long deckId = createDeck(subject, "Paginated Deck");
        addCard(subject, deckId, createPrinting("list-1"));
        addCard(subject, deckId, createPrinting("list-2"));

        mockMvc.perform(
                        get("/decks/{deckId}/revisions", deckId)
                                .param("page", "0")
                                .param("size", "2")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].revisionNumber").value(3))
                .andExpect(jsonPath("$.content[1].revisionNumber").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void shouldGetSingleRevisionByNumber() throws Exception {
        String subject = "auth0|history-get";
        long deckId = createDeck(subject, "Single Revision Deck");

        mockMvc.perform(
                        get("/decks/{deckId}/revisions/{revisionNumber}", deckId, 1)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(1))
                .andExpect(jsonPath("$.changeType").value("CREATED"))
                .andExpect(jsonPath("$.snapshot.name").value("Single Revision Deck"));
    }

    @Test
    void shouldReturn404ForNonexistentRevisionNumber() throws Exception {
        String subject = "auth0|history-missing-revision";
        long deckId = createDeck(subject, "Deck");

        mockMvc.perform(
                        get("/decks/{deckId}/revisions/{revisionNumber}", deckId, 999)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectForeignProfileForListGetDiffAndRestore() throws Exception {
        String owner = "auth0|history-owner";
        String foreign = "auth0|history-foreign";
        long deckId = createDeck(owner, "Owned Deck");

        mockMvc.perform(
                        get("/decks/{deckId}/revisions", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(foreign))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/decks/{deckId}/revisions/{revisionNumber}", deckId, 1)
                                .with(jwt().jwt(jwt -> jwt.subject(foreign))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/decks/{deckId}/revisions/1/diff/1", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(foreign))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/decks/{deckId}/revisions/1/restore", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(foreign)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedCurrentRevision\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDiffMetadataCardAndCategoryChangesBetweenRevisions() throws Exception {
        String subject = "auth0|history-diff";
        long deckId = createDeck(subject, "Original");
        rename(subject, deckId, "Renamed");
        long printingId = createPrinting("diff-card");
        addCard(subject, deckId, printingId);
        createCategory(subject, deckId, "Combo Package");

        mockMvc.perform(
                        get("/decks/{deckId}/revisions/1/diff/4", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadataChanges[0].field").value("name"))
                .andExpect(jsonPath("$.metadataChanges[0].before").value("Original"))
                .andExpect(jsonPath("$.metadataChanges[0].after").value("Renamed"))
                .andExpect(jsonPath("$.cardsAdded[0].cardPrintingId").value(printingId))
                .andExpect(jsonPath("$.categoriesAdded[0]").value("Combo Package"));
    }

    @Test
    void shouldReturn409WhenExpectedRevisionIsStale() throws Exception {
        String subject = "auth0|history-stale";
        long deckId = createDeck(subject, "Deck");

        mockMvc.perform(
                        post("/decks/{deckId}/revisions/1/restore", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedCurrentRevision\":999}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn404WhenRestoringNonexistentRevision() throws Exception {
        String subject = "auth0|history-restore-missing";
        long deckId = createDeck(subject, "Deck");

        mockMvc.perform(
                        post("/decks/{deckId}/revisions/999/restore", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedCurrentRevision\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRestoreProducingExactlyOneNewRevisionAndReapplyingOldState() throws Exception {
        String subject = "auth0|history-restore";
        long deckId = createDeck(subject, "Original");
        rename(subject, deckId, "Changed");
        addCard(subject, deckId, createPrinting("restorecrd"));
        assertThat(deckRevisionRepository.findByDeckIdOrderByRevisionNumberDesc(deckId)).hasSize(3);

        mockMvc.perform(
                        post("/decks/{deckId}/revisions/1/restore", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"expectedCurrentRevision\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(4))
                .andExpect(jsonPath("$.changeType").value("RESTORED"))
                .andExpect(jsonPath("$.snapshot.name").value("Original"))
                .andExpect(jsonPath("$.snapshot.cards.length()").value(0));

        assertThat(deckRevisionRepository.findByDeckIdOrderByRevisionNumberDesc(deckId)).hasSize(4);
        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(jsonPath("$.name").value("Original"))
                .andExpect(jsonPath("$.cardCount").value(0));
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

    private void rename(String subject, long deckId, String name) throws Exception {
        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isOk());
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

    private void createCategory(String subject, long deckId, String name) throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/categories", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)))
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
