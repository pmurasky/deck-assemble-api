package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class CommanderSuggestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommanderSuggestionService.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
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
        var ownedCards = ownedCards();
        var ownedOracleIds = ownedCards.keySet();
        return ownedCards.values().stream()
                .filter(CommanderSuggestionService::eligibleCommander)
                .map(commander -> suggestionFor(commander, ownedOracleIds))
                .flatMap(java.util.Optional::stream)
                .sorted(SUGGESTION_ORDER)
                .toList();
    }

    private Map<String, Card> ownedCards() {
        var profileId = profileId();
        var ownedPrintingIds = collectionService.getOwnedPrintingIds(profileId);
        var cardsByOracle = new LinkedHashMap<String, Card>();
        cardCatalogService
                .getCardsWithFacesByPrintingIds(ownedPrintingIds)
                .values()
                .forEach(card -> cardsByOracle.putIfAbsent(card.getScryfallOracleId(), card));
        return cardsByOracle;
    }

    private java.util.Optional<CommanderSuggestion> suggestionFor(
            Card commander, Set<String> ownedOracleIds) {
        try {
            var scores =
                    edhrecCommanderService.getCardScores(
                            commander.getScryfallOracleId(), commander.getName());
            return scores.isEmpty()
                    ? java.util.Optional.empty()
                    : java.util.Optional.of(evaluate(commander, scores, ownedOracleIds));
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Skipping commander suggestion for {}; EDHREC is unavailable",
                    commander.getName());
            return java.util.Optional.empty();
        }
    }

    private CommanderSuggestion evaluate(
            Card commander, Map<String, CardScore> scores, Set<String> ownedOracleIds) {
        var cardsByName = cardsByName(scores);
        var missing = missingCards(scores, cardsByName, ownedOracleIds);
        var prices = pricesFor(missing);
        var ownedCount = scores.size() - missing.size();
        return response(commander, scores.size(), ownedCount, missing, prices);
    }

    private Map<String, Card> cardsByName(Map<String, CardScore> scores) {
        var cardsByName = new HashMap<String, Card>();
        cardCatalogService
                .getCardsByNames(scores.keySet())
                .forEach(card -> cardsByName.put(card.getName(), card));
        return cardsByName;
    }

    private List<MissingCard> missingCards(
            Map<String, CardScore> scores,
            Map<String, Card> cardsByName,
            Set<String> ownedOracleIds) {
        var missing = new ArrayList<MissingCard>();
        var printingIds =
                cardCatalogService.getLatestPrintingIdByCardIds(
                        cardsByName.values().stream().map(Card::getId).toList());
        for (var name : scores.keySet()) {
            Card card = cardsByName.get(name);
            if (card == null || !ownedOracleIds.contains(card.getScryfallOracleId())) {
                missing.add(new MissingCard(card == null ? null : printingIds.get(card.getId())));
            }
        }
        return missing;
    }

    private Map<Long, CardPrice> pricesFor(List<MissingCard> missing) {
        return cardPriceService.latestPrices(
                missing.stream()
                        .map(MissingCard::printingId)
                        .filter(java.util.Objects::nonNull)
                        .toList());
    }

    private static CommanderSuggestion response(
            Card commander,
            int totalCards,
            int ownedCards,
            List<MissingCard> missing,
            Map<Long, CardPrice> prices) {
        var priceSummary = summarizePrices(missing, prices);
        var coverage =
                BigDecimal.valueOf(ownedCards)
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

    private static boolean eligibleCommander(Card card) {
        if (!Boolean.TRUE.equals(card.getActive())) {
            return false;
        }
        var text = new StringBuilder();
        card.getFaces()
                .forEach(
                        face ->
                                appendCommanderText(
                                        text, face.getTypeLine(), face.getOracleText()));
        return text.toString().contains("legendary creature")
                || text.toString().contains("can be your commander");
    }

    private static void appendCommanderText(
            StringBuilder text, @Nullable String typeLine, @Nullable String oracleText) {
        if (typeLine != null) {
            text.append(typeLine.toLowerCase(java.util.Locale.ROOT)).append(' ');
        }
        if (oracleText != null) {
            text.append(oracleText.toLowerCase(java.util.Locale.ROOT)).append(' ');
        }
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

    private record PriceSummary(BigDecimal cost, int unpricedCount) {}
}
