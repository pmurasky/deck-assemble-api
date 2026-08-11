package com.deckassemble.decks.application;

import java.util.List;

public record OwnershipSyncResponse(
        int changedCount,
        List<OwnershipChange> changes,
        int unavailableCount,
        List<PhysicalAvailability> physicalAvailability) {

    public OwnershipSyncResponse(int changedCount, List<OwnershipChange> changes) {
        this(changedCount, changes, 0, List.of());
    }

    public record OwnershipChange(
            long deckCardId, long cardPrintingId, String fromStatus, String toStatus) {}

    public record PhysicalAvailability(
            long deckCardId,
            long cardPrintingId,
            int deckQuantity,
            int allocatedQuantity,
            int availableQuantity,
            int missingQuantity) {}
}
