package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "magic_sets")
@EntityListeners(AuditingEntityListener.class)
public class MagicSet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scryfall_set_id", nullable = false, unique = true, length = 255)
  private String scryfallSetId;

  @Column(name = "set_code", nullable = false, unique = true, length = 10)
  private String setCode;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "set_type", length = 50)
  private String setType;

  @Column(name = "release_date")
  private LocalDate releaseDate;

  @Column(name = "card_count")
  private Integer cardCount;

  @Column(name = "digital")
  private Boolean digital;

  @Column(name = "foil_only")
  private Boolean foilOnly;

  @Column(name = "nonfoil_only")
  private Boolean nonfoilOnly;

  @Column(name = "icon_svg_uri", length = 500)
  private String iconSvgUri;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

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

  protected MagicSet() {}

  public MagicSet(String scryfallSetId, String setCode, String name) {
    this.scryfallSetId = scryfallSetId;
    this.setCode = setCode;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getScryfallSetId() {
    return scryfallSetId;
  }

  public String getSetCode() {
    return setCode;
  }

  public String getName() {
    return name;
  }

  public String getSetType() {
    return setType;
  }

  public void setSetType(String setType) {
    this.setType = setType;
  }

  public LocalDate getReleaseDate() {
    return releaseDate;
  }

  public void setReleaseDate(LocalDate releaseDate) {
    this.releaseDate = releaseDate;
  }

  public Integer getCardCount() {
    return cardCount;
  }

  public void setCardCount(Integer cardCount) {
    this.cardCount = cardCount;
  }

  public Boolean getDigital() {
    return digital;
  }

  public void setDigital(Boolean digital) {
    this.digital = digital;
  }

  public Boolean getFoilOnly() {
    return foilOnly;
  }

  public void setFoilOnly(Boolean foilOnly) {
    this.foilOnly = foilOnly;
  }

  public Boolean getNonfoilOnly() {
    return nonfoilOnly;
  }

  public void setNonfoilOnly(Boolean nonfoilOnly) {
    this.nonfoilOnly = nonfoilOnly;
  }

  public String getIconSvgUri() {
    return iconSvgUri;
  }

  public void setIconSvgUri(String iconSvgUri) {
    this.iconSvgUri = iconSvgUri;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }
}
