package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Selects deck-build candidate cards: loads EDHREC scores for the commander, collects candidates
 * from the player's collection or the optimal EDHREC pool, orders them by score, and applies
 * game-changer and budget limits.
 */
@Service
public class DeckCandidateSelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckCandidateSelector.class);
    private static final int LOW_POWER_MAX = 4;
    private static final int MEDIUM_POWER_MAX = 6;
    private static final int UNLIMITED_GAME_CHANGERS = Integer.MAX_VALUE;
    private static final int BRACKET_THREE_GAME_CHANGERS = 3;
    private static final Comparator<DeckCandidate> SCORE_ORDER =
            Comparator.comparing(DeckCandidate::hasScore)
                    .reversed()
                    .thenComparing(Comparator.comparingDouble(DeckCandidate::scoreValue).reversed())
                    .thenComparing(
                            Comparator.comparingLong(DeckCandidate::inclusionValue).reversed())
                    .thenComparing(candidate -> candidate.card().getName());

    private final CardCatalogService cardCatalogService;
    private final CollectionService collectionService;
    private final EdhrecCommanderService edhrecCommanderService;
    private final CardCategorizer categorizer;
    private final CardPriceService cardPriceService;

    public DeckCandidateSelector(
            CardCatalogService cardCatalogService,
            CollectionService collectionService,
            EdhrecCommanderService edhrecCommanderService,
            CardCategorizer categorizer,
            CardPriceService cardPriceService) {
        this.cardCatalogService = cardCatalogService;
        this.collectionService = collectionService;
        this.edhrecCommanderService = edhrecCommanderService;
        this.categorizer = categorizer;
        this.cardPriceService = cardPriceService;
    }

    public List<DeckCandidate> select(
            DeckBuildRequest request, List<Card> commanders, Set<String> identity, long profileId) {
        var ownedPrintingIds = collectionService.getOwnedPrintingIds(profileId);
        var commanderOracles =
                commanders.stream().map(Card::getScryfallOracleId).collect(Collectors.toSet());
        var scores = loadScores(commanders.get(0));
        var candidates =
                ownedOnly(request)
                        ? collectCandidates(
                                ownedPrintingIds, commanderOracles, identity, scores, request)
                        : collectOptimalCandidates(
                                commanderOracles, identity, scores, request, ownedPrintingIds);
        candidates.sort(SCORE_ORDER);
        candidates = withinGameChangerLimit(candidates, request.desiredPowerLevel());
        if (request.budgetLimit() != null) {
            return withinBudget(candidates, ownedPrintingIds, request.budgetLimit());
        }
        return candidates;
    }

    static boolean ownedOnly(DeckBuildRequest request) {
        return !Boolean.FALSE.equals(request.useOwnedCardsOnly());
    }

    private static List<DeckCandidate> withinGameChangerLimit(
            List<DeckCandidate> candidates, @Nullable Integer desiredPowerLevel) {
        var allowed = allowedGameChangers(desiredPowerLevel);
        if (allowed == UNLIMITED_GAME_CHANGERS) {
            return candidates;
        }
        var kept = new ArrayList<DeckCandidate>();
        var included = 0;
        for (var candidate : candidates) {
            if (!Boolean.TRUE.equals(candidate.card().getGameChanger()) || included++ < allowed) {
                kept.add(withGameChangerPolicy(candidate, allowed, desiredPowerLevel));
            }
        }
        return kept;
    }

    private static DeckCandidate withGameChangerPolicy(
            DeckCandidate candidate, int allowed, @Nullable Integer desiredPowerLevel) {
        if (!Boolean.TRUE.equals(candidate.card().getGameChanger())) {
            return candidate;
        }
        Map<String, String> evidence = new HashMap<>();
        evidence.put("allowedGameChangers", String.valueOf(allowed));
        if (desiredPowerLevel != null) {
            evidence.put("desiredPowerLevel", desiredPowerLevel.toString());
        }
        return candidate.withContribution(
                new ScoreContribution(
                        RecommendationReasonCode.GAME_CHANGER_POLICY, BigDecimal.ZERO, evidence));
    }

    private static int allowedGameChangers(@Nullable Integer desiredPowerLevel) {
        if (desiredPowerLevel == null || desiredPowerLevel > MEDIUM_POWER_MAX) {
            return UNLIMITED_GAME_CHANGERS;
        }
        return desiredPowerLevel > LOW_POWER_MAX ? BRACKET_THREE_GAME_CHANGERS : 0;
    }

    private List<DeckCandidate> withinBudget(
            List<DeckCandidate> candidates, Set<Long> ownedPrintingIds, BigDecimal budget) {
        var unownedIds =
                candidates.stream()
                        .map(DeckCandidate::printingId)
                        .filter(printingId -> !ownedPrintingIds.contains(printingId))
                        .toList();
        var prices = cardPriceService.latestPrices(unownedIds);
        var cost = BigDecimal.ZERO;
        var kept = new ArrayList<DeckCandidate>();
        for (var candidate : candidates) {
            var unit = unitPrice(prices.get(candidate.printingId()));
            var owned = ownedPrintingIds.contains(candidate.printingId());
            if (owned || cost.add(unit).compareTo(budget) <= 0) {
                cost = owned ? cost : cost.add(unit);
                kept.add(owned ? candidate : withBudgetEvidence(candidate, unit, cost, budget));
            }
        }
        return kept;
    }

    private static DeckCandidate withBudgetEvidence(
            DeckCandidate candidate, BigDecimal unit, BigDecimal cost, BigDecimal budget) {
        return candidate.withContribution(
                new ScoreContribution(
                        RecommendationReasonCode.BUDGET,
                        BigDecimal.ZERO,
                        Map.of(
                                "unitPrice", unit.toPlainString(),
                                "runningCost", cost.toPlainString(),
                                "budgetLimit", budget.toPlainString())));
    }

    private static BigDecimal unitPrice(@Nullable CardPrice price) {
        return price == null || price.usd() == null ? BigDecimal.ZERO : price.usd();
    }

    private List<DeckCandidate> collectOptimalCandidates(
            Set<String> commanderOracles,
            Set<String> identity,
            Map<String, CardScore> scores,
            DeckBuildRequest request,
            Set<Long> ownedPrintingIds) {
        var cardsByName = new HashMap<String, Card>();
        cardCatalogService
                .getCardsByNames(scores.keySet())
                .forEach(card -> cardsByName.put(card.getName(), card));
        var printingIds =
                cardCatalogService.getLatestPrintingIdByCardIds(
                        cardsByName.values().stream().map(Card::getId).toList());
        var seen = new HashSet<String>();
        var candidates = new ArrayList<DeckCandidate>();
        for (var card : cardsByName.values()) {
            var printingId = printingIds.get(card.getId());
            if (printingId != null
                    && DeckCandidate.isEligible(card, commanderOracles, identity)
                    && seen.add(card.getScryfallOracleId())) {
                candidates.add(toCandidate(printingId, card, scores, request, ownedPrintingIds));
            }
        }
        return candidates;
    }

    private DeckCandidate toCandidate(
            long printingId,
            Card card,
            Map<String, CardScore> scores,
            DeckBuildRequest request,
            Set<Long> ownedPrintingIds) {
        var category = categorizer.categorize(card);
        var score = scores.get(card.getName());
        var contributions =
                explain(card, category, score, request, ownedPrintingIds.contains(printingId));
        return new DeckCandidate(printingId, card, category, score, contributions);
    }

    private static List<ScoreContribution> explain(
            Card card,
            Category category,
            @Nullable CardScore score,
            DeckBuildRequest request,
            boolean owned) {
        var contributions = new ArrayList<ScoreContribution>();
        contributions.add(categoryNeed(category));
        if (owned) {
            contributions.add(ownedMarker());
        }
        if (score != null) {
            contributions.add(commanderSynergy(score));
        }
        comboLists(score).forEach(list -> contributions.add(combo(list)));
        if (matchesPlayStyle(card, request.playStyle())) {
            contributions.add(playStyleMarker(request.playStyle()));
        }
        return List.copyOf(contributions);
    }

    private static ScoreContribution ownedMarker() {
        return new ScoreContribution(
                RecommendationReasonCode.OWNED, BigDecimal.ZERO, Map.of("source", "collection"));
    }

    private static ScoreContribution playStyleMarker(String playStyle) {
        return new ScoreContribution(
                RecommendationReasonCode.PLAY_STYLE,
                BigDecimal.ZERO,
                Map.of("playStyle", playStyle));
    }

    private static ScoreContribution categoryNeed(Category category) {
        Map<String, String> evidence = new HashMap<>();
        evidence.put("category", category.name());
        var quota = DeckDraftPicker.QUOTAS.get(category);
        if (quota != null) {
            evidence.put("quota", quota.toString());
        }
        return new ScoreContribution(
                RecommendationReasonCode.CATEGORY_NEED, BigDecimal.ZERO, evidence);
    }

    private static ScoreContribution commanderSynergy(CardScore score) {
        var points = score.synergy() == null ? BigDecimal.ZERO : BigDecimal.valueOf(score.synergy());
        Map<String, String> evidence = new HashMap<>();
        if (score.synergy() != null) {
            evidence.put("synergy", score.synergy().toString());
        }
        if (score.inclusion() != null) {
            evidence.put("inclusion", score.inclusion().toString());
        }
        return new ScoreContribution(
                RecommendationReasonCode.COMMANDER_SYNERGY, points, evidence);
    }

    private static List<String> comboLists(@Nullable CardScore score) {
        if (score == null) {
            return List.of();
        }
        return score.cardlists().stream()
                .filter(list -> list.toLowerCase(Locale.ROOT).contains("combo"))
                .toList();
    }

    private static ScoreContribution combo(String cardlist) {
        return new ScoreContribution(
                RecommendationReasonCode.COMBO,
                BigDecimal.ZERO,
                Map.of("edhrecCardlist", cardlist));
    }

    private static boolean matchesPlayStyle(Card card, @Nullable String playStyle) {
        if (playStyle == null || playStyle.isBlank()) {
            return false;
        }
        var needle = playStyle.toLowerCase(Locale.ROOT);
        for (CardFace face : card.getFaces()) {
            if (face.getOracleText() != null
                    && face.getOracleText().toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private List<DeckCandidate> collectCandidates(
            Set<Long> ownedPrintingIds,
            Set<String> commanderOracles,
            Set<String> identity,
            Map<String, CardScore> scores,
            DeckBuildRequest request) {
        var seen = new HashSet<String>();
        var candidates = new ArrayList<DeckCandidate>();
        for (var entry : cardCatalogService.getCardsByPrintingIds(ownedPrintingIds).entrySet()) {
            var card = entry.getValue();
            if (DeckCandidate.isEligible(card, commanderOracles, identity)
                    && seen.add(card.getScryfallOracleId())) {
                candidates.add(
                        toCandidate(entry.getKey(), card, scores, request, ownedPrintingIds));
            }
        }
        return candidates;
    }

    private Map<String, CardScore> loadScores(Card commander) {
        try {
            return edhrecCommanderService.getCardScores(
                    commander.getScryfallOracleId(), commander.getName());
        } catch (RestClientException exception) {
            LOGGER.warn("EDHREC unavailable for {}; building without scores", commander.getName());
            return Map.of();
        }
    }
}
