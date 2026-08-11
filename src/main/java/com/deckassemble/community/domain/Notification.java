package com.deckassemble.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A recipient-facing notification. Carries only IDs and a reason code — never a copy of private
 * content — so the referenced resource stays the single source of truth.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "notifications")
public class Notification {

    /** Why this notification was raised. */
    public enum Reason {
        NEW_COMMENT,
        // Reserved for the future reply feature: no reply model/API exists yet.
        COMMENT_REPLY,
        NEW_FOLLOWER,
        DECK_FAVORITED,
        COLLABORATOR_ADDED,
        COLLABORATOR_REMOVED,
        DECK_FORKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    // Nullable and ON DELETE SET NULL: the notification is retained even if the profile that
    // triggered it is later deleted.
    @Column(name = "actor_id")
    private @Nullable Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private Reason reason;

    // Generic pointer (a deck, comment, profile, etc. id) — the reason disambiguates what kind
    // of resource this refers to. No FK: the target may be a UUID- or bigint-keyed entity.
    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Column(name = "read_at")
    private @Nullable Instant readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    protected Notification() {}

    public Notification(
            Long recipientId, @Nullable Long actorId, Reason reason, String resourceId) {
        this.recipientId = recipientId;
        this.actorId = actorId;
        this.reason = reason;
        this.resourceId = resourceId;
    }

    public void markRead(Instant when) {
        this.readAt = when;
    }

    public boolean isUnread() {
        return readAt == null;
    }

    public UUID getId() {
        return id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public @Nullable Long getActorId() {
        return actorId;
    }

    public Reason getReason() {
        return reason;
    }

    public String getResourceId() {
        return resourceId;
    }

    public @Nullable Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
