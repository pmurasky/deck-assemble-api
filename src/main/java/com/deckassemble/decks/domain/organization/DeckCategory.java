package com.deckassemble.decks.domain.organization;

import com.deckassemble.cards.domain.CardFunctionalCategory;
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
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A user-visible grouping of cards within a deck. The six defaults (one per {@link
 * CardFunctionalCategory}) are system-owned: their {@code functionalCategory} anchor keeps its
 * legality/recommendation meaning even when a user renames the category for presentation, per the
 * "system-owned defaults" constraint. User-created categories have no functional anchor.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_categories")
public class DeckCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "system_owned", nullable = false)
    private boolean systemOwned;

    @Enumerated(EnumType.STRING)
    @Column(name = "functional_category", length = 20)
    private CardFunctionalCategory functionalCategory;

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

    protected DeckCategory() {}

    public DeckCategory(
            Long deckId, Long profileId, String name, int displayOrder, boolean systemOwned) {
        this.deckId = deckId;
        this.profileId = profileId;
        this.name = name;
        this.displayOrder = displayOrder;
        this.systemOwned = systemOwned;
    }

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public Long getProfileId() {
        return profileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isSystemOwned() {
        return systemOwned;
    }

    public @Nullable CardFunctionalCategory getFunctionalCategory() {
        return functionalCategory;
    }

    public void setFunctionalCategory(@Nullable CardFunctionalCategory functionalCategory) {
        this.functionalCategory = functionalCategory;
    }
}
