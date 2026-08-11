package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.StorageLocation;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StorageLocationService {

    private final StorageLocationRepository locationRepository;
    private final CollectionCardPhysicalMetadataRepository metadataRepository;
    private final CollectionAccessGuard accessGuard;

    public StorageLocationService(
            StorageLocationRepository locationRepository,
            CollectionCardPhysicalMetadataRepository metadataRepository,
            CollectionAccessGuard accessGuard) {
        this.locationRepository = locationRepository;
        this.metadataRepository = metadataRepository;
        this.accessGuard = accessGuard;
    }

    public List<StorageLocation> list() {
        return locationRepository.findByProfileIdOrderByParentIdAscNameAsc(profileId());
    }

    public StorageLocation create(String name, @Nullable UUID parentId) {
        long profileId = profileId();
        validateParent(parentId, profileId);
        return locationRepository.save(new StorageLocation(profileId, name, parentId));
    }

    public StorageLocation update(UUID id, String name, @Nullable UUID parentId) {
        long profileId = profileId();
        StorageLocation location = owned(id, profileId);
        validateNoCycle(id, parentId, profileId);
        location.update(name, parentId);
        return locationRepository.save(location);
    }

    public void delete(UUID id) {
        StorageLocation location = owned(id, profileId());
        if (metadataRepository.existsByStorageLocationId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location contains cards.");
        }
        locationRepository.delete(location);
    }

    private StorageLocation owned(UUID id, long profileId) {
        return locationRepository
                .findByIdAndProfileId(id, profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void validateNoCycle(UUID id, @Nullable UUID parentId, long profileId) {
        UUID cursor = parentId;
        while (cursor != null) {
            if (cursor.equals(id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Location parent cycle.");
            }
            cursor = owned(cursor, profileId).getParentId();
        }
    }

    private void validateParent(@Nullable UUID parentId, long profileId) {
        if (parentId != null) {
            owned(parentId, profileId);
        }
    }

    private long profileId() {
        return accessGuard.profileId();
    }
}
