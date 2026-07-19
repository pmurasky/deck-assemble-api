package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "card_printings")
@EntityListeners(AuditingEntityListener.class)
public class CardPrinting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "card_id", nullable = false)
  private Card card;

  @ManyToOne(optional = false)
  @JoinColumn(name = "magic_set_id", nullable = false)
  private MagicSet magicSet;

  @Column(name = "scryfall_card_id", nullable = false, unique = true, length = 255)
  private String scryfallCardId;

  @Column(name = "collector_number", length = 50)
  private String collectorNumber;

  @Column(name = "rarity", length = 50)
  private String rarity;

  @Column(name = "artist", length = 255)
  private String artist;

  @Column(name = "flavor_text", columnDefinition = "text")
  private String flavorText;

  @Column(name = "image_uri_small", length = 500)
  private String imageUriSmall;

  @Column(name = "image_uri_normal", length = 500)
  private String imageUriNormal;

  @Column(name = "image_uri_large", length = 500)
  private String imageUriLarge;

  @Column(name = "released_at")
  private LocalDate releasedAt;

  @Column(name = "foil_available")
  private Boolean foilAvailable;

  @Column(name = "nonfoil_available")
  private Boolean nonfoilAvailable;

  @Column(name = "promo")
  private Boolean promo;

  @Column(name = "digital")
  private Boolean digital;

  @Column(name = "language", length = 10)
  private String language;

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

  protected CardPrinting() {}

  public CardPrinting(Card card, MagicSet magicSet, String scryfallCardId) {
    this.card = card;
    this.magicSet = magicSet;
    this.scryfallCardId = scryfallCardId;
  }

  public Long getId() {
    return id;
  }

  public Card getCard() {
    return card;
  }

  public MagicSet getMagicSet() {
    return magicSet;
  }

  public String getScryfallCardId() {
    return scryfallCardId;
  }

  public String getCollectorNumber() {
    return collectorNumber;
  }

  public void setCollectorNumber(String collectorNumber) {
    this.collectorNumber = collectorNumber;
  }

  public String getRarity() {
    return rarity;
  }

  public void setRarity(String rarity) {
    this.rarity = rarity;
  }

  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  public String getFlavorText() {
    return flavorText;
  }

  public void setFlavorText(String flavorText) {
    this.flavorText = flavorText;
  }

  public String getImageUriSmall() {
    return imageUriSmall;
  }

  public void setImageUriSmall(String imageUriSmall) {
    this.imageUriSmall = imageUriSmall;
  }

  public String getImageUriNormal() {
    return imageUriNormal;
  }

  public void setImageUriNormal(String imageUriNormal) {
    this.imageUriNormal = imageUriNormal;
  }

  public String getImageUriLarge() {
    return imageUriLarge;
  }

  public void setImageUriLarge(String imageUriLarge) {
    this.imageUriLarge = imageUriLarge;
  }

  public LocalDate getReleasedAt() {
    return releasedAt;
  }

  public void setReleasedAt(LocalDate releasedAt) {
    this.releasedAt = releasedAt;
  }

  public Boolean getFoilAvailable() {
    return foilAvailable;
  }

  public void setFoilAvailable(Boolean foilAvailable) {
    this.foilAvailable = foilAvailable;
  }

  public Boolean getNonfoilAvailable() {
    return nonfoilAvailable;
  }

  public void setNonfoilAvailable(Boolean nonfoilAvailable) {
    this.nonfoilAvailable = nonfoilAvailable;
  }

  public Boolean getPromo() {
    return promo;
  }

  public void setPromo(Boolean promo) {
    this.promo = promo;
  }

  public Boolean getDigital() {
    return digital;
  }

  public void setDigital(Boolean digital) {
    this.digital = digital;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
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
