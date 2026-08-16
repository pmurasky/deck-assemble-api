package com.deckassemble.cards.domain;

import java.util.List;
import org.jspecify.annotations.Nullable;

public record CardImportFace(
        String name,
        @Nullable String manaCost,
        @Nullable String typeLine,
        @Nullable String oracleText,
        @Nullable String power,
        @Nullable String toughness,
        @Nullable String loyalty,
        List<String> colors,
        @Nullable String imageUri) {

    public CardImportFace(String name, String imageUri) {
        this(name, null, null, null, null, null, null, List.of(), imageUri);
    }

    public CardFace toCardFace(Card card, int faceOrder) {
        var face = new CardFace(card, faceOrder, name);
        applyAttributes(face);
        face.setColors(String.join(",", colors));
        if (imageUri != null) {
            face.setImageUri(imageUri);
        }
        return face;
    }

    private void applyAttributes(CardFace face) {
        if (manaCost != null) {
            face.setManaCost(manaCost);
        }
        if (typeLine != null) {
            face.setTypeLine(typeLine);
        }
        if (oracleText != null) {
            face.setOracleText(oracleText);
        }
        if (power != null) {
            face.setPower(power);
        }
        if (toughness != null) {
            face.setToughness(toughness);
        }
        if (loyalty != null) {
            face.setLoyalty(loyalty);
        }
    }
}
