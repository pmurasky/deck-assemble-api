package com.deckassemble.cards.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "cards")
@EntityListeners(AuditingEntityListener.class)
public class Card {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "scryfall_oracle_id", nullable = false, unique = true, length = 255)
  private String scryfallOracleId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "mana_cost", length = 255)
  private String manaCost;

  @Column(name = "mana_value", precision = 10, scale = 1)
  private BigDecimal manaValue;

  @Column(name = "type_line", length = 255)
  private String typeLine;

  @Column(name = "oracle_text", columnDefinition = "text")
  private String oracleText;

  @Column(name = "power", length = 10)
  private String power;

  @Column(name = "toughness", length = 10)
  private String toughness;

  @Column(name = "loyalty", length = 10)
  private String loyalty;

  // ponytail: comma-separated WUBRG for now; switch to native array if queries need it
  @Column(name = "colors", length = 50)
  private String colors;

  @Column(name = "color_identity", length = 50)
  private String colorIdentity;

  // ponytail: comma-separated keywords; full-text search can be added later
  @Column(name = "keywords", columnDefinition = "text")
  private String keywords;

  @Column(name = "layout", length = 50)
  private String layout;

  @Column(name = "reserved")
  private Boolean reserved;

  @Column(name = "commander_rank")
  private Integer commanderRank;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("faceOrder ASC")
  private List<CardFace> faces = new ArrayList<>();

  @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CardLegality> legalities = new ArrayList<>();

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

  protected Card() {}

  public Card(String scryfallOracleId, String name) {
    this.scryfallOracleId = scryfallOracleId;
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getScryfallOracleId() {
    return scryfallOracleId;
  }

  public String getName() {
    return name;
  }

  public String getManaCost() {
    return manaCost;
  }

  public void setManaCost(String manaCost) {
    this.manaCost = manaCost;
  }

  public BigDecimal getManaValue() {
    return manaValue;
  }

  public void setManaValue(BigDecimal manaValue) {
    this.manaValue = manaValue;
  }

  public String getTypeLine() {
    return typeLine;
  }

  public void setTypeLine(String typeLine) {
    this.typeLine = typeLine;
  }

  public String getOracleText() {
    return oracleText;
  }

  public void setOracleText(String oracleText) {
    this.oracleText = oracleText;
  }

  public String getPower() {
    return power;
  }

  public void setPower(String power) {
    this.power = power;
  }

  public String getToughness() {
    return toughness;
  }

  public void setToughness(String toughness) {
    this.toughness = toughness;
  }

  public String getLoyalty() {
    return loyalty;
  }

  public void setLoyalty(String loyalty) {
    this.loyalty = loyalty;
  }

  public String getColors() {
    return colors;
  }

  public void setColors(String colors) {
    this.colors = colors;
  }

  public String getColorIdentity() {
    return colorIdentity;
  }

  public void setColorIdentity(String colorIdentity) {
    this.colorIdentity = colorIdentity;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getLayout() {
    return layout;
  }

  public void setLayout(String layout) {
    this.layout = layout;
  }

  public Boolean getReserved() {
    return reserved;
  }

  public void setReserved(Boolean reserved) {
    this.reserved = reserved;
  }

  public Integer getCommanderRank() {
    return commanderRank;
  }

  public void setCommanderRank(Integer commanderRank) {
    this.commanderRank = commanderRank;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public List<CardFace> getFaces() {
    return faces;
  }

  public List<CardLegality> getLegalities() {
    return legalities;
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
