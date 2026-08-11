package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.community.domain.Notification;
import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.community.domain.NotificationDedupeKey;
import com.deckassemble.community.domain.NotificationDedupeKeyRepository;
import com.deckassemble.community.domain.NotificationRepository;
import com.deckassemble.users.domain.Profile;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationDedupeKeyRepository dedupeKeyRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service =
                new NotificationService(
                        notificationRepository, dedupeKeyRepository, Duration.ofMinutes(5));
    }

    @Test
    void shouldCreateNotificationWhenNoUnreadDuplicateExists() {
        when(dedupeKeyRepository.insertIfAbsent(eq("20|NEW_FOLLOWER|10|10"), any())).thenReturn(1);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new CommunityEvent(Reason.NEW_FOLLOWER, 10L, 20L, "10"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getRecipientId()).isEqualTo(20L);
        assertThat(captor.getValue().getActorId()).isEqualTo(10L);
        assertThat(captor.getValue().getReason()).isEqualTo(Reason.NEW_FOLLOWER);
        assertThat(captor.getValue().getResourceId()).isEqualTo("10");
    }

    @Test
    void shouldSuppressSelfActionsAndUnreadDuplicates() {
        service.create(new CommunityEvent(Reason.DECK_FAVORITED, 10L, 10L, "7"));
        verifyNoInteractions(dedupeKeyRepository);

        when(dedupeKeyRepository.insertIfAbsent(eq("20|DECK_FAVORITED|7|10"), any())).thenReturn(0);
        when(dedupeKeyRepository.findLockedById("20|DECK_FAVORITED|7|10"))
                .thenReturn(
                        Optional.of(
                                new NotificationDedupeKey(
                                        "20|DECK_FAVORITED|7|10", Instant.now())));
        service.create(new CommunityEvent(Reason.DECK_FAVORITED, 10L, 20L, "7"));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldCreateExactlyOneNotificationWhenDuplicateKeyAlreadyExistsInsideWindow() {
        String key = "20|DECK_FAVORITED|7|10";
        when(dedupeKeyRepository.insertIfAbsent(eq(key), any())).thenReturn(1, 0);
        when(dedupeKeyRepository.findLockedById(key))
                .thenReturn(Optional.of(new NotificationDedupeKey(key, Instant.now())));

        CommunityEvent event = new CommunityEvent(Reason.DECK_FAVORITED, 10L, 20L, "7");
        service.create(event);
        service.create(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void shouldListOwnNotificationsWithUnreadCount() {
        Profile profile = profile(20L);
        Notification read = notification(20L, 10L, Reason.NEW_COMMENT, "7", Instant.now());
        Notification unread = notification(20L, 11L, Reason.DECK_FORKED, "7", null);
        when(notificationRepository.findByRecipientIdOrderByReadAtAscCreatedAtDesc(20L))
                .thenReturn(List.of(unread, read));
        when(notificationRepository.countByRecipientIdAndReadAtIsNull(20L)).thenReturn(1L);

        NotificationService.NotificationInbox inbox = service.list(profile);

        assertThat(inbox.unreadCount()).isEqualTo(1L);
        assertThat(inbox.notifications()).containsExactly(unread, read);
    }

    @Test
    void shouldMarkReadOnlyForOwnerAndRemainIdempotent() {
        Profile owner = profile(20L);
        UUID id = UUID.randomUUID();
        Notification notification = notification(20L, 10L, Reason.NEW_COMMENT, "7", null);
        when(notificationRepository.findByIdAndRecipientId(id, 20L))
                .thenReturn(Optional.of(notification));

        service.markRead(owner, id);
        service.markRead(owner, id);

        assertThat(notification.isUnread()).isFalse();
        verify(notificationRepository, times(2)).findByIdAndRecipientId(id, 20L);
    }

    @Test
    void shouldHideAnotherRecipientsNotification() {
        Profile owner = profile(20L);
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndRecipientId(id, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(owner, id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private static Profile profile(long id) {
        Profile profile = new Profile("auth|" + id, "Profile " + id);
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }

    private static Notification notification(
            long recipientId, Long actorId, Reason reason, String resourceId, Instant readAt) {
        Notification notification = new Notification(recipientId, actorId, reason, resourceId);
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(
                notification, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        if (readAt != null) {
            notification.markRead(readAt);
        }
        return notification;
    }
}
