package com.deckassemble.cards.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.application.BeginnerGuideDailyLimitExceededException;
import com.deckassemble.cards.application.BeginnerGuideRequestService;
import com.deckassemble.cards.domain.BeginnerGuide;
import com.deckassemble.cards.domain.BeginnerGuideDraft;
import com.deckassemble.cards.domain.BeginnerGuideRepository;
import com.deckassemble.cards.domain.BeginnerGuideStatus;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BeginnerGuideControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private BeginnerGuideRepository guideRepository;

    @MockitoBean private BeginnerGuideRequestService requestService;

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

    @Test
    void requestReturnsAcceptedGuideStatus() throws Exception {
        BeginnerGuide guide = draft(42L);
        when(requestService.request(42L)).thenReturn(guide);

        mockMvc.perform(post("/cards/42/beginner-guide/request").with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.cardId").value(42))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void requestReturnsTooManyRequestsWhenDailyLimitIsExhausted() throws Exception {
        when(requestService.request(42L)).thenThrow(BeginnerGuideDailyLimitExceededException.class);

        mockMvc.perform(post("/cards/42/beginner-guide/request").with(jwt()))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void reportMarksPublishedGuideAsReported() throws Exception {
        BeginnerGuide guide = saveGuide();
        guide.publish("reviewer", OffsetDateTime.parse("2026-08-16T13:00:00Z"));
        guideRepository.saveAndFlush(guide);

        mockMvc.perform(
                        post("/cards/{cardId}/beginner-guide/report", guide.getCardId())
                                .with(jwt()))
                .andExpect(status().isAccepted());

        assertThat(guideRepository.findById(guide.getCardId()).orElseThrow().getStatus())
                .isEqualTo(BeginnerGuideStatus.REPORTED);
    }

    @Test
    void reportAcceptsAlreadyReportedGuide() throws Exception {
        BeginnerGuide guide = saveGuide();
        guide.report();
        guideRepository.saveAndFlush(guide);

        mockMvc.perform(
                        post("/cards/{cardId}/beginner-guide/report", guide.getCardId())
                                .with(jwt()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("REPORTED"));
    }

    @Test
    void reportHidesDraftGuide() throws Exception {
        BeginnerGuide guide = saveGuide();

        mockMvc.perform(
                        post("/cards/{cardId}/beginner-guide/report", guide.getCardId())
                                .with(jwt()))
                .andExpect(status().isNotFound());
    }

    private BeginnerGuide saveGuide() {
        Card card = cardRepository.save(new Card("oracle-id", "Spider-Man"));
        return guideRepository.saveAndFlush(draft(card.getId()));
    }

    private BeginnerGuide draft(Long cardId) {
        BeginnerGuideDraft draft =
                new BeginnerGuideDraft("Summary", "Examples", "When", "Ruling", "hash");
        return new BeginnerGuide(cardId, draft, OffsetDateTime.parse("2026-08-16T12:00:00Z"));
    }
}
