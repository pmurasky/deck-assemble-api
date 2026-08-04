package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CommanderEligibility;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class CommanderSuggestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommanderSuggestionService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    // ponytail: fixed in-memory TTL; evict on collection mutation if staleness ever matters.
    private static final long CACHE_TTL_MINUTES = 15;
    private static final long CACHE_TTL_MILLIS = Duration.ofMinutes(CACHE_TTL_MINUTES).toMillis();
    private static final Comparator<CommanderSuggestion> SUGGESTION_ORDER =
            Comparator.comparing(CommanderSuggestion::coveragePercent)
                    .reversed()
                    .thenComparing(CommanderSuggestion::estimatedCompletionCostUsd)
                    .thenComparing(
                            CommanderSuggestion::commanderRank,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(CommanderSuggestion::commanderName);

    private final CardCatalogService cardCatalogService;
    private final CardPriceService cardPriceService;
    private final CollectionService collectionService;
    private final EdhrecCommanderService edhrecCommanderService;
    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final Map<Long, CachedSuggestions> cache = new ConcurrentHashMap<>();

    // checkstyle:ParameterNumber suppressed: this service coordinates existing module boundaries.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public CommanderSuggestionService(
            CardCatalogService cardCatalogService,
            CardPriceService cardPriceService,
            CollectionService collectionService,
            EdhrecCommanderService edhrecCommanderService,
            CurrentUser currentUser,
            ProfileService profileService) {
        this.cardCatalogService = cardCatalogService;
        this.cardPriceService = cardPriceService;
        this.collectionService = collectionService;
        this.edhrecCommanderService = edhrecCommanderService;
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    public List<CommanderSuggestion> suggest() {
        var profileId = profileId();
        var cached = cache.get(profileId);
        if (cached != null && !cached.isExpired()) {
            return cached.suggestions();
        }
        var ownedCards = ownedCards(profileId);
        var result = suggestions(scoresByCommander(ownedCards.values()), ownedCards.keySet());
        cache.put(profileId, new CachedSuggestions(result, System.currentTimeMillis()));
        return result;
    }

    private List<CommanderSuggestion> suggestions(
            Map<Card, Map<String, CardScore>> scoresByCommander, Set<String> ownedOracleIds) {
        if (scoresByCommander.isEmpty()) {
            return List.of();
        }
        var cardsByName = cardsByName(allScoreNames(scoresByCommander.values()));
        var printingIds = latestPrintingIds(cardsByName.values());
        var evaluations = evaluations(scoresByCommander, cardsByName, printingIds, ownedOracleIds);
        var prices = pricesFor(evaluations.stream().map(CommanderEvaluation::missing).toList());
        return evaluations.stream()
                .map(
                        evaluation ->
                                response(
                                        evaluation.commander(),
                                        evaluation.scores().size(),
                                        evaluation.missing(),
                                        prices))
                .sorted(SUGGESTION_ORDER)
                .toList();
    }

    private Map<String, Card> ownedCards(long profileId) {
        var ownedPrintingIds = collectionService.getOwnedPrintingIds(profileId);
        var cardsByOracle = new LinkedHashMap<String, Card>();
        cardCatalogService
                .getCardsByPrintingIds(ownedPrintingIds)
                .values()
                .forEach(card -> cardsByOracle.putIfAbsent(card.getScryfallOracleId(), card));
        return cardsByOracle;
    }

    private Map<Card, Map<String, CardScore>> scoresByCommander(Collection<Card> ownedCards) {
        var scoresByCommander = new LinkedHashMap<Card, Map<String, CardScore>>();
        ownedCards.stream()
                .filter(card -> Boolean.TRUE.equals(card.getActive()))
                .filter(CommanderEligibility::isEligible)
                .forEach(
                        commander ->
                                scoresFor(commander)
                                        .ifPresent(
                                                scores ->
                                                        scoresByCommander.put(commander, scores)));
        return scoresByCommander;
    }

    private Optional<Map<String, CardScore>> scoresFor(Card commander) {
        try {
            var scores =
                    edhrecCommanderService.getCardScores(
                            commander.getScryfallOracleId(), commander.getName());
            return scores.isEmpty() ? Optional.empty() : Optional.of(scores);
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Skipping commander suggestion for {}; EDHREC is unavailable",
                    commander.getName());
            return Optional.empty();
        }
    }

    private static Set<String> allScoreNames(Collection<Map<String, CardScore>> allScores) {
        var names = new LinkedHashSet<String>();
        allScores.forEach(scores -> names.addAll(scores.keySet()));
        return names;
    }

    private Map<String, Card> cardsByName(Set<String> names) {
        var cardsByName = new LinkedHashMap<String, Card>();
        cardCatalogService
                .getCardsByNames(names)
                .forEach(card -> cardsByName.put(card.getName(), card));
        return cardsByName;
    }

    private Map<Long, Long> latestPrintingIds(Collection<Card> cards) {
        return cardCatalogService.getLatestPrintingIdByCardIds(
                cards.stream().map(Card::getId).toList());
    }

    private List<CommanderEvaluation> evaluations(
            Map<Card, Map<String, CardScore>> scoresByCommander,
            Map<String, Card> cardsByName,
            Map<Long, Long> printingIds,
            Set<String> ownedOracleIds) {
        var evaluations = new ArrayList<CommanderEvaluation>();
        scoresByCommander.forEach(
                (commander, scores) ->
                        evaluations.add(
                                new CommanderEvaluation(
                                        commander,
                                        scores,
                                        missingCards(
                                                scores,
                                                cardsByName,
                                                printingIds,
                                                ownedOracleIds))));
        return evaluations;
    }

    private List<MissingCard> missingCards(
            Map<String, CardScore> scores,
            Map<String, Card> cardsByName,
            Map<Long, Long> printingIds,
            Set<String> ownedOracleIds) {
        var missing = new ArrayList<MissingCard>();
        for (var name : scores.keySet()) {
            Card card = cardsByName.get(name);
            if (card == null || !ownedOracleIds.contains(card.getScryfallOracleId())) {
                missing.add(new MissingCard(card == null ? null : printingIds.get(card.getId())));
            }
        }
        return missing;
    }

    private Map<Long, CardPrice> pricesFor(Collection<List<MissingCard>> missingLists) {
        return cardPriceService.latestPrices(
                missingLists.stream()
                        .flatMap(List::stream)
                        .map(MissingCard::printingId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList());
    }

    private static CommanderSuggestion response(
            Card commander,
            int totalCards,
            List<MissingCard> missing,
            Map<Long, CardPrice> prices) {
        var priceSummary = summarizePrices(missing, prices);
        var coverage =
                BigDecimal.valueOf(totalCards - missing.size())
                        .multiply(HUNDRED)
                        .divide(BigDecimal.valueOf(totalCards), 2, RoundingMode.HALF_UP);
        return new CommanderSuggestion(
                commander.getId(),
                commander.getName(),
                commander.getColorIdentity(),
                coverage,
                missing.size(),
                priceSummary.cost(),
                priceSummary.unpricedCount(),
                commander.getCommanderRank());
    }

    private static PriceSummary summarizePrices(
            List<MissingCard> missing, Map<Long, CardPrice> prices) {
        var cost = BigDecimal.ZERO;
        var unpriced = 0;
        for (var card : missing) {
            BigDecimal price =
                    card.printingId() == null ? null : usd(prices.get(card.printingId()));
            if (price == null) {
                unpriced++;
            } else {
                cost = cost.add(price);
            }
        }
        return new PriceSummary(cost, unpriced);
    }

    private static @Nullable BigDecimal usd(@Nullable CardPrice price) {
        return price == null ? null : price.usd();
    }

    private long profileId() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }

    private record MissingCard(@Nullable Long printingId) {}

    private record CachedSuggestions(List<CommanderSuggestion> suggestions, long createdAtMillis) {
        private boolean isExpired() {
            return createdAtMillis + CACHE_TTL_MILLIS < System.currentTimeMillis();
        }
    }

    private record CommanderEvaluation(
            Card commander, Map<String, CardScore> scores, List<MissingCard> missing) {}

    private record PriceSummary(BigDecimal cost, int unpricedCount) {}
}
