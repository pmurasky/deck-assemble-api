package com.deckassemble.community.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class CommunityCommentControllerIntegrationTest extends AbstractIntegrationTest {

    private static final Pattern SHARE_SLUG_PATTERN = Pattern.compile("\"shareSlug\":\"([^\"]+)\"");
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\":\"([^\"]+)\"");

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldCreateAndListACommentOnAPublishedPublicDeck() throws Exception {
        String owner = "auth0|comment-happy-owner";
        String commenter = "auth0|comment-happy-commenter";
        long deckId = createDeck(owner, "Commentable Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Great deck!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Great deck!"));

        // Listing is anonymous-reachable, same as the shared deck view itself.
        mockMvc.perform(get("/shared/decks/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].body").value("Great deck!"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldRejectAnonymousCommentCreation() throws Exception {
        String owner = "auth0|comment-anon-owner";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Hi\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotDiscloseAPrivateDeckWhenAttemptingToCommentOrList() throws Exception {
        String owner = "auth0|comment-private-owner";
        String stranger = "auth0|comment-private-stranger";
        long deckId = createDeck(owner, "Private Deck");
        // Publish then revert to private: slug stays assigned but must stop resolving.
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        patchVisibility(owner, deckId, DeckVisibility.PRIVATE);

        mockMvc.perform(get("/shared/decks/{slug}/comments", slug))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Hi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404ForAnUnknownSlugRatherThanLeakingItsAbsence() throws Exception {
        mockMvc.perform(get("/shared/decks/{slug}/comments", "does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectCommentingOnAVisibleDeckThatHasNeverBeenPublished() throws Exception {
        String owner = "auth0|comment-unpublished-owner";
        String commenter = "auth0|comment-unpublished-commenter";
        long deckId = createDeck(owner, "Never Pinned");
        String slug = publish(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Hi\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectCommentingWhenTheOwnerDisabledComments() throws Exception {
        String owner = "auth0|comment-disabled-owner";
        String commenter = "auth0|comment-disabled-commenter";
        long deckId = createDeck(owner, "Comments Off Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        patch("/decks/{deckId}/comments-enabled", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsEnabled").value(false));

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Hi\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectNonOwnerTogglingCommentsEnabled() throws Exception {
        String owner = "auth0|comment-toggle-owner";
        String stranger = "auth0|comment-toggle-stranger";
        long deckId = createDeck(owner, "Deck");

        mockMvc.perform(
                        patch("/decks/{deckId}/comments-enabled", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":false}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectACommentBodyExceedingTheLengthLimit() throws Exception {
        String owner = "auth0|comment-toolong-owner";
        String commenter = "auth0|comment-toolong-commenter";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        String tooLong = "a".repeat(2001);

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectABlankCommentBody() throws Exception {
        String owner = "auth0|comment-blank-owner";
        String commenter = "auth0|comment-blank-commenter";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCommentsPastTheRateLimit() throws Exception {
        String owner = "auth0|comment-ratelimit-owner";
        String commenter = "auth0|comment-ratelimit-commenter";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/shared/decks/{slug}/comments", slug)
                                    .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"body\":\"Comment " + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(
                        post("/shared/decks/{slug}/comments", slug)
                                .with(jwt().jwt(jwt -> jwt.subject(commenter)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"One too many\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldLetTheAuthorEditTheirOwnCommentButNotAStranger() throws Exception {
        String owner = "auth0|comment-edit-owner";
        String author = "auth0|comment-edit-author";
        String stranger = "auth0|comment-edit-stranger";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        String commentId = createComment(author, slug, "Original");

        mockMvc.perform(
                        patch("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Hijacked\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        patch("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(author)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Edited\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Edited"));
    }

    @Test
    void shouldLetTheAuthorSoftDeleteTheirOwnCommentAndHideItFromListing() throws Exception {
        String owner = "auth0|comment-delete-owner";
        String author = "auth0|comment-delete-author";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        String commentId = createComment(author, slug, "Delete me");

        mockMvc.perform(
                        delete("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(author))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/shared/decks/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

        // Deleted comments 404 on further edit attempts, same as a genuinely unknown comment.
        mockMvc.perform(
                        patch("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(author)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"body\":\"Too late\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldLetTheDeckOwnerModerateAndDeleteSomeoneElsesComment() throws Exception {
        String owner = "auth0|comment-moderate-owner";
        String author = "auth0|comment-moderate-author";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        String commentId = createComment(author, slug, "Needs moderation");

        mockMvc.perform(
                        delete("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/shared/decks/{slug}/comments", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectAStrangerDeletingSomeoneElsesComment() throws Exception {
        String owner = "auth0|comment-delete-forbid-owner";
        String author = "auth0|comment-delete-forbid-author";
        String stranger = "auth0|comment-delete-forbid-stranger";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);
        String commentId = createComment(author, slug, "Mine");

        mockMvc.perform(
                        delete("/shared/decks/{slug}/comments/{id}", slug, commentId)
                                .with(jwt().jwt(jwt -> jwt.subject(stranger))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn404WhenDeletingAnUnknownComment() throws Exception {
        String owner = "auth0|comment-delete-unknown-owner";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        mockMvc.perform(
                        delete("/shared/decks/{slug}/comments/{id}", slug, UUID.randomUUID())
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldPaginateCommentsMostRecentFirst() throws Exception {
        String owner = "auth0|comment-page-owner";
        String commenter = "auth0|comment-page-commenter";
        long deckId = createDeck(owner, "Deck");
        String slug = publishAndPin(owner, deckId, DeckVisibility.PUBLIC);

        for (int i = 0; i < 3; i++) {
            createComment(commenter, slug, "Comment " + i);
        }

        mockMvc.perform(
                        get("/shared/decks/{slug}/comments", slug)
                                .param("page", "0")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].body").value("Comment 2"))
                .andExpect(jsonPath("$.content[1].body").value("Comment 1"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(
                        get("/shared/decks/{slug}/comments", slug)
                                .param("page", "1")
                                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].body").value("Comment 0"));
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

    private void patchVisibility(String subject, long deckId, DeckVisibility visibility)
            throws Exception {
        publish(subject, deckId, visibility);
    }

    private String publishAndPin(String subject, long deckId, DeckVisibility visibility)
            throws Exception {
        String slug = publish(subject, deckId, visibility);
        mockMvc.perform(
                        post("/decks/{deckId}/publish", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
        return slug;
    }

    private String createComment(String subject, String slug, String body) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/shared/decks/{slug}/comments", slug)
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"body\":\"%s\"}".formatted(body)))
                        .andExpect(status().isCreated())
                        .andReturn();
        Matcher matcher = ID_PATTERN.matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
