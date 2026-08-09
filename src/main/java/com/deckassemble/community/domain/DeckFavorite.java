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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A profile's favorite of a deck. Immutable and append-only — unfavoriting deletes the row rather
 * than updating it.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_favorites")
public class DeckFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    protected DeckFavorite() {}

    public DeckFavorite(Long profileId, Long deckId) {
        this.profileId = profileId;
        this.deckId = deckId;
    }

    public UUID getId() {
        return id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
