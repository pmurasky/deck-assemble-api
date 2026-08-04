package com.deckassemble.recommendations.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.collections.application.CollectionService;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class EdhrecCacheWarmupJobTest {

    @Mock private CollectionService collectionService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private EdhrecCommanderService edhrecCommanderService;
    @InjectMocks private EdhrecCacheWarmupJob job;

    @Test
    void shouldWarmOnlyActiveEligibleCommanders() {
        var commander = commander("oracle-1", "Commander One");
        var notCommander = new Card("oracle-2", "Plain Bear");
        var inactive = commander("oracle-3", "Retired Commander");
        inactive.setActive(false);
        when(collectionService.getAllOwnedPrintingIds()).thenReturn(Set.of(1L, 2L, 3L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(1L, 2L, 3L)))
                .thenReturn(Map.of(1L, commander, 2L, notCommander, 3L, inactive));

        job.warmCommanderCache();

        verify(edhrecCommanderService).getCommanderData("oracle-1", "Commander One");
        verifyNoMoreInteractions(edhrecCommanderService);
    }

    @Test
    void shouldContinueWarmingWhenOneCommanderFails() {
        var failing = commander("oracle-1", "Failing Commander");
        var healthy = commander("oracle-2", "Healthy Commander");
        when(collectionService.getAllOwnedPrintingIds()).thenReturn(Set.of(1L, 2L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(1L, 2L)))
                .thenReturn(Map.of(1L, failing, 2L, healthy));
        when(edhrecCommanderService.getCommanderData("oracle-1", "Failing Commander"))
                .thenThrow(new RestClientException("boom"));
        when(edhrecCommanderService.getCommanderData("oracle-2", "Healthy Commander"))
                .thenReturn("{}");

        job.warmCommanderCache();

        verify(edhrecCommanderService).getCommanderData("oracle-2", "Healthy Commander");
    }

    private static Card commander(String oracleId, String name) {
        var card = new Card(oracleId, name);
        card.setActive(true);
        var face = new CardFace(card, 0, name);
        face.setTypeLine("Legendary Creature — Human");
        card.getFaces().add(face);
        return card;
    }
}
