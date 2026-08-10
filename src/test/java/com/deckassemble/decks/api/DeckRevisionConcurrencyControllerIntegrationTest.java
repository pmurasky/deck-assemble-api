package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers the optimistic-concurrency contract on collaborative deck edits:
 * expectedRevision/revisionNumber round-tripping, the 409 conflict on a stale base, a real
 * two-thread race proving the row lock serializes concurrent editors, and that an EDITOR
 * collaborator (not just the owner) can mutate while a VIEWER cannot. Split out of
 * DeckControllerIntegrationTest to stay under that class's PMD cyclomatic-complexity budget.
 */
class DeckRevisionConcurrencyControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;

    @Test
    void shouldReturnAndAdvanceRevisionNumberOnUpdate() throws Exception {
        String subject = "auth0|deck-rev-basic";
        long deckId = createDeck(subject);

        // createDeck records the CREATED revision, so the deck starts at revision 1.
        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Renamed\",\"expectedRevision\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(2));
    }

    @Test
    void shouldRejectUpdateWithStaleExpectedRevisionAsConflict() throws Exception {
        String subject = "auth0|deck-rev-stale";
        long deckId = createDeck(subject);

        // Deck is at revision 1; an edit claiming to be based on revision 0 is stale.
        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Renamed\",\"expectedRevision\":0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DECK_REVISION_CONFLICT"))
                .andExpect(jsonPath("$.currentRevision").value(1));
    }

    @Test
    void shouldLetExactlyOneOfTwoConcurrentEditsFromSameBaseRevisionWin() throws Exception {
        String subject = "auth0|deck-rev-race";
        long deckId = createDeck(subject);
        // Both editors read revision 1 and submit an edit based on it; the row lock must let only
        // one commit a new revision, forcing the other to lose the optimistic check with a 409.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<Integer> edit =
                () -> {
                    ready.countDown();
                    go.await();
                    return mockMvc.perform(
                                    patch("/decks/{deckId}", deckId)
                                            .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content("{\"name\":\"Racer\",\"expectedRevision\":1}"))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                };

        int first;
        int second;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> a = executor.submit(edit);
            Future<Integer> b = executor.submit(edit);
            ready.await();
            go.countDown();
            first = a.get(10, TimeUnit.SECONDS);
            second = b.get(10, TimeUnit.SECONDS);
        }

        AtomicInteger okCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();
        for (int statusCode : new int[] {first, second}) {
            if (statusCode == 200) {
                okCount.incrementAndGet();
            } else if (statusCode == 409) {
                conflictCount.incrementAndGet();
            }
        }
        assertThat(okCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // Exactly one new revision landed on top of the base: the deck is now at revision 2.
        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(2));
    }

    @Test
    void shouldLetAnEditorCollaboratorMutateButNotAViewer() throws Exception {
        String owner = "auth0|deck-collab-owner";
        String editor = "auth0|deck-collab-editor";
        String viewer = "auth0|deck-collab-viewer";
        long deckId = createDeck(owner);
        long editorProfileId = bootstrapProfile(editor);
        long viewerProfileId = bootstrapProfile(viewer);
        invite(owner, deckId, editorProfileId, "EDITOR");
        invite(owner, deckId, viewerProfileId, "VIEWER");

        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(editor)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"By Editor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("By Editor"));

        mockMvc.perform(
                        patch("/decks/{deckId}", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(viewer)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"By Viewer\"}"))
                .andExpect(status().isNotFound());
    }

    private long bootstrapProfile(String subject) throws Exception {
        mockMvc.perform(get("/decks").with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
        return profileRepository.findByAuthProviderSubject(subject).orElseThrow().getId();
    }

    private void invite(String owner, long deckId, long profileId, String role) throws Exception {
        mockMvc.perform(
                        post("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"profileId\":%d,\"role\":\"%s\"}"
                                                .formatted(profileId, role)))
                .andExpect(status().isOk());
    }

    private long createDeck(String subject) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/decks")
                                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"Deck\",\"formatCode\":\"COMMANDER\"}"))
                        .andExpect(status().isCreated())
                        .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
