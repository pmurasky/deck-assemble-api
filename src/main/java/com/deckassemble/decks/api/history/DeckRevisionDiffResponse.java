package com.deckassemble.decks.api.history;

import com.deckassemble.decks.application.history.DeckRevisionDiffService;
import java.util.List;

/** API view of the diff between two deck revisions. */
public record DeckRevisionDiffResponse(
        List<DeckRevisionDiffService.FieldChange> metadataChanges,
        List<DeckRevisionDiffService.CardChange> cardsAdded,
        List<DeckRevisionDiffService.CardChange> cardsRemoved,
        List<DeckRevisionDiffService.CardChange> cardsChanged,
        List<String> categoriesAdded,
        List<String> categoriesRemoved,
        List<String> tagsAdded,
        List<String> tagsRemoved) {

    public static DeckRevisionDiffResponse from(DeckRevisionDiffService.Diff diff) {
        return new DeckRevisionDiffResponse(
                diff.metadataChanges(),
                diff.cards().added(),
                diff.cards().removed(),
                diff.cards().changed(),
                diff.categories().added(),
                diff.categories().removed(),
                diff.tags().added(),
                diff.tags().removed());
    }
}
