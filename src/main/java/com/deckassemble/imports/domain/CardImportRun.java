package com.deckassemble.imports.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "card_import_runs")
public class CardImportRun {

    public enum Status {
        STARTED,
        COMPLETED,
        COMPLETED_WITH_ERRORS,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(nullable = false)
    private int recordsRead;

    @Column(nullable = false)
    private int recordsCreated;

    @Column(nullable = false)
    private int recordsUpdated;

    @Column(nullable = false)
    private int recordsFailed;

    @Column(length = 2000)
    private String errorSummary;

    private String createdBy;

    protected CardImportRun() {}

    public CardImportRun(
            String provider, String query, OffsetDateTime startedAt, String createdBy) {
        this.provider = provider;
        this.query = query;
        this.startedAt = startedAt;
        this.createdBy = createdBy;
        this.status = Status.STARTED;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getQuery() {
        return query;
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

    public int getRecordsRead() {
        return recordsRead;
    }

    public int getRecordsCreated() {
        return recordsCreated;
    }

    public int getRecordsUpdated() {
        return recordsUpdated;
    }

    public int getRecordsFailed() {
        return recordsFailed;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void recordCreated() {
        recordsRead++;
        recordsCreated++;
    }

    public void recordUpdated() {
        recordsRead++;
        recordsUpdated++;
    }

    public void recordSkipped() {
        recordsRead++;
        recordsFailed++;
    }

    public void complete(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
        this.status = recordsFailed == 0 ? Status.COMPLETED : Status.COMPLETED_WITH_ERRORS;
    }

    public void fail(OffsetDateTime completedAt, String errorSummary) {
        this.completedAt = completedAt;
        this.status = Status.FAILED;
        this.errorSummary =
                errorSummary == null
                        ? null
                        : errorSummary.substring(0, Math.min(errorSummary.length(), 2000));
    }
}
