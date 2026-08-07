package com.deckassemble.decks.application.alternatives;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.deckassemble.cards.application.CardCatalogService;
import com.deckassemble.cards.application.CardPriceService;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrice;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckCardNotFoundException;
import com.deckassemble.decks.application.DeckComboResponse;
import com.deckassemble.decks.application.DeckComboService;
import com.deckassemble.decks.application.OwnershipChecker;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckCard;
import com.deckassemble.decks.domain.DeckCardRepository;
import com.deckassemble.recommendations.application.CardCategorizer;
import com.deckassemble.recommendations.application.CardScore;
import com.deckassemble.recommendations.application.EdhrecCommanderService;
import com.deckassemble.recommendations.application.RecommendationReasonCode;
import com.deckassemble.recommendations.application.ScoreContribution;
import com.deckassemble.recommendations.domain.SpellbookCombo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

@ExtendWith(MockitoExtension.class)
class DeckCardAlternativeServiceTest {

    private static final long DECK_ID = 7L;
    private static final long PROFILE_ID = 42L;
    private static final long DECK_CARD_ID = 99L;
    private static final long TARGET_PRINTING_ID = 900L;

    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private DeckCardRepository deckCardRepository;
    @Mock private CardCatalogService cardCatalogService;
    @Mock private EdhrecCommanderService edhrecCommanderService;
    @Mock private OwnershipChecker ownershipChecker;
    @Mock private CardPriceService cardPriceService;
    @Mock private DeckComboService deckComboService;

    private DeckCardAlternativeService service;

    @BeforeEach
    void setUp() {
        service =
                new DeckCardAlternativeService(
                        deckAccessGuard,
                        deckCardRepository,
                        cardCatalogService,
                        edhrecCommanderService,
                        new CardCategorizer(),
                        ownershipChecker,
                        cardPriceService,
                        deckComboService);
    }

    @Test
    void shouldExcludeIllegalAndOffColorCandidates() {
        var altDraw = drawCard(10L, "Alt Draw", "oracle-alt-draw");
        var banned = bannedCard(11L, "Banned Draw", "oracle-banned-draw");
        var blue = drawCard(12L, "Blue Draw", "oracle-blue-draw");
        blue.setColorIdentity("U");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(altDraw, banned, blue),
                Map.of(
                        "Alt Draw", new CardScore(0.5, 10L),
                        "Banned Draw", new CardScore(0.9, 20L),
                        "Blue Draw", new CardScore(0.8, 30L)),
                Map.of(10L, 100L, 11L, 101L, 12L, 102L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives).extracting(DeckCardAlternative::name).containsExactly("Alt Draw");
        assertThat(reasonCodes(alternatives.getFirst()))
                .contains(
                        RecommendationReasonCode.CATEGORY_NEED,
                        RecommendationReasonCode.MANA_VALUE_DISTANCE,
                        RecommendationReasonCode.COMMANDER_SYNERGY,
                        RecommendationReasonCode.PRICE);
    }

