package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.application.physical.PhysicalCardMetadataService;
import com.deckassemble.collections.application.physical.StorageLocationService;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.physical.PhysicalMetadataValues;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhysicalCollectionController {

    private final StorageLocationService locationService;
    private final PhysicalCardMetadataService metadataService;

    public PhysicalCollectionController(
            StorageLocationService locationService, PhysicalCardMetadataService metadataService) {
        this.locationService = locationService;
        this.metadataService = metadataService;
    }

    @GetMapping("/collection-locations")
    public List<StorageLocationResponse> listLocations() {
        return locationService.list().stream().map(StorageLocationResponse::from).toList();
    }

    @PostMapping("/collection-locations")
    public ResponseEntity<StorageLocationResponse> createLocation(
            @Valid @RequestBody StorageLocationRequest request) {
        StorageLocationResponse location =
                StorageLocationResponse.from(
                        locationService.create(request.name(), request.parentId()));
        return ResponseEntity.created(URI.create("/collection-locations/" + location.id()))
                .body(location);
    }

    @PatchMapping("/collection-locations/{id}")
    public StorageLocationResponse updateLocation(
            @PathVariable UUID id, @Valid @RequestBody StorageLocationRequest request) {
        return StorageLocationResponse.from(
                locationService.update(id, request.name(), request.parentId()));
    }

    @DeleteMapping("/collection-locations/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/collections/{collectionId}/cards/{collectionCardId}/physical")
    public PhysicalCardMetadataResponse getMetadata(
            @PathVariable long collectionId, @PathVariable long collectionCardId) {
        return metadataService
                .get(collectionId, collectionCardId)
                .map(PhysicalCardMetadataResponse::from)
                .orElseGet(() -> PhysicalCardMetadataResponse.empty(collectionCardId));
    }

    @PatchMapping("/collections/{collectionId}/cards/{collectionCardId}/physical")
    public PhysicalCardMetadataResponse updateMetadata(
            @PathVariable long collectionId,
            @PathVariable long collectionCardId,
            @Valid @RequestBody PhysicalCardMetadataRequest request) {
        return PhysicalCardMetadataResponse.from(
                metadataService.update(collectionId, collectionCardId, values(request)));
    }

    @GetMapping("/collections/{collectionId}/cards/physical")
    public List<PhysicalCardMetadataResponse> listMetadata(
            @PathVariable long collectionId,
            @Nullable @RequestParam(required = false) UUID locationId,
            @Nullable @RequestParam(required = false) CardCondition condition,
            @Nullable @RequestParam(required = false) String language,
            @Nullable @RequestParam(required = false) PhysicalFinish finish) {
        return metadataService.list(collectionId, locationId, condition, language, finish).stream()
                .map(PhysicalCardMetadataResponse::from)
                .toList();
    }

    private PhysicalMetadataValues values(PhysicalCardMetadataRequest request) {
        return new PhysicalMetadataValues(
                request.condition(),
                request.language(),
                request.finish(),
                request.purchasePrice(),
                request.purchaseCurrency(),
                request.purchaseDate(),
                request.notes(),
                request.storageLocationId());
    }
}
