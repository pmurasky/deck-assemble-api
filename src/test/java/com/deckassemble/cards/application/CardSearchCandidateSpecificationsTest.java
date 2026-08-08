package com.deckassemble.cards.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.shared.security.CurrentUser;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Focused unit tests for the owned-quantity/price-range wiring that used to live in {@code
 * CardCatalogServiceTest} before this class was extracted. Full predicate correctness (the "owns 0
 * also matches unowned cards" range semantics, actual price matching) is covered against real
 * Postgres in CardControllerIntegrationTest; these tests only verify the port/service are called
 * exactly when expected.
 */
@ExtendWith(MockitoExtension.class)
class CardSearchCandidateSpecificationsTest {

    @Mock private CardPrintingRepository cardPrintingRepository;
    @Mock private CurrentUser currentUser;
    @Mock private CardOwnershipLookup cardOwnershipLookup;
    @Mock private CardPriceService cardPriceService;

    @Test
    void shouldSkipOwnershipLookupWhenAnonymous() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        service().ownedQuantitySpec(new CardSearchFilter.IntRange(1, null));

        verify(cardOwnershipLookup, never()).ownedQuantitiesBySubject(any());
    }

    @Test
    void shouldLookUpOwnedQuantitiesForAuthenticatedSubject() {
        when(currentUser.subject()).thenReturn(Optional.of("auth0|owner"));
        when(cardOwnershipLookup.ownedQuantitiesBySubject("auth0|owner")).thenReturn(Map.of());

        service().ownedQuantitySpec(new CardSearchFilter.IntRange(1, null));

        verify(cardOwnershipLookup).ownedQuantitiesBySubject("auth0|owner");
    }

    @Test
    void shouldFetchTrackedPricesForPriceRangeSpec() {
        when(cardPriceService.trackedPrintingIds()).thenReturn(java.util.Set.of());
        when(cardPriceService.latestPrices(java.util.Set.of())).thenReturn(Map.of());

        service()
                .priceRangeSpec(
                        new CardSearchFilter.PriceRange(java.math.BigDecimal.ONE, null, null));

        verify(cardPriceService).trackedPrintingIds();
    }

    private CardSearchCandidateSpecifications service() {
        return new CardSearchCandidateSpecifications(
                cardPrintingRepository, currentUser, cardOwnershipLookup, cardPriceService);
    }
}
