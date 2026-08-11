package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PhysicalCardMetadataResponse(
        Long collectionCardId,
        @Nullable CardCondition condition,
        @Nullable String language,
        @Nullable PhysicalFinish finish,
        @Nullable BigDecimal purchasePrice,
        @Nullable String purchaseCurrency,
        @Nullable LocalDate purchaseDate,
        @Nullable String notes,
        @Nullable UUID storageLocationId) {

    public static PhysicalCardMetadataResponse empty(long collectionCardId) {
        return new PhysicalCardMetadataResponse(
                collectionCardId, null, null, null, null, null, null, null, null);
    }

    public static PhysicalCardMetadataResponse from(CollectionCardPhysicalMetadata metadata) {
        return new PhysicalCardMetadataResponse(
                metadata.getCollectionCardId(),
                metadata.getCondition(),
                metadata.getLanguage(),
                metadata.getFinish(),
                metadata.getPurchasePrice(),
                metadata.getPurchaseCurrency(),
                metadata.getPurchaseDate(),
                metadata.getNotes(),
                metadata.getStorageLocationId());
    }
}
