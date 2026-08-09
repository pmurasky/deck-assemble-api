package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckPrimerServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private DeckAccessGuard deckAccessGuard;

    private DeckPrimerService service;

    @BeforeEach
    void setUp() {
        service = new DeckPrimerService(deckRepository, deckAccessGuard);
    }

    @Test
    void shouldStoreTitleAndMarkdownSourceOnTheOwnedDeck() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.owned(42L)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Deck result = service.updatePrimer(42L, "Aggro Guide", "# Intro\n\nPlay fast.");

        assertThat(result.getPrimerTitle()).isEqualTo("Aggro Guide");
        assertThat(result.getPrimerMarkdown()).isEqualTo("# Intro\n\nPlay fast.");
    }

    @Test
    void shouldPropagateNotFoundWhenTheCallerDoesNotOwnTheDeck() {
        when(deckAccessGuard.owned(42L)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.updatePrimer(42L, "Title", "Body"))
                .isInstanceOf(DeckNotFoundException.class);
    }
}
