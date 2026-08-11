package com.deckassemble.community.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.community.domain.Notification;
import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.community.domain.NotificationRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class NotificationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ProfileRepository profileRepository;

    @Test
    void shouldListOnlyCurrentProfileNotificationsWithUnreadCountAndMinimalPayload()
            throws Exception {
        Profile recipient = profile("auth|notifications-owner");
        Profile actor = profile("auth|notifications-actor");
        Profile other = profile("auth|notifications-other");
        notificationRepository.saveAndFlush(
                new Notification(recipient.getId(), actor.getId(), Reason.NEW_COMMENT, "deck-7"));
        notificationRepository.saveAndFlush(
                new Notification(other.getId(), actor.getId(), Reason.NEW_FOLLOWER, "profile-9"));

        mockMvc.perform(
                        get("/notifications")
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        recipient
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.notifications.length()").value(1))
                .andExpect(jsonPath("$.notifications[0].reason").value("NEW_COMMENT"))
                .andExpect(jsonPath("$.notifications[0].resourceId").value("deck-7"))
                .andExpect(jsonPath("$.notifications[0].body").doesNotExist());
    }

    @Test
    void shouldMarkOneAndAllNotificationsReadIdempotentlyForOwnerOnly() throws Exception {
        Profile owner = profile("auth|notifications-read-owner");
        Profile other = profile("auth|notifications-read-other");
        Notification own =
                notificationRepository.saveAndFlush(
                        new Notification(owner.getId(), other.getId(), Reason.DECK_FAVORITED, "7"));
        Notification hidden =
                notificationRepository.saveAndFlush(
                        new Notification(other.getId(), owner.getId(), Reason.NEW_FOLLOWER, "20"));

        mockMvc.perform(
                        post("/notifications/{id}/read", hidden.getId())
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        owner
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/notifications/{id}/read", own.getId())
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        owner
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        post("/notifications/{id}/read", own.getId())
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        owner
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isNoContent());
        mockMvc.perform(
                        post("/notifications/read-all")
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        owner
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/notifications")
                                .with(
                                        jwt().jwt(
                                                        jwt ->
                                                                jwt.subject(
                                                                        owner
                                                                                .getAuthProviderSubject()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    private Profile profile(String subject) {
        return profileRepository.saveAndFlush(new Profile(subject, subject));
    }
}
