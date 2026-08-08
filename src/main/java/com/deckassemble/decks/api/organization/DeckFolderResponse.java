package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckFolderService.FolderView;
import org.jspecify.annotations.Nullable;

public record DeckFolderResponse(@Nullable Long id, String name) {

    public static DeckFolderResponse from(FolderView view) {
        return new DeckFolderResponse(view.id(), view.name());
    }
}
