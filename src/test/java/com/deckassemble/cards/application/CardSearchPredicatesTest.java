package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.AbstractIntegrationTest;
import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFunctionalCategory;
import com.deckassemble.cards.domain.CardLegality;
import com.deckassemble.cards.domain.CardPrinting;
import com.deckassemble.cards.domain.CardPrintingRepository;
import com.deckassemble.cards.domain.CardRepository;
import com.deckassemble.cards.domain.MagicSet;
import com.deckassemble.cards.domain.MagicSetRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

/**
 * One focused test per typed search predicate, exercised against a real Postgres query (not mocks)
 * so JPA Criteria subqueries and text-matching semantics are verified for real.
 *
 * <p>{@code @Transactional} rolls back this test's own writes, but the Postgres testcontainer is
 * shared across the whole suite, so reads still see rows committed by other test classes. Every
 * card name here is prefixed with {@link #MARKER} and every filter also requires that marker in the
 * {@code query} field (always ANDed in by {@code cardPredicate}), so a filter can only ever match
 * the rows this test created, however wide the numeric range or however common the text.
 */
@Transactional
class CardSearchPredicatesTest extends AbstractIntegrationTest {

    private static final String MARKER = "Zqp-";

    @Autowired private CardRepository cardRepository;
    @Autowired private CardPrintingRepository cardPrintingRepository;
    @Autowired private MagicSetRepository magicSetRepository;

    @Test
    void shouldFilterByOracleText() {
        Card match = save(card("Swords to Plowshares", "Exile target creature."));
        save(card("Shock", "Deal 2 damage to any target."));

        var results = search(withOracleText("exile"));

        assertThat(results).extracting(Card::getName).containsExactly(match.getName());
    }

    @Test
    void shouldFilterByManaValueRange() {
        Card cheap = save(cardWithManaValue("Llanowar Elves", "1"));
        save(cardWithManaValue("Craterhoof Behemoth", "8"));

        var results = search(manaValueRangeFilter(BigDecimal.ZERO, BigDecimal.valueOf(2)));

        assertThat(results).extracting(Card::getName).containsExactly(cheap.getName());
    }

    @Test
    void shouldFilterByPowerRange() {
        Card small = save(cardWithPowerToughness("Llanowar Elves", "1", "1"));
        save(cardWithPowerToughness("Craterhoof Behemoth", "8", "8"));

        var results = search(powerRangeFilter(0, 2));

        assertThat(results).extracting(Card::getName).containsExactly(small.getName());
    }

    @Test
    void shouldExcludeNonNumericPowerFromRange() {
        save(cardWithPowerToughness("Tarmogoyf", "*", "*"));

        var results = search(powerRangeFilter(0, 20));

        assertThat(results).isEmpty();
    }

    @Test
    void shouldFilterByToughnessRange() {
        Card tough = save(cardWithPowerToughness("Wall of Omens", "0", "4"));
        save(cardWithPowerToughness("Llanowar Elves", "1", "1"));

        var results = search(toughnessRangeFilter(3, 10));

        assertThat(results).extracting(Card::getName).containsExactly(tough.getName());
    }

    @Test
    void shouldFilterByKeyword() {
        Card match = save(cardWithKeywords("Serra Angel", "Flying,Vigilance"));
        save(cardWithKeywords("Grizzly Bears", ""));

        var results = search(keywordFilter("flying"));

        assertThat(results).extracting(Card::getName).containsExactly(match.getName());
    }

    @Test
    void shouldFilterByFormatLegalityDefaultingToLegal() {
        Card legal = save(cardWithLegality("Sol Ring", "commander", "legal"));
        save(cardWithLegality("Braids, Cabal Minion", "commander", "banned"));

        var results = search(formatLegalityFilter("commander", null));

        assertThat(results).extracting(Card::getName).containsExactly(legal.getName());
    }

    @Test
    void shouldFilterByFormatLegalityExplicitStatus() {
        Card banned = save(cardWithLegality("Braids, Cabal Minion", "commander", "banned"));
        save(cardWithLegality("Sol Ring", "commander", "legal"));

        var results = search(formatLegalityFilter("commander", "banned"));

        assertThat(results).extracting(Card::getName).containsExactly(banned.getName());
    }

    @Test
    void shouldFilterByGameChanger() {
        Card changer = save(card("Mana Vault", ""));
        changer.setGameChanger(true);
        cardRepository.save(changer);
        save(card("Grizzly Bears", ""));

        var results = search(gameChangerFilter());

        assertThat(results).extracting(Card::getName).containsExactly(changer.getName());
    }

    @Test
    void shouldFilterByFunctionalCategoryRamp() {
        Card ramp = save(cardWithTypeText("Rampant Growth", "Sorcery", "add {g}."));
        save(cardWithTypeText("Divination", "Sorcery", "draw two cards."));

        var results = search(functionalCategoryFilter(CardFunctionalCategory.RAMP));

        assertThat(results).extracting(Card::getName).containsExactly(ramp.getName());
    }

