package com.deckassemble.cards.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.infrastructure.CardImportRunRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class CardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardImportRunRepository cardImportRunRepository;

    @Test
    void shouldReturnActiveCardsMatchingTheNameQuery() throws Exception {
        Card card = cardRepository.save(new Card("oracle-spider", "Spider-Man, Web-Slinger"));

        mockMvc.perform(get("/cards").queryParam("query", "spider").with(jwt()))
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

        mockMvc.perform(get("/cards/{cardId}", card.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(card.getId()))
                .andExpect(jsonPath("$.manaCost").value("{2}{U}{R}"))
                .andExpect(jsonPath("$.oracleText").value("Flying"));
    }

    @Test
    void shouldReturnNotFoundForAnUnknownCard() throws Exception {
        mockMvc.perform(get("/cards/{cardId}", 999_999L).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CARD_NOT_FOUND"));
    }

    @Test
    void shouldReturnCardPrintings() throws Exception {
        Card card = cardRepository.save(new Card("oracle-captain-america", "Captain America"));
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet("set-marvel-printings", "mpr", "Marvel Super Heroes"));
        CardPrinting printing = new CardPrinting(card, set, "printing-captain-america");
        printing.setCollectorNumber("12");
        printing.setRarity("rare");
        cardPrintingRepository.save(printing);

        mockMvc.perform(get("/cards/{cardId}/printings", card.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].setCode").value("mpr"))
                .andExpect(jsonPath("$[0].collectorNumber").value("12"));
    }

    @Test
    void shouldReturnEveryPrintingInASet() throws Exception {
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet("set-marvel-gallery", "msh", "Marvel Super Heroes"));
        Card card = cardRepository.save(new Card("oracle-wolverine", "Wolverine, Best There Is"));
        CardPrinting first =
                cardPrintingRepository.save(new CardPrinting(card, set, "printing-wolverine-1"));
        CardPrinting second =
                cardPrintingRepository.save(new CardPrinting(card, set, "printing-wolverine-2"));
        first.setImageUriNormal("https://img.example/wolverine-1.png");
        second.setImageUriNormal("https://img.example/wolverine-2.png");
        cardPrintingRepository.saveAll(java.util.List.of(first, second));

        mockMvc.perform(get("/sets/{setCode}/printings", "msh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].printingId").exists())
                .andExpect(jsonPath("$.content[1].printingId").exists());
    }

    @Test
    void shouldSearchPrintingsByCardNameWithinASet() throws Exception {
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-marvel-search", "mshs", "Marvel Search"));
        Card wolverine = cardRepository.save(new Card("oracle-wolverine-search", "Wolverine"));
        Card storm = cardRepository.save(new Card("oracle-storm-search", "Storm"));
        cardPrintingRepository.save(new CardPrinting(wolverine, set, "printing-wolverine-search"));
        cardPrintingRepository.save(new CardPrinting(storm, set, "printing-storm-search"));

        mockMvc.perform(get("/sets/{setCode}/printings", "mshs").queryParam("query", "wolverine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Wolverine"));
    }

    @Test
    void shouldFilterCardsBySetAndColorIdentity() throws Exception {
        Card card = cardRepository.save(new Card("oracle-storm", "Storm"));
        card.setColorIdentity("U,R");
        cardRepository.save(card);
        MagicSet set = magicSetRepository.save(new MagicSet("set-marvel-filter", "mar", "Marvel"));
        cardPrintingRepository.save(new CardPrinting(card, set, "printing-storm"));

        mockMvc.perform(
                        get("/cards")
                                .queryParam("setCode", "mar")
                                .queryParam("colorIdentity", "U")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Storm"));
    }

    @Test
    void shouldAllowAnonymousCardBrowsing() throws Exception {
        cardRepository.save(new Card("oracle-public", "Public Card"));

        mockMvc.perform(get("/cards").queryParam("query", "public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Public Card"));
    }

    @Test
    void shouldIncludePrintingDataInCardSummary() throws Exception {
        Card card = cardRepository.save(new Card("oracle-thor", "Thor"));
        MagicSet set =
                magicSetRepository.save(
                        new MagicSet("set-marvel-summary", "msu", "Marvel Summary"));
        CardPrinting printing = new CardPrinting(card, set, "printing-thor");
        printing.setRarity("mythic");
        printing.setImageUriNormal("https://img.example/thor.png");
        cardPrintingRepository.save(printing);

        mockMvc.perform(get("/cards").queryParam("query", "thor").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].printingId").value(printing.getId()))
                .andExpect(jsonPath("$.content[0].imageUrl").value("https://img.example/thor.png"))
                .andExpect(jsonPath("$.content[0].setCode").value("msu"))
                .andExpect(jsonPath("$.content[0].rarity").value("mythic"));
    }

    @Test
    void shouldReturnLatestImportRun() throws Exception {
        var run =
                new CardImportRun(
                        "scryfall",
                        "set:msh",
                        OffsetDateTime.parse("2026-07-19T20:00:00Z"),
                        "admin-sub");
        for (int index = 0; index < 300; index++) {
            run.recordCreated();
        }
        for (int index = 0; index < 153; index++) {
            run.recordUpdated();
        }
        run.complete(OffsetDateTime.parse("2026-07-19T20:05:00Z"));
        cardImportRunRepository.save(run);

        mockMvc.perform(get("/card-imports/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("scryfall"))
                .andExpect(jsonPath("$.query").value("set:msh"))
                .andExpect(jsonPath("$.recordsRead").value(453))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void shouldForbidCardImportsForNonAdministrators() throws Exception {
        mockMvc.perform(post("/admin/card-imports").queryParam("query", "set:mar").with(jwt()))
                .andExpect(status().isForbidden());
    }
}
