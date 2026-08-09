package com.deckassemble.decks.application.publishing;

import static org.assertj.core.api.Assertions.assertThat;

import com.deckassemble.decks.domain.publishing.DeckVisibility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeckVisibilityPolicyTest {

    private final DeckVisibilityPolicy policy = new DeckVisibilityPolicy();

    @ParameterizedTest
    @EnumSource(value = DeckVisibility.class, names = "PRIVATE")
    void shouldDenySharedViewForPrivateDecks(DeckVisibility visibility) {
        assertThat(policy.isSharedViewAllowed(visibility)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(
            value = DeckVisibility.class,
            names = {"UNLISTED", "PUBLIC"})
    void shouldAllowSharedViewForUnlistedAndPublicDecks(DeckVisibility visibility) {
        assertThat(policy.isSharedViewAllowed(visibility)).isTrue();
    }
}
