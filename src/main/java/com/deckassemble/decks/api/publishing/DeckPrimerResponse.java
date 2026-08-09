package com.deckassemble.decks.api.publishing;

import com.deckassemble.decks.domain.Deck;

/**
 * Owner-supplied deck guide, returned as raw Markdown source only. No {@code renderedHtml}/{@code
 * html} field: this codebase has no vetted CommonMark/HTML-sanitizer dependency (see
 * build.gradle.kts), and adding one is out of this task's scope (a separately approved dependency
 * decision). Jackson serializes {@code markdownSource} as a plain JSON string, so it round-trips
 * inertly even if it contains raw HTML/script content — there is no server-side HTML template that
 * would interpolate it unescaped.
 */
public record DeckPrimerResponse(long deckId, String title, String markdownSource) {

    public static DeckPrimerResponse from(Deck deck) {
        return new DeckPrimerResponse(
                deck.getId(), deck.getPrimerTitle(), deck.getPrimerMarkdown());
    }
}
