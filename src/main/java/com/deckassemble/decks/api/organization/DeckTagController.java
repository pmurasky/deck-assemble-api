package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckTagService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for a profile's reusable deck tags. */
@RestController
@RequestMapping("/deck-tags")
public class DeckTagController {

    private final DeckTagService deckTagService;

    public DeckTagController(DeckTagService deckTagService) {
        this.deckTagService = deckTagService;
    }

    @GetMapping
    public List<DeckTagResponse> list() {
        return deckTagService.list().stream().map(DeckTagResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DeckTagResponse> create(@Valid @RequestBody DeckTagRequest request) {
        DeckTagResponse created = DeckTagResponse.from(deckTagService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/v1/deck-tags/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{tagId}")
    public DeckTagResponse rename(
            @PathVariable long tagId, @Valid @RequestBody DeckTagRequest request) {
        return DeckTagResponse.from(deckTagService.rename(tagId, request.name()));
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> delete(@PathVariable long tagId) {
        deckTagService.delete(tagId);
        return ResponseEntity.noContent().build();
    }
}
