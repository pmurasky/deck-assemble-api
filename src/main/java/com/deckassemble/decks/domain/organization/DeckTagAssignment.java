package com.deckassemble.decks.domain.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** Links a {@link DeckTag} to one deck; a deck may carry many tags. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_tag_assignments")
public class DeckTagAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

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

    protected DeckTagAssignment() {}

    public DeckTagAssignment(Long deckId, Long tagId) {
        this.deckId = deckId;
        this.tagId = tagId;
    }

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getTagId() {
        return tagId;
    }
}
