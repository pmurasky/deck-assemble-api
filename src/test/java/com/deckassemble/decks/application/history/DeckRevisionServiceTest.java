package com.deckassemble.decks.application.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.history.DeckRevision;
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class DeckRevisionServiceTest {

    private static final long DECK_ID = 1L;
    private static final long PROFILE_ID = 42L;

    @Mock private DeckRevisionRepository revisionRepository;
    @Mock private DeckRepository deckRepository;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private DeckCategoryRepository deckCategoryRepository;
    @Mock private DeckTagAssignmentRepository deckTagAssignmentRepository;
    @Mock private DeckTagRepository deckTagRepository;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private Deck deck;

    @BeforeEach
    void stubCommonCollaborators() {
        deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient()
                .when(deckRepository.findLockedByIdAndProfileId(DECK_ID, PROFILE_ID))
                .thenReturn(Optional.of(deck));
        lenient().when(deckCardRepository.findByDeckId(DECK_ID)).thenReturn(List.of());
        lenient()
                .when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of());
        lenient().when(deckTagAssignmentRepository.findByDeckId(DECK_ID)).thenReturn(List.of());
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
    void shouldSnapshotCardsOrderedByIdRegardlessOfRepositoryOrder() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());
        DeckCard second = deckCard(20L, 101L, 2);
        DeckCard first = deckCard(10L, 100L, 1);
        when(deckCardRepository.findByDeckId(DECK_ID)).thenReturn(List.of(second, first));
        ArgumentCaptor<DeckRevision> captor = ArgumentCaptor.forClass(DeckRevision.class);

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CARD_ADDED);

        verify(revisionRepository).save(captor.capture());
        DeckSnapshot snapshot = readSnapshot(captor.getValue());
        assertThat(snapshot.cards())
                .extracting(DeckSnapshot.CardEntry::cardPrintingId)
                .containsExactly(100L, 101L);
    }

    @Test
    void shouldSnapshotCategoriesInDisplayOrder() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(category("Land", 0), category("Ramp", 1)));
        ArgumentCaptor<DeckRevision> captor = ArgumentCaptor.forClass(DeckRevision.class);

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.CATEGORY_CHANGED);

        verify(revisionRepository).save(captor.capture());
        DeckSnapshot snapshot = readSnapshot(captor.getValue());
        assertThat(snapshot.categoryNames()).containsExactly("Land", "Ramp");
    }

    @Test
    void shouldSnapshotTagNamesSortedAlphabeticallyRegardlessOfAssignmentOrder() {
        when(revisionRepository.findFirstByDeckIdOrderByRevisionNumberDesc(DECK_ID))
                .thenReturn(Optional.empty());
        when(deckTagAssignmentRepository.findByDeckId(DECK_ID))
                .thenReturn(
                        List.of(
                                new DeckTagAssignment(DECK_ID, 2L),
                                new DeckTagAssignment(DECK_ID, 1L)));
        when(deckTagRepository.findAllById(List.of(2L, 1L)))
                .thenReturn(List.of(tag(2L, "Zebra"), tag(1L, "Aggro")));
        ArgumentCaptor<DeckRevision> captor = ArgumentCaptor.forClass(DeckRevision.class);

        service().record(DECK_ID, PROFILE_ID, DeckChangeType.TAG_CHANGED);

        verify(revisionRepository).save(captor.capture());
        DeckSnapshot snapshot = readSnapshot(captor.getValue());
        assertThat(snapshot.tagNames()).containsExactly("Aggro", "Zebra");
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

    private DeckSnapshot readSnapshot(DeckRevision revision) {
        return mapper.readValue(revision.getSnapshot(), DeckSnapshot.class);
    }

    private DeckRevision existingRevision(int revisionNumber) {
        return new DeckRevision(
                DECK_ID,
                PROFILE_ID,
                revisionNumber,
                revisionNumber > 1 ? revisionNumber - 1 : null,
                new DeckRevision.Content(DeckChangeType.CREATED, null, "{}"));
    }

    private static DeckCard deckCard(long id, long printingId, int quantity) {
        DeckCard card = new DeckCard(DECK_ID, printingId, quantity, DeckCard.Section.MAIN_DECK);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static DeckCategory category(String name, int order) {
        return new DeckCategory(DECK_ID, name, order, false);
    }

    private static DeckTag tag(long id, String name) {
        DeckTag tag = new DeckTag(PROFILE_ID, name);
        ReflectionTestUtils.setField(tag, "id", id);
        return tag;
    }

    private DeckRevisionService service() {
        return new DeckRevisionService(
                revisionRepository,
                deckRepository,
                deckCardRepository,
                deckCategoryRepository,
                deckTagAssignmentRepository,
                deckTagRepository,
                mapper);
    }
}
