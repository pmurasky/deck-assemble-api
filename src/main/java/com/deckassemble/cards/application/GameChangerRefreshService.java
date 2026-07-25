package com.deckassemble.cards.application;

import com.deckassemble.cards.domain.CardSearchPage;
import com.deckassemble.cards.domain.ScryfallClient;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class GameChangerRefreshService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameChangerRefreshService.class);
    private static final String GAME_CHANGER_QUERY = "is:gamechanger";

    private final ScryfallClient scryfallClient;
    private final CardCatalogService cardCatalogService;

    public GameChangerRefreshService(
            ScryfallClient scryfallClient, CardCatalogService cardCatalogService) {
        this.scryfallClient = scryfallClient;
        this.cardCatalogService = cardCatalogService;
    }

    @Scheduled(cron = "0 45 6 * * SUN")
    public void refreshGameChangers() {
        try {
            var oracleIds = gameChangerOracleIds();
            if (oracleIds.isEmpty()) {
                LOGGER.warn("Scryfall returned no Game Changers; keeping existing flags");
                return;
            }
            LOGGER.info(
                    "Updated Game Changer flags for {} cards",
                    cardCatalogService.updateGameChangers(oracleIds));
        } catch (RestClientException | IllegalStateException exception) {
            LOGGER.warn("Failed to refresh Game Changers from Scryfall", exception);
        }
    }

    private Set<String> gameChangerOracleIds() {
        var oracleIds = new HashSet<String>();
        CardSearchPage page = scryfallClient.searchCards(GAME_CHANGER_QUERY);
        addOracleIds(page, oracleIds);
        while (page.hasMore()) {
            page = scryfallClient.searchCards(nextPage(page.nextPage()));
            addOracleIds(page, oracleIds);
        }
        return oracleIds;
    }

    private static void addOracleIds(CardSearchPage page, Set<String> oracleIds) {
        page.data().stream()
                .map(data -> data.oracleId())
                .filter(id -> id != null)
                .forEach(oracleIds::add);
    }

    private static URI nextPage(URI nextPage) {
        if (nextPage == null) {
            throw new IllegalStateException("Scryfall returned a next page without a URL");
        }
        return nextPage;
    }
}
