package com.deckassemble.collections.domain;

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
@Table(name = "collections")
@EntityListeners(AuditingEntityListener.class)
public class CardCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ponytail: plain FK, join to profiles only if a query ever needs it
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "default_collection", nullable = false)
    private boolean defaultCollection;

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

    protected CardCollection() {}

    public CardCollection(
            Long profileId, String name, String description, boolean defaultCollection) {
        this.profileId = profileId;
        this.name = name;
        this.description = description;
        this.defaultCollection = defaultCollection;
    }

    public Long getId() {
        return id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultCollection() {
        return defaultCollection;
    }

    public void setDefaultCollection(boolean defaultCollection) {
        this.defaultCollection = defaultCollection;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
