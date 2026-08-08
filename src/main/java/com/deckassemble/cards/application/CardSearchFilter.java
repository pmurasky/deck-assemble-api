package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardFunctionalCategory;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

// Suppressed: parameter object aggregating every independent, optional search filter exposed by
// GET /cards. Callers build one CardSearchFilter (via CardSearchRequest#toFilter) instead of
// passing filters as positional method parameters; splitting further would only hide the same
// field count behind more types without adding cohesion (see CardCatalogService#search). Public
// because the api-layer request DTO builds this application-layer type directly.
@SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
public record CardSearchFilter(
        String query,
        @Nullable String colorIdentity,
        @Nullable String type,
        @Nullable Boolean commanderEligible,
        @Nullable String oracleText,
        @Nullable BigDecimalRange manaValueRange,
        @Nullable IntRange powerRange,
        @Nullable IntRange toughnessRange,
        @Nullable String keyword,
        @Nullable FormatLegality formatLegality,
        @Nullable PriceRange priceRange,
        @Nullable CardFunctionalCategory functionalCategory,
        @Nullable Boolean gameChanger,
        @Nullable PrintingFilter printingFilter) {

    /** Inclusive numeric bounds; a null bound is unbounded on that side. */
    public record IntRange(@Nullable Integer min, @Nullable Integer max) {}

    /** Inclusive decimal bounds; a null bound is unbounded on that side. */
    public record BigDecimalRange(@Nullable BigDecimal min, @Nullable BigDecimal max) {}

    /** Inclusive price bounds in an allow-listed currency (see {@code CardPrice#forCurrency}). */
    public record PriceRange(
            @Nullable BigDecimal min, @Nullable BigDecimal max, @Nullable String currency) {}

    /** Format legality, e.g. formatCode "commander" + legalityStatus "legal" (the default). */
    public record FormatLegality(String formatCode, @Nullable String legalityStatus) {}

    /** Printing-scoped filters correlated to the same printing (set, rarity, number, ...). */
    public record PrintingFilter(
            @Nullable String setCode,
            @Nullable String rarity,
            @Nullable String collectorNumber,
            @Nullable String language,
            @Nullable String finish) {

        boolean isEmpty() {
            return setCode == null
                    && rarity == null
                    && collectorNumber == null
                    && language == null
                    && finish == null;
        }
    }

    /** Builds a filter carrying only the original set/color/type/commander-eligible criteria. */
    static CardSearchFilter basic(
            String query,
            @Nullable String setCode,
            @Nullable String colorIdentity,
            @Nullable String type,
            @Nullable Boolean commanderEligible) {
        PrintingFilter printingFilter =
                setCode == null ? null : new PrintingFilter(setCode, null, null, null, null);
        return new CardSearchFilter(
                query,
                colorIdentity,
                type,
                commanderEligible,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                printingFilter);
    }
}
