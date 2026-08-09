package com.deckassemble.decks.api.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import com.deckassemble.users.domain.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class DeckCollaborationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private DeckRepository deckRepository;

    @Test
    void shouldAllowOwnerToInviteListAndRevokeACollaborator() throws Exception {
        String owner = "auth0|collab-owner";
        String invitee = "auth0|collab-invitee";
        long deckId = createDeck(owner, "Team Deck");
        long inviteeProfileId = bootstrapProfile(invitee);

        invite(owner, deckId, inviteeProfileId, "EDITOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value(inviteeProfileId))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].profileId").value(inviteeProfileId))
                .andExpect(jsonPath("$[0].role").value("EDITOR"));

        mockMvc.perform(
                        delete(
                                        "/decks/{deckId}/collaborators/{profileId}",
                                        deckId,
                                        inviteeProfileId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldStoreDistinctViewerAndEditorRoles() throws Exception {
        String owner = "auth0|collab-roles-owner";
        String viewer = "auth0|collab-roles-viewer";
        String editor = "auth0|collab-roles-editor";
        long deckId = createDeck(owner, "Roled Deck");
        long viewerProfileId = bootstrapProfile(viewer);
        long editorProfileId = bootstrapProfile(editor);

        invite(owner, deckId, viewerProfileId, "VIEWER").andExpect(status().isOk());
        invite(owner, deckId, editorProfileId, "EDITOR").andExpect(status().isOk());

        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldTreatDuplicateInvitesAsIdempotentAndUpdateTheRoleInstead() throws Exception {
        String owner = "auth0|collab-dup-owner";
        String invitee = "auth0|collab-dup-invitee";
        long deckId = createDeck(owner, "Dup Deck");
        long inviteeProfileId = bootstrapProfile(invitee);

        invite(owner, deckId, inviteeProfileId, "VIEWER").andExpect(status().isOk());
        invite(owner, deckId, inviteeProfileId, "VIEWER").andExpect(status().isOk());
        invite(owner, deckId, inviteeProfileId, "EDITOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));

        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("EDITOR"));
    }

    @Test
    void shouldRejectInvitingTheOwnerAsACollaborator() throws Exception {
        String owner = "auth0|collab-self-owner";
        long deckId = createDeck(owner, "Solo Deck");
        long ownerProfileId =
                profileRepository.findByAuthProviderSubject(owner).orElseThrow().getId();

        invite(owner, deckId, ownerProfileId, "EDITOR").andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvitingAnUnknownProfileId() throws Exception {
        String owner = "auth0|collab-unknown-owner";
        long deckId = createDeck(owner, "Unknown Invitee Deck");

        invite(owner, deckId, 999_999_999L, "VIEWER").andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectACollaboratorManagingOtherCollaboratorsEvenAsEditor() throws Exception {
        String owner = "auth0|collab-guard-owner";
        String editor = "auth0|collab-guard-editor";
        String thirdParty = "auth0|collab-guard-third";
        long deckId = createDeck(owner, "Guarded Deck");
        long editorProfileId = bootstrapProfile(editor);
        long thirdPartyProfileId = bootstrapProfile(thirdParty);
        invite(owner, deckId, editorProfileId, "EDITOR").andExpect(status().isOk());

        // Only the owner may call this API — an EDITOR collaborator gets the same not-found
        // response as a total stranger, never a success.
        invite(editor, deckId, thirdPartyProfileId, "VIEWER").andExpect(status().isNotFound());
        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(editor))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        delete("/decks/{deckId}/collaborators/{profileId}", deckId, editorProfileId)
                                .with(jwt().jwt(jwt -> jwt.subject(editor))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAStrangerManagingCollaborators() throws Exception {
        String owner = "auth0|collab-stranger-owner";
        String stranger = "auth0|collab-stranger";
        long deckId = createDeck(owner, "Stranger Deck");
        long strangerProfileId = bootstrapProfile(stranger);

        invite(stranger, deckId, strangerProfileId, "VIEWER").andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectAnonymousManagingCollaborators() throws Exception {
        String owner = "auth0|collab-anon-owner";
        long deckId = createDeck(owner, "Anon Deck");

        mockMvc.perform(get("/decks/{deckId}/collaborators", deckId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRevokingACollaboratorThatWasNeverInvited() throws Exception {
        String owner = "auth0|collab-revoke-missing-owner";
        String stranger = "auth0|collab-revoke-missing-stranger";
        long deckId = createDeck(owner, "Never Invited Deck");
        long strangerProfileId = bootstrapProfile(stranger);

        mockMvc.perform(
                        delete(
                                        "/decks/{deckId}/collaborators/{profileId}",
                                        deckId,
                                        strangerProfileId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldManageCollaboratorsOnAPrivateDeckByDefault() throws Exception {
        String owner = "auth0|collab-private-owner";
        String invitee = "auth0|collab-private-invitee";
        long deckId = createDeck(owner, "Private By Default Deck");
        long inviteeProfileId = bootstrapProfile(invitee);

        assertThat(deckRepository.findById(deckId).orElseThrow().getVisibility())
                .isEqualTo(DeckVisibility.PRIVATE);

        invite(owner, deckId, inviteeProfileId, "VIEWER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void shouldContinueManagingCollaboratorsAfterTheDeckIsArchived() throws Exception {
        // No restriction elsewhere in the deck module blocks mutation on an ARCHIVED deck (see
        // DeckPublishingControllerIntegrationTest#shouldAllowPublishingAnAlreadyArchivedDeck for
        // the same precedent applied to publishing), so collaborator management follows suit
        // rather than inventing a new archived-deck rule.
        String owner = "auth0|collab-archived-owner";
        String invitee = "auth0|collab-archived-invitee";
        long deckId = createDeck(owner, "Archived Deck");
        long inviteeProfileId = bootstrapProfile(invitee);

        mockMvc.perform(
                        post("/decks/{deckId}/archive", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        invite(owner, deckId, inviteeProfileId, "EDITOR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));
        mockMvc.perform(
                        get("/decks/{deckId}/collaborators", deckId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(
                        delete(
                                        "/decks/{deckId}/collaborators/{profileId}",
                                        deckId,
                                        inviteeProfileId)
                                .with(jwt().jwt(jwt -> jwt.subject(owner))))
                .andExpect(status().isNoContent());
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

    /** Triggers lazy profile creation for a subject that has never authenticated before. */
    private long bootstrapProfile(String subject) throws Exception {
        mockMvc.perform(get("/decks").with(jwt().jwt(jwt -> jwt.subject(subject))))
                .andExpect(status().isOk());
        return profileRepository.findByAuthProviderSubject(subject).orElseThrow().getId();
    }

    private ResultActions invite(String subject, long deckId, long profileId, String role)
            throws Exception {
        return mockMvc.perform(
                post("/decks/{deckId}/collaborators", deckId)
                        .with(jwt().jwt(jwt -> jwt.subject(subject)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"profileId\":%d,\"role\":\"%s\"}".formatted(profileId, role)));
    }
}
