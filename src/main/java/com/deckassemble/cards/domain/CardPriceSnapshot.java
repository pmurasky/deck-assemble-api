package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "card_price_snapshots")
public class CardPriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_printing_id", nullable = false)
    private Long cardPrintingId;

    @Column(name = "usd", precision = 10, scale = 2)
    private BigDecimal usd;

    @Column(name = "usd_foil", precision = 10, scale = 2)
    private BigDecimal usdFoil;

    @Column(name = "eur", precision = 10, scale = 2)
    private BigDecimal eur;

    @Column(name = "tix", precision = 10, scale = 2)
    private BigDecimal tix;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    protected CardPriceSnapshot() {}

    public CardPriceSnapshot(Long cardPrintingId, CardPrice price, Instant fetchedAt) {
        this.cardPrintingId = cardPrintingId;
        this.usd = price.usd();
        this.usdFoil = price.usdFoil();
        this.eur = price.eur();
        this.tix = price.tix();
        this.fetchedAt = fetchedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getCardPrintingId() {
        return cardPrintingId;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public CardPrice toPrice() {
        return new CardPrice(usd, usdFoil, eur, tix);
    }
}
