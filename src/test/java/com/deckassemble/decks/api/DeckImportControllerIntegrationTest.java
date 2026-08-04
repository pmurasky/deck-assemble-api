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
import com.deckassemble.users.domain.ProfileRepository;
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
    @Autowired private ProfileRepository profileRepository;

    @Test
    void shouldPreviewImportWithoutMutatingDecksOrCards() throws Exception {
        String ownerSubject = "auth0|deck-import";
        createPrinting("Atraxa, Praetors' Voice", "2X2", "170");
        long deckCount = deckRepository.count();
        long deckCardCount = deckCardRepository.count();

        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(fixture("deckassemble.txt"))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject(ownerSubject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.metadata.format").value("DECKASSEMBLE_TEXT"))
                .andExpect(jsonPath("$.resolvedRows[0].row.quantity").value(1))
                .andExpect(jsonPath("$.totals.total").value(4))
                .andExpect(jsonPath("$.totals.resolved").value(1))
                .andExpect(jsonPath("$.totals.unmatched").value(3));

        var preview = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(ownerSubject).orElseThrow();
        String otherSubject = "auth0|deck-import-other";
        previewFor(otherSubject);
        var other = profileRepository.findByAuthProviderSubject(otherSubject).orElseThrow();
        assertThat(Duration.between(Instant.now(), preview.getExpiresAt()).toMinutes())
                .isBetween(29L, 30L);
        assertThat(preview.getProfileId()).isEqualTo(owner.getId());
        assertThat(previewRepository.findByTokenAndProfileId(preview.getToken(), owner.getId()))
                .isPresent();
        assertThat(previewRepository.findByTokenAndProfileId(preview.getToken(), other.getId()))
                .isEmpty();
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
        try (var stream = getClass().getResourceAsStream("/fixtures/deck-imports/" + name)) {
            return new MockMultipartFile("file", name, "text/plain", stream.readAllBytes());
        }
    }

    private void previewFor(String subject) throws Exception {
        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(fixture("deckassemble.txt"))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
    }

    private void createPrinting(String name, String setCode, String collectorNumber) {
        if (!printingRepository
                .findByCardNameIgnoreCaseAndMagicSetSetCodeIgnoreCaseAndCollectorNumberIgnoreCase(
                        name, setCode, collectorNumber)
                .isEmpty()) {
            return;
        }
        String uniqueId = java.util.UUID.randomUUID().toString();
        Card card = cardRepository.save(new Card("oracle-" + uniqueId, name));
        MagicSet set =
                magicSetRepository
                        .findBySetCode(setCode)
                        .orElseGet(
                                () ->
                                        magicSetRepository.save(
                                                new MagicSet("set-" + setCode, setCode, setCode)));
        CardPrinting printing = new CardPrinting(card, set, "printing-" + uniqueId);
        printing.setCollectorNumber(collectorNumber);
        printingRepository.save(printing);
    }
}
