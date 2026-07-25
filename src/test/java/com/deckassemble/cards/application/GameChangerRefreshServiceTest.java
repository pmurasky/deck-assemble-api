package com.deckassemble.cards.application;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.CardImportData;
import com.deckassemble.cards.domain.CardSearchPage;
import com.deckassemble.cards.domain.ScryfallClient;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class GameChangerRefreshServiceTest {

    @Mock private ScryfallClient scryfallClient;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldRefreshGameChangerFlagsFromScryfall() {
        when(scryfallClient.searchCards("is:gamechanger"))
                .thenReturn(new CardSearchPage(List.of(card("oracle-mana-vault")), false, null));

        service().refreshGameChangers();

        verify(cardCatalogService).updateGameChangers(java.util.Set.of("oracle-mana-vault"));
    }

    @Test
    void shouldKeepExistingFlagsWhenScryfallFails() {
        when(scryfallClient.searchCards("is:gamechanger"))
                .thenThrow(new RestClientException("down"));

        service().refreshGameChangers();

        verifyNoInteractions(cardCatalogService);
    }

    private static CardImportData card(String oracleId) {
        return new CardImportData(
                "printing",
                oracleId,
                "Mana Vault",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "set",
                "set",
                "Set",
                "1",
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                "en",
                null,
                true);
    }

    private GameChangerRefreshService service() {
        return new GameChangerRefreshService(scryfallClient, cardCatalogService);
    }
}
