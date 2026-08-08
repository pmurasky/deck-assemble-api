package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.CommanderPairingRules;
import com.deckassemble.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
// Justified: query facade over card repositories; methods are thin delegations (tracked in #3).
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
public class CardCatalogService {

    private final CardRepository cardRepository;
    private final CardPrintingRepository cardPrintingRepository;
    private final CommanderPairingRules pairingRules;
    private final CurrentUser currentUser;
    private final CardOwnershipLookup cardOwnershipLookup;
    private final CardPriceService cardPriceService;

    // Suppressed: cohesive catalog-facade collaborators (repositories, pairing rules, owned-
    // quantity/price lookups); each is an independent dependency consumed by search().
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public CardCatalogService(
            CardRepository cardRepository,
            CardPrintingRepository cardPrintingRepository,
            CommanderPairingRules pairingRules,
            CurrentUser currentUser,
            CardOwnershipLookup cardOwnershipLookup,
            CardPriceService cardPriceService) {
        this.cardRepository = cardRepository;
        this.cardPrintingRepository = cardPrintingRepository;
        this.pairingRules = pairingRules;
        this.currentUser = currentUser;
        this.cardOwnershipLookup = cardOwnershipLookup;
        this.cardPriceService = cardPriceService;
    }

    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> search(
            CardSearchFilter filter,
            CardSearchFilter.@Nullable IntRange ownedQuantityRange,
            @Nullable Long partnerForCardId,
            Pageable pageable) {
        Specification<Card> spec = cardSpec(filter);
        if (ownedQuantityRange != null) {
            spec = spec.and(ownedQuantitySpec(ownedQuantityRange));
        }
        if (filter.priceRange() != null) {
            spec = spec.and(priceRangeSpec(filter.priceRange()));
        }
        if (partnerForCardId == null) {
            return cardRepository
                    .findAll(spec, pageable)
                    .map(card -> CardSummaryResponse.from(card, latestPrinting(card.getId())));
        }
        return searchPartners(spec, getCard(partnerForCardId), pageable);
    }

