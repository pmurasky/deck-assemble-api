package com.deckassemble.cards.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record CardImportData(
        String id,
        String oracleId,
        String name,
        String manaCost,
        Double cmc,
        String typeLine,
        String oracleText,
        String power,
        String toughness,
        String loyalty,
        List<String> colors,
        List<String> colorIdentity,
        List<String> keywords,
        String layout,
        Boolean reserved,
        String setId,
        String set,
        String setName,
        String collectorNumber,
        String rarity,
        String artist,
        String flavorText,
        @Nullable CardImportImages images,
        LocalDate releasedAt,
        Boolean foil,
        Boolean nonfoil,
        Boolean promo,
        Boolean digital,
        String lang,
        Map<String, String> legalities) {}
