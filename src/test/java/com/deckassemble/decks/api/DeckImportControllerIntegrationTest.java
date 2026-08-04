package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

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
    void shouldCommitResolvedRowsOnlyOnceForDuplicateIdempotencyKey() throws Exception {
        String subject = "auth0|deck-import-commit";
        long deckCount = deckRepository.count();
        long deckCardCount = deckCardRepository.count();
        createPrinting("Idempotent Card", "IDM", "1");
        previewFor(subject, "1 Idempotent Card|IDM|1");
        var token = previewRepository.findAll().getLast().getToken();
        String request = "{\"previewToken\":\"%s\",\"name\":\"Imported Deck\"}".formatted(token);

        commit(subject, "commit-key", request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deck.name").value("Imported Deck"))
                .andExpect(jsonPath("$.deck.cardCount").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
        commit(subject, "commit-key", request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deck.cardCount").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
        commit(
                        subject,
                        "commit-key",
                        request(token, "Changed Retry", ",\"excludedLineNumbers\":[1]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deck.name").value("Imported Deck"))
                .andExpect(jsonPath("$.deck.cardCount").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        assertThat(deckRepository.count()).isEqualTo(deckCount + 1);
        assertThat(deckCardRepository.count()).isEqualTo(deckCardCount + 1);
    }

    @Test
    void shouldRequireExplicitExclusionOfUnresolvedRows() throws Exception {
        String subject = "auth0|deck-import-exclusions";
        createPrinting("Atraxa, Praetors' Voice", "2X2", "170");
        previewFor(subject);
        var token = previewRepository.findAll().getLast().getToken();

        commit(subject, "blocked-key", request(token, "Blocked Deck", ""))
                .andExpect(status().isConflict());
        commit(
                        subject,
                        "excluded-key",
                        request(token, "Imported Deck", ",\"excludedLineNumbers\":[4,6,8,999]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(3));
    }

    @Test
    void shouldHideForeignAndExpiredPreviews() throws Exception {
        String ownerSubject = "auth0|deck-import-owner";
        previewFor(ownerSubject);
        var preview = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(ownerSubject).orElseThrow();
        var expired =
                previewRepository.save(
                        new com.deckassemble.decks.domain.DeckImportPreview(
                                java.util.UUID.randomUUID(),
                                owner.getId(),
                                Instant.now().minusSeconds(1),
                                preview.getSourceSha256(),
                                preview.getCanonicalRows()));

        commit(
                        "auth0|deck-import-foreign",
                        "foreign-key",
                        request(preview.getToken(), "Foreign Deck", ""))
                .andExpect(status().isNotFound());
        commit(ownerSubject, "expired-key", request(expired.getToken(), "Expired Deck", ""))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldValidateReplayTokenBeforeUsingExistingIdempotencyKey() throws Exception {
        String subject = "auth0|deck-import-replay-owner";
        createPrinting("Replay Card", "RPL", "1");
        previewFor(subject, "1 Replay Card|RPL|1");
        var original = previewRepository.findAll().getLast();
        commit(subject, "replay-key", request(original.getToken(), "Original Deck", ""))
                .andExpect(status().isCreated());
        previewFor("auth0|deck-import-replay-foreign", "1 Replay Card|RPL|1");
        var foreign = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(subject).orElseThrow();
        var expired =
                previewRepository.save(
                        new com.deckassemble.decks.domain.DeckImportPreview(
                                java.util.UUID.randomUUID(),
                                owner.getId(),
                                Instant.now().minusSeconds(1),
                                original.getSourceSha256(),
                                original.getCanonicalRows()));

        commit(subject, "replay-key", request(foreign.getToken(), "Foreign Retry", ""))
                .andExpect(status().isNotFound());
        commit(subject, "replay-key", request(expired.getToken(), "Expired Retry", ""))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldConvergeConcurrentSameKeyCommitsAcrossDifferentPreviews() throws Exception {
        String subject = "auth0|deck-import-concurrent";
        long deckCount = deckRepository.count();
        createPrinting("Concurrent One", "CCO", "1");
        createPrinting("Concurrent Two", "CCT", "1");
        previewFor(subject, "1 Concurrent One|CCO|1");
        var firstToken = previewRepository.findAll().getLast().getToken();
        previewFor(subject, "1 Concurrent Two|CCT|1");
        var secondToken = previewRepository.findAll().getLast().getToken();
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first =
                    executor.submit(
                            () -> concurrentCommit(subject, firstToken, "First", ready, start));
            var second =
                    executor.submit(
                            () -> concurrentCommit(subject, secondToken, "Second", ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            var firstJson =
                    JsonMapper.builder()
                            .build()
                            .readTree(first.get().getResponse().getContentAsString());
            var secondJson =
                    JsonMapper.builder()
                            .build()
                            .readTree(second.get().getResponse().getContentAsString());

            assertThat(first.get().getResponse().getStatus()).isEqualTo(201);
            assertThat(second.get().getResponse().getStatus()).isEqualTo(201);
            assertThat(firstJson.get("deck").get("id").asLong())
                    .isEqualTo(secondJson.get("deck").get("id").asLong());
            assertThat(secondJson).isEqualTo(firstJson);
            assertThat(deckRepository.count()).isEqualTo(deckCount + 1);
        }
    }

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

    private void previewFor(String subject, String source) throws Exception {
        mockMvc.perform(
                        multipart("/decks/imports/preview")
                                .file(
                                        new MockMultipartFile(
                                                "file",
                                                "deck.txt",
                                                "text/plain",
                                                source.getBytes(StandardCharsets.UTF_8)))
                                .param("format", "DECKASSEMBLE_TEXT")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions commit(
            String subject, String idempotencyKey, String request) throws Exception {
        return mockMvc.perform(
                post("/decks/imports")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .with(jwt().jwt(jwt -> jwt.subject(subject))));
    }

    private org.springframework.test.web.servlet.MvcResult concurrentCommit(
            String subject,
            java.util.UUID token,
            String name,
            CountDownLatch ready,
            CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent commit start timed out");
        }
        return commit(subject, "concurrent-key", request(token, name, "")).andReturn();
    }

    private String request(java.util.UUID token, String name, String extraFields) {
        return "{\"previewToken\":\"%s\",\"name\":\"%s\"%s}".formatted(token, name, extraFields);
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
