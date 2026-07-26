package com.deckassemble.cards.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "card_printing_faces")
public class CardPrintingFace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "card_printing_id", nullable = false)
    private CardPrinting cardPrinting;

    @Column(name = "face_order", nullable = false)
    private Integer faceOrder;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "image_uri", nullable = false, length = 500)
    private String imageUri;

    protected CardPrintingFace() {}

    public CardPrintingFace(
            CardPrinting cardPrinting, Integer faceOrder, String name, String imageUri) {
        this.cardPrinting = cardPrinting;
        this.faceOrder = faceOrder;
        this.name = name;
        this.imageUri = imageUri;
    }

    public Long getId() {
        return id;
    }

    public CardPrinting getCardPrinting() {
        return cardPrinting;
    }

    public Integer getFaceOrder() {
        return faceOrder;
    }

    public String getName() {
        return name;
    }

    public String getImageUri() {
        return imageUri;
    }
}
