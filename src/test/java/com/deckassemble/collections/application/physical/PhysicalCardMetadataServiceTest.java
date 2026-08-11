package com.deckassemble.collections.application.physical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.CardCollection;
import com.deckassemble.collections.domain.CardCollectionRepository;
import com.deckassemble.collections.domain.CollectionCard;
import com.deckassemble.collections.domain.CollectionCardRepository;
import com.deckassemble.collections.domain.physical.CardCondition;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadata;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.PhysicalFinish;
import com.deckassemble.collections.domain.physical.PhysicalMetadataValues;
import com.deckassemble.collections.domain.physical.StorageLocation;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PhysicalCardMetadataServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private CardCollectionRepository collectionRepository;
    @Mock private CollectionCardRepository collectionCardRepository;
    @Mock private CollectionCardPhysicalMetadataRepository metadataRepository;
    @Mock private StorageLocationRepository locationRepository;
    @Mock private CollectionAccessGuard accessGuard;

    @Test
    void shouldReturnDefaultMetadataWhenUnset() {
        stubOwnedCard();
        when(metadataRepository.findByCollectionCardId(7L)).thenReturn(Optional.empty());

        var result = service().get(1L, 7L);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldUpdateMetadataWithScaledPriceAndLocation() {
        UUID locationId = UUID.randomUUID();
        stubOwnedCard();
        when(locationRepository.findByIdAndProfileId(locationId, PROFILE_ID))
                .thenReturn(Optional.of(new StorageLocation(PROFILE_ID, "Box", null)));
        when(metadataRepository.findByCollectionCardId(7L)).thenReturn(Optional.empty());
        when(metadataRepository.save(any(CollectionCardPhysicalMetadata.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service().update(1L, 7L, request(locationId, BigDecimal.valueOf(12.3), "usd"));

        assertThat(result.getCondition()).isEqualTo(CardCondition.NEAR_MINT);
        assertThat(result.getFinish()).isEqualTo(PhysicalFinish.FOIL);
        assertThat(result.getPurchasePrice()).isEqualByComparingTo("12.30");
        assertThat(result.getPurchaseCurrency()).isEqualTo("USD");
        assertThat(result.getStorageLocationId()).isEqualTo(locationId);
    }

    @Test
    void shouldRejectPriceWithTooManyFractionDigits() {
        stubOwnedCard();

        assertThatThrownBy(
                        () ->
                                service()
                                        .update(
                                                1L,
                                                7L,
                                                request(null, new BigDecimal("1.999"), "USD")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("scale");
    }

    @Test
    void shouldRejectAnotherUsersLocation() {
        UUID locationId = UUID.randomUUID();
        stubOwnedCard();
        when(locationRepository.findByIdAndProfileId(locationId, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service().update(1L, 7L, request(locationId, BigDecimal.ONE, "USD")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("location");
    }

    @Test
    void shouldFilterOwnedCollectionMetadata() {
        stubOwnedCollection();
        CollectionCardPhysicalMetadata metadata = new CollectionCardPhysicalMetadata(7L);
        metadata.update(
                new PhysicalMetadataValues(
                        CardCondition.LIGHTLY_PLAYED,
                        "en",
                        PhysicalFinish.NONFOIL,
                        null,
                        null,
                        null,
                        "trade",
                        null));
        when(metadataRepository.findByCollectionIdAndFilters(
                        1L, null, CardCondition.LIGHTLY_PLAYED, "en", null))
                .thenReturn(List.of(metadata));

        var result = service().list(1L, null, CardCondition.LIGHTLY_PLAYED, "en", null);

        assertThat(result)
                .singleElement()
                .satisfies(row -> assertThat(row.getNotes()).isEqualTo("trade"));
    }

    private PhysicalCardMetadataService service() {
        return new PhysicalCardMetadataService(
                collectionRepository,
                collectionCardRepository,
                metadataRepository,
                locationRepository,
                accessGuard);
    }

    private PhysicalMetadataValues request(UUID locationId, BigDecimal price, String currency) {
        return new PhysicalMetadataValues(
                CardCondition.NEAR_MINT,
                "en",
                PhysicalFinish.FOIL,
                price,
                currency,
                LocalDate.parse("2024-01-02"),
                "opened",
                locationId);
    }

    private void stubOwnedCard() {
        stubOwnedCollection();
        CollectionCard card = new CollectionCard(1L, 10L, 1, 0);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(collectionCardRepository.findByIdAndCollectionId(7L, 1L))
                .thenReturn(Optional.of(card));
    }

    private void stubOwnedCollection() {
        when(accessGuard.profileId()).thenReturn(PROFILE_ID);
        when(collectionRepository.findByIdAndProfileId(1L, PROFILE_ID))
                .thenReturn(Optional.of(new CardCollection(PROFILE_ID, "Collection", null, false)));
    }
}
