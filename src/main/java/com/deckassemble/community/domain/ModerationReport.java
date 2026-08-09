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
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A flag raised against a piece of community content or a profile, awaiting moderator review.
 * Reports are a retained audit trail: reporter_id is nullable (ON DELETE SET NULL) and the reported
 * resource is referenced generically, so a report survives deletion of either the reporter's
 * account or the resource it targets.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "moderation_reports")
public class ModerationReport {

    /** Moderation lifecycle state. */
    public enum Status {
        OPEN,
        RESOLVED,
        DISMISSED
    }

    /** What category of content this report targets. */
    public enum ResourceType {
        DECK,
        COMMENT,
        PROFILE
    }

    /** Why the resource was reported. */
    public enum Reason {
        SPAM,
        HARASSMENT,
        COPYRIGHT,
        INAPPROPRIATE_CONTENT,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_id")
    private @Nullable Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false, length = 36)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private Reason reason;

    @Column(name = "details", columnDefinition = "text")
    private @Nullable String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    protected ModerationReport() {}

    public ModerationReport(
            @Nullable Long reporterId,
            ResourceType resourceType,
            String resourceId,
            Reason reason,
            @Nullable String details) {
        this.reporterId = reporterId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.reason = reason;
        this.details = details;
    }

    public void resolve() {
        this.status = Status.RESOLVED;
    }

    public void dismiss() {
        this.status = Status.DISMISSED;
    }

    public UUID getId() {
        return id;
    }

    public @Nullable Long getReporterId() {
        return reporterId;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Reason getReason() {
        return reason;
    }

    public @Nullable String getDetails() {
        return details;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
