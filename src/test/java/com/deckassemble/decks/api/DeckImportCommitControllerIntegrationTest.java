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
import com.deckassemble.decks.domain.DeckImportPreview;
import com.deckassemble.decks.domain.DeckImportPreviewRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

class DeckImportCommitControllerIntegrationTest extends AbstractIntegrationTest {

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
        String body = request(token, "Imported Deck", "");

        assertOriginalResult(commit(subject, "commit-key", body));
        assertOriginalResult(commit(subject, "commit-key", body));
        assertOriginalResult(
                commit(
                        subject,
                        "commit-key",
                        request(token, "Changed Retry", ",\"excludedLineNumbers\":[1]")));
        assertThat(deckRepository.count()).isEqualTo(deckCount + 1);
        assertThat(deckCardRepository.count()).isEqualTo(deckCardCount + 1);
    }

    @Test
    void shouldRequireExplicitExclusionOfUnresolvedRows() throws Exception {
        String subject = "auth0|deck-import-exclusions";
        createPrinting("Atraxa, Praetors' Voice", "2X2", "170");
        previewFor(subject, fixture("deckassemble.txt"));
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
        previewFor(ownerSubject, fixture("deckassemble.txt"));
        var preview = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(ownerSubject).orElseThrow();
        var expired = expiredPreview(owner.getId(), preview);

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
        var expired = expiredPreview(owner.getId(), original);

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
            var firstResult = first.get();
            var secondResult = second.get();
            var mapper = JsonMapper.builder().build();

            assertThat(firstResult.getResponse().getStatus()).isEqualTo(201);
            assertThat(secondResult.getResponse().getStatus()).isEqualTo(201);
            assertThat(mapper.readTree(secondResult.getResponse().getContentAsString()))
                    .isEqualTo(mapper.readTree(firstResult.getResponse().getContentAsString()));
            assertThat(deckRepository.count()).isEqualTo(deckCount + 1);
        }
    }

    private void assertOriginalResult(ResultActions result) throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.deck.name").value("Imported Deck"))
                .andExpect(jsonPath("$.deck.cardCount").value(1))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
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

    private ResultActions commit(String subject, String idempotencyKey, String request)
            throws Exception {
        return mockMvc.perform(
                post("/decks/imports")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .with(jwt().jwt(jwt -> jwt.subject(subject))));
    }

    private MvcResult concurrentCommit(
            String subject, UUID token, String name, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent commit start timed out");
        }
        return commit(subject, "concurrent-key", request(token, name, "")).andReturn();
    }

    private DeckImportPreview expiredPreview(long profileId, DeckImportPreview source) {
        return previewRepository.save(
                new DeckImportPreview(
                        UUID.randomUUID(),
                        profileId,
                        Instant.now().minusSeconds(1),
                        source.getSourceSha256(),
                        source.getCanonicalRows()));
    }

    private String fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/deck-imports/" + name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String request(UUID token, String name, String extraFields) {
        return "{\"previewToken\":\"%s\",\"name\":\"%s\"%s}".formatted(token, name, extraFields);
    }

    private void createPrinting(String name, String setCode, String collectorNumber) {
        if (!printingRepository
                .findExactPrintingReference(name, setCode, collectorNumber)
                .isEmpty()) {
            return;
        }
        String uniqueId = UUID.randomUUID().toString();
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
