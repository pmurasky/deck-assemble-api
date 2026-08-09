package com.deckassemble.decks.api.collaboration;

import com.deckassemble.decks.application.collaboration.DeckCollaborationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Owner-only management of who else may collaborate on a deck. */
@RestController
@RequestMapping("/decks/{deckId}/collaborators")
public class DeckCollaborationController {

    private final DeckCollaborationService deckCollaborationService;

    public DeckCollaborationController(DeckCollaborationService deckCollaborationService) {
        this.deckCollaborationService = deckCollaborationService;
    }

    @GetMapping
    public List<DeckCollaboratorResponse> list(@PathVariable long deckId) {
        return deckCollaborationService.list(deckId).stream()
                .map(DeckCollaboratorResponse::from)
                .toList();
    }

    @PostMapping
    public DeckCollaboratorResponse invite(
            @PathVariable long deckId, @Valid @RequestBody DeckCollaboratorRequest request) {
        return DeckCollaboratorResponse.from(
                deckCollaborationService.invite(deckId, request.profileId(), request.role()));
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<Void> revoke(@PathVariable long deckId, @PathVariable long profileId) {
        deckCollaborationService.revoke(deckId, profileId);
        return ResponseEntity.noContent().build();
    }
}
