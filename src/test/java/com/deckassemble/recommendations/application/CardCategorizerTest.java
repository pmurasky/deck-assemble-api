package com.deckassemble.recommendations.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.cards.domain.Card;
import com.deckassemble.cards.domain.CardFace;
import com.deckassemble.recommendations.application.CardCategorizer.Category;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CardCategorizerTest {

    private final CardCategorizer categorizer = new CardCategorizer();

    @Test
    void shouldCategorizeLandByTypeLine() {
        assertThat(categorizer.categorize(card("Land", "{T}: Add {G}."))).isEqualTo(Category.LAND);
    }

    @Test
    void shouldCategorizeRampByManaProduction() {
        assertThat(categorizer.categorize(card("Creature — Elf Druid", "{T}: Add {G}.")))
                .isEqualTo(Category.RAMP);
    }

    @Test
    void shouldCategorizeRampByLandSearch() {
        assertThat(
                        categorizer.categorize(
                                card("Sorcery", "Search your library for a basic land card.")))
                .isEqualTo(Category.RAMP);
    }

    @Test
    void shouldCategorizeDrawByOracleText() {
        assertThat(categorizer.categorize(card("Instant", "Draw two cards.")))
                .isEqualTo(Category.DRAW);
    }

    @Test
    void shouldCategorizeWipeBeforeRemoval() {
        assertThat(categorizer.categorize(card("Sorcery", "Destroy all creatures.")))
                .isEqualTo(Category.WIPE);
    }

    @Test
    void shouldCategorizeRemovalByTargetedDestruction() {
        assertThat(categorizer.categorize(card("Instant", "Exile target creature.")))
                .isEqualTo(Category.REMOVAL);
    }

    @Test
    void shouldCategorizeAnythingElseAsSynergy() {
        assertThat(categorizer.categorize(card("Creature — Goblin", "Haste")))
                .isEqualTo(Category.SYNERGY);
    }

    @Test
    void shouldCategorizeBackFaceWhenFrontFaceHasNoMatch() {
        var card = new Card("oracle-1", "Front // Back");
        card.getFaces().add(face(card, 0, "Enchantment", "Flavorless."));
        card.getFaces().add(face(card, 1, null, "Draw a card."));

        assertThat(categorizer.categorize(card)).isEqualTo(Category.DRAW);
    }

    @Test
    void shouldCategorizeFromRawTextWithoutCardEntity() {
        assertThat(CardCategorizer.categorizeText("instant", "draw two cards."))
                .isEqualTo(Category.DRAW);
        assertThat(CardCategorizer.categorizeText("land", "")).isEqualTo(Category.LAND);
    }

    @Test
    void shouldReturnAllMatchingRolesForMultiPurposeCard() {
        var roles = categorizer.categorizeAll(card("Creature — Elf Druid", "{T}: Add {G}. Draw a card."));

        assertThat(roles).containsExactlyInAnyOrder(Category.RAMP, Category.DRAW);
    }

    @Test
    void shouldUseOracleTagsWhenCategorizingAll() {
        var card = card("Creature — Goblin", "Haste");
        card.setOracleTags("ramp,draw");

        assertThat(categorizer.categorizeAll(card))
                .containsExactlyInAnyOrder(Category.RAMP, Category.DRAW);
        assertThat(categorizer.categorize(card)).isEqualTo(Category.RAMP);
    }

    private static Card card(String typeLine, String oracleText) {
        var card = new Card("oracle-1", "Test Card");
        card.getFaces().add(face(card, 0, typeLine, oracleText));
        return card;
    }

    private static CardFace face(Card card, int order, String typeLine, String oracleText) {
        var face = new CardFace(card, order, "Face " + order);
        face.setTypeLine(typeLine);
        face.setOracleText(oracleText);
        ReflectionTestUtils.setField(face, "id", (long) order + 1);
        return face;
    }
}
