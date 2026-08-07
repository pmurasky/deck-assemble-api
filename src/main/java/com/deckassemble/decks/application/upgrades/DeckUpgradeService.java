package com.deckassemble.decks.application.upgrades;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.alternatives.DeckCardAlternative;
import com.deckassemble.decks.application.alternatives.DeckCardAlternativeService;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
import com.deckassemble.recommendations.application.CardCategorizer;
import com.deckassemble.recommendations.application.ScoreContribution;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/** Builds bounded, read-only upgrade plan proposals from alternatives and deck analysis gaps. */
@Service
public class DeckUpgradeService {

    private static final String DEFAULT_CURRENCY = "usd";
    private static final int DEFAULT_MAX_CHANGES = 10;
    private static final int ALTERNATIVE_LIMIT = 10;
    private static final String OWNED = "OWNED";
    private static final String PROXY = "PROXY";
    private static final String MAIN_DECK = "MAIN_DECK";
    private static final List<String> GAP_CATEGORIES = List.of("RAMP", "DRAW", "WIPE", "REMOVAL");

    private final DeckCardService deckCardService;
    private final DeckAnalysisService deckAnalysisService;
    private final DeckCardAlternativeService alternativeService;
    private final CardCatalogService cardCatalogService;
    private final CardPriceService cardPriceService;

    public DeckUpgradeService(
            DeckCardService deckCardService,
            DeckAnalysisService deckAnalysisService,
            DeckCardAlternativeService alternativeService,
            CardCatalogService cardCatalogService,
            CardPriceService cardPriceService) {
        this.deckCardService = deckCardService;
        this.deckAnalysisService = deckAnalysisService;
        this.alternativeService = alternativeService;
        this.cardCatalogService = cardCatalogService;
        this.cardPriceService = cardPriceService;
    }

    public DeckUpgradePlan plan(
            long deckId,
            Objective objective,
            @Nullable BigDecimal budget,
            @Nullable String currency,
            @Nullable Integer maxChanges) {
        String effectiveCurrency = currency == null ? DEFAULT_CURRENCY : currency;
        int effectiveMaxChanges = maxChanges == null ? DEFAULT_MAX_CHANGES : maxChanges;
        var cards = deckCardService.listCards(deckId);
        var before = deckAnalysisService.analyze(deckId);
        var selection =
                new PlanSelection(
                        objective,
                        effectiveCurrency,
                        budget,
                        gaps(objective, before),
                        cardCatalogService.getAnalysisViewsByPrintingIds(printingIdsOf(cards)),
                        cardPriceService.latestPrices(printingIdsOf(cards)),
                        before);
        selectSubstitutions(deckId, targets(cards, selection), selection, effectiveMaxChanges);
        return new DeckUpgradePlan(
                objective,
                effectiveCurrency,
                budget,
                effectiveMaxChanges,
                selection.substitutions(),
                beforeMetrics(before),
                selection.afterMetrics(before.legality().legal()));
    }

    private void selectSubstitutions(
            long deckId, List<DeckCardResponse> targets, PlanSelection selection, int maxChanges) {
        for (var target : targets) {
            if (selection.size() >= maxChanges) {
                return;
            }
            selectForTarget(deckId, target, selection);
        }
    }

    private void selectForTarget(long deckId, DeckCardResponse target, PlanSelection selection) {
        var alternatives =
                alternativeService.suggest(deckId, idOf(target), ALTERNATIVE_LIMIT, true);
        if (alternatives.isEmpty()) {
            return;
        }
        var alternativeIds = alternatives.stream().map(DeckCardAlternative::printingId).toList();
        var prices = cardPriceService.latestPrices(alternativeIds);
        var views = cardCatalogService.getAnalysisViewsByPrintingIds(alternativeIds);
        selection.substitute(target, alternatives, prices, views);
    }

