package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.CategoryTemplateService.TemplateView;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CategoryTemplateResponse(@Nullable Long id, String name, List<String> itemNames) {

    public static CategoryTemplateResponse from(TemplateView view) {
        return new CategoryTemplateResponse(view.id(), view.name(), view.itemNames());
    }
}
