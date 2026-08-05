package com.deckassemble.decks.api;

import com.deckassemble.decks.application.DeckCardAddRequest;
import com.deckassemble.decks.application.DeckCardResponse;
import com.deckassemble.decks.application.DeckCardService;
import com.deckassemble.decks.application.DeckCardUpdateRequest;
import com.deckassemble.decks.application.DeckComboResponse;
import com.deckassemble.decks.application.DeckComboService;
import com.deckassemble.decks.application.DeckCreateRequest;
import com.deckassemble.decks.application.DeckLegalityResponse;
import com.deckassemble.decks.application.DeckOwnershipService;
import com.deckassemble.decks.application.DeckResponse;
import com.deckassemble.decks.application.DeckService;
import com.deckassemble.decks.application.DeckUpdateRequest;
import com.deckassemble.decks.application.DeckWishlistResponse;
import com.deckassemble.decks.application.DeckWishlistService;
import com.deckassemble.decks.application.OwnershipSyncResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
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
    private final DeckCardService deckCardService;
    private final DeckComboService deckComboService;
    private final DeckWishlistService deckWishlistService;
    private final DeckOwnershipService deckOwnershipService;
    private final DeckAnalysisService deckAnalysisService;

    public DeckController(
            DeckService deckService,
            DeckCardService deckCardService,
            DeckComboService deckComboService,
            DeckWishlistService deckWishlistService,
            DeckOwnershipService deckOwnershipService,
            DeckAnalysisService deckAnalysisService) {
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckComboService = deckComboService;
        this.deckWishlistService = deckWishlistService;
        this.deckOwnershipService = deckOwnershipService;
        this.deckAnalysisService = deckAnalysisService;
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

    @GetMapping("/{deckId}/combos")
    public DeckComboResponse combos(@PathVariable long deckId) {
        return deckComboService.getCombos(deckId);
    }

    @GetMapping("/{deckId}/analysis")
    public DeckAnalysisResponse analysis(@PathVariable long deckId) {
        return deckAnalysisService.analyze(deckId);
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

    @PostMapping("/{deckId}/sync-ownership")
    public OwnershipSyncResponse syncOwnership(@PathVariable long deckId) {
        return deckOwnershipService.syncOwnership(deckId);
    }

    @GetMapping("/{deckId}/wishlist")
    public DeckWishlistResponse wishlist(@PathVariable long deckId) {
        return deckWishlistService.getWishlist(deckId);
    }

    @GetMapping("/{deckId}/cards")
    public List<DeckCardResponse> listCards(@PathVariable long deckId) {
        return deckCardService.listCards(deckId);
    }

    @PostMapping("/{deckId}/cards")
    public ResponseEntity<DeckCardResponse> addCard(
            @PathVariable long deckId, @Valid @RequestBody DeckCardAddRequest request) {
        DeckCardResponse added = deckCardService.addCard(deckId, request);
        return ResponseEntity.created(
                        URI.create("/api/v1/decks/" + deckId + "/cards/" + added.id()))
                .body(added);
    }

    @PatchMapping("/{deckId}/cards/{deckCardId}")
    public DeckCardResponse updateCard(
            @PathVariable long deckId,
            @PathVariable long deckCardId,
            @Valid @RequestBody DeckCardUpdateRequest request) {
        return deckCardService.updateCard(deckId, deckCardId, request);
    }

    @PostMapping("/{deckId}/cards/{deckCardId}/acquire")
    public DeckCardResponse acquireCard(@PathVariable long deckId, @PathVariable long deckCardId) {
        return deckOwnershipService.acquireCard(deckId, deckCardId);
    }

    @DeleteMapping("/{deckId}/cards/{deckCardId}")
    public ResponseEntity<Void> removeCard(
            @PathVariable long deckId, @PathVariable long deckCardId) {
        deckCardService.removeCard(deckId, deckCardId);
        return ResponseEntity.noContent().build();
    }
}
