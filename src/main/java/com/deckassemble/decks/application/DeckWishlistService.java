package com.deckassemble.decks.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reports the wishlist cards of a deck with current prices. */
@Service
@Transactional(readOnly = true)
public class DeckWishlistService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCardRepository deckCardRepository;
    private final CardCatalogService cardCatalogService;
    private final CardPriceService cardPriceService;

    public DeckWishlistService(
            DeckAccessGuard deckAccessGuard,
            DeckCardRepository deckCardRepository,
            CardCatalogService cardCatalogService,
            CardPriceService cardPriceService) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCardRepository = deckCardRepository;
        this.cardCatalogService = cardCatalogService;
        this.cardPriceService = cardPriceService;
    }

    public DeckWishlistResponse getWishlist(long deckId) {
        deckAccessGuard.owned(deckId);
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
}
