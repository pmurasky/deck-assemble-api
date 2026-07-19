package com.deckassemble.cards.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.infrastructure.CardPrintingRepository;
import com.deckassemble.cards.infrastructure.CardRepository;
import com.deckassemble.cards.infrastructure.MagicSetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class CardControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CardRepository cardRepository;
  @Autowired private CardPrintingRepository cardPrintingRepository;
  @Autowired private MagicSetRepository magicSetRepository;

  @Test
  void shouldReturnActiveCardsMatchingTheNameQuery() throws Exception {
    Card card = cardRepository.save(new Card("oracle-spider", "Spider-Man, Web-Slinger"));

    mockMvc
        .perform(get("/cards").queryParam("query", "spider").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(card.getId()))
        .andExpect(jsonPath("$.content[0].name").value("Spider-Man, Web-Slinger"));
  }

  @Test
  void shouldReturnTheActiveCardDetail() throws Exception {
    Card card = cardRepository.save(new Card("oracle-iron-man", "Iron Man, Armored Avenger"));
    card.setManaCost("{2}{U}{R}");
    card.setTypeLine("Legendary Artifact Creature — Human Hero");
    card.setOracleText("Flying");
    cardRepository.save(card);

    mockMvc
        .perform(get("/cards/{cardId}", card.getId()).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(card.getId()))
        .andExpect(jsonPath("$.manaCost").value("{2}{U}{R}"))
        .andExpect(jsonPath("$.oracleText").value("Flying"));
  }

  @Test
  void shouldReturnNotFoundForAnUnknownCard() throws Exception {
    mockMvc
        .perform(get("/cards/{cardId}", 999_999L).with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
  }

  @Test
  void shouldReturnCardPrintings() throws Exception {
    Card card = cardRepository.save(new Card("oracle-captain-america", "Captain America"));
    MagicSet set = magicSetRepository.save(new MagicSet("set-marvel", "mar", "Marvel Super Heroes"));
    CardPrinting printing = new CardPrinting(card, set, "printing-captain-america");
    printing.setCollectorNumber("12");
    printing.setRarity("rare");
    cardPrintingRepository.save(printing);

    mockMvc
        .perform(get("/cards/{cardId}/printings", card.getId()).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].setCode").value("mar"))
        .andExpect(jsonPath("$[0].collectorNumber").value("12"));
  }

  @Test
  void shouldForbidCardImportsForNonAdministrators() throws Exception {
    mockMvc
        .perform(post("/admin/card-imports").queryParam("query", "set:mar").with(jwt()))
        .andExpect(status().isForbidden());
  }
}
