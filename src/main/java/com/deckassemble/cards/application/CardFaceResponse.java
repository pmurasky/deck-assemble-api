package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardFace;

public record CardFaceResponse(String name, String imageUrl) {

    public static CardFaceResponse from(CardFace face) {
        return new CardFaceResponse(face.getName(), face.getImageUri());
    }
}