    @Test
    void shouldPreferLandOverRampWhenBothMarkersPresent() {
        // A land that also says "add {" is still LAND (higher priority), never RAMP.
        Card land = save(cardWithTypeText("Some Land", "Land", "add {g}."));

        assertThat(search(functionalCategoryFilter(CardFunctionalCategory.RAMP))).isEmpty();
        assertThat(search(functionalCategoryFilter(CardFunctionalCategory.LAND)))
                .extracting(Card::getName)
                .containsExactly(land.getName());
    }

    @Test
    void shouldFilterByPrintingRarityCollectorNumberLanguageAndFinish() {
        MagicSet set = magicSetRepository.save(new MagicSet("set-predicates", "prd", "Predicates"));
        Card card = save(card("Thoughtseize", ""));
        CardPrinting printing = new CardPrinting(card, set, "predicates-thoughtseize");
        printing.setRarity("rare");
        printing.setCollectorNumber("99");
        printing.setLanguage("en");
        printing.setFoilAvailable(true);
        printing.setNonfoilAvailable(false);
        cardPrintingRepository.save(printing);
        Card other = save(card("Duress", ""));
        CardPrinting otherPrinting = new CardPrinting(other, set, "predicates-duress");
        otherPrinting.setRarity("common");
        otherPrinting.setCollectorNumber("50");
        otherPrinting.setLanguage("ja");
        otherPrinting.setNonfoilAvailable(true);
        cardPrintingRepository.save(otherPrinting);

        var results =
                search(
                        printingFilter(
                                new CardSearchFilter.PrintingFilter(
                                        "prd", "rare", "99", "en", "foil")));

        assertThat(results).extracting(Card::getName).containsExactly(card.getName());
    }

    private List<Card> search(CardSearchFilter filter) {
        Specification<Card> spec =
                (root, criteriaQuery, builder) ->
                        CardSearchPredicates.cardPredicate(root, filter, criteriaQuery, builder);
        return cardRepository.findAll(spec);
    }

    private static CardSearchFilter withOracleText(String text) {
        return new CardSearchFilter(
                MARKER, null, null, null, text, null, null, null, null, null, null, null, null,
                null);
    }

    private static CardSearchFilter manaValueRangeFilter(BigDecimal min, BigDecimal max) {
        return new CardSearchFilter(
                MARKER,
                null,
                null,
                null,
                null,
                new CardSearchFilter.BigDecimalRange(min, max),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static CardSearchFilter powerRangeFilter(int min, int max) {
        return new CardSearchFilter(
                MARKER,
                null,
                null,
                null,
                null,
                null,
                new CardSearchFilter.IntRange(min, max),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static CardSearchFilter toughnessRangeFilter(int min, int max) {
        return new CardSearchFilter(
                MARKER,
                null,
                null,
                null,
                null,
                null,
                null,
                new CardSearchFilter.IntRange(min, max),
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private static CardSearchFilter keywordFilter(String keyword) {
        return new CardSearchFilter(
                MARKER, null, null, null, null, null, null, null, keyword, null, null, null, null,
                null);
    }

    private static CardSearchFilter formatLegalityFilter(String formatCode, String legalityStatus) {
        return new CardSearchFilter(
                MARKER,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new CardSearchFilter.FormatLegality(formatCode, legalityStatus),
                null,
                null,
                null,
                null);
    }

    private static CardSearchFilter gameChangerFilter() {
        return new CardSearchFilter(
                MARKER, null, null, null, null, null, null, null, null, null, null, null, true,
                null);
    }

    private static CardSearchFilter functionalCategoryFilter(CardFunctionalCategory category) {
        return new CardSearchFilter(
                MARKER, null, null, null, null, null, null, null, null, null, null, category, null,
                null);
    }

    private static CardSearchFilter printingFilter(CardSearchFilter.PrintingFilter printingFilter) {
        return new CardSearchFilter(
                MARKER,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                printingFilter);
    }

    private Card save(Card card) {
        return cardRepository.save(card);
    }

    private static Card card(String name, String oracleText) {
        String markedName = MARKER + name;
        Card card = new Card("predicates-oracle-" + markedName, markedName);
        card.setOracleText(oracleText);
        return card;
    }

    private static Card cardWithManaValue(String name, String manaValue) {
        Card card = card(name, "");
        card.setManaValue(new BigDecimal(manaValue));
        return card;
    }

    private static Card cardWithPowerToughness(String name, String power, String toughness) {
        Card card = card(name, "");
        card.setPower(power);
        card.setToughness(toughness);
        return card;
    }

    private static Card cardWithKeywords(String name, String keywords) {
        Card card = card(name, "");
        card.setKeywords(keywords);
        return card;
    }

    private static Card cardWithLegality(String name, String formatCode, String legalityStatus) {
        Card card = card(name, "");
        card.getLegalities().add(new CardLegality(card, formatCode, legalityStatus));
        return card;
    }

    private static Card cardWithTypeText(String name, String typeLine, String oracleText) {
        Card card = card(name, oracleText);
        card.setTypeLine(typeLine);
        return card;
    }
}