    private Specification<Card> ownedQuantitySpec(CardSearchFilter.IntRange range) {
        int min = range.min() == null ? 0 : range.min();
        int max = range.max() == null ? Integer.MAX_VALUE : range.max();
        Map<Long, Integer> quantityByCard = ownedQuantitiesByCard();
        Set<Long> matchingIds =
                quantityByCard.entrySet().stream()
                        .filter(entry -> entry.getValue() >= min && entry.getValue() <= max)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());
        return candidateIdSpec(matchingIds, min <= 0, quantityByCard.keySet());
    }

    // Justified: method-local map, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private Map<Long, Integer> ownedQuantitiesByCard() {
        Map<Long, Integer> quantityByPrinting =
                currentUser
                        .subject()
                        .map(cardOwnershipLookup::ownedQuantitiesBySubject)
                        .orElse(Map.of());
        if (quantityByPrinting.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> cardIdByPrinting = cardIdsByPrintingId(quantityByPrinting.keySet());
        Map<Long, Integer> quantityByCard = new HashMap<>();
        quantityByPrinting.forEach(
                (printingId, quantity) -> {
                    Long cardId = cardIdByPrinting.get(printingId);
                    if (cardId != null) {
                        quantityByCard.merge(cardId, quantity, Integer::sum);
                    }
                });
        return quantityByCard;
    }

    // ponytail: candidate ids are computed in Java from the caller's bounded owned collection,
    // same shape as the existing partner-candidate pattern; push to a SQL subquery if collection
    // sizes make this slow.
    private static Specification<Card> candidateIdSpec(
            Set<Long> matchingIds, boolean includeUnmatched, Set<Long> excludedFromUnmatched) {
        return (root, criteriaQuery, builder) -> {
            Predicate matches =
                    matchingIds.isEmpty() ? builder.disjunction() : root.get("id").in(matchingIds);
            if (!includeUnmatched) {
                return matches;
            }
            Predicate outsideOwned =
                    excludedFromUnmatched.isEmpty()
                            ? builder.conjunction()
                            : root.get("id").in(excludedFromUnmatched).not();
            return builder.or(matches, outsideOwned);
        };
    }

    // ponytail: materializes every tracked price to filter in Java, reusing CardPriceService's
    // existing latest-snapshot lookup rather than duplicating a correlated-subquery; push to SQL
    // if the tracked-price table size makes this slow.
    private Specification<Card> priceRangeSpec(CardSearchFilter.PriceRange range) {
        Set<Long> trackedPrintingIds = cardPriceService.trackedPrintingIds();
        Map<Long, CardPrice> prices = cardPriceService.latestPrices(trackedPrintingIds);
        Map<Long, Long> cardIdByPrinting = cardIdsByPrintingId(trackedPrintingIds);
        Set<Long> matchingIds =
                prices.entrySet().stream()
                        .filter(entry -> inRange(entry.getValue(), range))
                        .map(entry -> cardIdByPrinting.get(entry.getKey()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        return matchingIds.isEmpty()
                ? (root, criteriaQuery, builder) -> builder.disjunction()
                : (root, criteriaQuery, builder) -> root.get("id").in(matchingIds);
    }

    private static boolean inRange(CardPrice price, CardSearchFilter.PriceRange range) {
        BigDecimal value = price.forCurrency(range.currency());
        if (value == null) {
            return false;
        }
        if (range.min() != null && value.compareTo(range.min()) < 0) {
            return false;
        }
        return range.max() == null || value.compareTo(range.max()) <= 0;
    }

    private Map<Long, Long> cardIdsByPrintingId(Collection<Long> printingIds) {
        return cardPrintingRepository.findAllById(printingIds).stream()
                .collect(
                        Collectors.toMap(
                                CardPrinting::getId, printing -> printing.getCard().getId()));
    }

    // ponytail: in-memory filter + manual pagination; valid partner candidate sets are small
    // (backgrounds ~30, named partners exactly 1). Push to SQL if generic partner sets grow.
    private Page<CardSummaryResponse> searchPartners(
            Specification<Card> spec, Card primary, Pageable pageable) {
        List<Card> candidates =
                cardRepository.findAll(spec, pageable.getSort()).stream()
                        .filter(card -> pairingRules.canPair(primary, card))
                        .toList();
        int start = (int) Math.min(pageable.getOffset(), candidates.size());
        int end = Math.min(start + pageable.getPageSize(), candidates.size());
        Page<Card> page =
                new PageImpl<>(candidates.subList(start, end), pageable, candidates.size());
        return page.map(card -> CardSummaryResponse.from(card, latestPrinting(card.getId())));
    }

    @Transactional
    public int updateCommanderRanks(Map<String, Integer> ranksByName) {
        cardRepository.clearCommanderRanks();
        var commanders = cardRepository.findByNameIn(ranksByName.keySet());
        var assigned = 0;
        for (var card : commanders) {
            Integer rank = ranksByName.get(card.getName());
            if (rank != null) {
                card.setCommanderRank(rank);
                assigned++;
            }
        }
        cardRepository.saveAll(commanders);
        return assigned;
    }

    @Transactional
    public int updateGameChangers(Collection<String> oracleIds) {
        cardRepository.clearGameChangers();
        var gameChangers = cardRepository.findByScryfallOracleIdIn(oracleIds);
        gameChangers.forEach(card -> card.setGameChanger(true));
        cardRepository.saveAll(gameChangers);
        return gameChangers.size();
    }

    // ponytail: one printing lookup per card (N+1 at page size); batch fetch if pages get slow
    private @Nullable CardPrinting latestPrinting(long cardId) {
        return cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                .findFirst()
                .orElse(null);
    }

    private Specification<Card> cardSpec(CardSearchFilter filter) {
        return (root, criteria, builder) ->
                CardSearchPredicates.cardPredicate(root, filter, criteria, builder);
    }

    public Map<Long, String> getOracleIdsByPrintingIds(Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return Map.of();
        }
        return cardPrintingRepository.findAllById(cardPrintingIds).stream()
                .collect(
                        Collectors.toMap(
                                CardPrinting::getId,
                                printing -> printing.getCard().getScryfallOracleId()));
    }

    @Transactional(readOnly = true)
    public Map<Long, CardExportView> getExportViewsByPrintingIds(Collection<Long> cardPrintingIds) {
        return cardPrintingRepository.findAllById(cardPrintingIds).stream()
                .collect(
                        Collectors.toUnmodifiableMap(
                                CardPrinting::getId,
                                printing ->
                                        new CardExportView(
                                                printing.getId(),
                                                printing.getCard().getName(),
                                                printing.getFlavorName(),
                                                new CardExportView.PrintingReference(
                                                        printing.getMagicSet().getSetCode(),
                                                        printing.getCollectorNumber(),
                                                        printing.getScryfallCardId()))));
    }

    @Transactional(readOnly = true)
    public Map<Long, CardAnalysisView> getAnalysisViewsByPrintingIds(
            Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return Map.of();
        }
        var printings = cardPrintingRepository.findAllById(cardPrintingIds);
        printings.forEach(printing -> Hibernate.initialize(printing.getCard().getFaces()));
        return printings.stream()
                .collect(Collectors.toUnmodifiableMap(CardPrinting::getId, CardAnalysisView::from));
    }

    @Transactional(readOnly = true)
    public Card getCard(long cardId) {
        return cardRepository
                .findById(cardId)
                .filter(Card::getActive)
                .orElseThrow(CardNotFoundException::new);
    }

    /**
     * Like {@link #getCard(long)} but initializes the lazy faces collection before the session
     * closes, so callers outside a transaction can read faces.
     */
    @Transactional(readOnly = true)
    public Card getCardWithFaces(long cardId) {
        var card = getCard(cardId);
        Hibernate.initialize(card.getFaces());
        return card;
    }

    @Transactional(readOnly = true)
    public List<Card> getCardsByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return List.of();
        }
        var cards = cardRepository.findByNameIn(names);
        cards.forEach(CardCatalogService::initializeAssociations);
        return cards;
    }

    /**
     * Initializes the lazy faces and legalities collections before the session closes, so callers
     * outside a transaction (e.g. deck building, suggestions) can read them.
     */
    @Transactional(readOnly = true)
    public Map<Long, Card> getCardsByPrintingIds(Collection<Long> cardPrintingIds) {
        if (cardPrintingIds.isEmpty()) {
            return Map.of();
        }
        var cards =
                cardPrintingRepository.findAllById(cardPrintingIds).stream()
                        .collect(Collectors.toMap(CardPrinting::getId, CardPrinting::getCard));
        cards.values().forEach(CardCatalogService::initializeAssociations);
        return cards;
    }

    private static void initializeAssociations(Card card) {
        Hibernate.initialize(card.getFaces());
        Hibernate.initialize(card.getLegalities());
    }

    @Transactional(readOnly = true)
    // Justified: method-local map, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<Long, Long> getLatestPrintingIdByCardIds(Collection<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> printingIds = new java.util.HashMap<>();
        for (var cardId : cardIds) {
            cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                    .findFirst()
                    .ifPresent(printing -> printingIds.put(cardId, printing.getId()));
        }
        return printingIds;
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getById(long cardId) {
        return cardRepository
                .findById(cardId)
                .filter(card -> card.getActive())
                .map(card -> CardDetailResponse.from(card, latestPrinting(card.getId())))
                .orElseThrow(CardNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public CardSummaryResponse getSummaryByPrintingId(long cardPrintingId) {
        return cardPrintingRepository
                .findById(cardPrintingId)
                .filter(printing -> printing.getActive() && printing.getCard().getActive())
                .map(printing -> CardSummaryResponse.from(printing.getCard(), printing))
                .orElseThrow(CardNotFoundException::new);
    }

    public @Nullable String getNameById(long cardId) {
        return cardRepository.findById(cardId).map(Card::getName).orElse(null);
    }

    public void validateFinishAvailability(
            long cardPrintingId, int regularQuantity, int foilQuantity) {
        CardPrinting printing =
                cardPrintingRepository
                        .findById(cardPrintingId)
                        .filter(p -> p.getActive() && p.getCard().getActive())
                        .orElseThrow(CardNotFoundException::new);
        if (regularQuantity > 0 && Boolean.FALSE.equals(printing.getNonfoilAvailable())) {
            throw new FinishUnavailableException("nonfoil");
        }
        if (foilQuantity > 0 && Boolean.FALSE.equals(printing.getFoilAvailable())) {
            throw new FinishUnavailableException("foil");
        }
    }

    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> getSetPrintings(String setCode, Pageable pageable) {
        return getSetPrintings(setCode, "", pageable);
    }

    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> getSetPrintings(
            String setCode, String query, Pageable pageable) {
        Page<CardPrinting> printings =
                query.isBlank()
                        ? cardPrintingRepository
                                .findByMagicSetSetCodeAndActiveTrueAndCardActiveTrue(
                                        setCode, pageable)
                        : cardPrintingRepository
                                .findByMagicSetSetCodeAndActiveTrueAndCardActiveTrueAndCardNameContainingIgnoreCase(
                                        setCode, query, pageable);
        return printings.map(printing -> CardSummaryResponse.from(printing.getCard(), printing));
    }

    @Transactional(readOnly = true)
    public List<CardPrintingResponse> getPrintings(long cardId) {
        getById(cardId);
        return cardPrintingRepository.findByCardIdOrderByReleasedAtDesc(cardId).stream()
                .map(CardPrintingResponse::from)
                .toList();
    }

    // Suppressed: mirrors the controller's search signature; each parameter is an independent
    // optional filter consumed by the specification builder (same rationale as search()).
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> searchPrintings(
            String query,
            @Nullable String setCode,
            @Nullable String colorIdentity,
            @Nullable String type,
            @Nullable Boolean commanderEligible,
            @Nullable Long partnerForCardId,
            Pageable pageable) {
        Specification<CardPrinting> spec =
                printingCardSpec(query, colorIdentity, type, commanderEligible);
        if (setCode != null) {
            spec = spec.and(ownSetCodeSpec(setCode));
        }
        if (partnerForCardId != null) {
            var partnerFilter =
                    CardSearchFilter.basic(query, setCode, colorIdentity, type, commanderEligible);
            spec =
                    spec.and(
                            partnerCandidatesSpec(
                                    cardSpec(partnerFilter), getCard(partnerForCardId)));
        }
        return cardPrintingRepository
                .findAll(spec, pageable)
                .map(printing -> CardSummaryResponse.from(printing.getCard(), printing));
    }

    private Specification<CardPrinting> printingCardSpec(
            String query,
            @Nullable String colorIdentity,
            @Nullable String type,
            @Nullable Boolean commanderEligible) {
        var filter = CardSearchFilter.basic(query, null, colorIdentity, type, commanderEligible);
        return (root, criteria, builder) ->
                builder.and(
                        builder.isTrue(root.get("active")),
                        CardSearchPredicates.cardPredicate(
                                root.join("card"), filter, criteria, builder));
    }

    private Specification<CardPrinting> ownSetCodeSpec(String setCode) {
        return (root, criteria, builder) ->
                builder.equal(root.get("magicSet").get("setCode"), setCode);
    }

    // ponytail: candidate IDs are fetched then matched via IN; partner candidate sets are small
    // (backgrounds ~30, named partners exactly 1), so DB pagination stays correct and cheap.
    private Specification<CardPrinting> partnerCandidatesSpec(
            Specification<Card> cardSpec, Card primary) {
        List<Long> candidateIds =
                cardRepository.findAll(cardSpec).stream()
                        .filter(card -> pairingRules.canPair(primary, card))
                        .map(Card::getId)
                        .toList();
        return (root, criteria, builder) ->
                candidateIds.isEmpty()
                        ? builder.disjunction()
                        : root.join("card").get("id").in(candidateIds);
    }
}
