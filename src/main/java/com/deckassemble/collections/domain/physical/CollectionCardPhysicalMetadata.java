package com.deckassemble.collections.domain.physical;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "collection_card_physical_metadata")
@EntityListeners(AuditingEntityListener.class)
public class CollectionCardPhysicalMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "collection_card_id", nullable = false)
    private Long collectionCardId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", length = 30)
    private @Nullable CardCondition condition;

    @Column(name = "language", length = 10)
    private @Nullable String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "finish", length = 20)
    private @Nullable PhysicalFinish finish;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private @Nullable BigDecimal purchasePrice;

    @Column(name = "purchase_currency", length = 3)
    private @Nullable String purchaseCurrency;

    @Column(name = "purchase_date")
    private @Nullable LocalDate purchaseDate;

    @Column(name = "notes", length = 2000)
    private @Nullable String notes;

    @Column(name = "storage_location_id")
    private @Nullable UUID storageLocationId;

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

    protected CollectionCardPhysicalMetadata() {}

    public CollectionCardPhysicalMetadata(Long collectionCardId) {
        this.collectionCardId = collectionCardId;
    }

    public Long getCollectionCardId() {
        return collectionCardId;
    }

    public @Nullable CardCondition getCondition() {
        return condition;
    }

    public @Nullable String getLanguage() {
        return language;
    }

    public @Nullable PhysicalFinish getFinish() {
        return finish;
    }

    public @Nullable BigDecimal getPurchasePrice() {
        return purchasePrice;
    }

    public @Nullable String getPurchaseCurrency() {
        return purchaseCurrency;
    }

    public @Nullable LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public @Nullable String getNotes() {
        return notes;
    }

    public @Nullable UUID getStorageLocationId() {
        return storageLocationId;
    }

    public void update(PhysicalMetadataValues values) {
        this.condition = values.condition();
        this.language = values.language();
        this.finish = values.finish();
        this.purchasePrice = values.purchasePrice();
        this.purchaseCurrency = values.purchaseCurrency();
        this.purchaseDate = values.purchaseDate();
        this.notes = values.notes();
        this.storageLocationId = values.storageLocationId();
    }
}