    // ponytail: proposals only touch the main deck; swapping the commander would invalidate the
    // color identity every alternative was ranked against. Add commander upgrades if users ask.
    private static List<DeckCardResponse> targets(
            List<DeckCardResponse> cards, PlanSelection selection) {
        return cards.stream()
                .filter(card -> card.id() != null)
                .filter(card -> MAIN_DECK.equals(card.deckSection()))
                .filter(selection::isTarget)
                .sorted(Comparator.comparing(DeckCardResponse::id))
                .toList();
    }

    private static Set<String> gaps(Objective objective, DeckAnalysisResponse before) {
        if (objective != Objective.CLOSE_CATEGORY_GAPS) {
            return Set.of();
        }
        Set<String> gaps = new LinkedHashSet<>();
        for (String category : GAP_CATEGORIES) {
            if (before.functionalCategories().getOrDefault(category, 0) == 0) {
                gaps.add(category);
            }
        }
        return gaps;
    }

    private static List<Long> printingIdsOf(List<DeckCardResponse> cards) {
        return cards.stream().map(DeckCardResponse::cardPrintingId).toList();
    }

    private static long idOf(DeckCardResponse card) {
        return Objects.requireNonNull(card.id());
    }

    private static UpgradeMetrics beforeMetrics(DeckAnalysisResponse before) {
        return new UpgradeMetrics(
                before.ownershipBreakdown(),
                before.valueByCurrency(),
                before.missingCostByCurrency(),
                before.functionalCategories(),
                before.legality().legal());
    }

    private static String categoryOf(CardAnalysisView view) {
        var types = new StringBuilder();
        var text = new StringBuilder();
        appendLowercased(types, view.typeLine());
        view.faces()
                .forEach(
                        face -> {
                            appendLowercased(types, face.typeLine());
                            appendLowercased(text, face.oracleText());
                        });
        return CardCategorizer.categorizeText(types.toString(), text.toString()).name();
    }

    private static void appendLowercased(StringBuilder target, @Nullable String value) {
        if (value != null) {
            target.append(value.toLowerCase(Locale.ROOT)).append('\n');
        }
    }

    public enum Objective {
        REPLACE_PROXIES_WITH_OWNED,
        IMPROVE_UNDER_BUDGET,
        CLOSE_CATEGORY_GAPS
    }

    /** A bounded upgrade proposal: reasoned substitutions plus before/after metrics. */
    public record DeckUpgradePlan(
            Objective objective,
            String currency,
            @Nullable BigDecimal budget,
            int maxChanges,
            List<Substitution> substitutions,
            UpgradeMetrics before,
            UpgradeMetrics after) {

        public DeckUpgradePlan {
            substitutions = List.copyOf(substitutions);
        }
    }

    /** One proposed swap of a deck card for a ranked alternative. */
    public record Substitution(
            long deckCardId,
            long removedPrintingId,
            String removedName,
            String removedOwnershipStatus,
            int quantity,
            long addedPrintingId,
            String addedName,
            boolean addedOwned,
            @Nullable BigDecimal cost,
            List<ScoreContribution> reasons) {

        public Substitution {
            reasons = List.copyOf(reasons);
        }
    }

    /** Ownership, value, missing cost, and category counts for one deck state. */
    public record UpgradeMetrics(
            Map<String, Integer> ownershipBreakdown,
            Map<String, BigDecimal> valueByCurrency,
            Map<String, BigDecimal> missingCostByCurrency,
            Map<String, Integer> functionalCategories,
            boolean legal) {}

    /** Mutable per-plan accumulator: chosen substitutions plus the simulated after-state. */
    private static final class PlanSelection {

