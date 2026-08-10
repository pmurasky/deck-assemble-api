package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.collaboration.DeckRevisionConflictException;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeckPrimerServiceTest {

    @Mock private DeckRepository deckRepository;
    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckRevisionService deckRevisionService;

    private DeckPrimerService service;

    @BeforeEach
    void setUp() {
        service = new DeckPrimerService(deckRepository, deckAccessGuard, deckRevisionService);
    }

    @Test
    void shouldStoreTitleAndMarkdownSourceAndRecordARevision() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.editableLocked(42L)).thenReturn(deck);
        when(deckRepository.save(any(Deck.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DeckPrimerService.PrimerResult result =
                service.updatePrimer(42L, "Aggro Guide", "# Intro\n\nPlay fast.", null);

        assertThat(result.deck().getPrimerTitle()).isEqualTo("Aggro Guide");
        assertThat(result.deck().getPrimerMarkdown()).isEqualTo("# Intro\n\nPlay fast.");
        verify(deckRevisionService)
                .record(
                        eq(deck),
                        org.mockito.ArgumentMatchers.anyLong(),
                        eq(DeckChangeType.METADATA_UPDATED));
    }

    @Test
    void shouldPropagateNotFoundWhenTheCallerCannotEditTheDeck() {
        when(deckAccessGuard.editableLocked(42L)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.updatePrimer(42L, "Title", "Body", null))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldRejectWhenExpectedRevisionIsStale() {
        Deck deck = new Deck(1L, "Deck", "COMMANDER");
        when(deckAccessGuard.editableLocked(42L)).thenReturn(deck);
        doThrow(new DeckRevisionConflictException(9))
                .when(deckRevisionService)
                .assertExpectedRevision(42L, 4);

        assertThatThrownBy(() -> service.updatePrimer(42L, "Title", "Body", 4))
                .isInstanceOf(DeckRevisionConflictException.class);
    }
}
