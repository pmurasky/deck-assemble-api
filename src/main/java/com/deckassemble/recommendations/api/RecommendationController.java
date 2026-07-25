package com.deckassemble.recommendations.api;

import com.deckassemble.recommendations.application.DeckBuildRequest;
import com.deckassemble.recommendations.application.DeckBuildResult;
import com.deckassemble.recommendations.application.DeckBuilderService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
public class RecommendationController {

    private final DeckBuilderService deckBuilderService;

    public RecommendationController(DeckBuilderService deckBuilderService) {
        this.deckBuilderService = deckBuilderService;
    }

    @PostMapping("/builds")
    public ResponseEntity<DeckBuildResult> build(@Valid @RequestBody DeckBuildRequest request) {
        DeckBuildResult result = deckBuilderService.build(request);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + result.deck().id()))
                .body(result);
    }
}
