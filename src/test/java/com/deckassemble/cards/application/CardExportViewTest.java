package com.deckassemble.cards.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CardExportViewTest {

    @Test
    void shouldUseCanonicalNameWhenFlavorNameIsBlank() {
        var printing = new CardExportView.PrintingReference("TST", "1", "scryfall-id");
        var view = new CardExportView(1L, "Canonical Name", " ", printing);

        assertThat(view.displayName()).isEqualTo("Canonical Name");
    }
}
