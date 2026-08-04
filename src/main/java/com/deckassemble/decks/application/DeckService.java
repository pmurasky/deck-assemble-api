package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardNotFoundException;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.recommendations.domain.CommanderSpellbookClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Justified: orchestration service coordinating deck collaborators; decomposition tracked in #3.
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
@Service
@Transactional
public class DeckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeckService.class);

    private final DeckRepository deckRepository;
    private final DeckCardRepository deckCardRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final CardCatalogService cardCatalogService;
    private final CommanderLegalityEvaluator commanderLegalityEvaluator;
    private final OwnershipChecker ownershipChecker;
    private final CollectionService collectionService;
    private final CardPriceService cardPriceService;
    private final CommanderSpellbookClient commanderSpellbookClient;

    // Suppressed: collaborators are what this orchestration service needs; Spring injects them.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckService(
            DeckRepository deckRepository,
            DeckCardRepository deckCardRepository,
            DeckAccessGuard deckAccessGuard,
            CardCatalogService cardCatalogService,
            CommanderLegalityEvaluator commanderLegalityEvaluator,
            OwnershipChecker ownershipChecker,
            CollectionService collectionService,
            CardPriceService cardPriceService,
            CommanderSpellbookClient commanderSpellbookClient) {
        this.deckRepository = deckRepository;
        this.deckCardRepository = deckCardRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.cardCatalogService = cardCatalogService;
        this.commanderLegalityEvaluator = commanderLegalityEvaluator;
        this.ownershipChecker = ownershipChecker;
        this.collectionService = collectionService;
        this.cardPriceService = cardPriceService;
        this.commanderSpellbookClient = commanderSpellbookClient;
    }

    public List<DeckResponse> list() {
        return deckRepository.findByProfileIdOrderByNameAsc(profileId()).stream()
                .map(this::responseFor)
                .toList();
    }

    public DeckResponse create(DeckCreateRequest request) {
        Deck deck = new Deck(profileId(), request.name(), request.formatCode());
        deck.setDescription(request.description());
        deck.setCommanderCardId(request.commanderCardId());
        deck.setSecondaryCommanderCardId(request.secondaryCommanderCardId());
        deck.setUseOwnedCardsOnly(Boolean.TRUE.equals(request.useOwnedCardsOnly()));
        deck.setBudgetLimit(request.budgetLimit());
        deck.setDesiredPowerLevel(request.desiredPowerLevel());
        deck.setPlayStyle(request.playStyle());
        return responseFor(deckRepository.save(deck));
    }

    public DeckResponse getById(long deckId) {
        return responseFor(owned(deckId));
    }

    public DeckLegalityResponse legality(long deckId) {
        Deck deck = owned(deckId);
        return commanderLegalityEvaluator.evaluate(deck, deckCardRepository.findByDeckId(deckId));
    }

    @Transactional(readOnly = true)
    public DeckComboResponse getCombos(long deckId) {
        owned(deckId);
        var cards = deckCardRepository.findByDeckId(deckId);
        var names = cardCatalogService.getCardsByPrintingIds(cardPrintingIds(cards));
        var deckList = withNames(cards, names);
        if (deckList.isBlank()) {
            return new DeckComboResponse(true, List.of());
        }
        try {
            return new DeckComboResponse(true, commanderSpellbookClient.findCombos(deckList));
        } catch (org.springframework.web.client.RestClientException exception) {
            LOGGER.warn("Commander Spellbook lookup failed for deck {}", deckId, exception);
            return new DeckComboResponse(false, List.of());
        }
    }

    private static List<Long> cardPrintingIds(List<DeckCard> cards) {
        return cards.stream()
                .filter(DeckService::includedInSpellbookLookup)
                .map(DeckCard::getCardPrintingId)
                .toList();
    }

    private static String withNames(List<DeckCard> cards, Map<Long, Card> cardsByPrintingId) {
        return cards.stream()
                .filter(DeckService::includedInSpellbookLookup)
                .flatMap(card -> deckListLine(card, cardsByPrintingId).stream())
                .collect(Collectors.joining("\n"));
    }

    private static boolean includedInSpellbookLookup(DeckCard card) {
        return card.getDeckSection() == DeckCard.Section.COMMANDER
                || card.getDeckSection() == DeckCard.Section.MAIN_DECK
                || card.getDeckSection() == DeckCard.Section.COMPANION;
    }

    private static Optional<String> deckListLine(DeckCard card, Map<Long, Card> cardsByPrintingId) {
        Card catalogCard = cardsByPrintingId.get(card.getCardPrintingId());
        return catalogCard == null
                ? Optional.empty()
                : Optional.of(card.getQuantity() + " " + catalogCard.getName());
    }

    @Transactional(readOnly = true)
    public DeckWishlistResponse getWishlist(long deckId) {
        owned(deckId);
        var wishlistCards =
                deckCardRepository.findByDeckId(deckId).stream()
                        .filter(
                                card ->
                                        card.getOwnershipStatus()
                                                == DeckCard.OwnershipStatus.WISHLIST)
                        .toList();
        if (wishlistCards.isEmpty()) {
            return new DeckWishlistResponse(List.of(), null);
        }
        var printingIds = wishlistCards.stream().map(DeckCard::getCardPrintingId).toList();
        var cards = cardCatalogService.getCardsByPrintingIds(printingIds);
        var prices = cardPriceService.latestPrices(printingIds);
        var items =
                wishlistCards.stream().map(card -> toWishlistItem(card, cards, prices)).toList();
        return new DeckWishlistResponse(items, wishlistTotal(items));
    }

    private static BigDecimal wishlistTotal(List<DeckWishlistItem> items) {
        return items.stream()
                .map(DeckWishlistItem::lineTotalUsd)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static DeckWishlistItem toWishlistItem(
            DeckCard deckCard, Map<Long, Card> cards, Map<Long, CardPrice> prices) {
        var price = prices.get(deckCard.getCardPrintingId());
        var unitPrice = price != null ? price.usd() : null;
        var lineTotal =
                unitPrice != null
                        ? unitPrice.multiply(BigDecimal.valueOf(deckCard.getQuantity()))
                        : null;
        var card = cards.get(deckCard.getCardPrintingId());
        return new DeckWishlistItem(
                deckCard.getId(),
                deckCard.getCardPrintingId(),
                card != null ? card.getName() : "Unknown card",
                deckCard.getQuantity(),
                unitPrice,
                lineTotal);
    }

    public DeckResponse update(long deckId, DeckUpdateRequest request) {
        Deck deck = owned(deckId);
        applyCoreFields(deck, request);
        applyOptionFields(deck, request);
        return responseFor(deckRepository.save(deck));
    }

    private void applyCoreFields(Deck deck, DeckUpdateRequest request) {
        if (request.name() != null) {
            deck.setName(request.name());
        }
        if (request.formatCode() != null) {
            deck.setFormatCode(request.formatCode());
        }
        if (request.description() != null) {
            deck.setDescription(request.description());
        }
        if (request.commanderCardId() != null) {
            deck.setCommanderCardId(request.commanderCardId());
        }
        if (request.secondaryCommanderCardId() != null) {
            deck.setSecondaryCommanderCardId(request.secondaryCommanderCardId());
        }
    }

    private void applyOptionFields(Deck deck, DeckUpdateRequest request) {
        if (request.useOwnedCardsOnly() != null) {
            deck.setUseOwnedCardsOnly(request.useOwnedCardsOnly());
        }
        if (request.budgetLimit() != null) {
            deck.setBudgetLimit(request.budgetLimit());
        }
        if (request.desiredPowerLevel() != null) {
            deck.setDesiredPowerLevel(request.desiredPowerLevel());
        }
        if (request.playStyle() != null) {
            deck.setPlayStyle(request.playStyle());
        }
    }

    public void delete(long deckId) {
        deckRepository.delete(owned(deckId));
    }

    public DeckResponse archive(long deckId) {
        Deck deck = owned(deckId);
        deck.setStatus(Deck.Status.ARCHIVED);
        return responseFor(deckRepository.save(deck));
    }

    public DeckResponse duplicate(long deckId) {
        Deck source = owned(deckId);
        Deck copy = new Deck(profileId(), source.getName() + " (Copy)", source.getFormatCode());
        copyDetails(source, copy);
        Deck saved = deckRepository.save(copy);
        copyCards(deckId, saved);
        return responseFor(saved);
    }

    private void copyDetails(Deck source, Deck copy) {
        copy.setDescription(source.getDescription());
        copy.setCommanderCardId(source.getCommanderCardId());
        copy.setSecondaryCommanderCardId(source.getSecondaryCommanderCardId());
        copy.setUseOwnedCardsOnly(source.isUseOwnedCardsOnly());
        copy.setBudgetLimit(source.getBudgetLimit());
        copy.setDesiredPowerLevel(source.getDesiredPowerLevel());
        copy.setPlayStyle(source.getPlayStyle());
    }

    private void copyCards(long sourceDeckId, Deck copy) {
        deckCardRepository.findByDeckId(sourceDeckId).stream()
                .map(card -> copyCard(card, copy.getId()))
                .forEach(deckCardRepository::save);
    }

    private DeckCard copyCard(DeckCard card, long deckId) {
        DeckCard copy =
                new DeckCard(
                        deckId,
                        card.getCardPrintingId(),
                        card.getQuantity(),
                        card.getDeckSection());
        copy.setOwnershipStatus(card.getOwnershipStatus());
        return copy;
    }

    public OwnershipSyncResponse syncOwnership(long deckId) {
        owned(deckId);
        List<DeckCard> cards = deckCardRepository.findByDeckId(deckId);
        var ownedPrintingIds =
                ownershipChecker.filterOwnedPrintingIds(
                        profileId(), cards.stream().map(DeckCard::getCardPrintingId).toList());
        var changes =
                cards.stream()
                        .map(card -> syncCard(card, ownedPrintingIds))
                        .flatMap(Optional::stream)
                        .toList();
        return new OwnershipSyncResponse(changes.size(), changes);
    }

    private Optional<OwnershipSyncResponse.OwnershipChange> syncCard(
            DeckCard card, Set<Long> ownedPrintingIds) {
        var current = card.getOwnershipStatus();
        var target =
                ownedPrintingIds.contains(card.getCardPrintingId())
                        ? DeckCard.OwnershipStatus.OWNED
                        : current == DeckCard.OwnershipStatus.PROXY
                                ? DeckCard.OwnershipStatus.PROXY
                                : DeckCard.OwnershipStatus.WISHLIST;
        if (target == current) {
            return Optional.empty();
        }
        card.setOwnershipStatus(target);
        deckCardRepository.save(card);
        return Optional.of(
                new OwnershipSyncResponse.OwnershipChange(
                        card.getId(), card.getCardPrintingId(), current.name(), target.name()));
    }

    public DeckCardResponse acquireCard(long deckId, long deckCardId) {
        owned(deckId);
        DeckCard deckCard = ownedCard(deckId, deckCardId);
        collectionService.addToDefaultCollection(
                deckCard.getCardPrintingId(), deckCard.getQuantity(), 0);
        if (deckCard.getOwnershipStatus() != DeckCard.OwnershipStatus.OWNED) {
            deckCard.setOwnershipStatus(DeckCard.OwnershipStatus.OWNED);
            deckCardRepository.save(deckCard);
        }
        return responseFor(deckCard);
    }

    private Long profileId() {
        return deckAccessGuard.profileId();
    }

    private Deck owned(long deckId) {
        return deckAccessGuard.owned(deckId);
    }

    private DeckCard ownedCard(long deckId, long deckCardId) {
        return deckCardRepository
                .findByIdAndDeckId(deckCardId, deckId)
                .orElseThrow(DeckCardNotFoundException::new);
    }

    private DeckCardResponse responseFor(DeckCard card) {
        return DeckCardResponse.from(
                card, cardCatalogService.getSummaryByPrintingId(card.getCardPrintingId()));
    }

    private DeckResponse responseFor(Deck deck) {
        int cardCount =
                deckCardRepository.findByDeckId(deck.getId()).stream()
                        .mapToInt(DeckCard::getQuantity)
                        .sum();
        String commanderName =
                deck.getCommanderCardId() == null
                        ? null
                        : cardCatalogService.getNameById(deck.getCommanderCardId());
        return DeckResponse.from(
                deck, cardCount, commanderName, commanderSummary(deck.getCommanderCardId()));
    }

    private @Nullable CardSummaryResponse commanderSummary(@Nullable Long commanderCardId) {
        if (commanderCardId == null) {
            return null;
        }
        Long printingId = latestPrintingId(commanderCardId);
        return printingId == null ? null : summaryOrNull(printingId);
    }

    private @Nullable Long latestPrintingId(long cardId) {
        return cardCatalogService.getLatestPrintingIdByCardIds(List.of(cardId)).get(cardId);
    }

    private @Nullable CardSummaryResponse summaryOrNull(long printingId) {
        try {
            return cardCatalogService.getSummaryByPrintingId(printingId);
        } catch (CardNotFoundException exception) {
            return null;
        }
    }
}
