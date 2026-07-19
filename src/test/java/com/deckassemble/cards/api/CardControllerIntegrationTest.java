package com.deckassemble.cards.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.infrastructure.CardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class CardControllerIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CardRepository cardRepository;

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
}
