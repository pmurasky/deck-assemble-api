package com.deckassemble.cards.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BeginnerGuideControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private BeginnerGuideRepository guideRepository;

    @Test
    void getRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/cards/42/beginner-guide")).andExpect(status().isUnauthorized());
    }

    @Test
    void getReturnsPublishedGuide() throws Exception {
        BeginnerGuide guide = saveGuide();
        guide.publish("reviewer", OffsetDateTime.parse("2026-08-16T13:00:00Z"));
        guideRepository.saveAndFlush(guide);

        mockMvc.perform(get("/cards/{cardId}/beginner-guide", guide.getCardId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(guide.getCardId()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.summary").value("Summary"));
    }

    @Test
    void getHidesDraftGuide() throws Exception {
        BeginnerGuide guide = saveGuide();

        mockMvc.perform(get("/cards/{cardId}/beginner-guide", guide.getCardId()).with(jwt()))
                .andExpect(status().isNotFound());
    }

    private BeginnerGuide saveGuide() {
        Card card = cardRepository.save(new Card("oracle-id", "Spider-Man"));
        BeginnerGuideDraft draft =
                new BeginnerGuideDraft("Summary", "Examples", "When", "Ruling", "hash");
        return guideRepository.saveAndFlush(
                new BeginnerGuide(
                        card.getId(), draft, OffsetDateTime.parse("2026-08-16T12:00:00Z")));
    }
}
