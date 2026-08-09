package com.deckassemble.decks.api.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
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
                                                new DeckPrimerRequest(title, markdown))))
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
                                                        new DeckPrimerRequest("Danger", markdown))))
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
                                                new DeckPrimerRequest("Title", tooLong))))
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
                                                new DeckPrimerRequest(tooLong, "Body"))))
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
                                                new DeckPrimerRequest("Title", "Body"))))
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
                                                new DeckPrimerRequest("Title", "Body"))))
                .andExpect(status().isUnauthorized());
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
                                                new DeckPrimerRequest("Guide", "# How to play"))))
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
}
