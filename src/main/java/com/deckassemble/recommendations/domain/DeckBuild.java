package com.deckassemble.recommendations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "deck_builds")
@EntityListeners(AuditingEntityListener.class)
public class DeckBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "config", columnDefinition = "text")
    private String config;

    @Column(name = "score", precision = 6, scale = 2)
    private BigDecimal score;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    protected DeckBuild() {}

    public DeckBuild(Long deckId, String config, @Nullable BigDecimal score) {
        this.deckId = deckId;
        this.config = config;
        this.score = score;
    }

    public Long getId() {
        return id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public String getConfig() {
        return config;
    }

    public BigDecimal getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
