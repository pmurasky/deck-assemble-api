package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckTagService.TagView;
import org.jspecify.annotations.Nullable;

public record DeckTagResponse(@Nullable Long id, String name) {

    public static DeckTagResponse from(TagView view) {
        return new DeckTagResponse(view.id(), view.name());
    }
}
