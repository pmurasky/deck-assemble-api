package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class DeckTagControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckTagAssignmentRepository assignmentRepository;

    @Test
    void shouldRejectCaseInsensitiveDuplicateTagName() throws Exception {
        String subject = "auth0|tag-crud";
        createTag(subject, "Combo");

        mockMvc.perform(
                        post("/deck-tags")
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"combo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldHideTagsCreatedByOtherProfiles() throws Exception {
        createTag("auth0|tag-owner", "Private Tag");

        mockMvc.perform(get("/deck-tags").with(jwt().jwt(jwt -> jwt.subject("auth0|tag-other"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldAssignManyTagsToOneDeckAndBulkReplace() throws Exception {
        String subject = "auth0|tag-assign";
        long deckId = createDeck(subject, "Tag Deck");
        long tagA = createTag(subject, "Combo");
        long tagB = createTag(subject, "Ramp");
        long tagC = createTag(subject, "Budget");

        assignTags(subject, deckId, tagA, tagB).andExpect(status().isNoContent());
        assertThat(assignmentRepository.findByDeckId(deckId)).hasSize(2);

        assignTags(subject, deckId, tagB, tagC).andExpect(status().isNoContent());
        assertThat(assignmentRepository.findByDeckId(deckId)).hasSize(2);
        assertThat(assignmentRepository.findByDeckId(deckId))
                .extracting("tagId")
                .containsExactlyInAnyOrder(tagB, tagC);
    }

    @Test
    void shouldRejectAssigningTagNotOwnedByProfile() throws Exception {
        long deckId = createDeck("auth0|tag-foreign-deck", "Foreign Deck");
        long tagId = createTag("auth0|tag-foreign-owner", "Someone Else's Tag");

        assignTags("auth0|tag-foreign-deck", deckId, tagId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_TAG_NOT_FOUND"));
    }

    @Test
    void shouldDeleteTagRemovingAssignmentButRetainingDeck() throws Exception {
        String subject = "auth0|tag-delete";
        long deckId = createDeck(subject, "Delete Deck");
        long tagId = createTag(subject, "Doomed Tag");
        assignTags(subject, deckId, tagId).andExpect(status().isNoContent());

        mockMvc.perform(
                        delete("/deck-tags/{tagId}", tagId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());

        assertThat(assignmentRepository.findByDeckId(deckId)).isEmpty();
        assertThat(deckRepository.findById(deckId)).isPresent();
    }

    private ResultActions assignTags(String subject, long deckId, long... tagIds) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (long tagId : tagIds) {
            if (ids.length() > 0) {
                ids.append(',');
            }
            ids.append(tagId);
        }
        return mockMvc.perform(
                put("/decks/{deckId}/tags", deckId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagIds\":[" + ids + "]}"));
    }

    private long createTag(String subject, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/deck-tags")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"name\":\"" + name + "\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andReturn();
        return idFrom(result);
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
        return idFrom(result);
    }

    private long idFrom(MvcResult result) {
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