    @Test
    void shouldOrderOwnedFirstWhenRequested() {
        var unownedHigh = drawCard(10L, "Unowned High", "oracle-unowned-high");
        var ownedLow = drawCard(11L, "Owned Low", "oracle-owned-low");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(unownedHigh, ownedLow),
                Map.of(
                        "Unowned High", new CardScore(0.9, null),
                        "Owned Low", new CardScore(0.1, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(Set.of(101L), Map.of(), noCombos());

        var ownedFirst = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);
        var scoreFirst = service.suggest(DECK_ID, DECK_CARD_ID, 10, false);

        assertThat(ownedFirst)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Owned Low", "Unowned High");
        assertThat(ownedFirst.getFirst().owned()).isTrue();
        assertThat(reasonCodes(ownedFirst.getFirst())).contains(RecommendationReasonCode.OWNED);
        assertThat(scoreFirst)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Unowned High", "Owned Low");
    }

    @Test
    void shouldWarnWhenAlternativeBreaksComboWithTarget() {
        var altPiece = comboCard(10L, "Alt Piece", "oracle-alt-piece");
        var otherPiece = comboCard(11L, "Other Piece", "oracle-other-piece");
        stubDeckWithCommander();
        stubTarget(comboTarget());
        stubCandidates(
                List.of(altPiece, otherPiece),
                Map.of(
                        "Alt Piece", new CardScore(0.9, 10L),
                        "Other Piece", new CardScore(0.5, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(
                Set.of(),
                Map.of(),
                new DeckComboResponse(
                        true,
                        List.of(
                                new SpellbookCombo(
                                        "c1",
                                        List.of("Combo Piece", "Other Piece"),
                                        List.of(),
                                        "desc",
                                        ""))));

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(reasonCodes(alternatives.get(0))).contains(RecommendationReasonCode.COMBO);
        assertThat(reason(alternatives.get(0), RecommendationReasonCode.COMBO).evidence())
                .containsEntry("warning", "breaks combo with Combo Piece");
        assertThat(reasonCodes(alternatives.get(1))).doesNotContain(RecommendationReasonCode.COMBO);
    }

    @Test
    void shouldMarkMissingPricesAsUnknown() {
        var priced = drawCard(10L, "Priced Draw", "oracle-priced-draw");
        var unpriced = drawCard(11L, "Unpriced Draw", "oracle-unpriced-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(priced, unpriced),
                Map.of(
                        "Priced Draw", new CardScore(0.5, 10L),
                        "Unpriced Draw", new CardScore(0.5, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(
                Set.of(),
                Map.of(100L, new CardPrice(new BigDecimal("1.50"), null, null, null)),
                noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Priced Draw", "Unpriced Draw");
        assertThat(alternatives.get(0).priceUsd()).isEqualByComparingTo("1.50");
        assertThat(reason(alternatives.get(0), RecommendationReasonCode.PRICE).evidence())
                .containsEntry("usd", "1.50");
        assertThat(alternatives.get(1).priceUsd()).isNull();
        assertThat(reason(alternatives.get(1), RecommendationReasonCode.PRICE).evidence())
                .containsEntry("usd", "unknown");
    }

    @Test
    void shouldBreakTiesDeterministicallyByName() {
        var beta = drawCard(10L, "Beta Draw", "oracle-beta-draw");
        var alpha = drawCard(11L, "Alpha Draw", "oracle-alpha-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(beta, alpha),
                Map.of(
                        "Beta Draw", new CardScore(0.5, 10L),
                        "Alpha Draw", new CardScore(0.5, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Alpha Draw", "Beta Draw");
        assertThat(alternatives.get(0).total()).isEqualByComparingTo(alternatives.get(1).total());
    }

    @Test
    void shouldRankCloserManaValueHigher() {
        var close = drawCard(10L, "Close Draw", "oracle-close-draw");
        var far = drawCard(11L, "Far Draw", "oracle-far-draw");
        far.setManaValue(new BigDecimal("6"));
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(close, far),
                Map.of(
                        "Close Draw", new CardScore(0.5, 10L),
                        "Far Draw", new CardScore(0.5, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Close Draw", "Far Draw");
        assertThat(
                        reason(alternatives.get(0), RecommendationReasonCode.MANA_VALUE_DISTANCE)
                                .evidence())
                .containsEntry("distance", "0");
    }

    @Test
    void shouldLimitResults() {
        var first = drawCard(10L, "First Draw", "oracle-first-draw");
        var second = drawCard(11L, "Second Draw", "oracle-second-draw");
        var third = drawCard(12L, "Third Draw", "oracle-third-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(first, second, third),
                Map.of(
                        "First Draw", new CardScore(0.9, 10L),
                        "Second Draw", new CardScore(0.8, 20L),
                        "Third Draw", new CardScore(0.7, 30L)),
                Map.of(10L, 100L, 11L, 101L, 12L, 102L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 2, true);

        assertThat(alternatives)
                .extracting(DeckCardAlternative::name)
                .containsExactly("First Draw", "Second Draw");
    }

    @Test
    void shouldExcludeTargetAndCommanderOracles() {
        var targetDuplicate = drawCard(10L, "Target Duplicate", "oracle-target-draw");
        var altDraw = drawCard(11L, "Alt Draw", "oracle-alt-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(targetDuplicate, altDraw),
                Map.of(
                        "Target Duplicate", new CardScore(0.9, 10L),
                        "Alt Draw", new CardScore(0.5, 20L)),
                Map.of(10L, 100L, 11L, 101L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives).extracting(DeckCardAlternative::name).containsExactly("Alt Draw");
    }

    @Test
    void shouldThrowWhenDeckCardIsUnknown() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deckWithCommander());
        when(deckCardRepository.findByIdAndDeckId(DECK_CARD_ID, DECK_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suggest(DECK_ID, DECK_CARD_ID, 10, true))
                .isInstanceOf(DeckCardNotFoundException.class);
    }

    @Test
    void shouldReturnEmptyWhenDeckHasNoCommander() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(new Deck(PROFILE_ID, "Deck", "COMMANDER"));
        stubTarget();

        assertThat(service.suggest(DECK_ID, DECK_CARD_ID, 10, true)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEdhrecIsUnavailable() {
        stubDeckWithCommander();
        stubTarget();
        when(edhrecCommanderService.getCardScores("oracle-commander", "Test Commander"))
                .thenThrow(new RestClientException("down"));

        assertThat(service.suggest(DECK_ID, DECK_CARD_ID, 10, true)).isEmpty();
    }

    @Test
    void shouldSkipCandidateWithoutPrinting() {
        var printed = drawCard(10L, "Printed Draw", "oracle-printed-draw");
        var unprinted = drawCard(11L, "Unprinted Draw", "oracle-unprinted-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(printed, unprinted),
                Map.of(
                        "Printed Draw", new CardScore(0.5, 10L),
                        "Unprinted Draw", new CardScore(0.9, 20L)),
                Map.of(10L, 100L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives)
                .extracting(DeckCardAlternative::name)
                .containsExactly("Printed Draw");
    }

    @Test
    void shouldTolerateUnavailableComboCheck() {
        var altDraw = drawCard(10L, "Alt Draw", "oracle-alt-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(altDraw), Map.of("Alt Draw", new CardScore(0.5, 10L)), Map.of(10L, 100L));
        stubEnrichment(Set.of(), Map.of(), new DeckComboResponse(false, List.of()));

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives).extracting(DeckCardAlternative::name).containsExactly("Alt Draw");
        assertThat(reasonCodes(alternatives.getFirst()))
                .doesNotContain(RecommendationReasonCode.COMBO);
    }

    @Test
    void shouldIncludePartnerColorsWithSecondCommander() {
        var blue = drawCard(12L, "Blue Draw", "oracle-blue-draw");
        blue.setColorIdentity("U");
        var deck = deckWithCommander();
        deck.setSecondaryCommanderCardId(3L);
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deck);
        when(cardCatalogService.getCardWithFaces(1L)).thenReturn(commander());
        when(cardCatalogService.getCardWithFaces(3L)).thenReturn(partner());
        stubTarget();
        stubCandidates(
                List.of(blue), Map.of("Blue Draw", new CardScore(0.5, 10L)), Map.of(12L, 102L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives).extracting(DeckCardAlternative::name).containsExactly("Blue Draw");
    }

    @Test
    void shouldSkipCategoryBonusWhenCategoryDiffers() {
        var ramp =
                legalCard(
                        10L,
                        "Alt Ramp",
                        "oracle-alt-ramp",
                        "Sorcery",
                        "Search your library for a basic land card and put it onto the battlefield"
                                + " tapped.");
        ramp.setManaValue(null);
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(ramp), Map.of("Alt Ramp", new CardScore(0.5, 10L)), Map.of(10L, 100L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        assertThat(alternatives).extracting(DeckCardAlternative::name).containsExactly("Alt Ramp");
        assertThat(reasonCodes(alternatives.getFirst()))
                .doesNotContain(RecommendationReasonCode.CATEGORY_NEED);
    }

    @Test
    void shouldScoreInclusionOnlyScoreAsZeroSynergy() {
        var altDraw = drawCard(10L, "Alt Draw", "oracle-alt-draw");
        stubDeckWithCommander();
        stubTarget();
        stubCandidates(
                List.of(altDraw),
                Map.of("Alt Draw", new CardScore(null, 1500L)),
                Map.of(10L, 100L));
        stubEnrichment(Set.of(), Map.of(), noCombos());

        var alternatives = service.suggest(DECK_ID, DECK_CARD_ID, 10, true);

        var synergy = reason(alternatives.getFirst(), RecommendationReasonCode.COMMANDER_SYNERGY);
        assertThat(synergy.points()).isZero();
        assertThat(synergy.evidence()).containsEntry("inclusion", "1500");
    }

    private void stubDeckWithCommander() {
        when(deckAccessGuard.owned(DECK_ID)).thenReturn(deckWithCommander());
        when(cardCatalogService.getCardWithFaces(1L)).thenReturn(commander());
    }

    private static Deck deckWithCommander() {
        var deck = new Deck(PROFILE_ID, "Deck", "COMMANDER");
        deck.setCommanderCardId(1L);
        return deck;
    }

    private void stubTarget() {
        stubTarget(drawCard(2L, "Target Draw", "oracle-target-draw"));
    }

    private void stubTarget(Card card) {
        when(deckCardRepository.findByIdAndDeckId(DECK_CARD_ID, DECK_ID))
                .thenReturn(
                        Optional.of(
                                new DeckCard(
                                        DECK_ID,
                                        TARGET_PRINTING_ID,
                                        1,
                                        DeckCard.Section.MAIN_DECK)));
        when(cardCatalogService.getCardsByPrintingIds(Set.of(TARGET_PRINTING_ID)))
                .thenReturn(Map.of(TARGET_PRINTING_ID, card));
    }

    private void stubCandidates(
            List<Card> cards, Map<String, CardScore> scores, Map<Long, Long> printingByCardId) {
        when(edhrecCommanderService.getCardScores("oracle-commander", "Test Commander"))
                .thenReturn(scores);
        when(cardCatalogService.getCardsByNames(any())).thenReturn(cards);
        when(cardCatalogService.getLatestPrintingIdByCardIds(any())).thenReturn(printingByCardId);
    }

    private void stubEnrichment(
            Set<Long> owned, Map<Long, CardPrice> prices, DeckComboResponse combos) {
        when(deckAccessGuard.profileId()).thenReturn(PROFILE_ID);
        when(ownershipChecker.filterOwnedPrintingIds(eq(PROFILE_ID), any())).thenReturn(owned);
        when(cardPriceService.latestPrices(any())).thenReturn(prices);
        when(deckComboService.getCombos(DECK_ID)).thenReturn(combos);
    }

    private static DeckComboResponse noCombos() {
        return new DeckComboResponse(true, List.of());
    }

    private static List<RecommendationReasonCode> reasonCodes(DeckCardAlternative alternative) {
        return alternative.contributions().stream().map(ScoreContribution::code).toList();
    }

    private static ScoreContribution reason(
            DeckCardAlternative alternative, RecommendationReasonCode code) {
        return alternative.contributions().stream()
                .filter(contribution -> contribution.code() == code)
                .findFirst()
                .orElseThrow();
    }

    private static Card commander() {
        var commander = new Card("oracle-commander", "Test Commander");
        ReflectionTestUtils.setField(commander, "id", 1L);
        commander.setColorIdentity("W");
        return commander;
    }

    private static Card partner() {
        var partner = new Card("oracle-partner", "Blue Partner");
        ReflectionTestUtils.setField(partner, "id", 3L);
        partner.setColorIdentity("U");
        return partner;
    }

    private static Card drawCard(long id, String name, String oracleId) {
        return legalCard(id, name, oracleId, "Sorcery", "Draw a card.");
    }

    private static Card comboCard(long id, String name, String oracleId) {
        return legalCard(
                id, name, oracleId, "Artifact", "Whenever a creature enters, you gain 1 life.");
    }

    private static Card comboTarget() {
        return legalCard(
                2L,
                "Combo Piece",
                "oracle-combo-piece",
                "Artifact",
                "Whenever a creature enters, you gain 1 life.");
    }

    private static Card bannedCard(long id, String name, String oracleId) {
        var card = card(id, name, oracleId, "Sorcery", "Draw a card.");
        card.getLegalities().add(new CardLegality(card, "commander", "banned"));
        return card;
    }

    private static Card legalCard(
            long id, String name, String oracleId, String typeLine, String oracleText) {
        var card = card(id, name, oracleId, typeLine, oracleText);
        card.getLegalities().add(new CardLegality(card, "commander", "legal"));
        return card;
    }

    private static Card card(
            long id, String name, String oracleId, String typeLine, String oracleText) {
        var card = new Card(oracleId, name);
        ReflectionTestUtils.setField(card, "id", id);
        card.setColorIdentity("W");
        card.setManaValue(new BigDecimal("2"));
        var face = new CardFace(card, 0, name);
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        card.getFaces().add(face);
        return card;
    }
}
