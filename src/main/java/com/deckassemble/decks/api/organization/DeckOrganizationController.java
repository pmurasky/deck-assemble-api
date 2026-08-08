package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckCategoryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for deck organization categories and bulk category-to-card assignment. */
@RestController
@RequestMapping("/decks/{deckId}/categories")
public class DeckOrganizationController {

    private final DeckCategoryService deckCategoryService;

    public DeckOrganizationController(DeckCategoryService deckCategoryService) {
        this.deckCategoryService = deckCategoryService;
    }

    @GetMapping
    public List<DeckCategoryResponse> list(@PathVariable long deckId) {
        return deckCategoryService.list(deckId).stream().map(DeckCategoryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DeckCategoryResponse> create(
            @PathVariable long deckId, @Valid @RequestBody DeckCategoryRequest request) {
        DeckCategoryResponse created =
                DeckCategoryResponse.from(deckCategoryService.create(deckId, request.name()));
        return ResponseEntity.created(
                        URI.create("/api/v1/decks/" + deckId + "/categories/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{categoryId}")
    public DeckCategoryResponse rename(
            @PathVariable long deckId,
            @PathVariable long categoryId,
            @Valid @RequestBody DeckCategoryRequest request) {
        return DeckCategoryResponse.from(
                deckCategoryService.rename(deckId, categoryId, request.name()));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable long deckId, @PathVariable long categoryId) {
        deckCategoryService.delete(deckId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{categoryId}/cards")
    public DeckCategoryResponse assignCards(
            @PathVariable long deckId,
            @PathVariable long categoryId,
            @Valid @RequestBody DeckCategoryAssignmentRequest request) {
        return DeckCategoryResponse.from(
                deckCategoryService.assignCards(deckId, categoryId, request.deckCardIds()));
    }
}
