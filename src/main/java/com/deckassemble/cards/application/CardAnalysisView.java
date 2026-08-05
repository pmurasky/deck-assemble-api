package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardPrinting;
import java.math.BigDecimal;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Immutable card-printing data exposed to deck analysis consumers. */
public record CardAnalysisView(
        Long printingId,
        String name,
        @Nullable String manaCost,
        @Nullable BigDecimal manaValue,
        @Nullable String typeLine,
        @Nullable String colorIdentity,
        boolean gameChanger,
        List<Face> faces) {

    public static CardAnalysisView from(CardPrinting printing) {
        Card card = printing.getCard();
        return new CardAnalysisView(
                printing.getId(),
                card.getName(),
                card.getManaCost(),
                card.getManaValue(),
                card.getTypeLine(),
                card.getColorIdentity(),
                Boolean.TRUE.equals(card.getGameChanger()),
                card.getFaces().stream().map(CardAnalysisView::face).toList());
    }

    private static Face face(CardFace face) {
        return new Face(face.getManaCost(), face.getTypeLine(), face.getOracleText());
    }

    public record Face(
            @Nullable String manaCost, @Nullable String typeLine, @Nullable String oracleText) {}
}
