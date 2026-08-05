package com.deckassemble.collections.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.CollectionImportPreview;
import com.deckassemble.collections.domain.CollectionImportPreviewRepository;
import com.deckassemble.users.domain.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

class CollectionImportControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;
    @Autowired private CardCollectionRepository collectionRepository;
    @Autowired private CollectionCardRepository collectionCardRepository;
    @Autowired private CollectionImportPreviewRepository previewRepository;
    @Autowired private ProfileRepository profileRepository;

    @Test
    void shouldPreviewCollectionCsvWithoutMutatingCollections() throws Exception {
        String subject = "auth0|collection-import-preview";
        createPrinting("Atraxa, Praetors' Voice", "2X2", "170");
        long collectionCount = collectionRepository.count();
        long collectionCardCount = collectionCardRepository.count();

        mockMvc.perform(
                        multipart("/collections/imports/preview")
                                .file(fixture("moxfield.csv"))
                                .param("preset", "MOXFIELD")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(
                        jsonPath("$.resolvedRows[0].row.reference.name")
                                .value("Atraxa, Praetors' Voice"))
                .andExpect(jsonPath("$.resolvedRows[0].row.quantity").value(5))
                .andExpect(
                        jsonPath("$.unmatchedRows[1].row.reference.name")
                                .value("Lim-Dûl the Necromancer"))
                .andExpect(jsonPath("$.totals.total").value(5))
                .andExpect(jsonPath("$.totals.resolved").value(1))
                .andExpect(jsonPath("$.totals.unmatched").value(3))
                .andExpect(jsonPath("$.totals.invalid").value(1));

        var preview = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(subject).orElseThrow();
        assertThat(Duration.between(Instant.now(), preview.getExpiresAt()).toMinutes())
                .isBetween(29L, 30L);
        assertThat(preview.getProfileId()).isEqualTo(owner.getId());
        assertThat(preview.getStatus()).isEqualTo(CollectionImportPreview.Status.PENDING);
        assertThat(collectionRepository.count()).isEqualTo(collectionCount);
        assertThat(collectionCardRepository.count()).isEqualTo(collectionCardCount);
    }

    @Test
    void shouldApplyColumnMappingOverrides() throws Exception {
        String source = "qty,card_title\n3,Overridden Card";

        mockMvc.perform(
                        multipart("/collections/imports/preview")
                                .file(csv("overrides.csv", source))
                                .param("preset", "GENERIC")
                                .param("quantityColumn", "qty")
                                .param("nameColumn", "card_title")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|collection-overrides"))))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.unmatchedRows[0].row.reference.name").value("Overridden Card"))
                .andExpect(jsonPath("$.unmatchedRows[0].row.quantity").value(3));
    }

    @Test
    void shouldRejectUnsupportedPreset() throws Exception {
        mockMvc.perform(
                        multipart("/collections/imports/preview")
                                .file(fixture("generic.csv"))
                                .param("preset", "UNKNOWN")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|collection-preset"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCommitImportOnceForDuplicateIdempotencyKey() throws Exception {
        String subject = "auth0|collection-import-commit";
        createPrinting("Import Card", "IMP", "1");
        long collectionCount = collectionRepository.count();
        previewFor(subject, "quantity,name,set,collector_number\n2,Import Card,IMP,1");
        var token = previewRepository.findAll().getLast().getToken();
        String body = "{\"previewToken\":\"%s\",\"name\":\"Imported Collection\"}".formatted(token);

        var first = commit(subject, "commit-key", body).andReturn();
        assertOriginalResult(commit(subject, "commit-key", body));
        long collectionId =
                JsonMapper.builder()
                        .build()
                        .readTree(first.getResponse().getContentAsString())
                        .get("collection")
                        .get("id")
                        .asLong();
        assertThat(collectionRepository.count()).isEqualTo(collectionCount + 1);
        var cards = collectionCardRepository.findByCollectionId(collectionId);
        assertThat(cards).hasSize(1);
        assertThat(cards.getFirst().getRegularQuantity()).isEqualTo(2);
    }

    @Test
    void shouldRequireExplicitExclusionOfUnresolvedRows() throws Exception {
        String subject = "auth0|collection-import-exclusions";
        createPrinting("Included Card", "INC", "1");
        previewFor(
                subject,
                "quantity,name,set,collector_number\n"
                        + "1,Included Card,INC,1\n"
                        + "4,Ghost Card,GHO,9");
        var token = previewRepository.findAll().getLast().getToken();

        commit(
                        subject,
                        "blocked-key",
                        "{\"previewToken\":\"%s\",\"name\":\"Blocked\"}".formatted(token))
                .andExpect(status().isConflict());
        commit(
                        subject,
                        "excluded-key",
                        "{\"previewToken\":\"%s\",\"name\":\"Imported\",\"excludedLineNumbers\":[3]}"
                                .formatted(token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void shouldHideForeignAndExpiredPreviews() throws Exception {
        String ownerSubject = "auth0|collection-import-owner";
        previewFor(ownerSubject, "quantity,name\n1,Any Card");
        var preview = previewRepository.findAll().getLast();
        var owner = profileRepository.findByAuthProviderSubject(ownerSubject).orElseThrow();
        var expired =
                previewRepository.save(
                        new CollectionImportPreview(
                                UUID.randomUUID(),
                                owner.getId(),
                                Instant.now().minusSeconds(1),
                                preview.getSourceSha256(),
                                preview.getCanonicalRows()));

        commit("auth0|collection-import-foreign", "foreign-key", commitBody(preview.getToken()))
                .andExpect(status().isNotFound());
        commit(ownerSubject, "expired-key", commitBody(expired.getToken()))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/collections/imports/{token}/errors", preview.getToken())
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        "auth0|collection-import-foreign"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDownloadRejectedRowsWithReasonCodes() throws Exception {
        String subject = "auth0|collection-import-errors";
        createPrinting("Matched Card", "MAT", "1");
        createAmbiguousPrintings("Ambiguous Card");
        previewFor(
                subject,
                "quantity,name,set,collector_number\n"
                        + "2,Matched Card,MAT,1\n"
                        + "1,Ambiguous Card,,\n"
                        + "1,Ghost Card,GHO,9\n"
                        + "xyz,Broken Card,BRK,2");
        var token = previewRepository.findAll().getLast().getToken();

        errorsFor(subject, token)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
                .andExpect(
                        header().string(
                                        HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"collection-import-errors.csv\""))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "line_number,reason,quantity,name,set_code,collector_number,scryfall_id,detail")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "3,AMBIGUOUS,1,Ambiguous Card,,,,")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "4,UNMATCHED,1,Ghost Card,GHO,9,,")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "5,INVALID,0,Broken Card,BRK,2,,Invalid quantity 'xyz'")));

        commit(
                        subject,
                        "errors-key",
                        "{\"previewToken\":\"%s\",\"name\":\"Imported\",\"excludedLineNumbers\":[3,4,5]}"
                                .formatted(token))
                .andExpect(status().isCreated());
        errorsFor(subject, token)
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("3,AMBIGUOUS")));
    }

    private void assertOriginalResult(ResultActions result) throws Exception {
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.collection.name").value("Imported Collection"))
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    private void previewFor(String subject, String source) throws Exception {
        mockMvc.perform(
                        multipart("/collections/imports/preview")
                                .file(csv("collection.csv", source))
                                .param("preset", "GENERIC")
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
    }

    private ResultActions commit(String subject, String idempotencyKey, String request)
            throws Exception {
        return mockMvc.perform(
                post("/collections/imports")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
                        .with(jwt().jwt(jwt -> jwt.subject(subject))));
    }

    private ResultActions errorsFor(String subject, UUID token) throws Exception {
        return mockMvc.perform(
                get("/collections/imports/{token}/errors", token)
                        .with(jwt().jwt(jwt -> jwt.subject(subject))));
    }

    private MockMultipartFile fixture(String name) throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/collection-imports/" + name)) {
            return new MockMultipartFile("file", name, "text/csv", stream.readAllBytes());
        }
    }

    private static MockMultipartFile csv(String name, String source) {
        return new MockMultipartFile(
                "file", name, "text/csv", source.getBytes(StandardCharsets.UTF_8));
    }

    private static String commitBody(UUID token) {
        return "{\"previewToken\":\"%s\",\"name\":\"Imported\"}".formatted(token);
    }

    private void createAmbiguousPrintings(String name) {
        if (!cardRepository.findByNameIgnoreCase(name).isEmpty()) {
            return;
        }
        String uniqueId = UUID.randomUUID().toString();
        Card card = cardRepository.save(new Card("oracle-" + uniqueId, name));
        for (String setCode : new String[] {"AA1", "AA2"}) {
            MagicSet set =
                    magicSetRepository.save(
                            new MagicSet("set-" + uniqueId + setCode, setCode, setCode));
            CardPrinting printing = new CardPrinting(card, set, "printing-" + uniqueId + setCode);
            printing.setCollectorNumber("1");
            printingRepository.save(printing);
        }
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
