package com.deckassemble.cards.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPriceSnapshot;
import com.deckassemble.cards.domain.CardPriceSnapshotRepository;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingFace;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.imports.domain.CardImportRun;
import com.deckassemble.imports.domain.CardImportRunRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class CardControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardImportRunRepository cardImportRunRepository;
    @Autowired private CardPriceSnapshotRepository cardPriceSnapshotRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private CardCollectionRepository cardCollectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;

    @Test
    void shouldReturnActiveCardsMatchingTheNameQuery() throws Exception {
        Card card = cardRepository.save(new Card("oracle-spider", "Spider-Man, Web-Slinger"));

        mockMvc.perform(get("/cards").queryParam("query", "spider").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(card.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Spider-Man, Web-Slinger"));
    }

    @Test
    void shouldReturnCardsMatchingAFlavorNameQuery() throws Exception {
        MagicSet set = magicSetRepository.save(new MagicSet("set-flv", "flv", "Flavor Set"));
        Card card = cardRepository.save(new Card("oracle-beast-within", "Beast Within"));
        CardPrinting printing = new CardPrinting(card, set, "printing-mar-75");
        printing.setFlavorName("Grimm Fate");
        cardPrintingRepository.save(printing);

        mockMvc.perform(get("/cards").queryParam("query", "grimm").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(card.getId()))
                .andExpect(jsonPath("$.content[0].name").value("Beast Within"));
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
    void shouldReturnCardLegalities() throws Exception {
        Card card = new Card("oracle-captain-marvel", "Captain Marvel");
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));
        cardRepository.save(card);

        mockMvc.perform(get("/cards/{cardId}", card.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalities.commander").value("legal"));
    }

    @Test
    void shouldReturnCardLegalityInSearchResults() throws Exception {
        Card card = new Card("oracle-captain-marvel-search", "Captain Marvel Search");
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));
        cardRepository.save(card);

        mockMvc.perform(get("/cards").queryParam("query", "captain marvel search").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].legalities.commander").value("legal"));
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
    void shouldFilterCardsByTypeLine() throws Exception {
        Card creature = cardRepository.save(new Card("oracle-hulk", "Hulk, Incredible"));
        creature.setTypeLine("Legendary Creature — Human Warrior");
        cardRepository.save(creature);
        Card instant = cardRepository.save(new Card("oracle-repulse", "Repulse"));
        instant.setTypeLine("Instant");
        cardRepository.save(instant);

        mockMvc.perform(get("/cards").queryParam("type", "instant").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Repulse"));
    }

    @Test
    void shouldFilterCardsByCommanderEligibility() throws Exception {
        Card commander = cardRepository.save(new Card("oracle-atraxa", "Atraxa, Praetors' Voice"));
        commander.setTypeLine("Legendary Creature — Phyrexian Angel Horror");
        cardRepository.save(commander);
        Card planeswalker =
                cardRepository.save(new Card("oracle-ajani", "Ajani, Caller of the Pride"));
        planeswalker.setTypeLine("Legendary Planeswalker — Ajani");
        planeswalker.setOracleText(
                "+1: Something. Ajani, Caller of the Pride can be your commander.");
        cardRepository.save(planeswalker);
        Card instant = cardRepository.save(new Card("oracle-shock", "Shock"));
        instant.setTypeLine("Instant");
        cardRepository.save(instant);

        mockMvc.perform(get("/cards").queryParam("commanderEligible", "true").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath(
                                "$.content[*].name",
                                hasItems("Atraxa, Praetors' Voice", "Ajani, Caller of the Pride")))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Shock"))));
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
        printing.setReleasedAt(LocalDate.of(2025, 1, 1));
        printing.getFaces()
                .addAll(
                        List.of(
                                new CardPrintingFace(
                                        printing, 0, "Thor", "https://img.example/thor-front.png"),
                                new CardPrintingFace(
                                        printing,
                                        1,
                                        "Thor, God of Flips",
                                        "https://img.example/thor-back.png")));
        cardPrintingRepository.save(printing);
        CardPrinting latestPrinting = new CardPrinting(card, set, "printing-thor-latest");
        latestPrinting.setImageUriNormal("https://img.example/thor-latest.png");
        latestPrinting.setReleasedAt(LocalDate.of(2026, 1, 1));
        latestPrinting
                .getFaces()
                .addAll(
                        List.of(
                                new CardPrintingFace(
                                        latestPrinting,
                                        0,
                                        "Thor Redux",
                                        "https://img.example/thor-redux-front.png"),
                                new CardPrintingFace(
                                        latestPrinting,
                                        1,
                                        "Thor Redux, Flipped",
                                        "https://img.example/thor-redux-back.png")));
        cardPrintingRepository.save(latestPrinting);

        mockMvc.perform(get("/cards").queryParam("query", "thor").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].printingId").value(latestPrinting.getId()))
                .andExpect(
                        jsonPath("$.content[0].imageUrl")
                                .value("https://img.example/thor-latest.png"))
                .andExpect(jsonPath("$.content[0].faces.length()").value(2))
                .andExpect(jsonPath("$.content[0].faces[0].name").value("Thor Redux"))
                .andExpect(
                        jsonPath("$.content[0].faces[1].imageUrl")
                                .value("https://img.example/thor-redux-back.png"))
                .andExpect(jsonPath("$.content[0].setCode").value("msu"));
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

    @Test
    void shouldFilterByCompoundOracleTextManaValueAndFormatLegality() throws Exception {
        Card match = cardRepository.save(new Card("oracle-compound-match", "Compound Match"));
        match.setOracleText("Exile target creature.");
        match.setManaValue(java.math.BigDecimal.valueOf(1));
        match.getLegalities().add(new CardLegality(match, "commander", "legal"));
        cardRepository.save(match);
        Card wrongText = cardRepository.save(new Card("oracle-compound-text", "Compound Text"));
        wrongText.setOracleText("Draw a card.");
        wrongText.setManaValue(java.math.BigDecimal.valueOf(1));
        wrongText.getLegalities().add(new CardLegality(wrongText, "commander", "legal"));
        cardRepository.save(wrongText);
        Card wrongMv = cardRepository.save(new Card("oracle-compound-mv", "Compound Mv"));
        wrongMv.setOracleText("Exile target creature.");
        wrongMv.setManaValue(java.math.BigDecimal.valueOf(9));
        wrongMv.getLegalities().add(new CardLegality(wrongMv, "commander", "legal"));
        cardRepository.save(wrongMv);

        mockMvc.perform(
                        get("/cards")
                                .queryParam("oracleText", "exile")
                                .queryParam("maxManaValue", "2")
                                .queryParam("formatCode", "commander")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Compound Match"));
    }

    @Test
    void shouldRespectPageSizeAndSortBounds() throws Exception {
        cardRepository.save(new Card("oracle-page-a", "Page Bound Alpha"));
        cardRepository.save(new Card("oracle-page-b", "Page Bound Bravo"));
        cardRepository.save(new Card("oracle-page-c", "Page Bound Charlie"));

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Page Bound")
                                .queryParam("page", "0")
                                .queryParam("size", "2")
                                .queryParam("sort", "name,asc")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Page Bound Alpha"))
                .andExpect(jsonPath("$.content[1].name").value("Page Bound Bravo"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Page Bound")
                                .queryParam("page", "1")
                                .queryParam("size", "2")
                                .queryParam("sort", "name,asc")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Page Bound Charlie"));
    }

    @Test
    void shouldReturnEmptyResultsForAnInvertedManaValueRange() throws Exception {
        Card card = cardRepository.save(new Card("oracle-inverted-range", "Inverted Range"));
        card.setManaValue(java.math.BigDecimal.valueOf(3));
        cardRepository.save(card);

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Inverted Range")
                                .queryParam("minManaValue", "5")
                                .queryParam("maxManaValue", "1")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldReturnEmptyResultsForAnInvertedPowerRange() throws Exception {
        Card card = cardRepository.save(new Card("oracle-inverted-power", "Inverted Power"));
        card.setPower("7");
        cardRepository.save(card);

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Inverted Power")
                                .queryParam("minPower", "15")
                                .queryParam("maxPower", "5")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void shouldRejectAnUnknownFunctionalCategory() throws Exception {
        mockMvc.perform(
                        get("/cards")
                                .queryParam("functionalCategory", "not-a-category")
                                .with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_FILTER"));
    }

    @Test
    void shouldRejectAnOutOfBoundsPowerFilter() throws Exception {
        mockMvc.perform(get("/cards").queryParam("minPower", "-1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectANegativeManaValueFilter() throws Exception {
        mockMvc.perform(get("/cards").queryParam("minManaValue", "-1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFilterCardsByOwnedQuantityRange() throws Exception {
        String subject = "auth0|owned-quantity-search";
        Profile profile = profileRepository.save(new Profile(subject, subject));
        MagicSet set = magicSetRepository.save(new MagicSet("set-owned-qty", "oqs", "Owned Qty"));
        Card owned = cardRepository.save(new Card("oracle-owned-qty-owned", "Owned Qty Owned"));
        CardPrinting ownedPrinting =
                cardPrintingRepository.save(new CardPrinting(owned, set, "owned-qty-printing"));
        Card unowned =
                cardRepository.save(new Card("oracle-owned-qty-unowned", "Owned Qty Unowned"));
        cardPrintingRepository.save(new CardPrinting(unowned, set, "unowned-qty-printing"));
        CardCollection collection =
                cardCollectionRepository.save(
                        new CardCollection(profile.getId(), "Binder", null, true));
        collectionCardRepository.save(
                new CollectionCard(collection.getId(), ownedPrinting.getId(), 3, 0));

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Owned Qty")
                                .queryParam("minOwnedQuantity", "1")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Owned Qty Owned"));
    }

    @Test
    void shouldFilterCardsByPriceRange() throws Exception {
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-price-range", "prc", "Price Range"));
        Card cheap = cardRepository.save(new Card("oracle-price-cheap", "Price Cheap"));
        CardPrinting cheapPrinting =
                cardPrintingRepository.save(new CardPrinting(cheap, set, "price-cheap-printing"));
        cardPriceSnapshotRepository.save(
                new CardPriceSnapshot(
                        cheapPrinting.getId(),
                        new CardPrice(BigDecimal.valueOf(1), null, null, null),
                        Instant.now()));
        Card expensive = cardRepository.save(new Card("oracle-price-expensive", "Price Expensive"));
        CardPrinting expensivePrinting =
                cardPrintingRepository.save(
                        new CardPrinting(expensive, set, "price-expensive-printing"));
        cardPriceSnapshotRepository.save(
                new CardPriceSnapshot(
                        expensivePrinting.getId(),
                        new CardPrice(BigDecimal.valueOf(100), null, null, null),
                        Instant.now()));

        mockMvc.perform(
                        get("/cards")
                                .queryParam("query", "Price")
                                .queryParam("minPrice", "0")
                                .queryParam("maxPrice", "10")
                                .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Price Cheap"));
    }
}
