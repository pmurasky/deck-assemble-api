package com.deckassemble.community.api;

import com.deckassemble.community.domain.Notification;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record NotificationResponse(
        UUID id,
        @Nullable Long actorId,
        Notification.Reason reason,
        String resourceId,
        boolean unread,
        Instant createdAt) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getActorId(),
                notification.getReason(),
                notification.getResourceId(),
                notification.isUnread(),
                notification.getCreatedAt());
    }
}
