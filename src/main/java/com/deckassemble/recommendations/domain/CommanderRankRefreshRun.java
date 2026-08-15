package com.deckassemble.recommendations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Persistence record of a single commander-rank refresh against EDHREC, scheduled or manual. Retained so
 * operators can audit when ranks last changed and why a refresh failed.
 */
@Entity
@Table(name = "commander_rank_refresh_runs")
public class CommanderRankRefreshRun {

    private static final int ERROR_SUMMARY_MAX_LENGTH = 2000;

    public enum Status {
        STARTED,
        COMPLETED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private @Nullable Long id;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false)
    private int cardsUpdated;

    @Column(length = ERROR_SUMMARY_MAX_LENGTH)
    private @Nullable String errorSummary;

    @Column(nullable = false)
    private String triggeredBy;

    protected CommanderRankRefreshRun() {
        // JPA only.
    }

    public CommanderRankRefreshRun(OffsetDateTime startedAt, String triggeredBy) {
        this.startedAt = startedAt;
        this.triggeredBy = triggeredBy;
        this.status = Status.STARTED;
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public Status getStatus() {
        return status;
    }

    public int getCardsUpdated() {
        return cardsUpdated;
    }

    public @Nullable String getErrorSummary() {
        return errorSummary;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void complete(OffsetDateTime completedAt, int cardsUpdated) {
        this.completedAt = completedAt;
        this.cardsUpdated = cardsUpdated;
        this.status = Status.COMPLETED;
    }

    public void fail(OffsetDateTime completedAt, String errorSummary) {
        this.completedAt = completedAt;
        this.errorSummary = truncate(errorSummary, ERROR_SUMMARY_MAX_LENGTH);
        this.status = Status.FAILED;
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
