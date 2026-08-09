package com.deckassemble.decks.domain.collaboration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class DeckCollaboratorRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DeckCollaboratorRepository collaboratorRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldRejectDuplicateCollaboratorForSameDeckAndProfile() {
        Deck deck = saveDeck();
        Profile profile = saveProfile();
        collaboratorRepository.saveAndFlush(
                new DeckCollaborator(deck.getId(), profile.getId(), DeckCollaboratorRole.VIEWER));

        assertThatThrownBy(
                        () ->
                                collaboratorRepository.saveAndFlush(
                                        new DeckCollaborator(
                                                deck.getId(),
                                                profile.getId(),
                                                DeckCollaboratorRole.EDITOR)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindCollaboratorsByDeck() {
        Deck deck = saveDeck();
        Profile profile = saveProfile();
        collaboratorRepository.saveAndFlush(
                new DeckCollaborator(deck.getId(), profile.getId(), DeckCollaboratorRole.EDITOR));

        assertThat(collaboratorRepository.findByDeckId(deck.getId()))
                .extracting(DeckCollaborator::getRole)
                .containsExactly(DeckCollaboratorRole.EDITOR);
    }

    @Test
    void shouldChangeCollaboratorRole() {
        Deck deck = saveDeck();
        Profile profile = saveProfile();
        collaboratorRepository.saveAndFlush(
                new DeckCollaborator(deck.getId(), profile.getId(), DeckCollaboratorRole.VIEWER));

        DeckCollaborator collaborator =
                collaboratorRepository
                        .findByDeckIdAndProfileId(deck.getId(), profile.getId())
                        .orElseThrow();
        collaborator.changeRole(DeckCollaboratorRole.EDITOR);
        collaboratorRepository.saveAndFlush(collaborator);
        entityManager.clear();

        assertThat(
                        collaboratorRepository
                                .findByDeckIdAndProfileId(deck.getId(), profile.getId())
                                .orElseThrow()
                                .getRole())
                .isEqualTo(DeckCollaboratorRole.EDITOR);
    }

    @Test
    void shouldCascadeDeleteCollaboratorsWhenDeckIsDeleted() {
        Deck deck = saveDeck();
        Profile profile = saveProfile();
        collaboratorRepository.saveAndFlush(
                new DeckCollaborator(deck.getId(), profile.getId(), DeckCollaboratorRole.VIEWER));

        deckRepository.delete(deck);
        deckRepository.flush();
        entityManager.clear();

        assertThat(collaboratorRepository.findByDeckId(deck.getId())).isEmpty();
    }

    private Deck saveDeck() {
        Profile owner = profileRepository.save(new Profile("auth|" + System.nanoTime(), "Owner"));
        return deckRepository.save(new Deck(owner.getId(), "Test Deck", "commander"));
    }

    private Profile saveProfile() {
        return profileRepository.save(new Profile("auth|" + System.nanoTime(), "Collaborator"));
    }
}
