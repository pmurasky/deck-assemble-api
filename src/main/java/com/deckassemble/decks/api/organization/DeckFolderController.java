package com.deckassemble.decks.api.organization;

import com.deckassemble.decks.application.organization.DeckFolderService;
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

/** CRUD for a profile's deck folders. */
@RestController
@RequestMapping("/deck-folders")
public class DeckFolderController {

    private final DeckFolderService deckFolderService;

    public DeckFolderController(DeckFolderService deckFolderService) {
        this.deckFolderService = deckFolderService;
    }

    @GetMapping
    public List<DeckFolderResponse> list() {
        return deckFolderService.list().stream().map(DeckFolderResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<DeckFolderResponse> create(
            @Valid @RequestBody DeckFolderRequest request) {
        DeckFolderResponse created =
                DeckFolderResponse.from(deckFolderService.create(request.name()));
        return ResponseEntity.created(URI.create("/api/v1/deck-folders/" + created.id()))
                .body(created);
    }

    @PatchMapping("/{folderId}")
    public DeckFolderResponse rename(
            @PathVariable long folderId, @Valid @RequestBody DeckFolderRequest request) {
        return DeckFolderResponse.from(deckFolderService.rename(folderId, request.name()));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> delete(@PathVariable long folderId) {
        deckFolderService.delete(folderId);
        return ResponseEntity.noContent().build();
    }
}
