package com.deckassemble.cards.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.BeginnerGuideContent;
import com.deckassemble.cards.domain.BeginnerGuideGenerator;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.cards.domain.ScryfallClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class BeginnerGuideWorkflowIntegrationTest extends AbstractIntegrationTest {

    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;

    @MockitoBean private ScryfallClient scryfallClient;
    @MockitoBean private BeginnerGuideGenerator generator;

    @Test
    void requestPublishAndReadGuide() throws Exception {
        Card card = saveCardWithPrinting("Workflow Card", "workflow-printing");
        when(scryfallClient.getRulings("workflow-printing")).thenReturn(List.of("Workflow ruling"));
        when(generator.generate(any()))
                .thenReturn(new BeginnerGuideContent("Workflow summary", "Example", "Timing"));

        mockMvc.perform(
                        post("/cards/{cardId}/beginner-guide/request", card.getId())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(
                        post("/admin/beginner-guides/{cardId}/publish", card.getId())
                                .with(
                                        jwt().jwt(jwt -> jwt.subject("admin-1"))
                                                .authorities(List.of(ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/cards/{cardId}/beginner-guide", card.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Workflow summary"));
    }

    @Test
    void rejectAndRegenerateGuide() throws Exception {
        Card card = saveCardWithPrinting("Regeneration Card", "regeneration-printing");
        when(scryfallClient.getRulings("regeneration-printing")).thenReturn(List.of());
        when(generator.generate(any()))
                .thenReturn(
                        new BeginnerGuideContent("Initial", "Example", "Timing"),
                        new BeginnerGuideContent("Replacement", "Example", "Timing"));

        mockMvc.perform(
                        post("/cards/{cardId}/beginner-guide/request", card.getId())
                                .with(jwt().jwt(jwt -> jwt.subject("user-1"))))
                .andExpect(status().isAccepted());
        mockMvc.perform(
                        post("/admin/beginner-guides/{cardId}/reject", card.getId())
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        post("/admin/beginner-guides/{cardId}/regenerate", card.getId())
                                .with(jwt().authorities(List.of(ADMIN))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.summary").value("Replacement"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/cards/{cardId}/beginner-guide", card.getId()).with(jwt()))
                .andExpect(status().isNotFound());
        verify(generator, times(2)).generate(any());
    }

    private Card saveCardWithPrinting(String cardName, String printingId) {
        Card card = new Card("oracle-" + cardName, cardName);
        card.setOracleText("Oracle text");
        cardRepository.save(card);
        MagicSet magicSet = magicSetRepository.save(new MagicSet("set-id", "TST", "Test Set"));
        printingRepository.save(new CardPrinting(card, magicSet, printingId));
        return card;
    }
}
