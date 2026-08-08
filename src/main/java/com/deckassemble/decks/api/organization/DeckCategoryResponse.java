package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckCategoryService.CategoryView;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record DeckCategoryResponse(
        @Nullable Long id,
        String name,
        int displayOrder,
        boolean systemOwned,
        @Nullable String functionalCategory,
        List<Long> assignedDeckCardIds) {

    public static DeckCategoryResponse from(CategoryView view) {
        return new DeckCategoryResponse(
                view.id(),
                view.name(),
                view.displayOrder(),
                view.systemOwned(),
                view.functionalCategory() == null ? null : view.functionalCategory().name(),
                view.assignedDeckCardIds());
    }
}
