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
                        profileService);
    }

    @Test
    void shouldBuildFullDeckWithBasicsPadding() {
        var commander = commander();
        var pool = Map.of(11L, poolCard(11L, "Counterspell"), 12L, poolCard(12L, "Opt"));
        stubUser();
        when(cardCatalogService.getCard(COMMANDER_ID)).thenReturn(commander);
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

        var result = builderService.build(new DeckBuildRequest(COMMANDER_ID, null, null, null));

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
        when(cardCatalogService.getCard(COMMANDER_ID)).thenReturn(notALegend);

        assertThatThrownBy(
                        () ->
                                builderService.build(
                                        new DeckBuildRequest(COMMANDER_ID, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(deckService, never()).create(any());
    }

    @Test
    void shouldReportGapWhenNoBasicsAvailable() {
        var commander = commander();
        stubUser();
        when(cardCatalogService.getCard(COMMANDER_ID)).thenReturn(commander);
        when(collectionService.getOwnedPrintingIds(PROFILE_ID)).thenReturn(Set.of());
        when(edhrecCommanderService.getCardScores(any(), any())).thenReturn(Map.of());
        when(cardCatalogService.getCardsByNames(any())).thenReturn(List.of());
        when(deckService.create(any())).thenReturn(deckResponse());
        when(deckService.legality(DECK_ID)).thenReturn(new DeckLegalityResponse(false, List.of()));

        var result = builderService.build(new DeckBuildRequest(COMMANDER_ID, null, null, null));

        assertThat(result.gaps()).isNotEmpty();
        assertThat(result.cardCount()).isEqualTo(1);
    }

    @Test
    void shouldStillBuildWhenEdhrecIsUnavailable() {
        var commander = commander();
        stubUser();
        when(cardCatalogService.getCard(COMMANDER_ID)).thenReturn(commander);
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

        var result = builderService.build(new DeckBuildRequest(COMMANDER_ID, null, null, null));

        assertThat(result.cardCount()).isEqualTo(100);
        assertThat(result.score()).isNull();
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
