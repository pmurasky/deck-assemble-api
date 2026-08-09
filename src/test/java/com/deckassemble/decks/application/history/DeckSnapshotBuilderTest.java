package com.deckassemble.decks.application.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.decks.domain.organization.DeckCategory;
import com.deckassemble.decks.domain.organization.DeckCategoryRepository;
import com.deckassemble.decks.domain.organization.DeckTag;
import com.deckassemble.decks.domain.organization.DeckTagAssignment;
import com.deckassemble.decks.domain.organization.DeckTagAssignmentRepository;
import com.deckassemble.decks.domain.organization.DeckTagRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * Covers canonical snapshot assembly and its JSON round-trip, extracted from DeckRevisionService.
 */
@ExtendWith(MockitoExtension.class)
class DeckSnapshotBuilderTest {

    private static final long DECK_ID = 1L;
    private static final long PROFILE_ID = 42L;

    @Mock private DeckCardRepository deckCardRepository;
    @Mock private DeckCategoryRepository deckCategoryRepository;
    @Mock private DeckTagAssignmentRepository deckTagAssignmentRepository;
    @Mock private DeckTagRepository deckTagRepository;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private Deck deck;
    private DeckSnapshotBuilder builder;

    @BeforeEach
    void stubCommonCollaborators() {
        deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        ReflectionTestUtils.setField(deck, "id", DECK_ID);
        lenient().when(deckCardRepository.findByDeckId(DECK_ID)).thenReturn(List.of());
        lenient()
                .when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of());
        lenient().when(deckTagAssignmentRepository.findByDeckId(DECK_ID)).thenReturn(List.of());
        builder =
                new DeckSnapshotBuilder(
                        deckCardRepository,
                        deckCategoryRepository,
                        deckTagAssignmentRepository,
                        deckTagRepository,
                        mapper);
    }

    @Test
    void shouldSnapshotCardsOrderedByIdRegardlessOfRepositoryOrder() {
        DeckCard second = deckCard(20L, 101L, 2);
        DeckCard first = deckCard(10L, 100L, 1);
        when(deckCardRepository.findByDeckId(DECK_ID)).thenReturn(List.of(second, first));

        DeckSnapshot snapshot = mapper.readValue(builder.toJson(deck), DeckSnapshot.class);

        assertThat(snapshot.cards())
                .extracting(DeckSnapshot.CardEntry::cardPrintingId)
                .containsExactly(100L, 101L);
    }

    @Test
    void shouldSnapshotCategoriesInDisplayOrder() {
        when(deckCategoryRepository.findByDeckIdOrderByDisplayOrderAscIdAsc(DECK_ID))
                .thenReturn(List.of(category("Land", 0), category("Ramp", 1)));

        DeckSnapshot snapshot = mapper.readValue(builder.toJson(deck), DeckSnapshot.class);

        assertThat(snapshot.categoryNames()).containsExactly("Land", "Ramp");
    }

    @Test
    void shouldSnapshotTagNamesSortedAlphabeticallyRegardlessOfAssignmentOrder() {
        when(deckTagAssignmentRepository.findByDeckId(DECK_ID))
                .thenReturn(
                        List.of(
                                new DeckTagAssignment(DECK_ID, 2L),
                                new DeckTagAssignment(DECK_ID, 1L)));
        when(deckTagRepository.findAllById(List.of(2L, 1L)))
                .thenReturn(List.of(tag(2L, "Zebra"), tag(1L, "Aggro")));

        DeckSnapshot snapshot = mapper.readValue(builder.toJson(deck), DeckSnapshot.class);

        assertThat(snapshot.tagNames()).containsExactly("Aggro", "Zebra");
    }

    @Test
    void shouldRoundTripASnapshotThroughJson() {
        String json = builder.toJson(deck);

        DeckSnapshot snapshot = builder.fromJson(json);

        assertThat(snapshot.name()).isEqualTo(deck.getName());
        assertThat(snapshot.formatCode()).isEqualTo(deck.getFormatCode());
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
}
