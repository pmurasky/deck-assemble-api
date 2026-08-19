package com.deckassemble.decks.application.simulation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PracticeSessionServiceTest {

    private static final long DECK_ID = 1L;

    @Mock private DeckRevisionService deckRevisionService;
    @Mock private CardCatalogService cardCatalogService;

    @Test
    void shouldProduceIdenticalTurnSequenceForTheSameSeed() {
        // Given
        stubMixedLibrary(20, 20);

        // When
        PracticeSessionResponse first = service().start(DECK_ID, request(true, 42L));
        PracticeSessionResponse second = service().start(DECK_ID, request(true, 42L));

        // Then
        assertThat(second).usingRecursiveComparison().ignoringFields("sessionId").isEqualTo(first);
        for (int i = 0; i < 5; i++) {
            PracticeSessionResponse firstStep = service().step(DECK_ID, first.sessionId());
            PracticeSessionResponse secondStep = service().step(DECK_ID, second.sessionId());
            assertThat(secondStep)
                    .usingRecursiveComparison()
                    .ignoringFields("sessionId")
                    .isEqualTo(firstStep);
        }
    }

    @Test
    void shouldPlayOneLandPerTurnFromAnAllLandLibrary() {
        // Given
        stubHomogeneousLibrary(20, landCard(1L, "Forest", "G"));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 7L));

        // When
        PracticeSessionResponse turnOne = service().step(DECK_ID, started.sessionId());
        PracticeSessionResponse turnTwo = service().step(DECK_ID, started.sessionId());

        // Then
        assertThat(turnOne.turn()).isEqualTo(1);
        assertThat(turnOne.drawnCard()).isNull();
        assertThat(turnOne.landPlayed()).isEqualTo("Forest");
        assertThat(turnOne.landsInPlay()).isEqualTo(1);
        assertThat(turnTwo.drawnCard()).isEqualTo("Forest");
        assertThat(turnTwo.landPlayed()).isEqualTo("Forest");
        assertThat(turnTwo.landsInPlay()).isEqualTo(2);
    }

    @Test
    void shouldNeverPlayALandFromAnAllSpellLibrary() {
        // Given
        stubHomogeneousLibrary(20, spellCard(1L, "Bear", 2));
        PracticeSessionResponse started = service().start(DECK_ID, request(false, 9L));

        // When
        PracticeSessionResponse turnOne = service().step(DECK_ID, started.sessionId());

        // Then
        assertThat(turnOne.drawnCard()).isEqualTo("Bear");
        assertThat(turnOne.landPlayed()).isNull();
        assertThat(turnOne.landsInPlay()).isZero();
        assertThat(turnOne.castableSpells()).isEmpty();
    }

    @Test
    void shouldReportCastableSpellsOnceALandIsInPlay() {
        // Given: 4 Forests + 3 one-mana Bears fill the opening hand exactly.
        stubSnapshot(List.of(entry(1L, 4, "MAIN_DECK"), entry(2L, 3, "MAIN_DECK")));
        stubCatalog(Map.of(1L, landCard(1L, "Forest", "G"), 2L, spellCard(2L, "Bear", 1)));
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 3L));

        // When
        PracticeSessionResponse turnOne = service().step(DECK_ID, started.sessionId());

        // Then
        assertThat(started.hand()).hasSize(7);
        assertThat(turnOne.landPlayed()).isEqualTo("Forest");
        assertThat(turnOne.castableSpells()).containsExactly("Bear", "Bear", "Bear");
    }

    @Test
    void shouldMarkSessionFinishedWhenLibraryIsExhausted() {
        // Given: only one card left to draw after the opening hand.
        stubHomogeneousLibrary(8, spellCard(1L, "Bear", 2));
        PracticeSessionResponse started = service().start(DECK_ID, request(false, 5L));

        // When
        PracticeSessionResponse turnOne = service().step(DECK_ID, started.sessionId());

        // Then
        assertThat(turnOne.drawnCard()).isEqualTo("Bear");
        assertThat(turnOne.finished()).isTrue();
    }

    @Test
    void shouldResetSessionToTheSameOpeningHand() {
        // Given
        stubMixedLibrary(20, 20);
        PracticeSessionResponse started = service().start(DECK_ID, request(true, 42L));
        service().step(DECK_ID, started.sessionId());
        service().step(DECK_ID, started.sessionId());

        // When
        PracticeSessionResponse reset = service().reset(DECK_ID, started.sessionId());

        // Then
        assertThat(reset.turn()).isZero();
        assertThat(reset.sessionId()).isEqualTo(started.sessionId());
        assertThat(reset).usingRecursiveComparison().ignoringFields("sessionId").isEqualTo(started);
    }

    @Test
    void shouldRejectStepForUnknownSession() {
        // Given
        UUID unknown = UUID.randomUUID();

        // When / Then
        assertThatThrownBy(() -> service().step(DECK_ID, unknown))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("practice session");
    }

    @Test
    void shouldRejectLibrarySmallerThanTheOpeningHand() {
        // Given
        stubHomogeneousLibrary(5, spellCard(1L, "Bear", 2));

        // When / Then
        assertThatThrownBy(() -> service().start(DECK_ID, request(true, 1L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("library");
    }

    private void stubMixedLibrary(int landCount, int spellCount) {
        stubSnapshot(
                List.of(entry(1L, landCount, "MAIN_DECK"), entry(2L, spellCount, "MAIN_DECK")));
        stubCatalog(Map.of(1L, landCard(1L, "Forest", "G"), 2L, spellCard(2L, "Bear", 2)));
    }

    private void stubHomogeneousLibrary(int quantity, Card card) {
        stubSnapshot(List.of(entry(1L, quantity, "MAIN_DECK")));
        stubCatalog(Map.of(1L, card));
    }

    private void stubSnapshot(List<DeckSnapshot.CardEntry> cards) {
        lenient().when(deckRevisionService.snapshotAt(DECK_ID, 1)).thenReturn(snapshot(cards));
    }

    private void stubCatalog(Map<Long, Card> catalog) {
        lenient().when(cardCatalogService.getCardsByPrintingIds(any())).thenReturn(catalog);
    }

    private static DeckSnapshot.CardEntry entry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    private static Card landCard(long id, String name, String color) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Basic Land — " + name);
        card.setOracleText("{T}: Add {" + color + "}.");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static Card spellCard(long id, String name, int manaValue) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Creature — Bear");
        card.setOracleText("");
        card.setManaValue(BigDecimal.valueOf(manaValue));
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private static DeckSnapshot snapshot(List<DeckSnapshot.CardEntry> cards) {
        return new DeckSnapshot(
                "Deck",
                "COMMANDER",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "DRAFT",
                cards,
                List.of(),
                List.of());
    }

    private static PracticeSessionRequest request(boolean onThePlay, @Nullable Long seed) {
        return new PracticeSessionRequest(1, onThePlay, MulliganStrategy.NONE, null, null, seed);
    }

    private @Nullable PracticeSessionService service;

    private PracticeSessionService service() {
        if (service == null) {
            service = new PracticeSessionService(deckRevisionService, cardCatalogService);
        }
        return service;
    }
}
