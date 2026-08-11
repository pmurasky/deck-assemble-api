package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.domain.CollectionCard;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

final class PhysicalCardAllocationPlanner {

    private PhysicalCardAllocationPlanner() {}

    static List<AllocationSlice> plan(
            List<CollectionCard> cards,
            Map<Long, Integer> allocated,
            @Nullable Long requestedCollectionCardId,
            int quantity) {
        var slices = new ArrayList<AllocationSlice>();
        int remaining = quantity;
        for (CollectionCard card : matchingCards(cards, requestedCollectionCardId)) {
            int available = availableQuantity(card, allocated);
            int allocatedQuantity = Math.min(remaining, available);
            if (allocatedQuantity > 0) {
                slices.add(new AllocationSlice(card, allocatedQuantity));
                remaining -= allocatedQuantity;
            }
            if (remaining == 0) {
                return slices;
            }
        }
        throw PhysicalCardAllocationService.notEnoughCopies();
    }

    private static List<CollectionCard> matchingCards(
            List<CollectionCard> cards, @Nullable Long requestedCollectionCardId) {
        return cards.stream()
                .filter(
                        card ->
                                requestedCollectionCardId == null
                                        || card.getId().equals(requestedCollectionCardId))
                .toList();
    }

    private static int availableQuantity(CollectionCard card, Map<Long, Integer> allocated) {
        return card.getRegularQuantity()
                + card.getFoilQuantity()
                - allocated.getOrDefault(card.getId(), 0);
    }

    record AllocationSlice(CollectionCard card, int quantity) {}
}
