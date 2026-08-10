package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.CollectionService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
                kept.add(
                        CandidateScoreExplainer.withGameChangerPolicy(
                                candidate, allowed, desiredPowerLevel));
            }
        }
        return kept;
    }

    private static int allowedGameChangers(@Nullable Integer desiredPowerLevel) {
        if (desiredPowerLevel == null || desiredPowerLevel > MEDIUM_POWER_MAX) {
            return UNLIMITED_GAME_CHANGERS;
        }
        return desiredPowerLevel > LOW_POWER_MAX ? BRACKET_THREE_GAME_CHANGERS : 0;
    }

    private List<DeckCandidate> withinBudget(
            List<DeckCandidate> candidates, Set<Long> ownedPrintingIds, BigDecimal budget) {
        var prices =
                cardPriceService.latestPrices(unownedPrintingIds(candidates, ownedPrintingIds));
        var cost = BigDecimal.ZERO;
        var kept = new ArrayList<DeckCandidate>();
        for (var candidate : candidates) {
            var unit = unitPrice(prices.get(candidate.printingId()));
            var owned = ownedPrintingIds.contains(candidate.printingId());
            if (owned || cost.add(unit).compareTo(budget) <= 0) {
                cost = owned ? cost : cost.add(unit);
                kept.add(
                        owned
                                ? candidate
                                : CandidateScoreExplainer.withBudgetEvidence(
                                        candidate, unit, cost, budget));
            }
        }
        return kept;
    }

    private static List<Long> unownedPrintingIds(
            List<DeckCandidate> candidates, Set<Long> ownedPrintingIds) {
        return candidates.stream()
                .map(DeckCandidate::printingId)
                .filter(printingId -> !ownedPrintingIds.contains(printingId))
                .toList();
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
        var roles = categorizer.categorizeAll(card);
        var score = scores.get(card.getName());
        var contributions =
                CandidateScoreExplainer.explain(
                        card, category, score, request, ownedPrintingIds.contains(printingId));
        return new DeckCandidate(printingId, card, category, score, contributions, roles);
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
