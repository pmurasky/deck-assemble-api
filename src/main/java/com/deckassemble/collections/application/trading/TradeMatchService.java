package com.deckassemble.collections.application.trading;

import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.application.physical.PhysicalCardInventory;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListItem;
import com.deckassemble.collections.domain.trading.TradeListItemRepository;
import com.deckassemble.collections.domain.trading.TradeListRepository;
import com.deckassemble.collections.domain.trading.TradeListType;
import com.deckassemble.collections.domain.trading.TradeListVisibility;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.UseConcurrentHashMap"})
public class TradeMatchService {

    private final CollectionAccessGuard accessGuard;
    private final TradeListRepository listRepository;
    private final TradeListItemRepository itemRepository;
    private final PhysicalCardInventory inventory;
    private final CollectionCardPhysicalMetadataRepository metadataRepository;
    private final CardPrintingRepository printingRepository;
    private final CardPriceService priceService;

    // checkstyle:ParameterNumber suppressed: this service coordinates existing module boundaries.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public TradeMatchService(
            CollectionAccessGuard accessGuard,
            TradeListRepository listRepository,
            TradeListItemRepository itemRepository,
            PhysicalCardInventory inventory,
            CollectionCardPhysicalMetadataRepository metadataRepository,
            CardPrintingRepository printingRepository,
            CardPriceService priceService) {
        this.accessGuard = accessGuard;
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.inventory = inventory;
        this.metadataRepository = metadataRepository;
        this.printingRepository = printingRepository;
        this.priceService = priceService;
    }

    @SuppressWarnings("checkstyle:MethodLength")
    public TradeMatchView compare(long leftListId, long rightListId) {
        long requesterProfileId = accessGuard.profileId();
        TradeList left = visible(leftListId, requesterProfileId);
        TradeList right = visible(rightListId, requesterProfileId);
        List<TradeListItem> items =
                itemRepository.findByTradeListIdInOrderById(List.of(leftListId, rightListId));
        Map<Long, List<TradeListItem>> byList =
                items.stream().collect(Collectors.groupingBy(TradeListItem::getTradeListId));
        List<TradeMatchItemView> matches = new ArrayList<>();
        addDirectional(
                matches,
                left,
                byList.getOrDefault(leftListId, List.of()),
                right,
                byList.getOrDefault(rightListId, List.of()));
        addDirectional(
                matches,
                right,
                byList.getOrDefault(rightListId, List.of()),
                left,
                byList.getOrDefault(leftListId, List.of()));
        ValueTotals totals = totals(matches);
        return new TradeMatchView(
                leftListId, rightListId, matches, totals.deltas(), totals.unpriced());
    }

    private void addDirectional(
            List<TradeMatchItemView> matches,
            TradeList from,
            List<TradeListItem> fromItems,
            TradeList to,
            List<TradeListItem> toItems) {
        if (from.getType() != TradeListType.OFFERED || to.getType() != TradeListType.WANTED) {
            return;
        }
        Map<Long, Integer> remainingByCard = new HashMap<>();
        for (TradeListItem offered : fromItems) {
            for (TradeListItem wanted : toItems) {
                matchItem(matches, from, to, offered, wanted, remainingByCard);
            }
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void matchItem(
            List<TradeMatchItemView> matches,
            TradeList from,
            TradeList to,
            TradeListItem offered,
            TradeListItem wanted,
            Map<Long, Integer> remainingByCard) {
        if (!compatiblePrintings(offered.getCardPrintingId(), wanted.getCardPrintingId())) {
            return;
        }
        int remaining = Math.min(offered.getQuantity(), wanted.getQuantity());
        for (CollectionCard card :
                inventory.compatibleCards(from.getProfileId(), wanted.getCardPrintingId())) {
            int available = remainingAvailable(card, remainingByCard);
            int quantity = Math.min(remaining, matchingAvailable(card, offered, wanted, available));
            if (quantity > 0) {
                matches.add(match(from, to, offered, wanted, card, quantity, available));
                remainingByCard.put(card.getId(), available - quantity);
                remaining -= quantity;
            }
            if (remaining == 0) {
                return;
            }
        }
    }

    private int matchingAvailable(
            CollectionCard card, TradeListItem offered, TradeListItem wanted, int available) {
        if (available <= 0
                || !metadataMatches(card.getId(), offered)
                || !metadataMatches(card.getId(), wanted)) {
            return 0;
        }
        return available;
    }

    private int remainingAvailable(CollectionCard card, Map<Long, Integer> remainingByCard) {
        return remainingByCard.computeIfAbsent(
                card.getId(),
                id ->
                        inventory.ownedQuantity(card)
                                - inventory
                                        .allocatedByCollectionCardId(List.of(card), null)
                                        .getOrDefault(id, 0));
    }

    private boolean hasNoMetadataFilter(TradeListItem item) {
        return item.getCondition() == null
                && item.getFinish() == null
                && item.getLanguage() == null;
    }

    private boolean metadataMatches(long collectionCardId, TradeListItem item) {
        if (hasNoMetadataFilter(item)) {
            return true;
        }
        return metadataRepository.findByCollectionCardId(collectionCardId).stream()
                .anyMatch(metadata -> metadataMatches(metadata, item));
    }

    private boolean metadataMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return conditionMatches(metadata, item)
                && finishMatches(metadata, item)
                && languageMatches(metadata, item);
    }

    private boolean conditionMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getCondition() == null || item.getCondition() == metadata.getCondition();
    }

    private boolean finishMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getFinish() == null || item.getFinish() == metadata.getFinish();
    }

    private boolean languageMatches(CollectionCardPhysicalMetadata metadata, TradeListItem item) {
        return item.getLanguage() == null || item.getLanguage().equals(metadata.getLanguage());
    }

    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    private TradeMatchItemView match(
            TradeList from,
            TradeList to,
            TradeListItem offered,
            TradeListItem wanted,
            CollectionCard card,
            int quantity,
            int available) {
        return new TradeMatchItemView(
                from.getId(),
                to.getId(),
                offered.getId(),
                wanted.getId(),
                wanted.getCardPrintingId(),
                card.getId(),
                card.getCardPrintingId(),
                quantity,
                available,
                card.getCardPrintingId().equals(wanted.getCardPrintingId()));
    }

    private ValueTotals totals(List<TradeMatchItemView> matches) {
        Map<Long, CardPrice> prices = priceService.latestPrices(printingIds(matches));
        Map<String, DirectionalTotals> totals = new HashMap<>();
        int unpriced = 0;
        for (TradeMatchItemView match : matches) {
            CardPrice price = prices.get(match.matchedCollectionCardPrintingId());
            if (price == null || isEmpty(price)) {
                unpriced += match.quantity();
            } else {
                addPrice(totals, match, price);
            }
        }
        return new ValueTotals(valueViews(totals), unpriced);
    }

    private void addPrice(
            Map<String, DirectionalTotals> totals, TradeMatchItemView match, CardPrice price) {
        add(totals, "usd", price.usd(), match);
        add(totals, "usdFoil", price.usdFoil(), match);
        add(totals, "eur", price.eur(), match);
        add(totals, "tix", price.tix(), match);
    }

    private void add(
            Map<String, DirectionalTotals> totals,
            String currency,
            @Nullable BigDecimal amount,
            TradeMatchItemView match) {
        if (amount == null) {
            return;
        }
        BigDecimal total = amount.multiply(BigDecimal.valueOf(match.quantity()));
        totals.computeIfAbsent(currency, ignored -> new DirectionalTotals()).add(match, total);
    }

    private List<ValueDeltaView> valueViews(Map<String, DirectionalTotals> totals) {
        return totals.entrySet().stream()
                .map(
                        entry ->
                                new ValueDeltaView(
                                        entry.getKey(),
                                        entry.getValue().leftToRight,
                                        entry.getValue().rightToLeft))
                .toList();
    }

    private Collection<Long> printingIds(List<TradeMatchItemView> matches) {
        return matches.stream()
                .map(TradeMatchItemView::matchedCollectionCardPrintingId)
                .distinct()
                .toList();
    }

    private boolean isEmpty(CardPrice price) {
        return price.usd() == null
                && price.usdFoil() == null
                && price.eur() == null
                && price.tix() == null;
    }

    private boolean compatiblePrintings(long offeredPrintingId, long wantedPrintingId) {
        Map<Long, CardPrinting> printings =
                printingRepository
                        .findAllById(List.of(offeredPrintingId, wantedPrintingId))
                        .stream()
                        .collect(Collectors.toMap(CardPrinting::getId, printing -> printing));
        CardPrinting offered = printings.get(offeredPrintingId);
        CardPrinting wanted = printings.get(wantedPrintingId);
        return offered != null
                && wanted != null
                && offered.getCard()
                        .getScryfallOracleId()
                        .equals(wanted.getCard().getScryfallOracleId());
    }

    private TradeList visible(long id, long requesterProfileId) {
        TradeList list =
                listRepository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (list.getProfileId().equals(requesterProfileId)
                || list.getVisibility() != TradeListVisibility.PRIVATE) {
            return list;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    private static final class DirectionalTotals {
        private BigDecimal leftToRight = BigDecimal.ZERO;
        private BigDecimal rightToLeft = BigDecimal.ZERO;

        void add(TradeMatchItemView match, BigDecimal amount) {
            if (match.fromListId() < match.toListId()) {
                leftToRight = leftToRight.add(amount);
            } else {
                rightToLeft = rightToLeft.add(amount);
            }
        }
    }

    private record ValueTotals(List<ValueDeltaView> deltas, int unpriced) {}

    public record TradeMatchView(
            long leftListId,
            long rightListId,
            List<TradeMatchItemView> matches,
            List<ValueDeltaView> valueDeltas,
            int unpricedItems) {}

    public record TradeMatchItemView(
            Long fromListId,
            Long toListId,
            @Nullable Long fromItemId,
            @Nullable Long toItemId,
            Long requestedCardPrintingId,
            Long matchedCollectionCardId,
            Long matchedCollectionCardPrintingId,
            int quantity,
            int availableQuantity,
            boolean exactPrinting) {}

    public record ValueDeltaView(String currency, BigDecimal leftToRight, BigDecimal rightToLeft) {}
}
