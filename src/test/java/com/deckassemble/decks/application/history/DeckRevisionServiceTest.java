package com.deckassemble.decks.application.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.history.DeckRevision;
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Covers revision numbering, locking and suppression only — canonical snapshot assembly is {@link
 * DeckSnapshotBuilder}'s own responsibility and is tested there. Here, {@link DeckSnapshotBuilder}
 * is a plain mock: {@link DeckRevisionService} only needs to know it's called and its output stored
 * verbatim.
 */
@ExtendWith(MockitoExtension.class)
class DeckRevisionServiceTest {

    private static final long DECK_ID = 1L;
    private static final long PROFILE_ID = 42L;
    private static final String SNAPSHOT_JSON = "{\"name\":\"snapshot\"}";

    @Mock private DeckRevisionRepository revisionRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckSnapshotBuilder snapshotBuilder;

    private Deck deck;

    @BeforeEach
    void stubCommonCollaborators() {
        deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient()
                .when(deckRepository.findLockedByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient().when(snapshotBuilder.toJson(any(Deck.class))).thenReturn(SNAPSHOT_JSON);
        lenient().when(revisionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void shouldRecordFirstRevisionWithNoBase() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());
        ArgumentCaptor<DeckRevision> captor = ArgumentCaptor.forClass(DeckRevision.class);

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CREATED);

        verify(revisionRepository).save(captor.capture());
        DeckRevision saved = captor.getValue();
        assertThat(saved.getRevisionNumber()).isEqualTo(1);
        assertThat(saved.getBaseRevisionNumber()).isNull();
        assertThat(saved.getChangeType()).isEqualTo(DeckChangeType.CREATED);
        assertThat(saved.getProfileId()).isEqualTo(PROFILE_ID);
        assertThat(saved.getSnapshot()).isEqualTo(SNAPSHOT_JSON);
    }

    @Test
    void shouldRecordSubsequentRevisionWithBase() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.of(existingRevision(3)));
        ArgumentCaptor<DeckRevision> captor = ArgumentCaptor.forClass(DeckRevision.class);

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CARD_ADDED);

        verify(revisionRepository).save(captor.capture());
        DeckRevision saved = captor.getValue();
        assertThat(saved.getRevisionNumber()).isEqualTo(4);
        assertThat(saved.getBaseRevisionNumber()).isEqualTo(3);
    }

    @Test
    void shouldLockDeckRowBeforeAllocatingRevisionNumber() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CREATED);

        verify(deckRepository).findLockedByIdAndProfileId(DECK_ID, PROFILE_ID);
    }

    @Test
    void shouldThrowWhenDeckNotFoundUnderLock() {
        when(deckRepository.findLockedByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().record(DECK_ID, PROFILE_ID, DeckChangeType.CREATED))
                .isInstanceOf(DeckNotFoundException.class);
    }

    @Test
    void shouldBuildTheSnapshotFromTheLockedDeckInstance() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CARD_ADDED);

        verify(snapshotBuilder).toJson(deck);
    }

    @Test
    void shouldSuppressRecordingInsideWithoutRecording() {
        service()
                .withoutRecording(
                        () -> {
                            service().record(DECK_ID, PROFILE_ID, DeckChangeType.CREATED);
                            return null;
                        });

        verify(revisionRepository, never()).save(any());
    }

    @Test
    void shouldResumeRecordingAfterWithoutRecordingReturns() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());
        DeckRevisionService service = service();

        service.withoutRecording(
                () -> {
                    service.record(DECK_ID, PROFILE_ID, DeckChangeType.CREATED);
                    return null;
                });
        service.record(DECK_ID, PROFILE_ID, DeckChangeType.IMPORTED);

        verify(revisionRepository).save(any());
    }

    @Test
    void shouldReturnActionResultFromWithoutRecording() {
        String result = service().withoutRecording(() -> "value");

        assertThat(result).isEqualTo("value");
    }

    private DeckRevision existingRevision(int revisionNumber) {
        return new DeckRevision(
                DECK_ID,
                PROFILE_ID,
                revisionNumber,
                revisionNumber > 1 ? revisionNumber - 1 : null,
                new DeckRevision.Content(DeckChangeType.CREATED, null, "{}"));
    }

    private DeckRevisionService service() {
        return new DeckRevisionService(
                revisionRepository, deckRepository, deckAccessGuard, snapshotBuilder);
    }
}
