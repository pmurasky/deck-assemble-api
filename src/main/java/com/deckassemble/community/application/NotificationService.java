package com.deckassemble.community.application;

import com.deckassemble.community.domain.Notification;
import com.deckassemble.community.domain.NotificationRepository;
import com.deckassemble.users.domain.Profile;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final Duration dedupeWindow;

    public NotificationService(
            NotificationRepository notificationRepository,
            @Value("${community.notifications.dedupe-window:PT5M}") Duration dedupeWindow) {
        this.notificationRepository = notificationRepository;
        this.dedupeWindow = dedupeWindow;
    }

    public void create(CommunityEvent event) {
        if (Objects.equals(event.actorId(), event.recipientId()) || hasDuplicate(event)) {
            return;
        }
        notificationRepository.save(
                new Notification(
                        event.recipientId(), event.actorId(), event.reason(), event.resourceId()));
    }

    @Transactional(readOnly = true)
    public NotificationInbox list(Profile profile) {
        return new NotificationInbox(
                notificationRepository.findByRecipientIdOrderByReadAtAscCreatedAtDesc(
                        profile.getId()),
                notificationRepository.countByRecipientIdAndReadAtIsNull(profile.getId()));
    }

    public void markRead(Profile profile, UUID id) {
        Notification notification = owned(profile, id);
        if (notification.isUnread()) {
            notification.markRead(Instant.now());
        }
    }

    public void markAllRead(Profile profile) {
        Instant now = Instant.now();
        notificationRepository
                .findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(profile.getId())
                .forEach(notification -> notification.markRead(now));
    }

    private boolean hasDuplicate(CommunityEvent event) {
        return notificationRepository.existsUnreadDuplicate(
                event.recipientId(),
                event.reason(),
                event.resourceId(),
                event.actorId(),
                Instant.now().minus(dedupeWindow));
    }

    private Notification owned(Profile profile, UUID id) {
        return notificationRepository
                .findByIdAndRecipientId(id, profile.getId())
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Notification not found"));
    }

    public record NotificationInbox(List<Notification> notifications, long unreadCount) {}
}
