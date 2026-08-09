package com.deckassemble.decks.application.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.collaboration.DeckCollaborator;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRepository;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DeckCollaborationServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long DECK_ID = 10L;
    private static final long INVITEE_ID = 2L;

    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckCollaboratorRepository deckCollaboratorRepository;
    @Mock private ProfileRepository profileRepository;

    private DeckCollaborationService service;
    private Deck deck;

    @BeforeEach
    void setUp() {
        service =
                new DeckCollaborationService(
                        deckAccessGuard, deckCollaboratorRepository, profileRepository);
        deck = new Deck(OWNER_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
    }

    @Test
    void shouldListCollaboratorsForTheOwner() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        DeckCollaborator collaborator =
                new DeckCollaborator(DECK_ID, INVITEE_ID, DeckCollaboratorRole.VIEWER);
        when(deckCollaboratorRepository.findByDeckId(DECK_ID)).thenReturn(List.of(collaborator));

        assertThat(service.list(DECK_ID)).containsExactly(collaborator);
    }

    @Test
    void shouldRejectListingForANonOwner() {
        when(deckAccessGuard.owned(DECK_ID)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.list(DECK_ID)).isInstanceOf(DeckNotFoundException.class);
        verify(deckCollaboratorRepository, never()).findByDeckId(any());
    }

    @Test
    void shouldInviteANewCollaborator() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(profileRepository.existsById(INVITEE_ID)).thenReturn(true);
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, INVITEE_ID))
                .thenReturn(Optional.empty());
        when(deckCollaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator result = service.invite(DECK_ID, INVITEE_ID, DeckCollaboratorRole.EDITOR);

        assertThat(result.getDeckId()).isEqualTo(DECK_ID);
        assertThat(result.getProfileId()).isEqualTo(INVITEE_ID);
        assertThat(result.getRole()).isEqualTo(DeckCollaboratorRole.EDITOR);
    }

    @Test
    void shouldTreatReInvitingTheSameProfileAsIdempotentAndUpdateTheRole() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(profileRepository.existsById(INVITEE_ID)).thenReturn(true);
        DeckCollaborator existing =
                new DeckCollaborator(DECK_ID, INVITEE_ID, DeckCollaboratorRole.VIEWER);
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, INVITEE_ID))
                .thenReturn(Optional.of(existing));
        when(deckCollaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator result = service.invite(DECK_ID, INVITEE_ID, DeckCollaboratorRole.EDITOR);

        // Same row updated in place, not a second row created for the same (deck, profile) pair.
        assertThat(result).isSameAs(existing);
        assertThat(result.getRole()).isEqualTo(DeckCollaboratorRole.EDITOR);
    }

    @Test
    void shouldRejectInvitingTheDeckOwnerAsACollaborator() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);

        assertThatThrownBy(() -> service.invite(DECK_ID, OWNER_ID, DeckCollaboratorRole.VIEWER))
                .isInstanceOf(ResponseStatusException.class);
        verify(deckCollaboratorRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvitingAnUnknownProfile() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(profileRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.invite(DECK_ID, 999L, DeckCollaboratorRole.VIEWER))
                .isInstanceOf(ResponseStatusException.class);
        verify(deckCollaboratorRepository, never()).save(any());
    }

    @Test
    void shouldRejectInvitingForANonOwner() {
        when(deckAccessGuard.owned(DECK_ID)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.invite(DECK_ID, INVITEE_ID, DeckCollaboratorRole.VIEWER))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldRevokeAnExistingCollaborator() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        DeckCollaborator existing =
                new DeckCollaborator(DECK_ID, INVITEE_ID, DeckCollaboratorRole.EDITOR);
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, INVITEE_ID))
                .thenReturn(Optional.of(existing));

        service.revoke(DECK_ID, INVITEE_ID);

        verify(deckCollaboratorRepository).delete(existing);
    }

    @Test
    void shouldRejectRevokingACollaboratorThatWasNeverInvited() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, INVITEE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(DECK_ID, INVITEE_ID))
                .isInstanceOf(DeckCollaboratorNotFoundException.class);
    }

    @Test
    void shouldRejectRevokingForANonOwner() {
        when(deckAccessGuard.owned(DECK_ID)).thenThrow(new DeckNotFoundException());

        assertThatThrownBy(() -> service.revoke(DECK_ID, INVITEE_ID))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldAllowOwnerToManageCollaboratorsOnAnArchivedDeckLikeAnyOtherDeck() {
        // No precedent elsewhere in the deck module restricts mutation on ARCHIVED decks (see
        // DeckService#archive and DeckFolderService/DeckCardService, none of which special-case
        // Status.ARCHIVED), so collaborator management does not invent a new restriction either.
        deck.setStatus(Deck.Status.ARCHIVED);
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(profileRepository.existsById(INVITEE_ID)).thenReturn(true);
        when(deckCollaboratorRepository.findByDeckIdAndProfileId(DECK_ID, INVITEE_ID))
                .thenReturn(Optional.empty());
        when(deckCollaboratorRepository.save(any(DeckCollaborator.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DeckCollaborator result = service.invite(DECK_ID, INVITEE_ID, DeckCollaboratorRole.VIEWER);

        assertThat(result.getRole()).isEqualTo(DeckCollaboratorRole.VIEWER);
    }
}
