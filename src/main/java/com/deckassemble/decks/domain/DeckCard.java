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
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "deck_cards")
public class DeckCard {

  public enum Section {
    COMMANDER,
    MAIN_DECK,
    SIDEBOARD,
    COMPANION,
    MAYBE_BOARD
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "deck_id", nullable = false)
  private Long deckId;

  @Column(name = "card_printing_id", nullable = false)
  private Long cardPrintingId;

  @Column(nullable = false)
  private int quantity = 1;

  @Enumerated(EnumType.STRING)
  @Column(name = "deck_section", nullable = false, length = 20)
  private Section deckSection = Section.MAIN_DECK;

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

  protected DeckCard() {
  }

  public DeckCard(Long deckId, Long cardPrintingId, int quantity, Section deckSection) {
    this.deckId = deckId;
    this.cardPrintingId = cardPrintingId;
    this.quantity = quantity;
    this.deckSection = deckSection;
  }

  public Long getId() {
    return id;
  }

  public Long getDeckId() {
    return deckId;
  }

  public Long getCardPrintingId() {
    return cardPrintingId;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public Section getDeckSection() {
    return deckSection;
  }

  public void setDeckSection(Section deckSection) {
    this.deckSection = deckSection;
  }
}
