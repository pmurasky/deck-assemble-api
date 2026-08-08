package com.deckassemble.decks.domain.history;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * An immutable, append-only snapshot of a deck's canonical state at one point in time. Revisions
 * are never updated or deleted; restoring an earlier revision creates a new one on top.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_revisions")
public class DeckRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ponytail: plain FK, not @ManyToOne Deck — consistent with the rest of this module (see
    // Deck's ponytail comment).
    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "base_revision_number")
    private Integer baseRevisionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private DeckChangeType changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    protected DeckRevision() {}

    public DeckRevision(
            Long deckId,
            Long profileId,
            int revisionNumber,
            @Nullable Integer baseRevisionNumber,
            Content content) {
        this.deckId = deckId;
        this.profileId = profileId;
        this.revisionNumber = revisionNumber;
        this.baseRevisionNumber = baseRevisionNumber;
        this.changeType = content.changeType();
        this.metadata = content.metadata();
        this.snapshot = content.snapshot();
    }

    /** What this revision captures: why it exists, any extra context, and the deck snapshot. */
    public record Content(DeckChangeType changeType, @Nullable String metadata, String snapshot) {}

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public @Nullable Integer getBaseRevisionNumber() {
        return baseRevisionNumber;
    }

    public DeckChangeType getChangeType() {
        return changeType;
    }

    public @Nullable String getMetadata() {
        return metadata;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable String getCreatedBy() {
        return createdBy;
    }
}
