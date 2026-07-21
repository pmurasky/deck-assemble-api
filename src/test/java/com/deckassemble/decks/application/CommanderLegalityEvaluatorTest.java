package com.deckassemble.decks.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommanderLegalityEvaluatorTest {

    private static final MagicSet SET = new MagicSet("set-id", "tst", "Test Set");

    @Mock private CardRepository cardRepository;
    @Mock private CardPrintingRepository cardPrintingRepository;

    @Test
    void shouldAcceptLegalCommanderDeck() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Commander", "W"));
        DeckCard creature =
                mainDeckCard(10L, 1, card("oracle-a", "Card A", "Creature", "", "W", "legal"));
        DeckCard lands =
                mainDeckCard(11L, 98, card("oracle-b", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(creature, lands));

        assertThat(result.legal()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void shouldRequireCommanderFormat() {
        Deck deck = new Deck(1L, "Test", "modern");

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_FORMAT_REQUIRED");
        assertThat(result.legal()).isFalse();
    }

    @Test
    void shouldRequireCommander() {
        Deck deck = commanderDeck(null);

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_REQUIRED");
    }

    @Test
    void shouldFlagUnknownCommanderCard() {
        Deck deck = commanderDeck(99L);
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_NOT_FOUND");
    }

    @Test
    void shouldFlagIneligibleCommander() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, card("oracle-cmd", "Vanilla", "Creature — Bear", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_INELIGIBLE");
    }

    @Test
    void shouldFlagBannedCommander() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Banned One", "", "", "banned"));

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_LEGALITY_INVALID");
    }

    @Test
    void shouldFlagMissingLegalityData() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Mystery", "", "", null));

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_LEGALITY_UNKNOWN");
    }

    @Test
    void shouldFlagColorIdentityViolation() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "White Commander", "W"));
        DeckCard offColor =
                mainDeckCard(10L, 1, card("oracle-a", "Red Card", "Creature", "", "R", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(offColor));

        assertThat(codes(result)).contains("COLOR_IDENTITY_VIOLATION");
    }

    @Test
    void shouldFlagSingletonViolation() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Commander", ""));
        Card duplicate = card("oracle-a", "Card A", "Creature", "", "", "legal");
        DeckCard two = mainDeckCard(10L, 2, duplicate);

        DeckLegalityResponse result = evaluate(deck, List.of(two));

        assertThat(codes(result)).contains("SINGLETON_VIOLATION");
    }

    @Test
    void shouldAllowMultipleBasicLands() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Commander", ""));
        DeckCard lands =
                mainDeckCard(10L, 5, card("oracle-b", "Island", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result)).doesNotContain("SINGLETON_VIOLATION");
    }

    @Test
    void shouldFlagWrongDeckSize() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Commander", ""));
        DeckCard lands =
                mainDeckCard(10L, 50, card("oracle-b", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result)).contains("DECK_SIZE_INVALID");
    }

    @Test
    void shouldFlagMissingPrinting() {
        Deck deck = commanderDeck(1L);
        commanderAt(1L, legendary("oracle-cmd", "Commander", ""));
        when(cardPrintingRepository.findById(10L)).thenReturn(Optional.empty());
        DeckCard missing = new DeckCard(1L, 10L, 1, DeckCard.Section.MAIN_DECK);

        DeckLegalityResponse result = evaluate(deck, List.of(missing));

        assertThat(codes(result)).contains("CARD_NOT_FOUND");
    }

    @Test
    void shouldAcceptGenericPartnerPair() {
        Deck deck = partnerDeck(1L, 2L);
        commanderAt(1L, legendary("oracle-a", "Partner A", "W", "Partner", "legal"));
        commanderAt(2L, card("oracle-b", "Partner B", "Creature — Human", "Partner", "U", "legal"));
        DeckCard lands =
                mainDeckCard(10L, 98, card("oracle-c", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result))
                .doesNotContain("COMMANDER_PAIR_INVALID", "SECONDARY_COMMANDER_INELIGIBLE");
        assertThat(result.legal()).isTrue();
    }

    @Test
    void shouldRejectUnpairedCommanders() {
        Deck deck = partnerDeck(1L, 2L);
        commanderAt(1L, legendary("oracle-a", "Solo A", "W"));
        commanderAt(2L, legendary("oracle-b", "Solo B", "U"));

        DeckLegalityResponse result = evaluate(deck, List.of());

        assertThat(codes(result)).contains("COMMANDER_PAIR_INVALID");
    }

    @Test
    void shouldAcceptNamedPartnerPair() {
        Deck deck = partnerDeck(1L, 2L);
        commanderAt(
                1L,
                legendary(
                        "oracle-a",
                        "Toothy",
                        "G",
                        "Partner with Pir, Imaginative Rascal",
                        "legal"));
        commanderAt(2L, legendary("oracle-b", "Pir, Imaginative Rascal", "G"));
        DeckCard lands =
                mainDeckCard(10L, 98, card("oracle-c", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result)).doesNotContain("COMMANDER_PAIR_INVALID");
        assertThat(result.legal()).isTrue();
    }

    @Test
    void shouldAcceptBackgroundPair() {
        Deck deck = partnerDeck(1L, 2L);
        commanderAt(1L, legendary("oracle-a", "Hero", "W", "Choose a Background", "legal"));
        commanderAt(
                2L,
                card(
                        "oracle-b",
                        "Folk Hero",
                        "Legendary Enchantment — Background",
                        "Background",
                        "W",
                        "legal"));
        DeckCard lands =
                mainDeckCard(10L, 98, card("oracle-c", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result)).doesNotContain("COMMANDER_PAIR_INVALID");
        assertThat(result.legal()).isTrue();
    }

    @Test
    void shouldAcceptDoctorsCompanionPair() {
        Deck deck = partnerDeck(1L, 2L);
        commanderAt(1L, legendary("oracle-a", "The Doctor", "U", "Time Lord Doctor", "legal"));
        commanderAt(
                2L,
                card(
                        "oracle-b",
                        "Companion",
                        "Creature — Human",
                        "Doctor's Companion",
                        "W",
                        "legal"));
        DeckCard lands =
                mainDeckCard(10L, 98, card("oracle-c", "Forest", "Basic Land", "", "", "legal"));

        DeckLegalityResponse result = evaluate(deck, List.of(lands));

        assertThat(codes(result)).doesNotContain("COMMANDER_PAIR_INVALID");
        assertThat(result.legal()).isTrue();
    }

    private Deck commanderDeck(Long commanderCardId) {
        Deck deck = new Deck(1L, "Test", "COMMANDER");
        deck.setCommanderCardId(commanderCardId);
        return deck;
    }

    private Deck partnerDeck(Long commanderCardId, Long secondaryCommanderCardId) {
        Deck deck = commanderDeck(commanderCardId);
        deck.setSecondaryCommanderCardId(secondaryCommanderCardId);
        return deck;
    }

    private void commanderAt(long id, Card card) {
        when(cardRepository.findById(id)).thenReturn(Optional.of(card));
    }

    private DeckCard mainDeckCard(long printingId, int quantity, Card card) {
        when(cardPrintingRepository.findById(printingId))
                .thenReturn(Optional.of(new CardPrinting(card, SET, "scryfall-" + printingId)));
        return new DeckCard(1L, printingId, quantity, DeckCard.Section.MAIN_DECK);
    }

    private Card legendary(String oracleId, String name, String colorIdentity) {
        return legendary(oracleId, name, colorIdentity, "", "legal");
    }

    private Card legendary(
            String oracleId, String name, String colorIdentity, String oracleText, String status) {
        return card(
                oracleId, name, "Legendary Creature — Human", oracleText, colorIdentity, status);
    }

    private Card card(
            String oracleId,
            String name,
            String typeLine,
            String oracleText,
            String colorIdentity,
            String legalityStatus) {
        Card card = new Card(oracleId, name);
        card.setTypeLine(typeLine);
        card.setOracleText(oracleText);
        card.setColorIdentity(colorIdentity);
        if (legalityStatus != null) {
            card.getLegalities().add(new CardLegality(card, "commander", legalityStatus));
        }
        return card;
    }

    private DeckLegalityResponse evaluate(Deck deck, List<DeckCard> cards) {
        return new CommanderLegalityEvaluator(cardRepository, cardPrintingRepository)
                .evaluate(deck, cards);
    }

    private static List<String> codes(DeckLegalityResponse response) {
        return response.violations().stream().map(DeckLegalityResponse.Violation::code).toList();
    }
}
