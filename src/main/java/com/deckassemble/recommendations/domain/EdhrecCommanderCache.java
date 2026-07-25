package com.deckassemble.recommendations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "edhrec_commander_cache")
public class EdhrecCommanderCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "commander_oracle_id", nullable = false, unique = true)
    private String commanderOracleId;

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected EdhrecCommanderCache() {}

    public EdhrecCommanderCache(String commanderOracleId, String payload, Instant fetchedAt) {
        this.commanderOracleId = commanderOracleId;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCommanderOracleId() {
        return commanderOracleId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void refresh(String newPayload, Instant newFetchedAt) {
        payload = newPayload;
        fetchedAt = newFetchedAt;
    }
}
