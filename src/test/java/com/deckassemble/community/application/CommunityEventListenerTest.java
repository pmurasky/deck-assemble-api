package com.deckassemble.community.application;

import static org.mockito.Mockito.verify;

import com.deckassemble.community.domain.Notification.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommunityEventListenerTest {

    @Mock private NotificationService notificationService;

    @Test
    void shouldDelegateCommunityEventsToNotificationService() {
        CommunityEvent event = new CommunityEvent(Reason.COLLABORATOR_ADDED, 1L, 2L, "7");
        CommunityEventListener listener = new CommunityEventListener(notificationService);

        listener.handle(event);

        verify(notificationService).create(event);
    }
}
