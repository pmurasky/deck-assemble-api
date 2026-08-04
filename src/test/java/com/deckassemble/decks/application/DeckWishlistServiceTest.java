package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckWishlistServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CardPriceService cardPriceService;

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

    private DeckWishlistService service() {
        return new DeckWishlistService(
                new DeckAccessGuard(currentUser, profileService, deckRepository),
                deckCardRepository,
                cardCatalogService,
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
