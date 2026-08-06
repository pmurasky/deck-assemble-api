package com.deckassemble.recommendations.api;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardLegalityRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.recommendations.domain.EdhrecClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

class RecommendationControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String EDHREC_PAYLOAD =
            """
            {"container":{"json_dict":{"cardlists":[
              {"header":"Combo","cardviews":[{"name":"Test Pool Card","synergy":0.9,"inclusion":1000}]}
            ]}}}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardLegalityRepository cardLegalityRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;

    @MockitoBean private EdhrecClient edhrecClient;

    @Test
    void shouldExplainCandidateScoresInBuildResponse() throws Exception {
        long commanderId = createCommander();
        createCardWithPrinting("Test Pool Card", "Sorcery", "Draw a card.", "U");
        createCardWithPrinting("Island", "Basic Land — Island", "", "");
        when(edhrecClient.fetchCommanderData("test-commander")).thenReturn(EDHREC_PAYLOAD);

        mockMvc.perform(
                        post("/recommendations/builds")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|recommendations")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        ("{\"commanderCardId\":%d,\"useOwnedCardsOnly\":false,"
                                                        + "\"playStyle\":\"draw\",\"desiredPowerLevel\":7}")
                                                .formatted(commanderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoredCandidates").isArray())
                .andExpect(
                        jsonPath(
                                "$.scoredCandidates[*].contributions[*].code",
                                hasItem("COMMANDER_SYNERGY")))
                .andExpect(
                        jsonPath(
                                "$.scoredCandidates[*].contributions[*].code",
                                hasItem("CATEGORY_NEED")))
                .andExpect(
                        jsonPath(
                                "$.scoredCandidates[*].contributions[*].code", hasItem("COMBO")))
                .andExpect(
                        jsonPath(
                                "$.scoredCandidates[*].contributions[*].code",
                                hasItem("PLAY_STYLE")))
                .andExpect(jsonPath("$.scoredCandidates[*].total", hasItem(0.9)));
    }

    private long createCommander() {
        Card card = new Card("oracle-test-commander", "Test Commander");
        card.setColorIdentity("U");
        card.setTypeLine("Legendary Creature — Human");
        var face = new CardFace(card, 0, "Test Commander");
        face.setTypeLine("Legendary Creature — Human");
        card.getFaces().add(face);
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        createPrinting(card, "test-commander");
        return card.getId();
    }

    private void createCardWithPrinting(
            String name, String typeLine, String oracleText, String colorIdentity) {
        String identifier = name.toLowerCase(java.util.Locale.ROOT).replace(' ', '-');
        Card card = new Card("oracle-" + identifier, name);
        card.setColorIdentity(colorIdentity);
        card.setTypeLine(typeLine);
        card.setOracleText(oracleText);
        var face = new CardFace(card, 0, name);
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        card.getFaces().add(face);
        card = cardRepository.save(card);
        cardLegalityRepository.save(new CardLegality(card, "commander", "legal"));
        createPrinting(card, identifier);
    }

    private void createPrinting(Card card, String identifier) {
        String setCode = identifier.substring(0, Math.min(identifier.length(), 10));
        MagicSet set = magicSetRepository.save(new MagicSet("set-" + identifier, setCode, "S"));
        cardPrintingRepository.save(new CardPrinting(card, set, "printing-" + identifier));
    }
}
