package com.deckassemble.community.application;

import com.deckassemble.community.domain.Notification;

/** Community action that may become a notification after its transaction commits. */
public record CommunityEvent(
        Notification.Reason reason, Long actorId, Long recipientId, String resourceId) {}
