package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardPrinting;
import java.time.LocalDate;

public record CardPrintingResponse(
        Long id,
        String setCode,
        String collectorNumber,
        String rarity,
        String imageUri,
        LocalDate releasedAt) {

    public static CardPrintingResponse from(CardPrinting printing) {
        return new CardPrintingResponse(
                printing.getId(),
                printing.getMagicSet().getSetCode(),
                printing.getCollectorNumber(),
                printing.getRarity(),
                printing.getImageUriNormal(),
                printing.getReleasedAt());
    }
}
