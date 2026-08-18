package com.deckassemble.decks.application.upgrades;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardAnalysisView;
import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.alternatives.DeckCardAlternative;
import com.deckassemble.decks.application.alternatives.DeckCardAlternativeService;
import com.deckassemble.decks.application.analysis.CommanderBracket;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
import com.deckassemble.decks.application.upgrades.DeckUpgradeService.Objective;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckUpgradeServiceTest {

    private static final long DECK_ID = 7L;

    @Mock private DeckCardService deckCardService;
    @Mock private DeckAnalysisService deckAnalysisService;
    @Mock private DeckCardAlternativeService alternativeService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CardPriceService cardPriceService;

    private DeckUpgradeService service;

    @BeforeEach
    void setUp() {
        service =
                new DeckUpgradeService(
                        deckCardService,
                        deckAnalysisService,
                        alternativeService,
                        cardCatalogService,
                        cardPriceService);
    }

    @Test
    void shouldReplaceProxyWithOwnedAlternative() {
        var cards =
                List.of(
                        deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "OWNED", "Owned Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 1, "OWNED", 1),
                        amounts("usd", "7.00"),
                        amounts("usd", "2.00"),
                        categories("DRAW", 2),
                        true));
        stubViews(Map.of(100L, drawView(100L), 200L, drawView(200L), 300L, drawView(300L)));
        stubPrices(Map.of(100L, price("2.00"), 200L, price("5.00")));
        stubAlternatives(1L, alt(300L, "Owned Swap", true));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions()).hasSize(1);
        var substitution = plan.substitutions().getFirst();
        assertThat(substitution.deckCardId()).isEqualTo(1L);
        assertThat(substitution.removedPrintingId()).isEqualTo(100L);
        assertThat(substitution.removedOwnershipStatus()).isEqualTo("PROXY");
        assertThat(substitution.addedPrintingId()).isEqualTo(300L);
        assertThat(substitution.addedName()).isEqualTo("Owned Swap");
        assertThat(substitution.addedOwned()).isTrue();
        assertThat(substitution.cost()).isEqualByComparingTo("0");
        assertThat(plan.before().ownershipBreakdown()).containsEntry("PROXY", 1);
        assertThat(plan.after().ownershipBreakdown()).isEqualTo(new TreeMap<>(Map.of("OWNED", 2)));
        assertThat(plan.after().valueByCurrency()).containsEntry("usd", new BigDecimal("5.00"));
        assertThat(plan.after().missingCostByCurrency()).isEmpty();
        assertThat(plan.after().functionalCategories()).containsEntry("DRAW", 2);
        assertThat(plan.after().legal()).isTrue();
    }

    @Test
    void shouldOnlyTargetProxyCardsForReplaceProxies() {
        var cards =
                List.of(
                        deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "OWNED", "Owned Draw"),
                        deckCard(3L, 300L, 1, "MAIN_DECK", "WISHLIST", "Wishlist Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 1, "OWNED", 1, "WISHLIST", 1),
                        amounts("usd", "7.00"),
                        amounts("usd", "2.00"),
                        categories("DRAW", 3),
                        true));
        stubViews(Map.of(100L, drawView(100L), 200L, drawView(200L), 300L, drawView(300L)));
        stubPrices(Map.of(100L, price("2.00"), 200L, price("5.00")));
        stubAlternatives(1L, alt(400L, "Owned Swap", true));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions()).hasSize(1);
        verify(alternativeService, times(1)).suggest(anyLong(), anyLong(), anyInt(), anyBoolean());
        verify(alternativeService, never()).suggest(DECK_ID, 2L, 10, true);
        verify(alternativeService, never()).suggest(DECK_ID, 3L, 10, true);
    }

    @Test
    void shouldReturnNoSolutionWhenNoProxiesExist() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Owned Draw"));
        var before =
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "5.00"),
                        amounts(),
                        categories("DRAW", 1),
                        true);
        stubDeck(cards, before);
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("5.00")));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions()).isEmpty();
        assertThat(plan.after()).isEqualTo(plan.before());
        verify(alternativeService, never()).suggest(anyLong(), anyLong(), anyInt(), anyBoolean());
    }

    @Test
    void shouldHonorMaxChangesInDeterministicTargetOrder() {
        var cards =
                List.of(
                        deckCard(3L, 300L, 1, "MAIN_DECK", "PROXY", "Proxy Three"),
                        deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy One"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "PROXY", "Proxy Two"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 3),
                        amounts("usd", "6.00"),
                        amounts("usd", "6.00"),
                        categories("DRAW", 3),
                        true));
        stubViews(Map.of(100L, drawView(100L), 200L, drawView(200L), 300L, drawView(300L)));
        stubPrices(Map.of(100L, price("2.00"), 200L, price("2.00"), 300L, price("2.00")));
        stubAlternatives(1L, alt(400L, "Swap One", true));
        stubAlternatives(2L, alt(401L, "Swap Two", true));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, 2);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Swap One", "Swap Two");
        assertThat(plan.maxChanges()).isEqualTo(2);
        verify(alternativeService, never()).suggest(DECK_ID, 3L, 10, true);
    }

    @Test
    void shouldSkipProxyWithoutOwnedAlternative() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 1),
                        amounts("usd", "2.00"),
                        amounts("usd", "2.00"),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00")));
        stubAlternatives(1L, alt(300L, "Unowned Swap", false));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions()).isEmpty();
        assertThat(plan.after()).isEqualTo(plan.before());
    }

    @Test
    void shouldImproveWithinBudgetCeiling() {
        var cards =
                List.of(
                        deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw A"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "OWNED", "Draw B"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 2),
                        amounts("usd", "7.00"),
                        amounts(),
                        categories("DRAW", 2),
                        true));
        stubViews(Map.of(100L, drawView(100L), 200L, drawView(200L)));
        stubAlternatives(1L, alt(300L, "Upgrade A", false));
        stubAlternatives(
                2L, alt(301L, "Pricey Upgrade B", false), alt(302L, "Cheap Upgrade B", false));
        stubPrices(
                Map.of(
                        100L, price("2.00"),
                        200L, price("5.00"),
                        300L, price("3.00"),
                        301L, price("4.00"),
                        302L, price("1.50")));

        var plan =
                service.plan(
                        DECK_ID,
                        Objective.IMPROVE_UNDER_BUDGET,
                        new BigDecimal("5.00"),
                        "usd",
                        null);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Upgrade A", "Cheap Upgrade B");
        assertThat(plan.substitutions().get(0).cost()).isEqualByComparingTo("3.00");
        assertThat(plan.substitutions().get(1).cost()).isEqualByComparingTo("1.50");
        assertThat(plan.after().valueByCurrency()).containsEntry("usd", new BigDecimal("4.50"));
    }

    @Test
    void shouldSkipUnpricedAlternativeWhenBudgetIsSet() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw A"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "2.00"),
                        amounts(),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00")));
        stubAlternatives(1L, alt(300L, "Unpriced Upgrade", false));

        var plan =
                service.plan(
                        DECK_ID,
                        Objective.IMPROVE_UNDER_BUDGET,
                        new BigDecimal("10.00"),
                        null,
                        null);

        assertThat(plan.substitutions()).isEmpty();
    }

    @Test
    void shouldAllowUnboundedImprovementWithoutBudget() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw A"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "2.00"),
                        amounts(),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00"), 300L, price("1000.00")));
        stubAlternatives(1L, alt(300L, "Premium Upgrade", false));

        var plan = service.plan(DECK_ID, Objective.IMPROVE_UNDER_BUDGET, null, null, null);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Premium Upgrade");
        assertThat(plan.substitutions().getFirst().cost()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldRequirePriceForImprovementEvenWithoutBudget() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw A"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "2.00"),
                        amounts(),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00")));
        stubAlternatives(1L, alt(300L, "Unpriced Upgrade", false));

        var plan = service.plan(DECK_ID, Objective.IMPROVE_UNDER_BUDGET, null, null, null);

        assertThat(plan.substitutions()).isEmpty();
    }

    @Test
    void shouldCloseCategoryGapsWithMatchingAlternatives() {
        var cards =
                List.of(
                        deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw Spell"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "OWNED", "Filler One"),
                        deckCard(3L, 300L, 1, "MAIN_DECK", "OWNED", "Filler Two"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 3),
                        amounts("usd", "6.00"),
                        amounts(),
                        categories("DRAW", 1, "SYNERGY", 2),
                        true));
        stubViews(
                Map.of(
                        100L, drawView(100L),
                        200L, fillerView(200L),
                        300L, fillerView(300L),
                        400L, rampView(400L),
                        401L, rampView(401L),
                        402L, wipeView(402L)));
        stubPrices(Map.of(100L, price("2.00"), 200L, price("2.00"), 300L, price("2.00")));
        stubAlternatives(2L, alt(400L, "Ramp Rock", true));
        stubAlternatives(3L, alt(401L, "Ramp Stone", true), alt(402L, "Board Wipe", true));

        var plan = service.plan(DECK_ID, Objective.CLOSE_CATEGORY_GAPS, null, null, null);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Ramp Rock", "Board Wipe");
        assertThat(plan.after().functionalCategories())
                .isEqualTo(new TreeMap<>(Map.of("DRAW", 1, "RAMP", 1, "WIPE", 1)));
        verify(alternativeService, never()).suggest(DECK_ID, 1L, 10, true);
    }

    @Test
    void shouldNotProposeSameCardTwice() {
        var cards =
                List.of(
                        deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy One"),
                        deckCard(2L, 200L, 1, "MAIN_DECK", "PROXY", "Proxy Two"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 2),
                        amounts("usd", "4.00"),
                        amounts("usd", "4.00"),
                        categories("DRAW", 2),
                        true));
        stubViews(Map.of(100L, drawView(100L), 200L, drawView(200L)));
        stubPrices(Map.of(100L, price("2.00"), 200L, price("2.00")));
        stubAlternatives(1L, alt(400L, "Duplicate Swap", true));
        stubAlternatives(2L, alt(400L, "Duplicate Swap", true), alt(401L, "Other Swap", true));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Duplicate Swap", "Other Swap");
    }

    @Test
    void shouldOnlyTargetMainDeckCards() {
        var cards =
                List.of(
                        deckCard(null, 50L, 1, "COMMANDER", "OWNED", "Synthesized Commander"),
                        deckCard(2L, 200L, 1, "COMMANDER", "PROXY", "Commander Row"),
                        deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 2, "OWNED", 1),
                        amounts("usd", "9.00"),
                        amounts("usd", "4.00"),
                        categories("DRAW", 3),
                        true));
        stubViews(Map.of(50L, drawView(50L), 100L, drawView(100L), 200L, drawView(200L)));
        stubPrices(Map.of(50L, price("5.00"), 100L, price("2.00"), 200L, price("2.00")));
        stubAlternatives(1L, alt(400L, "Owned Swap", true));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.substitutions()).hasSize(1);
        verify(alternativeService, times(1)).suggest(anyLong(), anyLong(), anyInt(), anyBoolean());
        verify(alternativeService, never()).suggest(DECK_ID, 2L, 10, true);
    }

    @Test
    void shouldUseRequestedCurrencyForBudgetAndCost() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Draw A"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "2.00"),
                        amounts(),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(
                Map.of(
                        100L,
                        price("2.00"),
                        300L,
                        new CardPrice(
                                new BigDecimal("100.00"), null, new BigDecimal("8.00"), null)));
        stubAlternatives(1L, alt(300L, "Euro Upgrade", false));

        var plan =
                service.plan(
                        DECK_ID,
                        Objective.IMPROVE_UNDER_BUDGET,
                        new BigDecimal("10.00"),
                        "eur",
                        null);

        assertThat(plan.substitutions())
                .extracting(DeckUpgradeService.Substitution::addedName)
                .containsExactly("Euro Upgrade");
        assertThat(plan.substitutions().getFirst().cost()).isEqualByComparingTo("8.00");
        assertThat(plan.currency()).isEqualTo("eur");
    }

    @Test
    void shouldKeepAfterLegalFlagMatchingBefore() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "OWNED", "Owned Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("OWNED", 1),
                        amounts("usd", "5.00"),
                        amounts(),
                        categories("DRAW", 1),
                        false));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("5.00")));

        var plan = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(plan.before().legal()).isFalse();
        assertThat(plan.after().legal()).isFalse();
    }

    @Test
    void shouldProduceDeterministicPlansAcrossCalls() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 1),
                        amounts("usd", "2.00"),
                        amounts("usd", "2.00"),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00")));
        stubAlternatives(1L, alt(300L, "Owned Swap", true));

        var first = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);
        var second = service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void shouldNeverMutateSourceDeck() {
        var cards = List.of(deckCard(1L, 100L, 1, "MAIN_DECK", "PROXY", "Proxy Draw"));
        stubDeck(
                cards,
                analysis(
                        ownership("PROXY", 1),
                        amounts("usd", "2.00"),
                        amounts("usd", "2.00"),
                        categories("DRAW", 1),
                        true));
        stubViews(Map.of(100L, drawView(100L)));
        stubPrices(Map.of(100L, price("2.00")));
        stubAlternatives(1L, alt(300L, "Owned Swap", true));

        service.plan(DECK_ID, Objective.REPLACE_PROXIES_WITH_OWNED, null, null, null);

        verify(deckCardService).listCards(DECK_ID);
        verifyNoMoreInteractions(deckCardService);
    }

    private void stubDeck(List<DeckCardResponse> cards, DeckAnalysisResponse analysis) {
        when(deckCardService.listCards(DECK_ID)).thenReturn(cards);
        when(deckAnalysisService.analyze(DECK_ID)).thenReturn(analysis);
    }

    private void stubAlternatives(long deckCardId, DeckCardAlternative... alternatives) {
        when(alternativeService.suggest(DECK_ID, deckCardId, 10, true))
                .thenReturn(List.of(alternatives));
    }

    private void stubViews(Map<Long, CardAnalysisView> views) {
        when(cardCatalogService.getAnalysisViewsByPrintingIds(any()))
                .thenAnswer(invocation -> filterKeys(views, invocation.getArgument(0)));
    }

    private void stubPrices(Map<Long, CardPrice> prices) {
        when(cardPriceService.latestPrices(any()))
                .thenAnswer(invocation -> filterKeys(prices, invocation.getArgument(0)));
    }

    private static <V> Map<Long, V> filterKeys(Map<Long, V> source, Collection<Long> ids) {
        return source.entrySet().stream()
                .filter(entry -> ids.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static DeckCardResponse deckCard(
            Long id, long printingId, int quantity, String section, String status, String name) {
        return new DeckCardResponse(id, printingId, quantity, section, status, summary(name));
    }

    private static CardSummaryResponse summary(String name) {
        return new CardSummaryResponse(
                1L,
                "oracle-" + name,
                name,
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
                null,
                null,
                Map.of(),
                List.of(),
                null);
    }

    private static DeckAnalysisResponse analysis(
            Map<String, Integer> ownership,
            Map<String, BigDecimal> value,
            Map<String, BigDecimal> missing,
            Map<String, Integer> categories,
            boolean legal) {
        return new DeckAnalysisResponse(
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                0,
                0,
                0.0,
                ownership,
                value,
                missing,
                0,
                categories,
                List.of(),
                List.of(),
                new CommanderBracket(1, List.of()),
                new DeckLegalityResponse(legal, List.of()),
                new DeckAnalysisResponse.ComboSummary(true, 0, List.of()));
    }

    private static Map<String, Integer> ownership(Object... alternating) {
        return intMap(alternating);
    }

    private static Map<String, Integer> categories(Object... alternating) {
        return intMap(alternating);
    }

    // Justified: method-local TreeMap, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, Integer> intMap(Object... alternating) {
        Map<String, Integer> map = new TreeMap<>();
        for (int index = 0; index < alternating.length; index += 2) {
            map.put((String) alternating[index], (Integer) alternating[index + 1]);
        }
        return map;
    }

    // Justified: method-local TreeMap, never shared across threads.
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    private static Map<String, BigDecimal> amounts(Object... alternating) {
        Map<String, BigDecimal> map = new TreeMap<>();
        for (int index = 0; index < alternating.length; index += 2) {
            map.put((String) alternating[index], new BigDecimal((String) alternating[index + 1]));
        }
        return map;
    }

    private static DeckCardAlternative alt(long printingId, String name, boolean owned) {
        return new DeckCardAlternative(printingId, name, owned, null, BigDecimal.ONE, List.of());
    }

    private static CardPrice price(String usd) {
        return new CardPrice(new BigDecimal(usd), null, null, null);
    }

    private static CardAnalysisView drawView(long printingId) {
        return view(printingId, "Sorcery", "Draw a card.");
    }

    private static CardAnalysisView fillerView(long printingId) {
        return view(printingId, "Artifact", "Whenever a creature enters, you gain 1 life.");
    }

    private static CardAnalysisView rampView(long printingId) {
        return view(printingId, "Artifact", "{T}: Add {C}.");
    }

    private static CardAnalysisView wipeView(long printingId) {
        return view(printingId, "Sorcery", "Destroy all creatures.");
    }

    private static CardAnalysisView view(long printingId, String typeLine, String oracleText) {
        return new CardAnalysisView(
                printingId,
                "Card " + printingId,
                null,
                BigDecimal.ONE,
                typeLine,
                "W",
                false,
                List.of(new CardAnalysisView.Face(null, typeLine, oracleText)));
    }
}
