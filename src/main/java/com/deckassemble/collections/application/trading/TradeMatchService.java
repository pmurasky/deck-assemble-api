package com.deckassemble.collections.application.trading;

import com.deckassemble.collections.application.trading.TradeListVisibilityGuard.VisibleLists;
import com.deckassemble.collections.application.trading.TradeValueDeltaCalculator.ValueTotals;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.trading.TradeList;
import com.deckassemble.collections.domain.trading.TradeListItem;
import com.deckassemble.collections.domain.trading.TradeListItemRepository;
import com.deckassemble.collections.domain.trading.TradeListType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TradeMatchService {

    private final TradeListVisibilityGuard visibilityGuard;
    private final TradeListItemRepository itemRepository;
    private final TradeCollectionAvailability availability;
    private final TradePrintingCompatibility printingCompatibility;
    private final TradeValueDeltaCalculator valueDeltaCalculator;

    public TradeMatchService(
            TradeListVisibilityGuard visibilityGuard,
            TradeListItemRepository itemRepository,
            TradeCollectionAvailability availability,
            TradePrintingCompatibility printingCompatibility,
            TradeValueDeltaCalculator valueDeltaCalculator) {
        this.visibilityGuard = visibilityGuard;
        this.itemRepository = itemRepository;
        this.availability = availability;
        this.printingCompatibility = printingCompatibility;
        this.valueDeltaCalculator = valueDeltaCalculator;
    }

    public TradeMatchView compare(long leftListId, long rightListId) {
        VisibleLists lists = visibilityGuard.visible(leftListId, rightListId);
        Map<Long, List<TradeListItem>> itemsByList = itemsByList(leftListId, rightListId);
        List<TradeMatchItemView> matches = matches(leftListId, rightListId, lists, itemsByList);
        ValueTotals totals = valueDeltaCalculator.totals(leftListId, matches);
        return new TradeMatchView(
                leftListId, rightListId, matches, totals.deltas(), totals.unpriced());
    }

    private Map<Long, List<TradeListItem>> itemsByList(long leftListId, long rightListId) {
        return itemRepository
                .findByTradeListIdInOrderById(List.of(leftListId, rightListId))
                .stream()
                .collect(Collectors.groupingBy(TradeListItem::getTradeListId));
    }

    private List<TradeMatchItemView> matches(
            long leftListId,
            long rightListId,
            VisibleLists lists,
            Map<Long, List<TradeListItem>> itemsByList) {
        List<TradeMatchItemView> matches = new ArrayList<>();
        addDirectional(
                matches,
                lists.left(),
                itemsByList.getOrDefault(leftListId, List.of()),
                lists.right(),
                itemsByList.getOrDefault(rightListId, List.of()));
        addDirectional(
                matches,
                lists.right(),
                itemsByList.getOrDefault(rightListId, List.of()),
                lists.left(),
                itemsByList.getOrDefault(leftListId, List.of()));
        return matches;
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
        var context = new DirectionalContext(matches, from, to, new HashMap<>());
        for (TradeListItem offered : fromItems) {
            for (TradeListItem wanted : toItems) {
                matchItem(context, offered, wanted);
            }
        }
    }

    private void matchItem(
            DirectionalContext context, TradeListItem offered, TradeListItem wanted) {
        if (!printingCompatibility.compatible(
                offered.getCardPrintingId(), wanted.getCardPrintingId())) {
            return;
        }
        int remaining = Math.min(offered.getQuantity(), wanted.getQuantity());
        for (CollectionCard card :
                availability.compatibleCards(
                        context.from().getProfileId(), wanted.getCardPrintingId())) {
            remaining = matchCard(context, offered, wanted, card, remaining);
            if (remaining == 0) {
                return;
            }
        }
    }

    private int matchCard(
            DirectionalContext context,
            TradeListItem offered,
            TradeListItem wanted,
            CollectionCard card,
            int remaining) {
        int available = availability.remaining(card, context.remainingByCard());
        int quantity = Math.min(remaining, matchingAvailable(card, offered, wanted, available));
        if (quantity == 0) {
            return remaining;
        }
        context.matches()
                .add(
                        match(
                                new MatchContext(
                                        context.from(),
                                        context.to(),
                                        offered,
                                        wanted,
                                        card,
                                        quantity,
                                        available)));
        context.remainingByCard().put(card.getId(), available - quantity);
        return remaining - quantity;
    }

    private int matchingAvailable(
            CollectionCard card, TradeListItem offered, TradeListItem wanted, int available) {
        if (available <= 0 || !availability.metadataMatches(card, offered, wanted)) {
            return 0;
        }
        return available;
    }

    private TradeMatchItemView match(MatchContext context) {
        return new TradeMatchItemView(
                context.from().getId(),
                context.to().getId(),
                context.offered().getId(),
                context.wanted().getId(),
                context.wanted().getCardPrintingId(),
                context.card().getId(),
                context.card().getCardPrintingId(),
                context.quantity(),
                context.available(),
                context.card().getCardPrintingId().equals(context.wanted().getCardPrintingId()));
    }

    private record DirectionalContext(
            List<TradeMatchItemView> matches,
            TradeList from,
            TradeList to,
            Map<Long, Integer> remainingByCard) {}

    private record MatchContext(
            TradeList from,
            TradeList to,
            TradeListItem offered,
            TradeListItem wanted,
            CollectionCard card,
            int quantity,
            int available) {}

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

    public record ValueDeltaView(
            String currency, java.math.BigDecimal leftToRight, java.math.BigDecimal rightToLeft) {}
}
