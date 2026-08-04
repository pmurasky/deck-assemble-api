package com.deckassemble.cards.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class PrintingControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private MagicSetRepository magicSetRepository;

    @Test
    void shouldReturnEveryPrintingMatchingTheNameQuery() throws Exception {
        MagicSet set = magicSetRepository.save(new MagicSet("set-serum", "sr1", "Serum Set"));
        Card card = cardRepository.save(new Card("oracle-serum", "Super-Soldier Serum"));
        CardPrinting standard = new CardPrinting(card, set, "serum-38");
        standard.setCollectorNumber("38");
        standard.setImageUriNormal("https://img.example/serum-38.png");
        cardPrintingRepository.save(standard);
        CardPrinting showcase = new CardPrinting(card, set, "serum-299");
        showcase.setCollectorNumber("299");
        showcase.setImageUriNormal("https://img.example/serum-299.png");
        cardPrintingRepository.save(showcase);

        mockMvc.perform(get("/printings").queryParam("query", "super-soldier").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].printingId").value(standard.getId()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://img.example/serum-38.png"))
                .andExpect(jsonPath("$.content[1].printingId").value(showcase.getId()))
                .andExpect(jsonPath("$.content[1].imageUrl").value("https://img.example/serum-299.png"));
    }

    @Test
    void shouldFilterPrintingsBySetCode() throws Exception {
        MagicSet msh = magicSetRepository.save(new MagicSet("set-fantastic-a", "s2a", "Fantastic Set A"));
        MagicSet msc = magicSetRepository.save(new MagicSet("set-fantastic-b", "s2b", "Fantastic Set B"));
        Card card = cardRepository.save(new Card("oracle-fantastic", "Mister Fantastic"));
        cardPrintingRepository.save(new CardPrinting(card, msh, "fantastic-66"));
        cardPrintingRepository.save(new CardPrinting(card, msc, "fantastic-2"));

        mockMvc.perform(
                        get("/printings")
                                .queryParam("query", "fantastic")
                                .queryParam("setCode", "s2b")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].setCode").value("s2b"));
    }
}
