package com.deckassemble.decks.api.history;

import com.deckassemble.decks.application.history.DeckRevisionDiffService;
import com.deckassemble.decks.application.history.DeckRevisionRestoreService;
import com.deckassemble.decks.application.history.DeckRevisionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lists, inspects, diffs, and restores a deck's revision history. */
@RestController
@RequestMapping("/decks/{deckId}/revisions")
public class DeckHistoryController {

    private final DeckRevisionService deckRevisionService;
    private final DeckRevisionDiffService deckRevisionDiffService;
    private final DeckRevisionRestoreService deckRevisionRestoreService;

    public DeckHistoryController(
            DeckRevisionService deckRevisionService,
            DeckRevisionDiffService deckRevisionDiffService,
            DeckRevisionRestoreService deckRevisionRestoreService) {
        this.deckRevisionService = deckRevisionService;
        this.deckRevisionDiffService = deckRevisionDiffService;
        this.deckRevisionRestoreService = deckRevisionRestoreService;
    }

    @GetMapping
    public Page<DeckRevisionResponse> list(
            @PathVariable long deckId, @PageableDefault(size = 20) Pageable pageable) {
        return deckRevisionService.list(deckId, pageable).map(DeckRevisionResponse::from);
    }

    @GetMapping("/{revisionNumber}")
    public DeckRevisionResponse get(@PathVariable long deckId, @PathVariable int revisionNumber) {
        return DeckRevisionResponse.from(deckRevisionService.get(deckId, revisionNumber));
    }

    @GetMapping("/{revisionNumber}/diff/{otherRevisionNumber}")
    public DeckRevisionDiffResponse diff(
            @PathVariable long deckId,
            @PathVariable int revisionNumber,
            @PathVariable int otherRevisionNumber) {
        return DeckRevisionDiffResponse.from(
                deckRevisionDiffService.diff(deckId, revisionNumber, otherRevisionNumber));
    }

    @PostMapping("/{revisionNumber}/restore")
    public DeckRevisionResponse restore(
            @PathVariable long deckId,
            @PathVariable int revisionNumber,
            @Valid @RequestBody RestoreDeckRevisionRequest request) {
        return DeckRevisionResponse.from(
                deckRevisionRestoreService.restore(
                        deckId, revisionNumber, request.expectedCurrentRevision()));
    }
}
