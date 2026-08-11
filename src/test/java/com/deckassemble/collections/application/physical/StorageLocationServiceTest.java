package com.deckassemble.collections.application.physical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.collections.application.CollectionAccessGuard;
import com.deckassemble.collections.domain.physical.CollectionCardPhysicalMetadataRepository;
import com.deckassemble.collections.domain.physical.StorageLocation;
import com.deckassemble.collections.domain.physical.StorageLocationRepository;
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
class StorageLocationServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private StorageLocationRepository locationRepository;
    @Mock private CollectionCardPhysicalMetadataRepository metadataRepository;
    @Mock private CollectionAccessGuard accessGuard;

    @Test
    void shouldCreateChildLocationForCurrentProfile() {
        UUID parentId = UUID.randomUUID();
        StorageLocation parent = location(parentId, null, "Binder");
        when(accessGuard.profileId()).thenReturn(PROFILE_ID);
        when(locationRepository.findByIdAndProfileId(parentId, PROFILE_ID))
                .thenReturn(Optional.of(parent));
        when(locationRepository.save(any(StorageLocation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = service().create("Page 1", parentId);

        assertThat(result.getName()).isEqualTo("Page 1");
        assertThat(result.getParentId()).isEqualTo(parentId);
    }

    @Test
    void shouldRejectParentCycleOnUpdate() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        StorageLocation root = location(rootId, null, "Root");
        StorageLocation child = location(childId, rootId, "Child");
        when(accessGuard.profileId()).thenReturn(PROFILE_ID);
        when(locationRepository.findByIdAndProfileId(rootId, PROFILE_ID))
                .thenReturn(Optional.of(root));
        when(locationRepository.findByIdAndProfileId(childId, PROFILE_ID))
                .thenReturn(Optional.of(child));

        assertThatThrownBy(() -> service().update(rootId, "Root", childId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void shouldRejectDeleteWhenCardsReferenceLocation() {
        UUID locationId = UUID.randomUUID();
        when(accessGuard.profileId()).thenReturn(PROFILE_ID);
        when(locationRepository.findByIdAndProfileId(locationId, PROFILE_ID))
                .thenReturn(Optional.of(location(locationId, null, "Box")));
        when(locationRepository.findByProfileIdOrderByParentIdAscNameAsc(PROFILE_ID))
                .thenReturn(List.of(location(locationId, null, "Box")));
        when(metadataRepository.existsByStorageLocationId(locationId)).thenReturn(true);

        assertThatThrownBy(() -> service().delete(locationId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("contains cards");
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void shouldRejectDeleteWhenCardsReferenceDescendantLocation() {
        UUID rootId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();
        UUID grandchildId = UUID.randomUUID();
        when(accessGuard.profileId()).thenReturn(PROFILE_ID);
        when(locationRepository.findByIdAndProfileId(rootId, PROFILE_ID))
                .thenReturn(Optional.of(location(rootId, null, "Root")));
        when(locationRepository.findByProfileIdOrderByParentIdAscNameAsc(PROFILE_ID))
                .thenReturn(
                        List.of(
                                location(rootId, null, "Root"),
                                location(childId, rootId, "Child"),
                                location(grandchildId, childId, "Grandchild")));
        when(metadataRepository.existsByStorageLocationId(any()))
                .thenAnswer(inv -> inv.getArgument(0).equals(grandchildId));

        assertThatThrownBy(() -> service().delete(rootId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("contains cards");
        verify(locationRepository, never()).delete(any());
    }

    private StorageLocationService service() {
        return new StorageLocationService(locationRepository, metadataRepository, accessGuard);
    }

    private StorageLocation location(UUID id, UUID parentId, String name) {
        StorageLocation location = new StorageLocation(PROFILE_ID, name, parentId);
        ReflectionTestUtils.setField(location, "id", id);
        return location;
    }
}
