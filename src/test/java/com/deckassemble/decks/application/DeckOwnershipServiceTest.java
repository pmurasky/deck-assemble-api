package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService;
import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.application.history.DeckRevisionService;
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
class DeckOwnershipServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private OwnershipChecker ownershipChecker;
    @Mock private CollectionService collectionService;
    @Mock private PhysicalCardAllocationService allocationService;
    @Mock private DeckRevisionService deckRevisionService;
    @Mock private DeckCollaborationPolicy deckCollaborationPolicy;

    @Test
    void shouldFlipWishlistToOwnedWhenSyncing() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        card.setOwnershipStatus(DeckCard.OwnershipStatus.WISHLIST);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        stubAvailable(1L, card, 1, 0);

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
        stubAvailable(1L, card, 0, 1);

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
        stubAvailable(1L, card, 0, 1);

        var result = service().syncOwnership(1L);

        assertThat(result.changedCount()).isZero();
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.PROXY);
        verify(deckCardRepository, never()).save(any(DeckCard.class));
    }

    @Test
    void shouldMarkWishlistOwnedWhenAlternatePhysicalPrintingIsAvailable() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        card.setOwnershipStatus(DeckCard.OwnershipStatus.WISHLIST);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        stubAvailable(1L, card, 1, 0);

        var result = service().syncOwnership(1L);

        assertThat(result.changedCount()).isEqualTo(1);
        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.OWNED);
    }

    @Test
    void shouldMarkOwnedCardUnavailableWhenPhysicalCopyIsAllocatedElsewhere() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        DeckCard card = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        ReflectionTestUtils.setField(card, "id", 7L);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(card));
        stubAvailable(1L, card, 0, 1);

        var result = service().syncOwnership(1L);

        assertThat(card.getOwnershipStatus()).isEqualTo(DeckCard.OwnershipStatus.WISHLIST);
        assertThat(result.unavailableCount()).isEqualTo(1);
        assertThat(result.physicalAvailability().get(0).missingQuantity()).isEqualTo(1);
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
        verify(deckCardRepository, never()).save(any(DeckCard.class));
    }

    private DeckOwnershipService service() {
        var guard =
                new DeckAccessGuard(
                        currentUser, profileService, deckRepository, deckCollaborationPolicy);
        return new DeckOwnershipService(
                guard,
                deckCardRepository,
                new DeckCardService(
                        guard,
                        deckCardRepository,
                        cardCatalogService,
                        ownershipChecker,
                        deckRevisionService),
                collectionService,
                allocationService);
    }

    private void stubAvailable(long deckId, DeckCard card, int available, int missing) {
        when(allocationService.availabilityFor(eq(PROFILE_ID), eq(deckId), any()))
                .thenReturn(
                        List.of(
                                new PhysicalCardAllocationService.CardAvailability(
                                        card.getId(),
                                        card.getCardPrintingId(),
                                        card.getQuantity(),
                                        available + missing,
                                        0,
                                        available,
                                        missing)));
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
