package com.deckassemble.cards.api;

import com.deckassemble.cards.application.CardSearchFilter;
import com.deckassemble.cards.application.InvalidCardSearchFilterException;
import com.deckassemble.cards.domain.CardFunctionalCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

/**
 * Every typed, allow-listed GET /cards filter, bound from flat query params via
 * {@code @ModelAttribute}. Replaces the previous individual {@code @RequestParam} list on {@link
 * CardController#search}; {@link #toFilter()} and {@link #ownedQuantityRange()} translate this flat
 * wire shape into the cohesive {@link CardSearchFilter} the application layer consumes.
 */
// Suppressed: one request object for every independent, optional search filter (the whole point
// of introducing it was to replace N positional controller params with one typed object).
@SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
public record CardSearchRequest(
        @Size(max = 100) String query,
        @Nullable String setCode,
        @Nullable String colorIdentity,
        @Nullable String type,
        @Nullable Boolean commanderEligible,
        @Nullable Long partnerForCardId,
        @Nullable String oracleText,
        @Nullable @DecimalMin("0") BigDecimal minManaValue,
        @Nullable @DecimalMin("0") BigDecimal maxManaValue,
        @Nullable @Min(0) @Max(20) Integer minPower,
        @Nullable @Min(0) @Max(20) Integer maxPower,
        @Nullable @Min(0) @Max(20) Integer minToughness,
        @Nullable @Min(0) @Max(20) Integer maxToughness,
        @Nullable String rarity,
        @Nullable String keyword,
        @Nullable String formatCode,
        @Nullable String legalityStatus,
        @Nullable @DecimalMin("0") BigDecimal minPrice,
        @Nullable @DecimalMin("0") BigDecimal maxPrice,
        @Nullable String currency,
        @Nullable @Min(0) Integer minOwnedQuantity,
        @Nullable @Min(0) Integer maxOwnedQuantity,
        @Nullable String functionalCategory,
        @Nullable Boolean gameChanger,
        @Nullable String collectorNumber,
        @Nullable String language,
        @Nullable String finish) {

    public CardSearchRequest {
        query = query == null ? "" : query;
    }

    CardSearchFilter toFilter() {
        return new CardSearchFilter(
                query,
                colorIdentity,
                type,
                commanderEligible,
                oracleText,
                range(minManaValue, maxManaValue),
                intRange(minPower, maxPower),
                intRange(minToughness, maxToughness),
                keyword,
                formatLegality(),
                priceRange(),
                parsedFunctionalCategory(),
                gameChanger,
                printingFilter());
    }

    CardSearchFilter.@Nullable IntRange ownedQuantityRange() {
        return intRange(minOwnedQuantity, maxOwnedQuantity);
    }

    private static CardSearchFilter.@Nullable BigDecimalRange range(
            @Nullable BigDecimal min, @Nullable BigDecimal max) {
        return min == null && max == null ? null : new CardSearchFilter.BigDecimalRange(min, max);
    }

    private static CardSearchFilter.@Nullable IntRange intRange(
            @Nullable Integer min, @Nullable Integer max) {
        return min == null && max == null ? null : new CardSearchFilter.IntRange(min, max);
    }

    private CardSearchFilter.@Nullable FormatLegality formatLegality() {
        return formatCode == null
                ? null
                : new CardSearchFilter.FormatLegality(formatCode, legalityStatus);
    }

    private CardSearchFilter.@Nullable PriceRange priceRange() {
        return minPrice == null && maxPrice == null
                ? null
                : new CardSearchFilter.PriceRange(minPrice, maxPrice, currency);
    }

    private @Nullable CardFunctionalCategory parsedFunctionalCategory() {
        if (functionalCategory == null) {
            return null;
        }
        try {
            return CardFunctionalCategory.valueOf(functionalCategory.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidCardSearchFilterException(
                    "functionalCategory", functionalCategory, exception);
        }
    }

    private CardSearchFilter.@Nullable PrintingFilter printingFilter() {
        if (setCode == null
                && rarity == null
                && collectorNumber == null
                && language == null
                && finish == null) {
            return null;
        }
        return new CardSearchFilter.PrintingFilter(
                setCode, rarity, collectorNumber, language, finish);
    }
}
