package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationPartView;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationView;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record PhysicalCardAllocationResponse(
        @Nullable Long id,
        long deckId,
        long deckCardId,
        long deckCardPrintingId,
        @Nullable Long collectionCardId,
        @Nullable Long collectionCardPrintingId,
        int deckQuantity,
        int quantity,
        int ownedQuantity,
        int allocatedQuantity,
        int availableQuantity,
        int missingQuantity,
        boolean exactPrinting,
        List<PhysicalCardAllocationPartResponse> allocations) {

    public static PhysicalCardAllocationResponse from(AllocationView view) {
        return new PhysicalCardAllocationResponse(
                view.id(),
                view.deckId(),
                view.deckCardId(),
                view.deckCardPrintingId(),
                view.collectionCardId(),
                view.collectionCardPrintingId(),
                view.deckQuantity(),
                view.quantity(),
                view.ownedQuantity(),
                view.allocatedQuantity(),
                view.availableQuantity(),
                view.missingQuantity(),
                view.exactPrinting(),
                view.allocations().stream().map(PhysicalCardAllocationPartResponse::from).toList());
    }

    public record PhysicalCardAllocationPartResponse(
            Long id,
            Long collectionCardId,
            Long collectionCardPrintingId,
            int quantity,
            boolean exactPrinting) {

        static PhysicalCardAllocationPartResponse from(AllocationPartView view) {
            return new PhysicalCardAllocationPartResponse(
                    view.id(),
                    view.collectionCardId(),
                    view.collectionCardPrintingId(),
                    view.quantity(),
                    view.exactPrinting());
        }
    }
}
