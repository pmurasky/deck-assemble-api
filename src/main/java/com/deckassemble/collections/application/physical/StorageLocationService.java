package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.StorageLocation;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
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
        long profileId = profileId();
        StorageLocation location = owned(id, profileId);
        if (containsCardsInSubtree(id, profileId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Location contains cards.");
        }
        locationRepository.delete(location);
    }

    private boolean containsCardsInSubtree(UUID rootId, long profileId) {
        Set<UUID> subtree = subtreeIds(rootId, profileId);
        return subtree.stream().anyMatch(metadataRepository::existsByStorageLocationId);
    }

    private Set<UUID> subtreeIds(UUID rootId, long profileId) {
        List<StorageLocation> locations =
                locationRepository.findByProfileIdOrderByParentIdAscNameAsc(profileId);
        Set<UUID> subtree = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            UUID current = queue.remove();
            if (!subtree.add(current)) {
                continue;
            }
            locations.stream()
                    .filter(location -> current.equals(location.getParentId()))
                    .map(StorageLocation::getId)
                    .filter(id -> !subtree.contains(id))
                    .forEach(queue::add);
        }
        return subtree;
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
