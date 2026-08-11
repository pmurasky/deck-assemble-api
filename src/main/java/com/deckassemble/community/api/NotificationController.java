package com.deckassemble.community.api;

import com.deckassemble.community.application.NotificationService;
import com.deckassemble.shared.security.CurrentProfile;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentProfile currentProfile;

    public NotificationController(
            NotificationService notificationService, CurrentProfile currentProfile) {
        this.notificationService = notificationService;
        this.currentProfile = currentProfile;
    }

    @GetMapping
    public NotificationInboxResponse list() {
        NotificationService.NotificationInbox inbox =
                notificationService.list(currentProfile.requireProfile());
        return new NotificationInboxResponse(
                inbox.notifications().stream().map(NotificationResponse::from).toList(),
                inbox.unreadCount());
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(currentProfile.requireProfile(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(currentProfile.requireProfile());
        return ResponseEntity.noContent().build();
    }
}