        private final Objective objective;
        private final String currency;
        @Nullable private final BigDecimal budget;
        private final Set<String> remainingGaps;
        private final Map<Long, CardAnalysisView> targetViews;
        private final Map<Long, CardPrice> targetPrices;
        private final List<Substitution> substitutions = new ArrayList<>();
        private final Set<String> addedNames = new HashSet<>();
        private final Map<String, Integer> ownership;
        private final Map<String, BigDecimal> value;
        private final Map<String, BigDecimal> missing;
        private final Map<String, Integer> categories;
        private BigDecimal spent = BigDecimal.ZERO;

        // Suppressed: cohesive per-plan state (objective, currency, budget, gaps, deck lookups,
        // baseline analysis); each part is consumed by the substitution loop.
        @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
        PlanSelection(
                Objective objective,
                String currency,
                @Nullable BigDecimal budget,
                Set<String> remainingGaps,
                Map<Long, CardAnalysisView> targetViews,
                Map<Long, CardPrice> targetPrices,
                DeckAnalysisResponse before) {
            this.objective = objective;
            this.currency = currency;
            this.budget = budget;
            this.remainingGaps = new LinkedHashSet<>(remainingGaps);
            this.targetViews = targetViews;
            this.targetPrices = targetPrices;
            this.ownership = new TreeMap<>(before.ownershipBreakdown());
            this.value = new TreeMap<>(before.valueByCurrency());
            this.missing = new TreeMap<>(before.missingCostByCurrency());
            this.categories = new TreeMap<>(before.functionalCategories());
        }

        int size() {
            return substitutions.size();
        }

        List<Substitution> substitutions() {
            return List.copyOf(substitutions);
        }

        boolean isTarget(DeckCardResponse card) {
            return switch (objective) {
                case REPLACE_PROXIES_WITH_OWNED -> PROXY.equals(card.ownershipStatus());
                case IMPROVE_UNDER_BUDGET -> true;
                case CLOSE_CATEGORY_GAPS -> !remainingGaps.isEmpty() && isSynergy(card);
            };
        }

        private boolean isSynergy(DeckCardResponse card) {
            var view = targetViews.get(card.cardPrintingId());
            return view != null && CardCategorizer.Category.SYNERGY.name().equals(categoryOf(view));
        }

        void substitute(
                DeckCardResponse target,
                List<DeckCardAlternative> alternatives,
                Map<Long, CardPrice> prices,
                Map<Long, CardAnalysisView> views) {
            for (var alternative : alternatives) {
                if (accepts(target, alternative, prices, views)) {
                    apply(target, alternative, prices, views);
                    return;
                }
            }
        }

        private boolean accepts(
                DeckCardResponse target,
                DeckCardAlternative alternative,
                Map<Long, CardPrice> prices,
                Map<Long, CardAnalysisView> views) {
            if (addedNames.contains(alternative.name())) {
                return false;
            }
            return switch (objective) {
                case REPLACE_PROXIES_WITH_OWNED -> alternative.owned();
                case IMPROVE_UNDER_BUDGET -> fitsBudget(target.quantity(), alternative, prices);
                case CLOSE_CATEGORY_GAPS ->
                        closesGap(target.quantity(), alternative, prices, views);
            };
        }

        private boolean closesGap(
                int quantity,
                DeckCardAlternative alternative,
                Map<Long, CardPrice> prices,
                Map<Long, CardAnalysisView> views) {
            var view = views.get(alternative.printingId());
            if (view == null || !remainingGaps.contains(categoryOf(view))) {
                return false;
            }
            return budget == null || fitsBudget(quantity, alternative, prices);
        }

        private boolean fitsBudget(
                int quantity, DeckCardAlternative alternative, Map<Long, CardPrice> prices) {
            var cost = unitCost(alternative, prices);
            return cost != null
                    && (budget == null
                            || spent.add(cost.multiply(BigDecimal.valueOf(quantity)))
                                            .compareTo(budget)
                                    <= 0);
        }

        private @Nullable BigDecimal unitCost(
                DeckCardAlternative alternative, Map<Long, CardPrice> prices) {
            if (alternative.owned()) {
                return BigDecimal.ZERO;
            }
            var price = prices.get(alternative.printingId());
            return price == null ? null : priceOf(price, currency);
        }

