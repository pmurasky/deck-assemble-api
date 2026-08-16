package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/** Stored beginner guidance and its admin-review state for one card. */
@Entity
@Table(name = "card_beginner_guides")
public class BeginnerGuide {
    @Id private Long cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BeginnerGuideStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(nullable = false, columnDefinition = "text")
    private String examples;

    @Column(name = "when_to_use", nullable = false, columnDefinition = "text")
    private String whenToUse;

    @Column(name = "source_rulings_snapshot", nullable = false, columnDefinition = "text")
    private String sourceRulingsSnapshot;

    @Column(name = "source_oracle_hash", nullable = false, length = 64)
    private String sourceOracleHash;

    @Column(nullable = false)
    private OffsetDateTime generatedAt;

    private @Nullable OffsetDateTime publishedAt;
    private @Nullable String reviewedBy;

    protected BeginnerGuide() {
        // JPA only.
    }

    public BeginnerGuide(Long cardId, BeginnerGuideDraft draft, OffsetDateTime generatedAt) {
        this.cardId = cardId;
        this.status = BeginnerGuideStatus.DRAFT;
        this.generatedAt = generatedAt;
        applyContent(draft);
    }

    public void replaceContent(BeginnerGuideDraft draft) {
        applyContent(draft);
    }

    private void applyContent(BeginnerGuideDraft draft) {
        this.summary = draft.summary();
        this.examples = draft.examples();
        this.whenToUse = draft.whenToUse();
        this.sourceRulingsSnapshot = draft.sourceRulingsSnapshot();
        this.sourceOracleHash = draft.sourceOracleHash();
    }

    public void publish(String reviewer, OffsetDateTime publishedAt) {
        this.status = BeginnerGuideStatus.PUBLISHED;
        this.reviewedBy = reviewer;
        this.publishedAt = publishedAt;
    }

    public void markStale() {
        this.status = BeginnerGuideStatus.STALE;
    }

    public void report() {
        this.status = BeginnerGuideStatus.REPORTED;
    }

    public Long getCardId() {
        return cardId;
    }

    public BeginnerGuideStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getExamples() {
        return examples;
    }

    public String getWhenToUse() {
        return whenToUse;
    }

    public String getSourceRulingsSnapshot() {
        return sourceRulingsSnapshot;
    }

    public String getSourceOracleHash() {
        return sourceOracleHash;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public @Nullable OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public @Nullable String getReviewedBy() {
        return reviewedBy;
    }
}
