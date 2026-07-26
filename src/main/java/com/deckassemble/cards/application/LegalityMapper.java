package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardLegality;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class LegalityMapper {

    private LegalityMapper() {}

    static Map<String, String> byFormat(List<CardLegality> legalities) {
        // ponytail: concurrent imports once left duplicate rows; keep first rather than 500
        return legalities.stream()
                .collect(
                        Collectors.toMap(
                                CardLegality::getFormatCode,
                                CardLegality::getLegalityStatus,
                                (existing, duplicate) -> existing));
    }
}
