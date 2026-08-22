package com.deckassemble.decks.application.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.PracticeCard;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.application.simulation.MulliganStrategy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MockitoExtension.class)
abstract class MatchServiceTestSupport {

    static final long YOUR_DECK_ID = 1L;

    static final long OPPONENT_DECK_ID = 2L;

    static final long CALLER_PROFILE_ID = 42L;

    static final long YOUR_COMMANDER_CARD_ID = 100L;

    static final long OPPONENT_COMMANDER_CARD_ID = 200L;

    @Mock private DeckRevisionService deckRevisionService;

    @Mock private CardCatalogService cardCatalogService;

    void stubMatchDecks(int mainDeckQuantity, Map<Long, Card> catalog) {
        stubMatchDecks(
                mainDeckQuantity, catalog, YOUR_COMMANDER_CARD_ID, OPPONENT_COMMANDER_CARD_ID);
    }

    void stubMatchDecks(
            int mainDeckQuantity,
            Map<Long, Card> catalog,
            long yourCommanderCardId,
            long opponentCommanderCardId) {
        lenient()
                .when(deckRevisionService.snapshotAt(YOUR_DECK_ID, 1))
                .thenReturn(
                        snapshot(
                                List.of(
                                        entry(1L, mainDeckQuantity, "MAIN_DECK"),
                                        entry(10L, 1, "COMMANDER")),
                                yourCommanderCardId));
        lenient()
                .when(deckRevisionService.snapshotAtForSharedAccess(OPPONENT_DECK_ID, 2))
                .thenReturn(
                        snapshot(
                                List.of(
                                        entry(2L, mainDeckQuantity, "MAIN_DECK"),
                                        entry(20L, 1, "COMMANDER")),
                                opponentCommanderCardId));
        lenient()
                .when(cardCatalogService.getPracticeCardsByPrintingIds(any()))
                .thenReturn(practiceCatalog(catalog));
    }

    static Map<Long, Card> defaultCatalog() {
        Map<Long, Card> catalog = new ConcurrentHashMap<>();
        catalog.put(1L, creatureCard(1L, "Bear", "2", "2"));
        catalog.put(10L, creatureCard(YOUR_COMMANDER_CARD_ID, "Your Commander", "5", "5"));
        catalog.put(2L, creatureCard(2L, "Elite", "3", "3"));
        catalog.put(20L, creatureCard(OPPONENT_COMMANDER_CARD_ID, "Opponent Commander", "4", "4"));
        return catalog;
    }

    static Map<Long, PracticeCard> practiceCatalog(Map<Long, Card> catalog) {
        return catalog.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> new PracticeCard(entry.getKey(), entry.getValue(), null)));
    }

    static DeckSnapshot.CardEntry entry(long printingId, int quantity, String section) {
        return new DeckSnapshot.CardEntry(printingId, quantity, section, "OWNED");
    }

    static Card creatureCard(long id, String name, String power, String toughness) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Creature — Bear");
        card.setOracleText("");
        card.setPower(power);
        card.setToughness(toughness);
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    static Card landCard(long id, String name) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Land");
        card.setOracleText("");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    static Card instantCard(long id, String name) {
        Card card = new Card("oracle-" + id, name);
        card.setTypeLine("Instant");
        card.setOracleText("");
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    void advanceSteps(Match match, int count) {
        for (int i = 0; i < count; i++) {
            match.advanceStepNow();
        }
    }

    static DeckSnapshot snapshot(List<DeckSnapshot.CardEntry> cards, long commanderCardId) {
        return new DeckSnapshot(
                "Deck",
                "COMMANDER",
                null,
                commanderCardId,
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

    static MatchRequest request(boolean callerOnThePlay, @Nullable Long seed) {
        return new MatchRequest(
                YOUR_DECK_ID,
                1,
                OPPONENT_DECK_ID,
                2,
                MulliganStrategy.NONE,
                null,
                null,
                seed,
                callerOnThePlay);
    }

    void castAndResolve(Match match, long printingId) {
        service().castSpell(match.id(), CALLER_PROFILE_ID, printingId, null);
        service().passPriority(match.id(), CALLER_PROFILE_ID);
        service().passPriority(match.id(), CALLER_PROFILE_ID);
    }

    void assertBadRequest(ThrowingCallable action, String reason) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> {
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                            assertThat(exception.getReason()).contains(reason);
                        });
    }

    @Nullable MatchService service;

    MatchService service() {
        if (service == null) {
            service = new MatchService(deckRevisionService, cardCatalogService);
        }
        return service;
    }
}
