package com.deckassemble.collections.domain.physical;

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

@Entity
@Table(name = "physical_card_allocations")
@EntityListeners(AuditingEntityListener.class)
public class PhysicalCardAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "deck_card_id", nullable = false)
    private Long deckCardId;

    @Column(name = "collection_card_id", nullable = false)
    private Long collectionCardId;

    @Column(nullable = false)
    private int quantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    protected PhysicalCardAllocation() {}

    public PhysicalCardAllocation(
            Long deckId, Long deckCardId, Long collectionCardId, int quantity) {
        this.deckId = deckId;
        this.deckCardId = deckCardId;
        this.collectionCardId = collectionCardId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getDeckCardId() {
        return deckCardId;
    }

    public Long getCollectionCardId() {
        return collectionCardId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
