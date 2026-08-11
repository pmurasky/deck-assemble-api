package com.deckassemble.collections.application.physical;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.application.CollectionCardNotFoundException;
import com.deckassemble.collections.application.CollectionNotFoundException;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.physical.PhysicalMetadataValues;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class PhysicalCardMetadataService {

    private final CardCollectionRepository collectionRepository;
    private final CollectionCardRepository collectionCardRepository;
    private final CollectionCardPhysicalMetadataRepository metadataRepository;
    private final StorageLocationRepository locationRepository;
    private final CollectionAccessGuard accessGuard;

    public PhysicalCardMetadataService(
            CardCollectionRepository collectionRepository,
            CollectionCardRepository collectionCardRepository,
            CollectionCardPhysicalMetadataRepository metadataRepository,
            StorageLocationRepository locationRepository,
            CollectionAccessGuard accessGuard) {
        this.collectionRepository = collectionRepository;
        this.collectionCardRepository = collectionCardRepository;
        this.metadataRepository = metadataRepository;
        this.locationRepository = locationRepository;
        this.accessGuard = accessGuard;
    }

    public Optional<CollectionCardPhysicalMetadata> get(long collectionId, long collectionCardId) {
        ownedCard(collectionId, collectionCardId);
        return metadataRepository.findByCollectionCardId(collectionCardId);
    }

    public CollectionCardPhysicalMetadata update(
            long collectionId, long collectionCardId, PhysicalMetadataValues values) {
        ownedCard(collectionId, collectionCardId);
        CollectionCardPhysicalMetadata metadata = metadata(collectionCardId);
        metadata.update(validated(values));
        return metadataRepository.save(metadata);
    }

    public List<CollectionCardPhysicalMetadata> list(
            long collectionId,
            @Nullable UUID locationId,
            @Nullable CardCondition condition,
            @Nullable String language,
            @Nullable PhysicalFinish finish) {
        ownedCollection(collectionId);
        return metadataRepository.findByCollectionIdAndFilters(
                collectionId, locationId, condition, normalize(language), finish);
    }

    private PhysicalMetadataValues validated(PhysicalMetadataValues values) {
        validateLocation(values.storageLocationId());
        return new PhysicalMetadataValues(
                values.condition(),
                normalize(values.language()),
                values.finish(),
                price(values.purchasePrice()),
                currency(values.purchaseCurrency()),
                values.purchaseDate(),
                values.notes(),
                values.storageLocationId());
    }

    private CollectionCardPhysicalMetadata metadata(long collectionCardId) {
        return metadataRepository
                .findByCollectionCardId(collectionCardId)
                .orElseGet(() -> new CollectionCardPhysicalMetadata(collectionCardId));
    }

    private void validateLocation(@Nullable UUID locationId) {
        if (locationId == null) {
            return;
        }
        if (locationRepository.findByIdAndProfileId(locationId, profileId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage location.");
        }
    }

    private @Nullable BigDecimal price(@Nullable BigDecimal price) {
        if (price == null) {
            return null;
        }
        try {
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Price scale must be <= 2.", exception);
        }
    }

    private @Nullable String currency(@Nullable String currency) {
        if (currency == null) {
            return null;
        }
        return currency.toUpperCase(Locale.ROOT);
    }

    private @Nullable String normalize(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private CardCollection ownedCollection(long collectionId) {
        return collectionRepository
                .findByIdAndProfileId(collectionId, profileId())
                .orElseThrow(CollectionNotFoundException::new);
    }

    private CollectionCard ownedCard(long collectionId, long collectionCardId) {
        ownedCollection(collectionId);
        return collectionCardRepository
                .findByIdAndCollectionId(collectionCardId, collectionId)
                .orElseThrow(CollectionCardNotFoundException::new);
    }

    private long profileId() {
        return accessGuard.profileId();
    }
}
