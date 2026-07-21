package com.deckassemble.decks.domain;

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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "decks")
public class Deck {

    public enum Status {
        DRAFT,
        ACTIVE,
        ARCHIVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ponytail: plain FK, not @ManyToOne Profile — keeps decks module decoupled from users module
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "format_code", nullable = false, length = 50)
    private String formatCode;

    @Column(length = 2000)
    private String description;

    // ponytail: plain FK, not @ManyToOne Card — keeps decks module decoupled from cards module
    @Column(name = "commander_card_id")
    private Long commanderCardId;

    @Column(name = "secondary_commander_card_id")
    private Long secondaryCommanderCardId;

    @Column(name = "use_owned_cards_only", nullable = false)
    private boolean useOwnedCardsOnly;

    @Column(name = "budget_limit", precision = 10, scale = 2)
    private BigDecimal budgetLimit;

    @Column(name = "desired_power_level")
    private Integer desiredPowerLevel;

    @Column(name = "play_style", length = 50)
    private String playStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

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

    protected Deck() {}

    public Deck(Long profileId, String name, String formatCode) {
        this.profileId = profileId;
        this.name = name;
        this.formatCode = formatCode;
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

    public String getFormatCode() {
        return formatCode;
    }

    public void setFormatCode(String formatCode) {
        this.formatCode = formatCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCommanderCardId() {
        return commanderCardId;
    }

    public void setCommanderCardId(Long commanderCardId) {
        this.commanderCardId = commanderCardId;
    }

    public Long getSecondaryCommanderCardId() {
        return secondaryCommanderCardId;
    }

    public void setSecondaryCommanderCardId(Long secondaryCommanderCardId) {
        this.secondaryCommanderCardId = secondaryCommanderCardId;
    }

    public boolean isUseOwnedCardsOnly() {
        return useOwnedCardsOnly;
    }

    public void setUseOwnedCardsOnly(boolean useOwnedCardsOnly) {
        this.useOwnedCardsOnly = useOwnedCardsOnly;
    }

    public BigDecimal getBudgetLimit() {
        return budgetLimit;
    }

    public void setBudgetLimit(BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public Integer getDesiredPowerLevel() {
        return desiredPowerLevel;
    }

    public void setDesiredPowerLevel(Integer desiredPowerLevel) {
        this.desiredPowerLevel = desiredPowerLevel;
    }

    public String getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(String playStyle) {
        this.playStyle = playStyle;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
