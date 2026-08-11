package com.deckassemble.collections.domain.physical;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, UUID> {

    List<StorageLocation> findByProfileIdOrderByParentIdAscNameAsc(Long profileId);

    Optional<StorageLocation> findByIdAndProfileId(UUID id, Long profileId);
}
