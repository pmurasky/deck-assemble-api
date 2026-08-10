package com.deckassemble.decks.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.history.DeckRevision;
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

class DeckFolderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private DeckRepository deckRepository;
    @Autowired private DeckRevisionRepository deckRevisionRepository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void shouldCreateRenameAndRejectCaseInsensitiveDuplicateFolderName() throws Exception {
        String subject = "auth0|folder-crud";
        createFolder(subject, "Commander");

        mockMvc.perform(
                        post("/deck-folders")
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"commander\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldHideFoldersCreatedByOtherProfiles() throws Exception {
        createFolder("auth0|folder-owner", "Private Folder");

        mockMvc.perform(
                        get("/deck-folders")
                                .with(jwt().jwt(jwt -> jwt.subject("auth0|folder-other"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldAssignOneFolderPerDeckReplacingPriorFolder() throws Exception {
        String subject = "auth0|folder-assign";
        long deckId = createDeck(subject, "Assign Deck");
        long folderA = createFolder(subject, "Folder A");
        long folderB = createFolder(subject, "Folder B");

        assignFolder(subject, deckId, folderA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").isNumber());
        assertThat(deckRepository.findById(deckId).orElseThrow().getFolderId()).isEqualTo(folderA);

        assignFolder(subject, deckId, folderB).andExpect(status().isOk());
        assertThat(deckRepository.findById(deckId).orElseThrow().getFolderId()).isEqualTo(folderB);
    }

    @Test
    void shouldClearFolderWhenAssignedNull() throws Exception {
        String subject = "auth0|folder-clear";
        long deckId = createDeck(subject, "Clear Deck");
        long folderId = createFolder(subject, "Folder");
        assignFolder(subject, deckId, folderId).andExpect(status().isOk());

        mockMvc.perform(
                        put("/decks/{deckId}/folder", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"folderId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").isNumber());

        assertThat(deckRepository.findById(deckId).orElseThrow().getFolderId()).isNull();
    }

    @Test
    void shouldRejectAssigningFolderNotOwnedByProfile() throws Exception {
        long deckId = createDeck("auth0|folder-foreign-deck", "Foreign Deck");
        long folderId = createFolder("auth0|folder-foreign-owner", "Someone Else's Folder");

        assignFolder("auth0|folder-foreign-deck", deckId, folderId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DECK_FOLDER_NOT_FOUND"));
    }

    @Test
    void shouldDeleteFolderClearingReferenceButRetainingDeck() throws Exception {
        String subject = "auth0|folder-delete";
        long deckId = createDeck(subject, "Delete Deck");
        long folderId = createFolder(subject, "Folder To Delete");
        assignFolder(subject, deckId, folderId).andExpect(status().isOk());

        mockMvc.perform(
                        delete("/deck-folders/{folderId}", folderId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());

        assertThat(deckRepository.findById(deckId).orElseThrow().getFolderId()).isNull();
        mockMvc.perform(get("/decks/{deckId}", deckId).with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRecordFolderChangedRevisionReflectingClearedFolderIdAfterFolderDeletion()
            throws Exception {
        String subject = "auth0|folder-delete-history";
        long deckId = createDeck(subject, "History Deck");
        long folderId = createFolder(subject, "Folder To Delete");
        assignFolder(subject, deckId, folderId).andExpect(status().isOk());

        mockMvc.perform(
                        delete("/deck-folders/{folderId}", folderId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isNoContent());

        DeckRevision latest =
                deckRevisionRepository
                        .findFirstByDeckIdOrderByRevisionNumberDesc(deckId)
                        .orElseThrow();
        assertThat(latest.getChangeType()).isEqualTo(DeckChangeType.FOLDER_CHANGED);
        DeckSnapshot snapshot = objectMapper.readValue(latest.getSnapshot(), DeckSnapshot.class);
        assertThat(snapshot.folderId()).isNull();
    }

    @Test
    void shouldRejectRenamingToDuplicateName() throws Exception {
        String subject = "auth0|folder-rename-dup";
        createFolder(subject, "Aggro");
        long otherId = createFolder(subject, "Control");

        mockMvc.perform(
                        patch("/deck-folders/{folderId}", otherId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Aggro\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRenameFolderToNewAvailableName() throws Exception {
        String subject = "auth0|folder-rename-ok";
        long folderId = createFolder(subject, "Aggro");

        mockMvc.perform(
                        patch("/deck-folders/{folderId}", folderId)
                                .with(jwt().jwt(jwt -> jwt.subject(subject)))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Midrange\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Midrange"));

        mockMvc.perform(get("/deck-folders").with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(jsonPath("$[0].name").value("Midrange"));
    }

    private org.springframework.test.web.servlet.ResultActions assignFolder(
            String subject, long deckId, long folderId) throws Exception {
        return mockMvc.perform(
                put("/decks/{deckId}/folder", deckId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folderId\":" + folderId + "}"));
    }

    private long createFolder(String subject, String name) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/deck-folders")
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
