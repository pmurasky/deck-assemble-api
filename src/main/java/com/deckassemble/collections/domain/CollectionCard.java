package com.deckassemble.collections.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "collection_cards")
@EntityListeners(AuditingEntityListener.class)
public class CollectionCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "collection_id", nullable = false)
  private Long collectionId;

  // ponytail: plain FK to card_printings; card details come from the cards API
  @Column(name = "card_printing_id", nullable = false)
  private Long cardPrintingId;

  @Column(name = "regular_quantity", nullable = false)
  private int regularQuantity;

  @Column(name = "foil_quantity", nullable = false)
  private int foilQuantity;

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

  protected CollectionCard() {}

  public CollectionCard(Long collectionId, Long cardPrintingId, int regularQuantity, int foilQuantity) {
    this.collectionId = collectionId;
    this.cardPrintingId = cardPrintingId;
    this.regularQuantity = regularQuantity;
    this.foilQuantity = foilQuantity;
  }

  public Long getId() {
    return id;
  }

  public Long getCollectionId() {
    return collectionId;
  }

  public Long getCardPrintingId() {
    return cardPrintingId;
  }

  public int getRegularQuantity() {
    return regularQuantity;
  }

  public void setRegularQuantity(int regularQuantity) {
    this.regularQuantity = regularQuantity;
  }

  public int getFoilQuantity() {
    return foilQuantity;
  }

  public void setFoilQuantity(int foilQuantity) {
    this.foilQuantity = foilQuantity;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
