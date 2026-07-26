package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardPrintingFace;

public record CardFaceResponse(String name, String imageUrl) {

    public static CardFaceResponse from(CardPrintingFace face) {
        return new CardFaceResponse(face.getName(), face.getImageUri());
    }
}
