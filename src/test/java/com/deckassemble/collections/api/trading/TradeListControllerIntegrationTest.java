package com.deckassemble.collections.api.trading;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class TradeListControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;

    @Test
    void shouldCreateListReadItAndDeleteIt() throws Exception {
        Fixture fixture = fixture("crud");

        MvcResult created =
                mockMvc.perform(
                                post("/trade-lists")
                                        .with(jwt().jwt(jwt -> jwt.subject(fixture.subject())))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(request(fixture.printingId())))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.name").value("Binder"))
                        .andExpect(jsonPath("$.items[0].quantity").value(1))
                        .andReturn();
        long id = idFrom(created);

        mockMvc.perform(
                        get("/trade-lists/{id}", id)
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) id));

        mockMvc.perform(
                        delete("/trade-lists/{id}", id)
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject()))))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldRejectInvalidItemQuantity() throws Exception {
        Fixture fixture = fixture("invalid");

        mockMvc.perform(
                        post("/trade-lists")
                                .with(jwt().jwt(jwt -> jwt.subject(fixture.subject())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        request(fixture.printingId())
                                                .replace("\"quantity\": 1", "\"quantity\": 0")))
                .andExpect(status().isBadRequest());
    }

    private Fixture fixture(String key) {
        String subject = "auth0|trade-api-" + key;
        profileRepository.save(new Profile(subject, subject));
        Card card = cardRepository.save(new Card("oracle-trade-api-" + key, "Trade API"));
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-trade-api-" + key, setCode(key), "Set"));
        long printingId =
                printingRepository
                        .save(new CardPrinting(card, set, "printing-trade-api-" + key))
                        .getId();
        return new Fixture(subject, printingId);
    }

    private String request(long printingId) {
        return """
                {
                  "name": "Binder",
                  "type": "OFFERED",
                  "visibility": "PUBLIC",
                  "items": [{"cardPrintingId": %d, "quantity": 1}]
                }
                """
                .formatted(printingId);
    }

    private String setCode(String key) {
        String code = "s" + Integer.toUnsignedString(key.hashCode(), 36);
        return code.length() <= 10 ? code : code.substring(0, 10);
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private record Fixture(String subject, long printingId) {}
}
