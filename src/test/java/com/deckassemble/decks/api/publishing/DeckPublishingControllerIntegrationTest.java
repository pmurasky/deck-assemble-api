package com.deckassemble.decks.api.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

class DeckPublishingControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern SHARE_SLUG_PATTERN = Pattern.compile("\"shareSlug\":\"([^\"]+)\"");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DeckRepository deckRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private MagicSetRepository magicSetRepository;
    @Autowired private CardPrintingRepository printingRepository;

    @ParameterizedTest
    @EnumSource(
            value = DeckVisibility.class,
            names = {"UNLISTED", "PUBLIC"})
    void shouldAssignSlugAndResolveSharedViewForOwnerStrangerAndAnonymous(DeckVisibility visibility)
            throws Exception {
        String owner = "auth0|publishing-owner-" + visibility;
        String stranger = "auth0|publishing-stranger-" + visibility;
        long deckId = createDeck(owner, "Shareable " + visibility);

        String slug = publish(owner, deckId, visibility);
        assertThat(slug).isNotBlank();

        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deckId").value(deckId))
                .andExpect(jsonPath("$.visibility").value(visibility.name()));
        mockMvc.perform(
                        get("/shared/decks/{slug}", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk());
        mockMvc.perform(
                        get("/shared/decks/{slug}", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenySharedViewOfAPrivateDeckForOwnerStrangerAndAnonymous() throws Exception {
        String owner = "auth0|publishing-private-owner";
        String stranger = "auth0|publishing-private-stranger";
        long deckId = createDeck(owner, "Stays Private");

        // Publish then revert to private: the slug stays assigned (stable), but must stop
        // resolving — proves the visibility gate, not merely "no slug yet".
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);
        publish(owner, deckId, DeckVisibility.PRIVATE);

        mockMvc.perform(get("/shared/decks/{slug}", slug)).andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/shared/decks/{slug}", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/shared/decks/{slug}", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForAnUnknownSlug() throws Exception {
        mockMvc.perform(get("/shared/decks/{slug}", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldKeepTheSameSlugStableAcrossVisibilityToggles() throws Exception {
        String owner = "auth0|publishing-stable-slug";
        long deckId = createDeck(owner, "Toggling Deck");

        String firstSlug = publish(owner, deckId, DeckVisibility.UNLISTED);
        publish(owner, deckId, DeckVisibility.PRIVATE);
        String secondSlug = publish(owner, deckId, DeckVisibility.PUBLIC);

        assertThat(secondSlug).isEqualTo(firstSlug);
    }

    @Test
    void shouldGenerateDistinctNonSequentialSlugsAcrossDecks() throws Exception {
        String owner = "auth0|publishing-nonsequential";
        long deckA = createDeck(owner, "Deck A");
        long deckB = createDeck(owner, "Deck B");

        String slugA = publish(owner, deckA, DeckVisibility.PUBLIC);
        String slugB = publish(owner, deckB, DeckVisibility.PUBLIC);

        assertThat(slugA).isNotEqualTo(slugB);
        assertThat(slugA).matches("^[A-Za-z0-9_-]+$");
        assertThat(slugB).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void shouldRejectNonOwnerPatchingPublishing() throws Exception {
        String owner = "auth0|publishing-patch-owner";
        String stranger = "auth0|publishing-patch-stranger";
        long deckId = createDeck(owner, "Owned Deck");

        mockMvc.perform(
                        patch("/decks/{deckId}/publishing", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousPatchingPublishing() throws Exception {
        String owner = "auth0|publishing-patch-anon-owner";
        long deckId = createDeck(owner, "Owned Deck");

        mockMvc.perform(
                        patch("/decks/{deckId}/publishing", deckId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"visibility\":\"PUBLIC\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldStillResolveSharedViewAfterArchivingTheDeck() throws Exception {
        String owner = "auth0|publishing-archive-after";
        long deckId = createDeck(owner, "Archive Me");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/decks/{deckId}/archive", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/shared/decks/{slug}", slug)).andExpect(status().isOk());
    }

    @Test
    void shouldAllowPublishingAnAlreadyArchivedDeck() throws Exception {
        String owner = "auth0|publishing-archive-before";
        long deckId = createDeck(owner, "Archive First");
        mockMvc.perform(
                        post("/decks/{deckId}/archive", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk());

        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);

        assertThat(slug).isNotBlank();
        mockMvc.perform(get("/shared/decks/{slug}", slug)).andExpect(status().isOk());
    }

    @Test
    void shouldStoreAndReturnPrimerTitleAndUtf8MarkdownSourceForOwner() throws Exception {
        String owner = "auth0|primer-owner-utf8";
        long deckId = createDeck(owner, "UTF-8 Primer Deck");
        String title = "Café Primer — 你好 🐉";
        String markdown = "# Café Primer — 你好 🐉\n\nPlay lands, then dragons. Ünïcödé throughout.";

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest(title, markdown, null))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.deckId").value(deckId))
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.markdownSource").value(markdown));
    }

    @Test
    void shouldReturnDangerousHtmlInMarkdownSourceAsAnInertJsonStringWithoutStrippingIt()
            throws Exception {
        String owner = "auth0|primer-owner-danger";
        long deckId = createDeck(owner, "Danger Primer Deck");
        String markdown = "Beware: <script>alert('xss')</script> and <img src=x onerror=alert(1)>.";

        MvcResult result =
                mockMvc.perform(
                                put("/decks/{deckId}/primer", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        new DeckPrimerRequest(
                                                                "Danger", markdown, null))))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.markdownSource").value(markdown))
                        .andReturn();

        // Round-trips byte-for-byte as a plain JSON string value — never executed, never
        // silently stripped. That is the correct "store raw, don't render" behavior for this
        // task: there is no server-side HTML template that would interpolate this unescaped.
        DeckPrimerResponse response =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(), DeckPrimerResponse.class);
        assertThat(response.markdownSource()).isEqualTo(markdown);
    }

    @Test
    void shouldRejectPrimerMarkdownSourceExceedingTheSizeLimit() throws Exception {
        String owner = "auth0|primer-owner-toolong";
        long deckId = createDeck(owner, "Too Long Primer Deck");
        String tooLong = "a".repeat(20_001);

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest("Title", tooLong, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPrimerTitleExceedingTheSizeLimit() throws Exception {
        String owner = "auth0|primer-owner-titletoolong";
        long deckId = createDeck(owner, "Too Long Title Deck");
        String tooLong = "a".repeat(201);

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest(tooLong, "Body", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonOwnerUpdatingThePrimer() throws Exception {
        String owner = "auth0|primer-patch-owner";
        String stranger = "auth0|primer-patch-stranger";
        long deckId = createDeck(owner, "Owned Primer Deck");

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest("Title", "Body", null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousUpdatingThePrimer() throws Exception {
        String owner = "auth0|primer-patch-anon-owner";
        long deckId = createDeck(owner, "Owned Primer Deck");

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest("Title", "Body", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnRevisionNumberAndRejectStalePrimerEdit() throws Exception {
        String owner = "auth0|primer-rev-owner";
        long deckId = createDeck(owner, "Primer Rev Deck");

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest("Guide", "Body", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").isNumber());

        // The deck already advanced past revision 0, so a primer edit claiming that base conflicts.
        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest("Guide 2", "Body 2", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DECK_REVISION_CONFLICT"));
    }

    @ParameterizedTest
    @EnumSource(
            value = DeckVisibility.class,
            names = {"UNLISTED", "PUBLIC"})
    void shouldSurfaceThePrimerOnTheSharedViewForAnyoneTheVisibilityPermits(
            DeckVisibility visibility) throws Exception {
        String owner = "auth0|primer-shared-" + visibility;
        String stranger = "auth0|primer-shared-stranger-" + visibility;
        long deckId = createDeck(owner, "Primer Shared " + visibility);
        String slug = publish(owner, deckId, visibility);

        mockMvc.perform(
                        put("/decks/{deckId}/primer", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new DeckPrimerRequest(
                                                        "Guide", "# How to play", null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerTitle").value("Guide"))
                .andExpect(jsonPath("$.primerMarkdown").value("# How to play"));
        mockMvc.perform(
                        get("/shared/decks/{slug}", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primerTitle").value("Guide"));
    }

    @Test
    void shouldKeepSharedContentPinnedAfterPrivateEditsUntilRepublished() throws Exception {
        String owner = "auth0|publish-pin-owner";
        long deckId = createDeck(owner, "Original Name");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);
        pin(owner, deckId);

        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original Name"));

        rename(owner, deckId, "Edited Name");

        // The live deck changed, but the shared view is pinned to the revision at publish time.
        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original Name"));
        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(jsonPath("$.name").value("Edited Name"));

        pin(owner, deckId);

        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Edited Name"));
    }

    @Test
    void shouldFallBackToLiveStateForAVisibleDeckThatWasNeverPublished() throws Exception {
        String owner = "auth0|publish-never-owner";
        long deckId = createDeck(owner, "Live Only");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);

        rename(owner, deckId, "Still Live");

        mockMvc.perform(get("/shared/decks/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Still Live"));
    }

    @Test
    void shouldRejectNonOwnerPublishing() throws Exception {
        String owner = "auth0|publish-owner-guard";
        String stranger = "auth0|publish-stranger-guard";
        long deckId = createDeck(owner, "Owned Deck");

        mockMvc.perform(
                        post("/decks/{deckId}/publish", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousPublishing() throws Exception {
        String owner = "auth0|publish-anon-guard";
        long deckId = createDeck(owner, "Owned Deck");

        mockMvc.perform(post("/decks/{deckId}/publish", deckId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForkAPublishedDeckCopyingThePinnedSnapshotContent() throws Exception {
        String owner = "auth0|fork-source-owner";
        String forker = "auth0|fork-caller";
        long sourceDeckId = createDeck(owner, "Fork Source");
        addCard(owner, sourceDeckId, createPrinting("fork-card"));
        String slug = publish(owner, sourceDeckId, DeckVisibility.PUBLIC);
        pin(owner, sourceDeckId); // pins revision 2 (CREATED, then CARD_ADDED)
        // Edit privately after pinning: the fork must NOT see this, only the pinned content.
        rename(owner, sourceDeckId, "Renamed After Pin");

        MvcResult result =
                mockMvc.perform(
                                post("/shared/decks/{slug}/fork", slug)
                                        .with(jwt().jwt(jwt -> jwt.subject(forker))))
                        .andExpect(status().isCreated())
                        .andReturn();
        DeckForkResponse forkResponse =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(), DeckForkResponse.class);

        assertThat(forkResponse.name()).isEqualTo("Fork Source");
        assertThat(forkResponse.sourceDeckId()).isEqualTo(sourceDeckId);
        assertThat(forkResponse.sourceRevisionNumber()).isEqualTo(2);
        mockMvc.perform(
                        get("/decks/{deckId}", forkResponse.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fork Source"))
                .andExpect(jsonPath("$.cardCount").value(1));
        // Forked deck is owned by the forker, not the source owner.
        mockMvc.perform(
                        get("/decks/{deckId}", forkResponse.deckId())
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectForkingADeckThatIsVisibleButHasNeverBeenPublished() throws Exception {
        String owner = "auth0|fork-unpublished-owner";
        String forker = "auth0|fork-unpublished-caller";
        long deckId = createDeck(owner, "Never Pinned");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/shared/decks/{slug}/fork", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectForkingAPrivateDeckEvenWithALingeringSlugFromAPastPublish() throws Exception {
        String owner = "auth0|fork-private-owner";
        String forker = "auth0|fork-private-caller";
        long deckId = createDeck(owner, "Goes Private");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);
        pin(owner, deckId);
        // Revert to PRIVATE: the slug lingers (stable), but must stop resolving for fork too.
        publish(owner, deckId, DeckVisibility.PRIVATE);

        mockMvc.perform(
                        post("/shared/decks/{slug}/fork", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenForkingAnUnknownSlug() throws Exception {
        String forker = "auth0|fork-unknown-caller";

        mockMvc.perform(
                        post("/shared/decks/{slug}/fork", "does-not-exist")
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousForking() throws Exception {
        String owner = "auth0|fork-anon-owner";
        long deckId = createDeck(owner, "Owned Deck");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);
        pin(owner, deckId);

        mockMvc.perform(post("/shared/decks/{slug}/fork", slug))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSurviveSourceDeckDeletionAfterFork() throws Exception {
        String owner = "auth0|fork-survive-delete-owner";
        String forker = "auth0|fork-survive-delete-caller";
        long sourceDeckId = createDeck(owner, "Deleted Later");
        String slug = publish(owner, sourceDeckId, DeckVisibility.PUBLIC);
        pin(owner, sourceDeckId);
        long forkedDeckId = fork(forker, slug).deckId();

        mockMvc.perform(
                        delete("/decks/{deckId}", sourceDeckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNoContent());

        Deck forked = deckRepository.findById(forkedDeckId).orElseThrow();
        assertThat(forked.getSourceDeckId()).isEqualTo(sourceDeckId);
        assertThat(forked.getSourceRevisionNumber()).isEqualTo(1);
        mockMvc.perform(
                        get("/decks/{deckId}", forkedDeckId)
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSurviveSourceDeckPrivacyChangeAfterFork() throws Exception {
        String owner = "auth0|fork-survive-privacy-owner";
        String forker = "auth0|fork-survive-privacy-caller";
        long sourceDeckId = createDeck(owner, "Goes Private Later");
        String slug = publish(owner, sourceDeckId, DeckVisibility.PUBLIC);
        pin(owner, sourceDeckId);
        long forkedDeckId = fork(forker, slug).deckId();

        publish(owner, sourceDeckId, DeckVisibility.PRIVATE);

        Deck forked = deckRepository.findById(forkedDeckId).orElseThrow();
        assertThat(forked.getSourceDeckId()).isEqualTo(sourceDeckId);
        mockMvc.perform(
                        get("/decks/{deckId}", forkedDeckId)
                                .with(jwt().jwt(jwt -> jwt.subject(forker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goes Private Later"));
    }

    private long createDeck(String subject, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"%s\",\"formatCode\":\"COMMANDER\"}"
                                                        .formatted(name)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String publish(String subject, long deckId, DeckVisibility visibility)
            throws Exception {
        MvcResult result =
                mockMvc.perform(
                                patch("/decks/{deckId}/publishing", deckId)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"visibility\":\"%s\"}".formatted(visibility)))
                        .andExpect(status().isOk())
                        .andReturn();
        Matcher matcher = SHARE_SLUG_PATTERN.matcher(result.getResponse().getContentAsString());
        return matcher.find() ? matcher.group(1) : null;
    }

    private void pin(String subject, long deckId) throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/publish", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
    }

    private void rename(String subject, long deckId, String name) throws Exception {
        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isOk());
    }

    private DeckForkResponse fork(String subject, String slug) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/shared/decks/{slug}/fork", slug)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject))))
                        .andExpect(status().isCreated())
                        .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), DeckForkResponse.class);
    }

    private void addCard(String subject, long deckId, long printingId) throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/cards", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"cardPrintingId\":%d,\"quantity\":1}"
                                                .formatted(printingId)))
                .andExpect(status().isCreated());
    }

    private long createPrinting(String identifier) {
        Card card = cardRepository.save(new Card("oracle-" + identifier, "Card " + identifier));
        MagicSet set =
                magicSetRepository.save(new MagicSet("set-" + identifier, identifier, "Deck Set"));
        return printingRepository
                .save(new CardPrinting(card, set, "printing-" + identifier))
                .getId();
    }
}
