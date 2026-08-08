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

/** Links a {@link DeckCategory} to one {@code DeckCard} row within the same deck. */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_category_assignments")
public class DeckCategoryAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_category_id", nullable = false)
    private Long deckCategoryId;

    @Column(name = "deck_card_id", nullable = false)
    private Long deckCardId;

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

    protected DeckCategoryAssignment() {}

    public DeckCategoryAssignment(Long deckCategoryId, Long deckCardId) {
        this.deckCategoryId = deckCategoryId;
        this.deckCardId = deckCardId;
    }

    public Long getId() {
        return id;
    }

    public Long getDeckCategoryId() {
        return deckCategoryId;
    }

    public Long getDeckCardId() {
        return deckCardId;
    }
}
