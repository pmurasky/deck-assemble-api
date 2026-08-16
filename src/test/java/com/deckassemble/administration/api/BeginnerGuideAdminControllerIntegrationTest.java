package com.deckassemble.administration.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BeginnerGuideAdminControllerIntegrationTest extends AbstractIntegrationTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private BeginnerGuideRepository guideRepository;

    @Test
    void listRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/beginner-guides").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listReturnsDefaultReviewQueue() throws Exception {
        BeginnerGuide draft = saveGuide("Draft Card");
        BeginnerGuide published = saveGuide("Published Card");
        published.publish("admin", OffsetDateTime.parse("2026-08-16T13:00:00Z"));
        guideRepository.save(published);

        mockMvc.perform(get("/admin/beginner-guides").with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].cardId").value(draft.getCardId()))
                .andExpect(jsonPath("$.content[0].cardName").value("Draft Card"))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void editUpdatesDraftContent() throws Exception {
        BeginnerGuide guide = saveGuide("Editable Card");

        mockMvc.perform(
                        put("/admin/beginner-guides/{cardId}", guide.getCardId())
                                .with(jwt().authorities(List.of(ADMIN)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "summary": "Updated summary",
                                          "examples": "Updated examples",
                                          "whenToUse": "Updated timing"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Updated summary"))
                .andExpect(jsonPath("$.examples").value("Updated examples"))
                .andExpect(jsonPath("$.whenToUse").value("Updated timing"));
    }

    private BeginnerGuide saveGuide(String cardName) {
        Card card = cardRepository.save(new Card("oracle-" + cardName, cardName));
        BeginnerGuideDraft draft =
                new BeginnerGuideDraft("Summary", "Examples", "When", "Ruling", "a".repeat(64));
        return guideRepository.save(
                new BeginnerGuide(
                        card.getId(), draft, OffsetDateTime.parse("2026-08-16T12:00:00Z")));
    }
}
