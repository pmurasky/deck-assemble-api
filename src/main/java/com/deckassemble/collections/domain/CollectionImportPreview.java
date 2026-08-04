package com.deckassemble.collections.domain;

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
@Table(name = "collection_import_previews")
public class CollectionImportPreview {

    public enum Status {
        PENDING,
        COMMITTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, unique = true)
    private UUID token;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_rows", nullable = false, columnDefinition = "jsonb")
    private String canonicalRows;

    @Column(name = "source_sha256", nullable = false, length = 64)
    private String sourceSha256;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "committed_collection_id")
    private @Nullable Long committedCollectionId;

    @Column(name = "idempotency_key")
    private @Nullable String idempotencyKey;

    protected CollectionImportPreview() {}

    public CollectionImportPreview(
            UUID token,
            Long profileId,
            Instant expiresAt,
            String sourceSha256,
            String canonicalRows) {
        this.profileId = profileId;
        this.token = token;
        this.canonicalRows = canonicalRows;
        this.sourceSha256 = sourceSha256;
        this.expiresAt = expiresAt;
    }

    public void markCommitted(String idempotencyKey, Long committedCollectionId) {
        this.idempotencyKey = idempotencyKey;
        this.committedCollectionId = committedCollectionId;
        this.status = Status.COMMITTED;
    }

    public Long getId() {
        return id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public UUID getToken() {
        return token;
    }

    public String getCanonicalRows() {
        return canonicalRows;
    }

    public String getSourceSha256() {
        return sourceSha256;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public @Nullable Long getCommittedCollectionId() {
        return committedCollectionId;
    }

    public @Nullable String getIdempotencyKey() {
        return idempotencyKey;
    }
}
