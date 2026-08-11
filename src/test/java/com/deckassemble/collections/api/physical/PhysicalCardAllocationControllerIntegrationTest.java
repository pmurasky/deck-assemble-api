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
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class PhysicalCardAllocationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckCardRepository deckCardRepository;
    @Autowired private CardCollectionRepository collectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;

    @Test
    void shouldAllocateUpdateListAndReleasePhysicalCard() throws Exception {
        Fixture fixture = fixture("api-happy", 2, 2);

        MvcResult created =
                mockMvc.perform(
                                post("/decks/{deckId}/physical-cards", fixture.deckId())
                                        .with(jwt().jwt(jwt -> jwt.subject(fixture.subject())))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request(fixture.deckCardId(), 1)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.deckCardId").value((int) fixture.deckCardId()))
                        .andExpect(jsonPath("$.quantity").value(1))
                        .andReturn();
        long allocationId = idFrom(created);

        mockMvc.perform(
                        patch(
                                        "/decks/{deckId}/physical-cards/{allocationId}",
                                        fixture.deckId(),
                                        allocationId)
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"quantity\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));

        mockMvc.perform(
                        get("/decks/{deckId}/physical-cards", fixture.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value((int) allocationId));

        mockMvc.perform(
                        delete(
                                        "/decks/{deckId}/physical-cards/{allocationId}",
                                        fixture.deckId(),
                                        allocationId)
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        get("/decks/{deckId}/physical-cards", fixture.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldListMissingPhysicalCards() throws Exception {
        Fixture fixture = fixture("api-missing", 2, 1);

        mockMvc.perform(
                        get("/decks/{deckId}/physical-cards/unavailable", fixture.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].deckCardId").value((int) fixture.deckCardId()))
                .andExpect(jsonPath("$[0].missingQuantity").value(1));
    }

    @Test
    void shouldHideAnotherUsersDeckPhysicalCards() throws Exception {
        Fixture fixture = fixture("api-private", 1, 1);

        mockMvc.perform(
                        post("/decks/{deckId}/physical-cards", fixture.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|alloc-other")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request(fixture.deckCardId(), 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_NOT_FOUND"));
    }

    private Fixture fixture(String key, int deckQuantity, int ownedQuantity) {
        String subject = "auth0|alloc-" + key;
        long profileId = profileRepository.save(new Profile(subject, subject)).getId();
        Card card = cardRepository.save(new Card("oracle-" + key, "Physical Allocation"));
        MagicSet set = magicSetRepository.save(new MagicSet("set-" + key, setCode(key), "Set"));
        long printingId =
                printingRepository.save(new CardPrinting(card, set, "printing-" + key)).getId();
        long deckId = deckRepository.save(new Deck(profileId, "Deck", "COMMANDER")).getId();
        long deckCardId =
                deckCardRepository
                        .save(
                                new DeckCard(
                                        deckId,
                                        printingId,
                                        deckQuantity,
                                        DeckCard.Section.MAIN_DECK))
                        .getId();
        long collectionId =
                collectionRepository
                        .save(new CardCollection(profileId, "Collection", null, true))
                        .getId();
        collectionCardRepository.save(
                new CollectionCard(collectionId, printingId, ownedQuantity, 0));
        return new Fixture(subject, deckId, deckCardId);
    }

    private String request(long deckCardId, int quantity) {
        return "{\"deckCardId\":%d,\"quantity\":%d}".formatted(deckCardId, quantity);
    }

    private String setCode(String key) {
        String code = "s" + Integer.toUnsignedString(key.hashCode(), 36);
        return code.length() <= 10 ? code : code.substring(0, 10);
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private record Fixture(String subject, long deckId, long deckCardId) {}
}