        private static @Nullable BigDecimal priceOf(CardPrice price, String currency) {
            return switch (currency) {
                case "usdFoil" -> price.usdFoil();
                case "eur" -> price.eur();
                case "tix" -> price.tix();
                default -> price.usd();
            };
        }

        private void apply(
                DeckCardResponse target,
                DeckCardAlternative alternative,
                Map<Long, CardPrice> prices,
                Map<Long, CardAnalysisView> views) {
            int quantity = target.quantity();
            var cost = unitCost(alternative, prices);
            substitutions.add(
                    new Substitution(
                            idOf(target),
                            target.cardPrintingId(),
                            target.card().name(),
                            target.ownershipStatus(),
                            quantity,
                            alternative.printingId(),
                            alternative.name(),
                            alternative.owned(),
                            cost,
                            alternative.contributions()));
            if (cost != null) {
                spent = spent.add(cost.multiply(BigDecimal.valueOf(quantity)));
            }
            addedNames.add(alternative.name());
            applyMetrics(target, alternative, quantity, prices, views);
        }

        private void applyMetrics(
                DeckCardResponse target,
                DeckCardAlternative alternative,
                int quantity,
                Map<Long, CardPrice> prices,
                Map<Long, CardAnalysisView> views) {
            mergeCount(ownership, target.ownershipStatus(), -quantity);
            mergeCount(ownership, OWNED, quantity);
            var removedView = targetViews.get(target.cardPrintingId());
            if (removedView != null) {
                mergeCount(categories, categoryOf(removedView), -quantity);
            }
            var addedView = views.get(alternative.printingId());
            if (addedView != null) {
                mergeCount(categories, categoryOf(addedView), quantity);
                remainingGaps.remove(categoryOf(addedView));
            }
            applyAmounts(value, targetPrices.get(target.cardPrintingId()), -quantity);
            if (!OWNED.equals(target.ownershipStatus())) {
                applyAmounts(missing, targetPrices.get(target.cardPrintingId()), -quantity);
            }
            applyAmounts(value, prices.get(alternative.printingId()), quantity);
        }

        private static void mergeCount(Map<String, Integer> map, String key, int delta) {
            map.merge(key, delta, Integer::sum);
        }

        private static void applyAmounts(
                Map<String, BigDecimal> map, @Nullable CardPrice price, int signedQuantity) {
            if (price == null) {
                return;
            }
            addAmount(map, "usd", price.usd(), signedQuantity);
            addAmount(map, "usdFoil", price.usdFoil(), signedQuantity);
            addAmount(map, "eur", price.eur(), signedQuantity);
            addAmount(map, "tix", price.tix(), signedQuantity);
        }

        private static void addAmount(
                Map<String, BigDecimal> map,
                String currency,
                @Nullable BigDecimal amount,
                int qty) {
            if (amount != null) {
                map.merge(currency, amount.multiply(BigDecimal.valueOf(qty)), BigDecimal::add);
            }
        }

        UpgradeMetrics afterMetrics(boolean legal) {
            return new UpgradeMetrics(
                    positiveCounts(ownership),
                    positiveAmounts(value),
                    positiveAmounts(missing),
                    positiveCounts(categories),
                    legal);
        }

        private static Map<String, Integer> positiveCounts(Map<String, Integer> source) {
            Map<String, Integer> cleaned = new TreeMap<>(source);
            cleaned.values().removeIf(count -> count <= 0);
            return cleaned;
        }

        private static Map<String, BigDecimal> positiveAmounts(Map<String, BigDecimal> source) {
            Map<String, BigDecimal> cleaned = new TreeMap<>(source);
            cleaned.values().removeIf(amount -> amount.compareTo(BigDecimal.ZERO) <= 0);
            return cleaned;
        }
    }
}
