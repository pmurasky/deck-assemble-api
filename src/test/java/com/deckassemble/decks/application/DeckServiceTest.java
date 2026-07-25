package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CommanderLegalityEvaluator commanderLegalityEvaluator;
    @Mock private OwnershipChecker ownershipChecker;
    @Mock private CollectionService collectionService;
    @Mock private com.deckassemble.cards.application.CardPriceService cardPriceService;

    @Test
    void shouldListDecksForCurrentProfile() {
        stubUser();
        when(deckRepository.findByProfileIdOrderByNameAsc(PROFILE_ID))
                .thenReturn(List.of(deck(1L), deck(2L)));
        when(deckCardRepository.findByDeckId(any(Long.class))).thenReturn(List.of());

        List<DeckResponse> result = service().list();

        assertThat(result).extracting(DeckResponse::name).containsExactly("Deck", "Deck");
    }

    @Test
    void shouldCreateDeckWithDefaults() {
        stubUser();
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckCreateRequest request =
                new DeckCreateRequest(
                        "New Deck", "COMMANDER", null, null, null, null, null, null, null);

        DeckResponse result = service().create(request);

        assertThat(result.name()).isEqualTo("New Deck");
        assertThat(result.useOwnedCardsOnly()).isFalse();
        assertThat(result.status()).isEqualTo("DRAFT");
    }

    @Test
    void shouldThrowWhenDeckNotOwned() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById(1L)).isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldDelegateLegalityToEvaluator() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        List<DeckCard> cards = List.of(new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK));
        when(deckCardRepository.findByDeckId(1L)).thenReturn(cards);
        DeckLegalityResponse expected = new DeckLegalityResponse(true, List.of());
        when(commanderLegalityEvaluator.evaluate(deck, cards)).thenReturn(expected);

        assertThat(service().legality(1L)).isEqualTo(expected);
    }

    @Test
    void shouldApplyOnlyProvidedFieldsOnUpdate() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());
        DeckUpdateRequest request =
                new DeckUpdateRequest("Renamed", null, null, null, null, null, null, null, null);

        DeckResponse result = service().update(1L, request);

        assertThat(result.name()).isEqualTo("Renamed");
        assertThat(result.formatCode()).isEqualTo("COMMANDER");
    }

    @Test
    void shouldDeleteOwnedDeck() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));

        service().delete(1L);

        verify(deckRepository).delete(deck);
    }

    @Test
    void shouldArchiveDeck() {
        stubUser();
        Deck deck = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deckCardRepository.findByDeckId(any())).thenReturn(List.of());

        DeckResponse result = service().archive(1L);

        assertThat(result.status()).isEqualTo("ARCHIVED");
    }

    @Test
    void shouldDuplicateDeckWithCards() {
        stubUser();
        Deck source = deck(1L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(source));
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(
                        inv -> {
                            Deck copy = inv.getArgument(0);
                            ReflectionTestUtils.setField(copy, "id", 2L);
                            return copy;
                        });
        when(deckCardRepository.findByDeckId(1L))
                .thenReturn(
                        List.of(
                                new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK),
                                new DeckCard(1L, 11L, 4, DeckCard.Section.SIDEBOARD)));
        when(deckCardRepository.findByDeckId(2L)).thenReturn(List.of());

        DeckResponse result = service().duplicate(1L);

        assertThat(result.name()).isEqualTo("Deck (Copy)");
        ArgumentCaptor<DeckCard> saved = ArgumentCaptor.forClass(DeckCard.class);
        verify(deckCardRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .allSatisfy(card -> assertThat(card.getDeckId()).isEqualTo(2L));
    }

    @Test
    void shouldAddCardWithDefaults() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckIdAndCardPrintingIdAndDeckSection(
                        1L, 10L, DeckCard.Section.MAIN_DECK))
                .thenReturn(Optional.empty());
        when(deckCardRepository.save(any(DeckCard.class))).thenAnswer(inv -> inv.getArgument(0));

        DeckCardResponse result = service().addCard(1L, new DeckCardAddRequest(10L, null, null));

        assertThat(result.quantity()).isEqualTo(1);
        assertThat(result.deckSection()).isEqualTo("MAIN_DECK");
    }

    @Test
    void shouldMarkNewCardAsOwnedWhenPrintingInCollection() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckIdAndCardPrintingIdAndDeckSection(
                        1L, 10L, DeckCard.Section.MAIN_DECK))
                .thenReturn(Optional.empty());
        when(deckCardRepository.save(any(DeckCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ownershipChecker.isOwned(PROFILE_ID, 10L)).thenReturn(true);

        DeckCardResponse result = service().addCard(1L, new DeckCardAddRequest(10L, null, null));

        assertThat(result.ownershipStatus()).isEqualTo("OWNED");
    }

    @Test
    void shouldMarkNewCardAsWishlistWhenPrintingNotOwned() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckIdAndCardPrintingIdAndDeckSection(
                        1L, 10L, DeckCard.Section.MAIN_DECK))
                .thenReturn(Optional.empty());
        when(deckCardRepository.save(any(DeckCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ownershipChecker.isOwned(PROFILE_ID, 10L)).thenReturn(false);

        DeckCardResponse result = service().addCard(1L, new DeckCardAddRequest(10L, null, null));

        assertThat(result.ownershipStatus()).isEqualTo("WISHLIST");
    }

    @Test
    void shouldMergeQuantityWhenCardAlreadyInSection() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard existing = new DeckCard(1L, 10L, 2, DeckCard.Section.MAIN_DECK);
        when(deckCardRepository.findByDeckIdAndCardPrintingIdAndDeckSection(
                        1L, 10L, DeckCard.Section.MAIN_DECK))
                .thenReturn(Optional.of(existing));
        when(deckCardRepository.save(any(DeckCard.class))).thenAnswer(inv -> inv.getArgument(0));

        DeckCardResponse result = service().addCard(1L, new DeckCardAddRequest(10L, 3, null));

        assertThat(result.quantity()).isEqualTo(5);
    }

    @Test
    void shouldUpdateCardFields() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 2, DeckCard.Section.MAIN_DECK);
        when(deckCardRepository.findByIdAndDeckId(7L, 1L)).thenReturn(Optional.of(card));
        when(deckCardRepository.save(any(DeckCard.class))).thenAnswer(inv -> inv.getArgument(0));

        DeckCardResponse result =
                service()
                        .updateCard(
                                1L, 7L, new DeckCardUpdateRequest(4, DeckCard.Section.SIDEBOARD));

        assertThat(result.quantity()).isEqualTo(4);
        assertThat(result.deckSection()).isEqualTo("SIDEBOARD");
    }

    @Test
    void shouldThrowWhenRemovingCardNotInDeck() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByIdAndDeckId(7L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().removeCard(1L, 7L))
                .isInstanceOf(DeckCardNotFoundException.class);
    }

    @Test
    void shouldFlipWishlistToOwnedWhenSyncing() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        card.setOwnershipStatus(DeckCard.OwnershipStatus.WISHLIST);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        when(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, List.of(10L)))
                .thenReturn(java.util.Set.of(10L));

        var result = service().syncOwnership(1L);

        assertThat(result.changedCount()).isEqualTo(1);
        assertThat(result.changes().get(0).fromStatus()).isEqualTo("WISHLIST");
        assertThat(result.changes().get(0).toStatus()).isEqualTo("OWNED");
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.OWNED);
        verify(deckCardRepository).save(card);
    }

    @Test
    void shouldFlipOwnedToWishlistWhenCardNoLongerOwned() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        when(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, List.of(10L)))
                .thenReturn(java.util.Set.of());

        var result = service().syncOwnership(1L);

        assertThat(result.changedCount()).isEqualTo(1);
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.WISHLIST);
    }

    @Test
    void shouldKeepProxyStatusWhenSyncing() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        card.setOwnershipStatus(DeckCard.OwnershipStatus.PROXY);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        when(ownershipChecker.filterOwnedPrintingIds(PROFILE_ID, List.of(10L)))
                .thenReturn(java.util.Set.of());

        var result = service().syncOwnership(1L);

        assertThat(result.changedCount()).isZero();
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.PROXY);
        verify(deckCardRepository, org.mockito.Mockito.never()).save(any(DeckCard.class));
    }

    @Test
    void shouldAddToCollectionAndFlipWishlistWhenAcquiring() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        card.setOwnershipStatus(DeckCard.OwnershipStatus.WISHLIST);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByIdAndDeckId(7L, 1L)).thenReturn(Optional.of(card));

        var result = service().acquireCard(1L, 7L);

        verify(collectionService).addToDefaultCollection(10L, 1, 0);
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.OWNED);
        verify(deckCardRepository).save(card);
        assertThat(result.ownershipStatus()).isEqualTo("OWNED");
    }

    @Test
    void shouldNotResaveWhenAcquiringAlreadyOwnedCard() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByIdAndDeckId(7L, 1L)).thenReturn(Optional.of(card));

        service().acquireCard(1L, 7L);

        verify(collectionService).addToDefaultCollection(10L, 1, 0);
        verify(deckCardRepository, org.mockito.Mockito.never()).save(any(DeckCard.class));
    }

    @Test
    void shouldRejectUnauthenticatedUser() {
        when(currentUser.subject()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().list()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnWishlistWithPricesAndTotal() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard wishlistCard = new DeckCard(1L, 10L, 2, DeckCard.Section.MAIN_DECK);
        wishlistCard.setOwnershipStatus(DeckCard.OwnershipStatus.WISHLIST);
        ReflectionTestUtils.setField(wishlistCard, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(wishlistCard));
        var card = new com.deckassemble.cards.domain.Card("oracle-x", "Rhystic Study");
        when(cardCatalogService.getCardsByPrintingIds(List.of(10L)))
                .thenReturn(java.util.Map.of(10L, card));
        when(cardPriceService.latestPrices(List.of(10L)))
                .thenReturn(
                        java.util.Map.of(
                                10L,
                                new com.deckassemble.cards.domain.CardPrice(
                                        new java.math.BigDecimal("4.50"), null, null, null)));

        DeckWishlistResponse result = service().getWishlist(1L);

        assertThat(result.items()).hasSize(1);
        var item = result.items().get(0);
        assertThat(item.cardName()).isEqualTo("Rhystic Study");
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPriceUsd()).isEqualByComparingTo("4.50");
        assertThat(item.lineTotalUsd()).isEqualByComparingTo("9.00");
        assertThat(result.totalUsd()).isEqualByComparingTo("9.00");
    }

    @Test
    void shouldReturnEmptyWishlistWhenNoWishlistCards() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckId(1L))
                .thenReturn(List.of(new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK)));

        DeckWishlistResponse result = service().getWishlist(1L);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalUsd()).isNull();
    }

    private DeckService service() {
        return new DeckService(
                deckRepository,
                deckCardRepository,
                currentUser,
                profileService,
                cardCatalogService,
                commanderLegalityEvaluator,
                ownershipChecker,
                collectionService,
                cardPriceService);
    }

    private void stubUser() {
        Profile profile = new Profile("sub", "User");
        ReflectionTestUtils.setField(profile, "id", PROFILE_ID);
        when(currentUser.subject()).thenReturn(Optional.of("sub"));
        when(profileService.getOrCreate("sub")).thenReturn(profile);
    }

    private Deck deck(long id) {
        Deck deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", id);
        return deck;
    }
}
