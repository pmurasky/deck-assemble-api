package com.deckassemble.decks.application.alternatives;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.DeckComboService;
import com.deckassemble.decks.application.OwnershipChecker;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.recommendations.application.CardCategorizer;
import com.deckassemble.recommendations.application.CardScore;
import com.deckassemble.recommendations.application.CommanderResolver;
import com.deckassemble.recommendations.application.DeckCandidate;
import com.deckassemble.recommendations.application.EdhrecCommanderService;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/** Suggests ranked replacement cards for a deck card, owned alternatives first on request. */
@Service
public class DeckCardAlternativeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckCardAlternativeService.class);

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final CardCatalogService cardCatalogService;
    private final EdhrecCommanderService edhrecCommanderService;
    private final CardCategorizer categorizer;
    private final OwnershipChecker ownershipChecker;
    private final CardPriceService cardPriceService;
    private final DeckComboService deckComboService;

    // Suppressed: cohesive collaborator set for alternatives (access, catalog, scores, ownership,
    // prices, combos); each is consumed by a distinct ranking factor.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckCardAlternativeService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            CardCatalogService cardCatalogService,
            EdhrecCommanderService edhrecCommanderService,
            CardCategorizer categorizer,
            OwnershipChecker ownershipChecker,
            CardPriceService cardPriceService,
            DeckComboService deckComboService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.cardCatalogService = cardCatalogService;
        this.edhrecCommanderService = edhrecCommanderService;
        this.categorizer = categorizer;
        this.ownershipChecker = ownershipChecker;
        this.cardPriceService = cardPriceService;
        this.deckComboService = deckComboService;
    }

    public List<DeckCardAlternative> suggest(
            long deckId, long deckCardId, int limit, boolean ownedFirst) {
        var deck = deckAccessGuard.owned(deckId);
        var target = targetCard(deckId, deckCardId);
        var commanders = commanders(deck);
        var scores =
                commanders.isEmpty()
                        ? Map.<String, CardScore>of()
                        : loadScores(commanders.getFirst());
        var candidates = enrich(deckId, target.getName(), eligible(commanders, target, scores));
        var ranked =
                DeckCardAlternativeRanker.rank(
                        candidates, target, categorizer.categorize(target), ownedFirst);
        return ranked.subList(0, Math.min(limit, ranked.size()));
    }

    private Card targetCard(long deckId, long deckCardId) {
        var deckCard =
                deckCardRepository
                        .findByIdAndDeckId(deckCardId, deckId)
                        .orElseThrow(DeckCardNotFoundException::new);
        var card =
                cardCatalogService
                        .getCardsByPrintingIds(Set.of(deckCard.getCardPrintingId()))
                        .get(deckCard.getCardPrintingId());
        if (card == null) {
            throw new DeckCardNotFoundException();
        }
        return card;
    }

    private List<Card> commanders(Deck deck) {
        var commanders = new ArrayList<Card>();
        if (deck.getCommanderCardId() != null) {
            commanders.add(cardCatalogService.getCardWithFaces(deck.getCommanderCardId()));
        }
        if (deck.getSecondaryCommanderCardId() != null) {
            commanders.add(cardCatalogService.getCardWithFaces(deck.getSecondaryCommanderCardId()));
        }
        return commanders;
    }

    private Map<String, CardScore> loadScores(Card commander) {
        try {
            return edhrecCommanderService.getCardScores(
                    commander.getScryfallOracleId(), commander.getName());
        } catch (RestClientException exception) {
            LOGGER.warn("EDHREC unavailable for {}; no alternatives", commander.getName());
            return Map.of();
        }
    }

    private List<AlternativeCandidate> eligible(
            List<Card> commanders, Card target, Map<String, CardScore> scores) {
        if (scores.isEmpty()) {
            return List.of();
        }
        var identity = CommanderResolver.colorIdentity(commanders);
        var excluded = excludedOracles(commanders, target);
        var cardsByName = cardsByName(scores);
        var printingIds = cardCatalogService.getLatestPrintingIdByCardIds(cardIds(cardsByName));
        var candidates = new ArrayList<AlternativeCandidate>();
        for (var entry : cardsByName.entrySet()) {
            var score = scores.get(entry.getKey());
            if (score != null) {
                toCandidate(entry.getValue(), score, printingIds, excluded, identity)
                        .ifPresent(candidates::add);
            }
        }
        return candidates;
    }

    private Optional<AlternativeCandidate> toCandidate(
            Card card,
            CardScore score,
            Map<Long, Long> printingIds,
            Set<String> excluded,
            Set<String> identity) {
        var printingId = printingIds.get(card.getId());
        if (printingId == null
                || excluded.contains(card.getScryfallOracleId())
                || !DeckCandidate.isEligible(card, Set.of(), identity)) {
            return Optional.empty();
        }
        return Optional.of(
                new AlternativeCandidate(
                        printingId, card, categorizer.categorize(card), score, false, null, false));
    }

    private List<AlternativeCandidate> enrich(
            long deckId, String targetName, List<AlternativeCandidate> eligible) {
        if (eligible.isEmpty()) {
            return List.of();
        }
        var printingIds = eligible.stream().map(AlternativeCandidate::printingId).toList();
        var owned =
                ownershipChecker.filterOwnedPrintingIds(deckAccessGuard.profileId(), printingIds);
        var prices = cardPriceService.latestPrices(printingIds);
        var breakers = comboBreakers(deckId, targetName, names(eligible));
        return eligible.stream()
                .map(candidate -> enrich(candidate, owned, prices, breakers))
                .toList();
    }

    private static AlternativeCandidate enrich(
            AlternativeCandidate candidate,
            Set<Long> owned,
            Map<Long, CardPrice> prices,
            Set<String> breakers) {
        var price = prices.get(candidate.printingId());
        return new AlternativeCandidate(
                candidate.printingId(),
                candidate.card(),
                candidate.category(),
                candidate.score(),
                owned.contains(candidate.printingId()),
                price == null ? null : price.usd(),
                breakers.contains(candidate.card().getName()));
    }

    private Set<String> comboBreakers(long deckId, String targetName, Set<String> candidateNames) {
        var response = deckComboService.getCombos(deckId);
        if (!response.available()) {
            return Set.of();
        }
        var targetCombos =
                response.combos().stream()
                        .filter(combo -> combo.cards().contains(targetName))
                        .toList();
        return candidateNames.stream()
                .filter(name -> breaksAny(targetCombos, name))
                .collect(Collectors.toSet());
    }

    private static boolean breaksAny(List<SpellbookCombo> targetCombos, String candidateName) {
        return targetCombos.stream().anyMatch(combo -> !combo.cards().contains(candidateName));
    }

    private Map<String, Card> cardsByName(Map<String, CardScore> scores) {
        var cardsByName = new HashMap<String, Card>();
        cardCatalogService
                .getCardsByNames(scores.keySet())
                .forEach(card -> cardsByName.put(card.getName(), card));
        return cardsByName;
    }

    private static Set<String> excludedOracles(List<Card> commanders, Card target) {
        var oracles =
                commanders.stream()
                        .map(Card::getScryfallOracleId)
                        .collect(Collectors.toCollection(HashSet::new));
        oracles.add(target.getScryfallOracleId());
        return oracles;
    }

    private static List<Long> cardIds(Map<String, Card> cardsByName) {
        return cardsByName.values().stream().map(Card::getId).toList();
    }

    private static Set<String> names(List<AlternativeCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> candidate.card().getName())
                .collect(Collectors.toSet());
    }
}
