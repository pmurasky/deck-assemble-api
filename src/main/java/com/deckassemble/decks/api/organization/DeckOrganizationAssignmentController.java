package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckFolderService;
import com.deckassemble.decks.application.organization.DeckTagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deck-scoped organization mutations that don't fit under {@code /decks/{deckId}/categories}: a
 * deck's single folder and its set of tags.
 */
@RestController
@RequestMapping("/decks/{deckId}")
public class DeckOrganizationAssignmentController {

    private final DeckFolderService deckFolderService;
    private final DeckTagService deckTagService;

    public DeckOrganizationAssignmentController(
            DeckFolderService deckFolderService, DeckTagService deckTagService) {
        this.deckFolderService = deckFolderService;
        this.deckTagService = deckTagService;
    }

    @PutMapping("/folder")
    public ResponseEntity<Void> assignFolder(
            @PathVariable long deckId, @Valid @RequestBody DeckFolderAssignmentRequest request) {
        deckFolderService.assignToDeck(deckId, request.folderId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/tags")
    public ResponseEntity<Void> assignTags(
            @PathVariable long deckId, @Valid @RequestBody DeckTagAssignmentRequest request) {
        deckTagService.assignToDeck(deckId, request.tagIds());
        return ResponseEntity.noContent().build();
    }
}
