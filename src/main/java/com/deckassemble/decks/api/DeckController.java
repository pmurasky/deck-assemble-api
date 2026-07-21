package com.deckassemble.decks.api;

import com.deckassemble.decks.application.DeckService;
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

@RestController
@RequestMapping("/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public List<DeckResponse> list() {
        return deckService.list();
    }

    @PostMapping
    public ResponseEntity<DeckResponse> create(@Valid @RequestBody DeckCreateRequest request) {
        DeckResponse created = deckService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + created.id())).body(created);
    }

    @GetMapping("/{deckId}")
    public DeckResponse getById(@PathVariable long deckId) {
        return deckService.getById(deckId);
    }

    @GetMapping("/{deckId}/legality")
    public DeckLegalityResponse legality(@PathVariable long deckId) {
        return deckService.legality(deckId);
    }

    @PatchMapping("/{deckId}")
    public DeckResponse update(
            @PathVariable long deckId, @Valid @RequestBody DeckUpdateRequest request) {
        return deckService.update(deckId, request);
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> delete(@PathVariable long deckId) {
        deckService.delete(deckId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deckId}/duplicate")
    public ResponseEntity<DeckResponse> duplicate(@PathVariable long deckId) {
        DeckResponse copy = deckService.duplicate(deckId);
        return ResponseEntity.created(URI.create("/api/v1/decks/" + copy.id())).body(copy);
    }

    @PostMapping("/{deckId}/archive")
    public DeckResponse archive(@PathVariable long deckId) {
        return deckService.archive(deckId);
    }

    @GetMapping("/{deckId}/cards")
    public List<DeckCardResponse> listCards(@PathVariable long deckId) {
        return deckService.listCards(deckId);
    }

    @PostMapping("/{deckId}/cards")
    public ResponseEntity<DeckCardResponse> addCard(
            @PathVariable long deckId, @Valid @RequestBody DeckCardAddRequest request) {
        DeckCardResponse added = deckService.addCard(deckId, request);
        return ResponseEntity.created(
                        URI.create("/api/v1/decks/" + deckId + "/cards/" + added.id()))
                .body(added);
    }

    @PatchMapping("/{deckId}/cards/{deckCardId}")
    public DeckCardResponse updateCard(
            @PathVariable long deckId,
            @PathVariable long deckCardId,
            @Valid @RequestBody DeckCardUpdateRequest request) {
        return deckService.updateCard(deckId, deckCardId, request);
    }

    @DeleteMapping("/{deckId}/cards/{deckCardId}")
    public ResponseEntity<Void> removeCard(
            @PathVariable long deckId, @PathVariable long deckCardId) {
        deckService.removeCard(deckId, deckCardId);
        return ResponseEntity.noContent().build();
    }
}
