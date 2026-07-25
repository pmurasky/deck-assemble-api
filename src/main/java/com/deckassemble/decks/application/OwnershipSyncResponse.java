package com.deckassemble.decks.application;

import java.util.List;

public record OwnershipSyncResponse(int changedCount, List<OwnershipChange> changes) {

    public record OwnershipChange(
            long deckCardId, long cardPrintingId, String fromStatus, String toStatus) {}
}
