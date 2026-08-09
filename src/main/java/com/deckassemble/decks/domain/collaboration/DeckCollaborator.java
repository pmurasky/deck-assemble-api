package com.deckassemble.decks.domain.collaboration;

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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Grants one profile {@link DeckCollaboratorRole} access to edit or view someone else's deck. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_collaborators")
public class DeckCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ponytail: plain FK, not @ManyToOne Deck/Profile — consistent with the rest of the decks
    // module (see Deck's ponytail comment).
    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private DeckCollaboratorRole role;

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

    protected DeckCollaborator() {}

    public DeckCollaborator(Long deckId, Long profileId, DeckCollaboratorRole role) {
        this.deckId = deckId;
        this.profileId = profileId;
        this.role = role;
    }

    public void changeRole(DeckCollaboratorRole role) {
        this.role = role;
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

    public DeckCollaboratorRole getRole() {
        return role;
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
