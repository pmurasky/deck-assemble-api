package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Hibernate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardCatalogService {

    private final CardRepository cardRepository;
    private final CardPrintingRepository cardPrintingRepository;

    public CardCatalogService(
            CardRepository cardRepository, CardPrintingRepository cardPrintingRepository) {
        this.cardRepository = cardRepository;
        this.cardPrintingRepository = cardPrintingRepository;
    }

    // Suppressed: mirrors the controller's search signature; each parameter is an independent
    // optional filter consumed by the specification builder.
    @SuppressWarnings("checkstyle:ParameterNumber")
    @Transactional(readOnly = true)
    public Page<CardSummaryResponse> search(
            String query,
            String setCode,
            String colorIdentity,
            String type,
            Boolean commanderEligible,
            Pageable pageable) {
        return cardRepository
                .findAll(
                        specification(query, setCode, colorIdentity, type, commanderEligible),
                        pageable)
                .map(card -> CardSummaryResponse.from(card, latestPrinting(card.getId())));
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

    private Specification<Card> specification(
            String query,
            String setCode,
            String colorIdentity,
            String type,
            Boolean commanderEligible) {
        Specification<Card> result = activeSpec().and(nameSpec(query));
        if (colorIdentity != null) {
            result = result.and(colorIdentitySpec(colorIdentity));
        }
        if (setCode != null) {
            result = result.and(setCodeSpec(setCode));
        }
        if (type != null) {
            result = result.and(typeLineSpec(type));
        }
        if (Boolean.TRUE.equals(commanderEligible)) {
            result = result.and(commanderEligibleSpec());
        }
        return result;
    }

    private Specification<Card> commanderEligibleSpec() {
        return (root, criteria, builder) -> {
            var typeLine = builder.lower(root.get("typeLine"));
            var legendaryCreature =
                    builder.and(
                            builder.like(typeLine, "%legendary%"),
                            builder.like(typeLine, "%creature%"));
            var canBeCommander =
                    builder.like(builder.lower(root.get("oracleText")), "%can be your commander%");
            return builder.or(legendaryCreature, canBeCommander);
        };
    }

    private Specification<Card> activeSpec() {
        return (root, criteria, builder) -> builder.isTrue(root.get("active"));
    }

    private Specification<Card> nameSpec(String query) {
        return (root, criteria, builder) ->
                builder.like(builder.lower(root.get("name")), "%" + query.toLowerCase() + "%");
    }

    private Specification<Card> colorIdentitySpec(String colorIdentity) {
        return (root, criteria, builder) ->
                builder.like(root.get("colorIdentity"), "%" + colorIdentity + "%");
    }

    private Specification<Card> typeLineSpec(String type) {
        return (root, criteria, builder) ->
                builder.like(builder.lower(root.get("typeLine")), "%" + type.toLowerCase() + "%");
    }

    private Specification<Card> setCodeSpec(String setCode) {
        return (root, criteria, builder) -> {
            var subquery = criteria.subquery(Long.class);
            var printings = subquery.from(CardPrinting.class);
            return builder.exists(
                    subquery.select(printings.get("id"))
                            .where(
                                    builder.equal(printings.get("card").get("id"), root.get("id")),
                                    builder.equal(
                                            printings.get("magicSet").get("setCode"), setCode)));
        };
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
}
