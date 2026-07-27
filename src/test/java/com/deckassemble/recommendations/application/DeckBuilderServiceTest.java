package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import com.deckassemble.recommendations.domain.DeckBuild;
import com.deckassemble.recommendations.domain.DeckBuildRepository;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class DeckBuilderServiceTest {

    private static final long PROFILE_ID = 42L;
    private static final long COMMANDER_ID = 1L;
    private static final long DECK_ID = 100L;

    @Mock private CardCatalogService cardCatalogService;
    @Mock private CollectionService collectionService;
    @Mock private EdhrecCommanderService edhrecCommanderService;
    @Mock private CardCategorizer cardCategorizer;
    @Mock private DeckService deckService;
    @Mock private DeckBuildRepository deckBuildRepository;
    @Mock private com.deckassemble.shared.security.CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private com.deckassemble.cards.application.CardPriceService cardPriceService;

    private DeckBuilderService builderService;

    @BeforeEach
    void setUp() {
        builderService =
                new DeckBuilderService(
                        cardCatalogService,
                        collectionService,
                        edhrecCommanderService,
                        cardCategorizer,
                        deckService,
                        deckBuildRepository,
                        JsonMapper.builder().build(),
                        currentUser,
                        profileService,
                        cardPriceService);
    }

    @Test
    void shouldBuildFullDeckWithBasicsPadding() {
        var commander = commander();
        var pool = Map.of(11L, poolCard(11L, "Counterspell"), 12L, poolCard(12L, "Opt"));
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(11L, 12L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(11L, 12L))).thenReturn(pool);
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(Map.of("Counterspell", new CardScore(0.9, 1000L)));
        when(cardCategorizer.categorize(any())).thenReturn(Category.SYNERGY, Category.SYNERGY);
        var island = basicLand("Island");
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of(island));
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, island.getId(), 99L));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any())).thenAnswer(invocation -> cardResponse("OWNED"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        var result =
                builderService.build(
                        new DeckBuildRequest(COMMANDER_ID, null, null, null, null, null));

        assertThat(result.cardCount()).isEqualTo(100);
        assertThat(result.ownedCount()).isEqualTo(100);
        assertThat(result.gaps()).isEmpty();
        assertThat(result.legality().legal()).isTrue();
        var createCaptor = ArgumentCaptor.forClass(DeckCreateRequest.class);
        verify(deckService).create(createCaptor.capture());
        assertThat(createCaptor.getValue().commanderCardId()).isEqualTo(COMMANDER_ID);
        assertThat(createCaptor.getValue().useOwnedCardsOnly()).isTrue();
        verify(deckService, times(100)).addCard(anyLong(), any(DeckCardAddRequest.class));
        verify(deckBuildRepository).save(any(DeckBuild.class));
    }

    @Test
    void shouldRejectIneligibleCommander() {
        var notALegend = commander();
        notALegend.getFaces().clear();
        notALegend.getFaces().add(face(notALegend, "Creature — Merfolk"));
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(notALegend);

        assertThatThrownBy(
                        () ->
                                builderService.build(
                                        new DeckBuildRequest(
                                                COMMANDER_ID, null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(deckService, never()).create(any());
    }

    @Test
    void shouldReportGapWhenNoBasicsAvailable() {
        var commander = commander();
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());
        when(edhrecCommanderService.getCardScores(any(), any())).thenReturn(Map.of());
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of());
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(false, List.of()));

        var result =
                builderService.build(
                        new DeckBuildRequest(COMMANDER_ID, null, null, null, null, null));

        assertThat(result.gaps()).isNotEmpty();
        assertThat(result.cardCount()).isEqualTo(1);
    }

    @Test
    void shouldStillBuildWhenEdhrecIsUnavailable() {
        var commander = commander();
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());
        when(cardCatalogService.getCardsByPrintingIds(Set.of())).thenReturn(Map.of());
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenThrow(new RestClientException("edhrec down"));
        var island = basicLand("Island");
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of(island));
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, island.getId(), 99L));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any())).thenAnswer(invocation -> cardResponse("OWNED"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        var result =
                builderService.build(
                        new DeckBuildRequest(COMMANDER_ID, null, null, null, null, null));

        assertThat(result.cardCount()).isEqualTo(100);
        assertThat(result.score()).isNull();
    }

    @Test
    void shouldBuildOptimalDeckFromEdhrecPool() {
        var commander = commander();
        var rhystic = poolCard(20L, "Rhystic Study");
        var island = basicLand("Island");
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(Map.of("Rhystic Study", new CardScore(0.95, 5000L)));
        when(cardCatalogService.getCardsByNames(any()))
                .thenAnswer(
                        invocation -> {
                            java.util.Collection<String> names = invocation.getArgument(0);
                            return names.contains("Island") ? List.of(island) : List.of(rhystic);
                        });
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, 20L, 77L, island.getId(), 99L));
        when(cardCategorizer.categorize(any())).thenReturn(Category.SYNERGY);
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any()))
                .thenAnswer(invocation -> cardResponse("WISHLIST"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        var result =
                builderService.build(
                        new DeckBuildRequest(COMMANDER_ID, null, null, null, false, null));

        assertThat(result.cardCount()).isEqualTo(100);
        assertThat(result.wishlistCount()).isEqualTo(100);
        var createCaptor = ArgumentCaptor.forClass(DeckCreateRequest.class);
        verify(deckService).create(createCaptor.capture());
        assertThat(createCaptor.getValue().useOwnedCardsOnly()).isFalse();
        var addCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);
        verify(deckService, times(100)).addCard(anyLong(), addCaptor.capture());
        assertThat(addCaptor.getAllValues())
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(77L));
    }

    @Test
    void shouldExcludeUnownedCardsOverBudget() {
        var commander = commander();
        var expensive = poolCard(20L, "Expensive Card");
        var cheap = poolCard(21L, "Cheap Card");
        var island = basicLand("Island");
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(
                        Map.of(
                                "Expensive Card", new CardScore(0.9, 100L),
                                "Cheap Card", new CardScore(0.8, 90L)));
        when(cardCatalogService.getCardsByNames(any()))
                .thenAnswer(
                        invocation -> {
                            java.util.Collection<String> names = invocation.getArgument(0);
                            return names.contains("Island")
                                    ? List.of(island)
                                    : List.of(expensive, cheap);
                        });
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, 20L, 77L, 21L, 78L, island.getId(), 99L));
        when(cardCategorizer.categorize(any())).thenReturn(Category.SYNERGY);
        when(cardPriceService.latestPrices(any()))
                .thenReturn(
                        Map.of(
                                77L,
                                        new com.deckassemble.cards.domain.CardPrice(
                                                new java.math.BigDecimal("10.00"),
                                                null,
                                                null,
                                                null),
                                78L,
                                        new com.deckassemble.cards.domain.CardPrice(
                                                new java.math.BigDecimal("1.00"),
                                                null,
                                                null,
                                                null)));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any()))
                .thenAnswer(invocation -> cardResponse("WISHLIST"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        var result =
                builderService.build(
                        new DeckBuildRequest(
                                COMMANDER_ID,
                                null,
                                null,
                                null,
                                false,
                                new java.math.BigDecimal("5.00")));

        var addCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);
        verify(deckService, times(100)).addCard(anyLong(), addCaptor.capture());
        assertThat(addCaptor.getAllValues())
                .noneSatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(77L))
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(78L));
        assertThat(result.cardCount()).isEqualTo(100);
    }

    @Test
    void shouldExcludeGameChangersAtLowPower() {
        var commander = commander();
        var gameChanger = poolCard(11L, "Mana Vault");
        gameChanger.setGameChanger(true);
        var island = basicLand("Island");
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(11L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(11L)))
                .thenReturn(Map.of(11L, gameChanger));
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(Map.of("Mana Vault", new CardScore(0.9, 1000L)));
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of(island));
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, island.getId(), 99L));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any())).thenAnswer(invocation -> cardResponse("OWNED"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        builderService.build(new DeckBuildRequest(COMMANDER_ID, null, 4, null, null, null));

        var addCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);
        verify(deckService, times(100)).addCard(anyLong(), addCaptor.capture());
        assertThat(addCaptor.getAllValues())
                .noneSatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(11L));
    }

    @Test
    void shouldKeepGameChangersAtHighPower() {
        var commander = commander();
        var gameChanger = poolCard(11L, "Mana Vault");
        gameChanger.setGameChanger(true);
        var island = basicLand("Island");
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of(11L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(11L)))
                .thenReturn(Map.of(11L, gameChanger));
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(Map.of("Mana Vault", new CardScore(0.9, 1000L)));
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of(island));
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, island.getId(), 99L));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any())).thenAnswer(invocation -> cardResponse("OWNED"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        builderService.build(new DeckBuildRequest(COMMANDER_ID, null, 7, null, null, null));

        var addCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);
        verify(deckService, times(100)).addCard(anyLong(), addCaptor.capture());
        assertThat(addCaptor.getAllValues())
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(11L));
    }

    @Test
    void shouldKeepOnlyThreeGameChangersAtMediumPower() {
        var commander = commander();
        var first = gameChangerCard(11L, "First");
        var second = gameChangerCard(12L, "Second");
        var third = gameChangerCard(13L, "Third");
        var fourth = gameChangerCard(14L, "Fourth");
        var island = basicLand("Island");
        stubUser();
        when(cardCatalogService.getCardWithFaces(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID))
                .thenReturn(Set.of(11L, 12L, 13L, 14L));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(11L, 12L, 13L, 14L)))
                .thenReturn(Map.of(11L, first, 12L, second, 13L, third, 14L, fourth));
        when(edhrecCommanderService.getCardScores(any(), any()))
                .thenReturn(
                        Map.of(
                                "First", new CardScore(0.9, 1000L),
                                "Second", new CardScore(0.8, 900L),
                                "Third", new CardScore(0.7, 800L),
                                "Fourth", new CardScore(0.6, 700L)));
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of(island));
        when(cardCatalogService.getLatestPrintingIdByCardIds(any()))
                .thenReturn(Map.of(COMMANDER_ID, 90L, island.getId(), 99L));
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.addCard(anyLong(), any())).thenAnswer(invocation -> cardResponse("OWNED"));
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(true, List.of()));

        builderService.build(new DeckBuildRequest(COMMANDER_ID, null, 5, null, null, null));

        var addCaptor = ArgumentCaptor.forClass(DeckCardAddRequest.class);
        verify(deckService, times(100)).addCard(anyLong(), addCaptor.capture());
        assertThat(addCaptor.getAllValues())
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(11L))
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(12L))
                .anySatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(13L))
                .noneSatisfy(request -> assertThat(request.cardPrintingId()).isEqualTo(14L));
    }

    private void stubUser() {
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        var profile = new Profile("sub", "user");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(profileService.getOrCreate("sub")).thenReturn(profile);
    }

    private Card commander() {
        var card = card(COMMANDER_ID, "Tetsuko Umezawa");
        card.setColorIdentity("U");
        card.getFaces().add(face(card, "Legendary Creature — Human Rogue"));
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));
        return card;
    }

    private Card poolCard(long id, String name) {
        var card = card(id, name);
        card.setColorIdentity("U");
        card.getFaces().add(face(card, "Instant"));
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));
        return card;
    }

    private Card gameChangerCard(long id, String name) {
        var card = poolCard(id, name);
        card.setGameChanger(true);
        return card;
    }

    private Card basicLand(String name) {
        var card = card(50L, name);
        card.setColorIdentity("");
        return card;
    }

    private Card card(long id, String name) {
        var card = new Card("oracle-" + name, name);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private CardFace face(Card card, String typeLine) {
        var face = new CardFace(card, 0, card.getName());
        face.setTypeLine(typeLine);
        return face;
    }

    private DeckResponse deckResponse() {
        return new DeckResponse(
                DECK_ID,
                "Tetsuko Umezawa EDHREC Build",
                "COMMANDER",
                null,
                COMMANDER_ID,
                null,
                true,
                null,
                null,
                null,
                "DRAFT",
                0,
                "Tetsuko Umezawa",
                java.time.Instant.now());
    }

    private DeckCardResponse cardResponse(String ownershipStatus) {
        return new DeckCardResponse(1L, 99L, 1, "MAIN_DECK", ownershipStatus, null);
    }
}
