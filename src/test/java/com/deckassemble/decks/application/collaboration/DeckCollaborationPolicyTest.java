package com.deckassemble.decks.application.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.collaboration.DeckCollaborator;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRepository;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DeckCollaborationPolicyTest {

    private static final long OWNER_ID = 1L;
    private static final long DECK_ID = 10L;

    @Mock private DeckCollaboratorRepository deckCollaboratorRepository;

    private DeckCollaborationPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new DeckCollaborationPolicy(deckCollaboratorRepository);
    }

    @Test
    void shouldRecognizeTheOwner() {
        Deck deck = deck();

        assertThat(policy.isOwner(deck, OWNER_ID)).isTrue();
        assertThat(policy.isOwner(deck, 999L)).isFalse();
    }

    @Test
    void shouldAllowOwnerToViewAndEditWithoutConsultingCollaborators() {
        Deck deck = deck();

        assertThat(policy.canView(deck, OWNER_ID)).isTrue();
        assertThat(policy.canEdit(deck, OWNER_ID)).isTrue();
    }

    @Test
    void shouldAllowAnEditorCollaboratorToViewAndEdit() {
        Deck deck = deck();
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, 2L))
                .thenReturn(
                        Optional.of(
                                new DeckCollaborator(DECK_ID, 2L, DeckCollaboratorRole.EDITOR)));

        assertThat(policy.canView(deck, 2L)).isTrue();
        assertThat(policy.canEdit(deck, 2L)).isTrue();
    }

    @Test
    void shouldAllowAViewerCollaboratorToViewButNotEdit() {
        Deck deck = deck();
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, 3L))
                .thenReturn(
                        Optional.of(
                                new DeckCollaborator(DECK_ID, 3L, DeckCollaboratorRole.VIEWER)));

        assertThat(policy.canView(deck, 3L)).isTrue();
        assertThat(policy.canEdit(deck, 3L)).isFalse();
    }

    @Test
    void shouldDenyViewAndEditToAStrangerWithNoCollaboratorRow() {
        Deck deck = deck();
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, 4L))
                .thenReturn(Optional.empty());

        assertThat(policy.canView(deck, 4L)).isFalse();
        assertThat(policy.canEdit(deck, 4L)).isFalse();
    }

    private static Deck deck() {
        Deck deck = new Deck(OWNER_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        return deck;
    }
}
