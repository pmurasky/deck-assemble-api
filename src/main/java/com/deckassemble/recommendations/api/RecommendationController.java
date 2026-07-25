package com.deckassemble.recommendations.api;

import com.deckassemble.recommendations.application.CommanderSuggestion;
import com.deckassemble.recommendations.application.CommanderSuggestionService;
import com.deckassemble.recommendations.application.DeckBuildRequest;
import com.deckassemble.recommendations.application.DeckBuildResult;
import com.deckassemble.recommendations.application.DeckBuilderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final DeckBuilderService deckBuilderService;
    private final CommanderSuggestionService commanderSuggestionService;

    public RecommendationController(
            DeckBuilderService deckBuilderService,
            CommanderSuggestionService commanderSuggestionService) {
        this.deckBuilderService = deckBuilderService;
        this.commanderSuggestionService = commanderSuggestionService;
    }

    @PostMapping("/builds")
    public ResponseEntity<DeckBuildResult> build(@Valid @RequestBody DeckBuildRequest request) {
        DeckBuildResult result = deckBuilderService.build(request);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + result.deck().id()))
                .body(result);
    }

    @GetMapping("/commanders")
    public List<CommanderSuggestion> commanders() {
        return commanderSuggestionService.suggest();
    }
}
