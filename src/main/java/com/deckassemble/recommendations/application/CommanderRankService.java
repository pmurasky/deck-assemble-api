package com.deckassemble.recommendations.application;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.recommendations.domain.EdhrecClient;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class CommanderRankService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommanderRankService.class);

    private final EdhrecClient edhrecClient;
    private final CardCatalogService cardCatalogService;
    private final ObjectMapper objectMapper;

    public CommanderRankService(
            EdhrecClient edhrecClient,
            CardCatalogService cardCatalogService,
            ObjectMapper objectMapper) {
        this.edhrecClient = edhrecClient;
        this.cardCatalogService = cardCatalogService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(cron = "0 30 6 * * SUN")
    public void refreshCommanderRanks() {
        Map<String, Integer> ranksByName;
        try {
            ranksByName = parseTopCommanders(edhrecClient.fetchTopCommanders());
        } catch (RestClientException | JacksonException exception) {
            LOGGER.warn("Failed to refresh commander ranks from EDHREC", exception);
            return;
        }
        if (ranksByName.isEmpty()) {
            LOGGER.warn("EDHREC returned no top commanders; keeping existing ranks");
            return;
        }
        var updated = cardCatalogService.updateCommanderRanks(ranksByName);
        LOGGER.info("Updated commander ranks for {} cards", updated);
    }

    private Map<String, Integer> parseTopCommanders(String payload) {
        var ranksByName = new LinkedHashMap<String, Integer>();
        var cardlists =
                objectMapper
                        .readTree(payload)
                        .path("container")
                        .path("json_dict")
                        .path("cardlists");
        if (!cardlists.isArray() || cardlists.isEmpty()) {
            return ranksByName;
        }
        var rank = 1;
        for (JsonNode cardview : cardlists.get(0).path("cardviews")) {
            var name = cardview.path("name").asString();
            if (!name.isBlank() && !ranksByName.containsKey(name)) {
                ranksByName.put(name, rank++);
            }
        }
        return ranksByName;
    }
}
