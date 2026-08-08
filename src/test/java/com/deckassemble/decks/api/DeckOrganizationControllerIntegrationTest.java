package com.deckassemble.decks.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class DeckOrganizationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldSeedDefaultCategoriesOrderedOnFirstList() throws Exception {
        String subject = "auth0|org-defaults";
        long deckId = createDeck(subject, "Org Defaults Deck");

        mockMvc.perform(
                        get("/decks/{deckId}/categories", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("Land"))
                .andExpect(jsonPath("$[0].functionalCategory").value("LAND"))
                .andExpect(jsonPath("$[0].systemOwned").value(true))
                .andExpect(jsonPath("$[5].name").value("Synergy"))
                .andExpect(jsonPath("$[5].functionalCategory").value("SYNERGY"));
    }

    @Test
    void shouldCreateRenameAndRejectDuplicateCategoryName() throws Exception {
        String subject = "auth0|org-crud";
        long deckId = createDeck(subject, "Org Crud Deck");

        long categoryId = createCategory(subject, deckId, "Combos");

        mockMvc.perform(
                        patch("/decks/{deckId}/categories/{categoryId}", deckId, categoryId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Win Conditions\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Win Conditions"));

        mockMvc.perform(
                        post("/decks/{deckId}/categories", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Win Conditions\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldPreventDeletingSystemCategoryButAllowDeletingUserCategory() throws Exception {
        String subject = "auth0|org-delete";
        long deckId = createDeck(subject, "Org Delete Deck");
        long landCategoryId = firstCategoryId(subject, deckId);
        long userCategoryId = createCategory(subject, deckId, "Combos");

        mockMvc.perform(
                        delete("/decks/{deckId}/categories/{categoryId}", deckId, landCategoryId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isConflict());

        mockMvc.perform(
                        delete("/decks/{deckId}/categories/{categoryId}", deckId, userCategoryId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldBulkReplaceAssignmentsIdempotently() throws Exception {
        String subject = "auth0|org-assign";
        long deckId = createDeck(subject, "Org Assign Deck");
        long categoryId = createCategory(subject, deckId, "Combos");
        long cardA = addCardAndReturnId(subject, deckId, createPrinting("Org Card A"));
        long cardB = addCardAndReturnId(subject, deckId, createPrinting("Org Card B"));
        long cardC = addCardAndReturnId(subject, deckId, createPrinting("Org Card C"));

        assignCards(subject, deckId, categoryId, "[" + cardA + "," + cardB + "]")
                .andExpect(jsonPath("$.assignedDeckCardIds.length()").value(2));
        assignCards(subject, deckId, categoryId, "[" + cardA + "," + cardB + "]")
                .andExpect(jsonPath("$.assignedDeckCardIds.length()").value(2));

        assignCards(subject, deckId, categoryId, "[" + cardB + "," + cardC + "]")
                .andExpect(jsonPath("$.assignedDeckCardIds.length()").value(2))
                .andExpect(jsonPath("$.assignedDeckCardIds").value(not(hasItem((int) cardA))));
    }

    @Test
    void shouldRejectAssigningCardNotInDeck() throws Exception {
        String subject = "auth0|org-assign-invalid";
        long deckId = createDeck(subject, "Org Assign Invalid Deck");
        long otherDeckId = createDeck(subject, "Org Other Deck");
        long categoryId = createCategory(subject, deckId, "Combos");
        long foreignCard =
                addCardAndReturnId(subject, otherDeckId, createPrinting("Org Foreign Card"));

        assignCardsRaw(subject, deckId, categoryId, "[" + foreignCard + "]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_CARD_NOT_FOUND"));
    }

    @Test
    void shouldRejectAssigningToNonexistentCategory() throws Exception {
        String subject = "auth0|org-assign-missing-category";
        long deckId = createDeck(subject, "Org Missing Category Deck");
        long cardId = addCardAndReturnId(subject, deckId, createPrinting("Org Card Missing Cat"));

        assignCardsRaw(subject, deckId, 999_999L, "[" + cardId + "]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldHideCategoriesForForeignDeck() throws Exception {
        long deckId = createDeck("auth0|org-private", "Org Private Deck");

        mockMvc.perform(
                        get("/decks/{deckId}/categories", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|org-other"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    private ResultActions assignCards(
            String subject, long deckId, long categoryId, String cardIdsJson) throws Exception {
        return assignCardsRaw(subject, deckId, categoryId, cardIdsJson).andExpect(status().isOk());
    }

    private ResultActions assignCardsRaw(
            String subject, long deckId, long categoryId, String cardIdsJson) throws Exception {
        return mockMvc.perform(
                put("/decks/{deckId}/categories/{categoryId}/cards", deckId, categoryId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deckCardIds\":" + cardIdsJson + "}"));
    }

    // ponytail: no JSON parser on the test classpath; the first category is always the
    // system-seeded "Land" default (CardFunctionalCategory.values() order), so its id is the
    // first "id" field in the response body.
    private long firstCategoryId(String subject, long deckId) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                get("/decks/{deckId}/categories", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject))))
                        .andExpect(status().isOk())
                        .andReturn();
        String body = result.getResponse().getContentAsString();
        Matcher matcher = Pattern.compile("\"id\":(\\d+)").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("No category id found in: " + body);
        }
        return Long.parseLong(matcher.group(1));
    }

    private long createCategory(String subject, long deckId, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks/{deckId}/categories", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"" + name + "\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
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

    private long createPrinting(String name) {
        String identifier = name.toLowerCase(Locale.ROOT).replace(' ', '-');
        Card card = cardRepository.save(new Card("oracle-" + identifier, name));
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet(
                                "set-" + identifier,
                                identifier.substring(0, Math.min(identifier.length(), 10)),
                                "Org"));
        return cardPrintingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
