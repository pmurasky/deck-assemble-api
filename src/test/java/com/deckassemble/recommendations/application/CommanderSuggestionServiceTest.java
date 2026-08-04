package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommanderSuggestionServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private CardCatalogService cardCatalogService;
    @Mock private CardPriceService cardPriceService;
    @Mock private CollectionService collectionService;
    @Mock private EdhrecCommanderService edhrecCommanderService;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;

    private CommanderSuggestionService service;

    @BeforeEach
    void setUp() {
        service =
                new CommanderSuggestionService(
                        cardCatalogService,
                        cardPriceService,
                        collectionService,
                        edhrecCommanderService,
                        currentUser,
                        profileService);
    }

    @Test
    void shouldRankOwnedCommandersByCoverageThenCost() {
        var highCoverage = commander(1L, "High Coverage", "high", 4);
        var lowCoverage = commander(2L, "Low Coverage", "low", 2);
        var ownedStaple = card(3L, "Owned Staple", "owned");
        var missingStaple = card(4L, "Missing Staple", "missing");
        stubUser();
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(10L, 11L, 12L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(10L, 11L, 12L)))
                .thenReturn(Map.of(10L, highCoverage, 11L, lowCoverage, 12L, ownedStaple));
        when(edhrecCommanderService.getCardScores("high", "High Coverage"))
                .thenReturn(Map.of("Owned Staple", new CardScore(0.5, 10L)));
        when(edhrecCommanderService.getCardScores("low", "Low Coverage"))
                .thenReturn(Map.of("Missing Staple", new CardScore(0.5, 10L)));
        when(cardCatalogService.getCardsByNames(Set.of("Owned Staple", "Missing Staple")))
                .thenReturn(List.of(ownedStaple, missingStaple));
        when(cardCatalogService.getLatestPrintingIdByCardIds(List.of(3L, 4L)))
                .thenReturn(Map.of(3L, 12L, 4L, 13L));
        when(cardPriceService.latestPrices(List.of(13L)))
                .thenReturn(Map.of(13L, new CardPrice(new BigDecimal("7.50"), null, null, null)));

        var suggestions = service.suggest();

        assertThat(suggestions)
                .extracting(CommanderSuggestion::commanderName)
                .containsExactly("High Coverage", "Low Coverage");
        assertThat(suggestions.get(0).coveragePercent()).isEqualByComparingTo("100.00");
        assertThat(suggestions.get(1).missingCardCount()).isEqualTo(1);
        assertThat(suggestions.get(1).estimatedCompletionCostUsd()).isEqualByComparingTo("7.50");
    }

    @Test
    void shouldCountCatalogMissingCardsAsUnpriced() {
        var commander = commander(1L, "Commander", "commander", 1);
        stubUser();
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(10L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(10L)))
                .thenReturn(Map.of(10L, commander));
        when(edhrecCommanderService.getCardScores("commander", "Commander"))
                .thenReturn(Map.of("Unknown Staple", new CardScore(0.5, 10L)));
        when(cardCatalogService.getCardsByNames(Set.of("Unknown Staple"))).thenReturn(List.of());
        when(cardCatalogService.getLatestPrintingIdByCardIds(List.of())).thenReturn(Map.of());
        when(cardPriceService.latestPrices(List.of())).thenReturn(Map.of());

        var suggestion = service.suggest().getFirst();

        assertThat(suggestion.coveragePercent()).isEqualByComparingTo("0.00");
        assertThat(suggestion.estimatedCompletionCostUsd()).isEqualByComparingTo("0");
        assertThat(suggestion.unpricedMissingCardCount()).isEqualTo(1);
    }

    @Test
    void shouldExcludeOwnedNonCommanders() {
        var nonCommander = card(1L, "Not A Commander", "not-a-commander");
        stubUser();
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(10L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(10L)))
                .thenReturn(Map.of(10L, nonCommander));

        assertThat(service.suggest()).isEmpty();
    }

    private void stubUser() {
        var profile = new Profile("sub", "user");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileService.getOrCreate("sub")).thenReturn(profile);
    }

    private static Card commander(long id, String name, String oracleId, int rank) {
        var card = card(id, name, oracleId);
        card.setCommanderRank(rank);
        var face = new CardFace(card, 0, name);
        face.setTypeLine("Legendary Creature — Human");
        card.getFaces().add(face);
        return card;
    }

    private static Card card(long id, String name, String oracleId) {
        var card = new Card(oracleId, name);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }
}
