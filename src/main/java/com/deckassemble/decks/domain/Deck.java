package com.deckassemble.decks.domain;

import com.deckassemble.decks.domain.publishing.DeckVisibility;
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
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "decks")
// Justified: JPA entity mapping table columns; field count follows the schema.
// CyclomaticComplexity suppressed: plain accessors dominate the class metric.
@SuppressWarnings({"PMD.TooManyFields", "PMD.CyclomaticComplexity"})
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

    // ponytail: plain FK, not @ManyToOne DeckFolder — consistent with the commander-card-id style
    // above even though DeckFolder is in-module; a deck has at most one folder.
    @Column(name = "folder_id")
    private Long folderId;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckVisibility visibility = DeckVisibility.PRIVATE;

    // Assigned once, on first move away from PRIVATE, and never changed afterwards — see
    // DeckPublishingService. Stays set even if visibility later returns to PRIVATE, so a
    // previously shared link is stable; DeckVisibilityPolicy is what blocks it from resolving.
    @Column(name = "share_slug", length = 32)
    private String shareSlug;

    // Guide/writeup an owner attaches to a deck. Raw Markdown only — see DeckPrimerResponse for
    // why no rendered-HTML field exists yet (no vetted renderer/sanitizer dependency present).
    @Column(name = "primer_title", length = 200)
    private String primerTitle;

    @Column(name = "primer_markdown", columnDefinition = "text")
    private String primerMarkdown;

    // Set together by DeckPublishingService#publish: the deck-revision number pinned as the
    // shared/fork representation, and when that pin was taken. Null until first published; stays
    // set (and stale) across further private edits until republished — that staleness is the
    // whole point, see DeckPublishingService#getShared.
    @Column(name = "published_revision_number")
    private Integer publishedRevisionNumber;

    @Column(name = "published_at")
    private Instant publishedAt;

    // Owner-controlled toggle gating new comment creation on this deck's shared view (see
    // CommentService); defaults enabled. Existing comments stay visible even when disabled — this
    // only blocks new ones.
    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled = true;

    // ponytail: plain FK, no DB constraint — same pattern as commanderCardId/folderId above, but
    // load-bearing here rather than incidental: DeckForkService must survive the source deck being
    // deleted or turned back PRIVATE after the fork exists (attribution is metadata about how this
    // deck came to be, not a live reference to the source). See DeckForkServiceTest.
    @Column(name = "source_deck_id")
    private Long sourceDeckId;

    @Column(name = "source_revision_number")
    private Integer sourceRevisionNumber;

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

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public Long getCommanderCardId() {
        return commanderCardId;
    }

    public void setCommanderCardId(@Nullable Long commanderCardId) {
        this.commanderCardId = commanderCardId;
    }

    public Long getSecondaryCommanderCardId() {
        return secondaryCommanderCardId;
    }

    public void setSecondaryCommanderCardId(@Nullable Long secondaryCommanderCardId) {
        this.secondaryCommanderCardId = secondaryCommanderCardId;
    }

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(@Nullable Long folderId) {
        this.folderId = folderId;
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

    public void setBudgetLimit(@Nullable BigDecimal budgetLimit) {
        this.budgetLimit = budgetLimit;
    }

    public Integer getDesiredPowerLevel() {
        return desiredPowerLevel;
    }

    public void setDesiredPowerLevel(@Nullable Integer desiredPowerLevel) {
        this.desiredPowerLevel = desiredPowerLevel;
    }

    public String getPlayStyle() {
        return playStyle;
    }

    public void setPlayStyle(@Nullable String playStyle) {
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public DeckVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(DeckVisibility visibility) {
        this.visibility = visibility;
    }

    public String getShareSlug() {
        return shareSlug;
    }

    public void setShareSlug(@Nullable String shareSlug) {
        this.shareSlug = shareSlug;
    }

    public String getPrimerTitle() {
        return primerTitle;
    }

    public void setPrimerTitle(@Nullable String primerTitle) {
        this.primerTitle = primerTitle;
    }

    public String getPrimerMarkdown() {
        return primerMarkdown;
    }

    public void setPrimerMarkdown(@Nullable String primerMarkdown) {
        this.primerMarkdown = primerMarkdown;
    }

    public Integer getPublishedRevisionNumber() {
        return publishedRevisionNumber;
    }

    public void setPublishedRevisionNumber(@Nullable Integer publishedRevisionNumber) {
        this.publishedRevisionNumber = publishedRevisionNumber;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(@Nullable Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isCommentsEnabled() {
        return commentsEnabled;
    }

    public void setCommentsEnabled(boolean commentsEnabled) {
        this.commentsEnabled = commentsEnabled;
    }

    public Long getSourceDeckId() {
        return sourceDeckId;
    }

    public void setSourceDeckId(@Nullable Long sourceDeckId) {
        this.sourceDeckId = sourceDeckId;
    }

    public Integer getSourceRevisionNumber() {
        return sourceRevisionNumber;
    }

    public void setSourceRevisionNumber(@Nullable Integer sourceRevisionNumber) {
        this.sourceRevisionNumber = sourceRevisionNumber;
    }
}
