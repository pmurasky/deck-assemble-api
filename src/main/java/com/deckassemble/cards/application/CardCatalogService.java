package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.CommanderPairingRules;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    private final CardSearchCandidateSpecifications candidateSpecifications;

    public CardCatalogService(
            CardRepository cardRepository,
            CardPrintingRepository cardPrintingRepository,
            CommanderPairingRules pairingRules,
            CardSearchCandidateSpecifications candidateSpecifications) {
        this.cardRepository = cardRepository;
        this.cardPrintingRepository = cardPrintingRepository;
        this.pairingRules = pairingRules;
        this.candidateSpecifications = candidateSpecifications;
    }

    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> search(
            CardSearchFilter filter,
            CardSearchFilter.@Nullable IntRange ownedQuantityRange,
            @Nullable Long partnerForCardId,
            Pageable pageable) {
        Specification<Card> spec = cardSpec(filter);
        if (ownedQuantityRange != null) {
            spec = spec.and(candidateSpecifications.ownedQuantitySpec(ownedQuantityRange));
        }
        if (filter.priceRange() != null) {
            spec = spec.and(candidateSpecifications.priceRangeSpec(filter.priceRange()));
        }
        Map<Long, Integer> ownedByCard = candidateSpecifications.ownedQuantitiesByCardOrNull();
        if (partnerForCardId == null) {
            return cardRepository.findAll(spec, pageable).map(card -> toSummary(card, ownedByCard));
        }
        return searchPartners(spec, getCard(partnerForCardId), pageable, ownedByCard);
    }

    private CardSummaryResponse toSummary(Card card, @Nullable Map<Long, Integer> ownedByCard) {
        Integer owned = ownedByCard == null ? null : ownedByCard.getOrDefault(card.getId(), 0);
        return CardSummaryResponse.from(card, latestPrinting(card.getId()), owned);
    }

    // ponytail: in-memory filter + manual pagination; valid partner candidate sets are small
    // (backgrounds ~30, named partners exactly 1). Push to SQL if generic partner sets grow.
    private Page<CardSummaryResponse> searchPartners(
            Specification<Card> spec,
            Card primary,
            Pageable pageable,
            @Nullable Map<Long, Integer> ownedByCard) {
        List<Card> candidates =
                cardRepository.findAll(spec, pageable.getSort()).stream()
                        .filter(card -> pairingRules.canPair(primary, card))
                        .toList();
        int start = (int) Math.min(pageable.getOffset(), candidates.size());
        int end = Math.min(start + pageable.getPageSize(), candidates.size());
        Page<Card> page =
                new PageImpl<>(candidates.subList(start, end), pageable, candidates.size());
        return page.map(card -> toSummary(card, ownedByCard));
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
                                                        printing.getScryfallCardId()),
                                                printing.getImageUriNormal())));
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
