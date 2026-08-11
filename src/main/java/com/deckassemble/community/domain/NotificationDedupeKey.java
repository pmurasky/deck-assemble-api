package com.deckassemble.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_dedupe_keys")
public class NotificationDedupeKey {

    @Id
    @Column(name = "dedupe_key", nullable = false, length = 200)
    private String id;

    @Column(name = "last_notification_at", nullable = false)
    private Instant lastNotificationAt;

    protected NotificationDedupeKey() {}

    public NotificationDedupeKey(String id, Instant lastNotificationAt) {
        this.id = id;
        this.lastNotificationAt = lastNotificationAt;
    }

    public boolean isInsideWindow(Instant since) {
        return !lastNotificationAt.isBefore(since);
    }

    public void markNotified(Instant when) {
        lastNotificationAt = when;
    }

    public String getId() {
        return id;
    }

    public Instant getLastNotificationAt() {
        return lastNotificationAt;
    }
}
