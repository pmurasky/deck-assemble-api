package com.deckassemble.decks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "deck_import_previews")
public class DeckImportPreview {

    public enum Status {
        PENDING,
        COMMITTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "source_sha256", nullable = false, length = 64)
    private String sourceSha256;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_rows", nullable = false, columnDefinition = "jsonb")
    private String canonicalRows;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "idempotency_key")
    private @Nullable String idempotencyKey;

    @Column(name = "committed_deck_id")
    private @Nullable Long committedDeckId;

    protected DeckImportPreview() {}

    public DeckImportPreview(
            UUID token,
            Long profileId,
            Instant expiresAt,
            String sourceSha256,
            String canonicalRows) {
        this.token = token;
        this.profileId = profileId;
        this.expiresAt = expiresAt;
        this.sourceSha256 = sourceSha256;
        this.canonicalRows = canonicalRows;
    }

    public void markCommitted(String idempotencyKey, Long committedDeckId) {
        this.idempotencyKey = idempotencyKey;
        this.committedDeckId = committedDeckId;
        this.status = Status.COMMITTED;
    }

    public void storeCanonicalRows(String canonicalRows) {
        this.canonicalRows = canonicalRows;
    }

    public Long getId() {
        return id;
    }

    public UUID getToken() {
        return token;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public String getCanonicalRows() {
        return canonicalRows;
    }

    public Status getStatus() {
        return status;
    }

    public @Nullable String getIdempotencyKey() {
        return idempotencyKey;
    }

    public @Nullable Long getCommittedDeckId() {
        return committedDeckId;
    }
}
