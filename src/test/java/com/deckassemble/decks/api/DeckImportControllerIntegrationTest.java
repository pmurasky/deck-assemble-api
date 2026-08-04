package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import com.deckassemble.decks.domain.DeckRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

class DeckImportControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckCardRepository deckCardRepository;
    @Autowired private DeckImportPreviewRepository previewRepository;

    @Test
    void shouldPreviewImportWithoutMutatingDecksOrCards() throws Exception {
        createPrinting("Atraxa, Praetors' Voice", "2X2", "170");
        long deckCount = deckRepository.count();
        long deckCardCount = deckCardRepository.count();

        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(fixture("deckassemble.txt"))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|deck-import"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.metadata.format").value("DECKASSEMBLE_TEXT"))
                .andExpect(jsonPath("$.resolvedRows[0].row.quantity").value(1))
                .andExpect(jsonPath("$.totals.total").value(4))
                .andExpect(jsonPath("$.totals.resolved").value(1))
                .andExpect(jsonPath("$.totals.unmatched").value(3));

        var preview = previewRepository.findAll().getLast();
        assertThat(Duration.between(Instant.now(), preview.getExpiresAt()).toMinutes())
                .isBetween(29L, 30L);
        assertThat(deckRepository.count()).isEqualTo(deckCount);
        assertThat(deckCardRepository.count()).isEqualTo(deckCardCount);
    }

    @Test
    void shouldRejectOversizedImportFile() throws Exception {
        byte[] oversized = new byte[1024 * 1024 + 1];

        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(
                                        new MockMultipartFile(
                                                "file", "deck.txt", "text/plain", oversized))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|oversized-import"))))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void shouldRejectImportWithTooManyRows() throws Exception {
        String rows = "1 Missing Card|TST|1\n".repeat(501);

        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "deck.txt",
                                                "text/plain",
                                                rows.getBytes(StandardCharsets.UTF_8)))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|row-limit-import"))))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void shouldRejectUnsupportedImportFormat() throws Exception {
        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(fixture("deckassemble.txt"))
                                .param("format", "UNKNOWN")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|unsupported-import"))))
                .andExpect(status().isBadRequest());
    }

    private MockMultipartFile fixture(String name) throws Exception {
        byte[] bytes =
                getClass().getResourceAsStream("/fixtures/deck-imports/" + name).readAllBytes();
        return new MockMultipartFile("file", name, "text/plain", bytes);
    }

    private void createPrinting(String name, String setCode, String collectorNumber) {
        Card card = cardRepository.save(new Card("oracle-" + setCode, name));
        MagicSet set = magicSetRepository.save(new MagicSet("set-" + setCode, setCode, setCode));
        CardPrinting printing = new CardPrinting(card, set, "printing-" + setCode);
        printing.setCollectorNumber(collectorNumber);
        printingRepository.save(printing);
    }
}
