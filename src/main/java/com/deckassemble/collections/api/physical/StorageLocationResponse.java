package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.domain.physical.StorageLocation;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StorageLocationResponse(UUID id, String name, @Nullable UUID parentId) {

    public static StorageLocationResponse from(StorageLocation location) {
        return new StorageLocationResponse(
                location.getId(), location.getName(), location.getParentId());
    }
}
