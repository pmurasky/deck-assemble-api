package com.deckassemble.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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

/** A profile's comment on a visible published deck. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_comments")
public class DeckComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ponytail: plain FK, not @ManyToOne — consistent with the rest of the codebase's entities.
    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    // Soft-delete marker: a deleted comment is excluded from listing/pagination entirely (no
    // tombstone rendering) but the row is retained rather than hard-removed, matching moderation
    // reports' retained-audit-trail precedent (see ModerationReport).
    @Column(name = "deleted_at")
    private @Nullable Instant deletedAt;

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

    protected DeckComment() {}

    public DeckComment(Long deckId, Long profileId, String body) {
        this.deckId = deckId;
        this.profileId = profileId;
        this.body = body;
    }

    public void editBody(String body) {
        this.body = body;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public UUID getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getBody() {
        return body;
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

    public @Nullable Instant getDeletedAt() {
        return deletedAt;
    }
}
