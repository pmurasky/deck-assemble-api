package com.deckassemble.collections.api;

import com.deckassemble.collections.application.CollectionCardAddRequest;
import com.deckassemble.collections.application.CollectionCardResponse;
import com.deckassemble.collections.application.CollectionCardUpdateRequest;
import com.deckassemble.collections.application.CollectionCreateRequest;
import com.deckassemble.collections.application.CollectionResponse;
import com.deckassemble.collections.application.CollectionService;
import com.deckassemble.collections.application.CollectionUpdateRequest;
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
@RequestMapping("/collections")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public List<CollectionResponse> list() {
        return collectionService.list();
    }

    @PostMapping
    public ResponseEntity<CollectionResponse> create(
            @Valid @RequestBody CollectionCreateRequest request) {
        CollectionResponse collection = collectionService.create(request);
        return ResponseEntity.created(URI.create("/collections/" + collection.id()))
                .body(collection);
    }

    @GetMapping("/{collectionId}")
    public CollectionResponse getById(@PathVariable long collectionId) {
        return collectionService.getById(collectionId);
    }

    @PatchMapping("/{collectionId}")
    public CollectionResponse update(
            @PathVariable long collectionId, @Valid @RequestBody CollectionUpdateRequest request) {
        return collectionService.update(collectionId, request);
    }

    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> delete(@PathVariable long collectionId) {
        collectionService.delete(collectionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{collectionId}/cards")
    public List<CollectionCardResponse> listCards(@PathVariable long collectionId) {
        return collectionService.listCards(collectionId);
    }

    @PostMapping("/{collectionId}/cards")
    public ResponseEntity<CollectionCardResponse> addCard(
            @PathVariable long collectionId, @Valid @RequestBody CollectionCardAddRequest request) {
        CollectionCardResponse card = collectionService.addCard(collectionId, request);
        URI location = URI.create("/collections/" + collectionId + "/cards/" + card.id());
        return ResponseEntity.created(location).body(card);
    }

    @PatchMapping("/{collectionId}/cards/{collectionCardId}")
    public CollectionCardResponse updateCard(
            @PathVariable long collectionId,
            @PathVariable long collectionCardId,
            @Valid @RequestBody CollectionCardUpdateRequest request) {
        return collectionService.updateCard(collectionId, collectionCardId, request);
    }

    @DeleteMapping("/{collectionId}/cards/{collectionCardId}")
    public ResponseEntity<Void> removeCard(
            @PathVariable long collectionId, @PathVariable long collectionCardId) {
        collectionService.removeCard(collectionId, collectionCardId);
        return ResponseEntity.noContent().build();
    }
}
