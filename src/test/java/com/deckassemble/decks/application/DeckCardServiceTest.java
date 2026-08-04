package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardSummaryResponse;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import com.deckassemble.users.domain.Profile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckCardServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private OwnershipChecker ownershipChecker;

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
    void shouldSynthesizeCommanderEntryWhenRowMissing() {
        stubUser();
        Deck deck = deck(1L);
        deck.setCommanderCardId(101L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        DeckCard main = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(main));
        var commander = mock(CardSummaryResponse.class);
        when(cardCatalogService.getLatestPrintingIdByCardIds(List.of(101L)))
                .thenReturn(Map.of(101L, 201L));
        when(cardCatalogService.getSummaryByPrintingId(201L)).thenReturn(commander);
        when(cardCatalogService.getSummaryByPrintingId(10L))
                .thenReturn(mock(CardSummaryResponse.class));
        when(ownershipChecker.isOwned(PROFILE_ID, 201L)).thenReturn(true);

        List<DeckCardResponse> result = service().listCards(1L);

        assertThat(result).hasSize(2);
        DeckCardResponse synthesized = result.get(1);
        assertThat(synthesized.deckSection()).isEqualTo("COMMANDER");
        assertThat(synthesized.quantity()).isEqualTo(1);
        assertThat(synthesized.cardPrintingId()).isEqualTo(201L);
        assertThat(synthesized.ownershipStatus()).isEqualTo("OWNED");
        assertThat(synthesized.card()).isEqualTo(commander);
    }

    @Test
    void shouldNotSynthesizeCommanderWhenRowExists() {
        stubUser();
        Deck deck = deck(1L);
        deck.setCommanderCardId(101L);
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck));
        DeckCard commanderRow = new DeckCard(1L, 201L, 1, DeckCard.Section.COMMANDER);
        when(deckCardRepository.findByDeckId(1L)).thenReturn(List.of(commanderRow));

        List<DeckCardResponse> result = service().listCards(1L);

        assertThat(result).hasSize(1);
        verify(cardCatalogService, never()).getLatestPrintingIdByCardIds(any());
    }

    private DeckCardService service() {
        return new DeckCardService(
                new DeckAccessGuard(currentUser, profileService, deckRepository),
                deckCardRepository,
                cardCatalogService,
                ownershipChecker);
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
