package com.deckassemble.decks.domain.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.users.domain.Profile;
import com.deckassemble.users.domain.ProfileRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Transactional
class DeckRevisionRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private DeckRevisionRepository revisionRepository;
    @Autowired private DeckRepository deckRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EntityManager entityManager;

    @Test
    void shouldRejectDuplicateRevisionNumberForSameDeck() {
        Deck deck = saveDeck();
        revisionRepository.saveAndFlush(revision(deck, 1, DeckChangeType.CREATED));

        assertThatThrownBy(
                        () ->
                                revisionRepository.saveAndFlush(
                                        revision(deck, 1, DeckChangeType.METADATA_UPDATED)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSequentialRevisionNumbersForSameDeck() {
        Deck deck = saveDeck();
        revisionRepository.saveAndFlush(revision(deck, 1, DeckChangeType.CREATED));

        revisionRepository.saveAndFlush(revision(deck, 2, DeckChangeType.METADATA_UPDATED));

        assertThat(revisionRepository.findByDeckIdOrderByRevisionNumberDesc(deck.getId()))
                .extracting(DeckRevision::getRevisionNumber)
                .containsExactly(2, 1);
    }

    @Test
    void shouldLoadPersistedSnapshotUnchangedAfterReload() {
        Deck deck = saveDeck();
        DeckSnapshot original = sampleSnapshot();
        DeckRevision saved =
                revisionRepository.saveAndFlush(
                        new DeckRevision(
                                deck.getId(),
                                deck.getProfileId(),
                                1,
                                null,
                                new DeckRevision.Content(
                                        DeckChangeType.CREATED,
                                        null,
                                        objectMapper.writeValueAsString(original))));
        entityManager.clear();

        DeckRevision reloaded = revisionRepository.findById(saved.getId()).orElseThrow();
        DeckSnapshot roundTripped =
                objectMapper.readValue(reloaded.getSnapshot(), DeckSnapshot.class);

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void shouldFindRevisionByDeckIdAndRevisionNumber() {
        Deck deck = saveDeck();
        revisionRepository.saveAndFlush(revision(deck, 1, DeckChangeType.CREATED));

        assertThat(revisionRepository.findByDeckIdAndRevisionNumber(deck.getId(), 1))
                .isPresent()
                .get()
                .extracting(DeckRevision::getChangeType)
                .isEqualTo(DeckChangeType.CREATED);
    }

    @Test
    void shouldFindLatestRevisionForDeck() {
        Deck deck = saveDeck();
        revisionRepository.saveAndFlush(revision(deck, 1, DeckChangeType.CREATED));
        revisionRepository.saveAndFlush(revision(deck, 2, DeckChangeType.CARD_ADDED));

        assertThat(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(deck.getId()))
                .isPresent()
                .get()
                .extracting(DeckRevision::getRevisionNumber)
                .isEqualTo(2);
    }

    private DeckRevision revision(Deck deck, int revisionNumber, DeckChangeType changeType) {
        return new DeckRevision(
                deck.getId(),
                deck.getProfileId(),
                revisionNumber,
                revisionNumber > 1 ? revisionNumber - 1 : null,
                new DeckRevision.Content(
                        changeType, null, objectMapper.writeValueAsString(sampleSnapshot())));
    }

    private Deck saveDeck() {
        Profile profile =
                profileRepository.save(new Profile("auth|" + System.nanoTime(), "Tester"));
        return deckRepository.save(new Deck(profile.getId(), "Test Deck", "commander"));
    }

    private DeckSnapshot sampleSnapshot() {
        return new DeckSnapshot(
                "Test Deck",
                "commander",
                "A deck for testing",
                1L,
                null,
                null,
                false,
                null,
                7,
                "midrange",
                "DRAFT",
                List.of(new DeckSnapshot.CardEntry(10L, 1, "COMMANDER", "OWNED")),
                List.of("Ramp"),
                List.of("Competitive"));
    }
}
