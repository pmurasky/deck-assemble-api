package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.recommendations.domain.CommanderSpellbookClient;
import com.deckassemble.recommendations.domain.SpellbookCombo;
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
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class DeckComboServiceTest {

    private static final long PROFILE_ID = 42L;

    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CurrentUser currentUser;
    @Mock private ProfileService profileService;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private CommanderSpellbookClient commanderSpellbookClient;
    @Mock private DeckCollaborationPolicy deckCollaborationPolicy;

    @Test
    void shouldReturnIncludedSpellbookCombosForDeck() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckId(1L))
                .thenReturn(List.of(new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK)));
        when(cardCatalogService.getCardsByPrintingIds(List.of(10L)))
                .thenReturn(
                        java.util.Map.of(
                                10L, new com.deckassemble.cards.domain.Card("oracle", "Sol Ring")));
        when(commanderSpellbookClient.findCombos("1 Sol Ring"))
                .thenReturn(
                        List.of(
                                new SpellbookCombo(
                                        "combo-1",
                                        List.of("Sol Ring", "Hullbreaker Horror"),
                                        List.of("Infinite mana"),
                                        "Loop artifacts.",
                                        "None")));

        DeckComboResponse result = service().getCombos(1L);

        assertThat(result.available()).isTrue();
        assertThat(result.combos()).extracting(SpellbookCombo::id).containsExactly("combo-1");
    }

    @Test
    void shouldReportUnavailableWhenSpellbookFails() {
        stubUser();
        when(deckRepository.findByIdAndProfileId(1L, PROFILE_ID)).thenReturn(Optional.of(deck(1L)));
        when(deckCardRepository.findByDeckId(1L))
                .thenReturn(List.of(new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK)));
        when(cardCatalogService.getCardsByPrintingIds(List.of(10L)))
                .thenReturn(
                        java.util.Map.of(
                                10L, new com.deckassemble.cards.domain.Card("oracle", "Sol Ring")));
        when(commanderSpellbookClient.findCombos("1 Sol Ring"))
                .thenThrow(new RestClientException("down"));

        DeckComboResponse result = service().getCombos(1L);

        assertThat(result.available()).isFalse();
        assertThat(result.combos()).isEmpty();
    }

    private DeckComboService service() {
        return new DeckComboService(
                new DeckAccessGuard(
                        currentUser, profileService, deckRepository, deckCollaborationPolicy),
                deckCardRepository,
                cardCatalogService,
                commanderSpellbookClient);
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
