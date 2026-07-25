package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.application.CollectionService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OwnershipCheckerTest {

    private static final long PROFILE_ID = 42L;

    @Mock private CardCatalogService cardCatalogService;
    @Mock private CollectionService collectionService;

    @InjectMocks private OwnershipChecker ownershipChecker;

    @Test
    void shouldReportOwnedWhenSameOracleOwnedViaDifferentPrinting() {
        when(cardCatalogService.getOracleIdsByPrintingIds(List.of(10L)))
                .thenReturn(Map.of(10L, "oracle-1"));
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(99L));
        when(cardCatalogService.getOracleIdsByPrintingIds(Set.of(99L)))
                .thenReturn(Map.of(99L, "oracle-1"));

        assertThat(ownershipChecker.isOwned(PROFILE_ID, 10L)).isTrue();
    }

    @Test
    void shouldReportNotOwnedWhenOracleDiffers() {
        when(cardCatalogService.getOracleIdsByPrintingIds(List.of(10L)))
                .thenReturn(Map.of(10L, "oracle-1"));
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(99L));
        when(cardCatalogService.getOracleIdsByPrintingIds(Set.of(99L)))
                .thenReturn(Map.of(99L, "oracle-2"));

        assertThat(ownershipChecker.isOwned(PROFILE_ID, 10L)).isFalse();
    }

    @Test
    void shouldReturnEmptyForEmptyInputWithoutQueryingServices() {
        assertThat(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, Set.of())).isEmpty();

        verifyNoInteractions(cardCatalogService, collectionService);
    }

    @Test
    void shouldReturnEmptyWhenCollectionIsEmpty() {
        when(cardCatalogService.getOracleIdsByPrintingIds(List.of(10L)))
                .thenReturn(Map.of(10L, "oracle-1"));
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());

        assertThat(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, List.of(10L))).isEmpty();
    }

    @Test
    void shouldFilterToOnlyOwnedPrintings() {
        when(cardCatalogService.getOracleIdsByPrintingIds(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, "oracle-1", 11L, "oracle-2"));
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(99L));
        when(cardCatalogService.getOracleIdsByPrintingIds(Set.of(99L)))
                .thenReturn(Map.of(99L, "oracle-1"));

        assertThat(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, List.of(10L, 11L)))
                .containsExactlyInAnyOrder(10L);
    }
}
