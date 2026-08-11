package com.deckassemble.community.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CommunityEventListener {

    private final NotificationService notificationService;

    public CommunityEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CommunityEvent event) {
        notificationService.create(event);
    }
}
