package com.deckassemble.decks.api;

import com.deckassemble.decks.api.alternatives.DeckCardAlternativeResponse;
import com.deckassemble.decks.api.comparison.DeckComparisonResponse;
import com.deckassemble.decks.api.organization.DeckFolderAssignmentRequest;
import com.deckassemble.decks.api.organization.DeckTagAssignmentRequest;
import com.deckassemble.decks.api.upgrades.DeckUpgradePlanResponse;
import com.deckassemble.decks.api.upgrades.DeckUpgradeRequest;
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
import com.deckassemble.decks.application.alternatives.DeckCardAlternativeService;
import com.deckassemble.decks.application.analysis.DeckAnalysisResponse;
import com.deckassemble.decks.application.analysis.DeckAnalysisService;
import com.deckassemble.decks.application.comparison.DeckComparisonService;
import com.deckassemble.decks.application.organization.DeckFolderService;
import com.deckassemble.decks.application.organization.DeckTagService;
import com.deckassemble.decks.application.upgrades.DeckUpgradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Suppressed: cohesive deck aggregate controller (same rationale as the constructor's
// parameter-count suppression below) — every method is a thin pass-through to one collaborator
// serving a /decks/{deckId}/... subresource; splitting by subresource would scatter that
// aggregate's routes across many single-purpose controllers for no behavioral benefit.
@SuppressWarnings("PMD.TooManyMethods")
@RestController
@RequestMapping("/decks")
public class DeckController {

    private final DeckService deckService;
    private final DeckCardService deckCardService;
    private final DeckComboService deckComboService;
    private final DeckWishlistService deckWishlistService;
    private final DeckOwnershipService deckOwnershipService;
    private final DeckAnalysisService deckAnalysisService;
    private final DeckCardAlternativeService deckCardAlternativeService;
    private final DeckComparisonService deckComparisonService;
    private final DeckUpgradeService deckUpgradeService;
    private final DeckFolderService deckFolderService;
    private final DeckTagService deckTagService;

    // Suppressed: cohesive deck aggregate controller; each collaborator serves deck subresources
    // (cards, combos, wishlist, ownership, analysis, alternatives, comparison, upgrades, folder,
    // tags) under /decks/{deckId}.
    @SuppressWarnings({"checkstyle:ParameterNumber", "PMD.ExcessiveParameterList"})
    public DeckController(
            DeckService deckService,
            DeckCardService deckCardService,
            DeckComboService deckComboService,
            DeckWishlistService deckWishlistService,
            DeckOwnershipService deckOwnershipService,
            DeckAnalysisService deckAnalysisService,
            DeckCardAlternativeService deckCardAlternativeService,
            DeckComparisonService deckComparisonService,
            DeckUpgradeService deckUpgradeService,
            DeckFolderService deckFolderService,
            DeckTagService deckTagService) {
        this.deckService = deckService;
        this.deckCardService = deckCardService;
        this.deckComboService = deckComboService;
        this.deckWishlistService = deckWishlistService;
        this.deckOwnershipService = deckOwnershipService;
        this.deckAnalysisService = deckAnalysisService;
        this.deckCardAlternativeService = deckCardAlternativeService;
        this.deckComparisonService = deckComparisonService;
        this.deckUpgradeService = deckUpgradeService;
        this.deckFolderService = deckFolderService;
        this.deckTagService = deckTagService;
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

    @GetMapping("/{deckId}/comparison/{otherDeckId}")
    public DeckComparisonResponse comparison(
            @PathVariable long deckId, @PathVariable long otherDeckId) {
        return DeckComparisonResponse.from(deckComparisonService.compare(deckId, otherDeckId));
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

    @GetMapping("/{deckId}/cards/{deckCardId}/alternatives")
    public List<DeckCardAlternativeResponse> alternatives(
            @PathVariable long deckId,
            @PathVariable long deckCardId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit,
            @RequestParam(defaultValue = "true") boolean ownedFirst) {
        return deckCardAlternativeService.suggest(deckId, deckCardId, limit, ownedFirst).stream()
                .map(DeckCardAlternativeResponse::from)
                .toList();
    }

    @PostMapping("/{deckId}/upgrade-plans")
    public DeckUpgradePlanResponse upgradePlans(
            @PathVariable long deckId, @Valid @RequestBody DeckUpgradeRequest request) {
        return DeckUpgradePlanResponse.from(
                deckUpgradeService.plan(
                        deckId,
                        DeckUpgradeService.Objective.valueOf(request.objective().name()),
                        request.budget(),
                        request.currency(),
                        request.maxChanges()));
    }

    @DeleteMapping("/{deckId}/cards/{deckCardId}")
    public ResponseEntity<Void> removeCard(
            @PathVariable long deckId, @PathVariable long deckCardId) {
        deckCardService.removeCard(deckId, deckCardId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{deckId}/folder")
    public ResponseEntity<Void> assignFolder(
            @PathVariable long deckId, @Valid @RequestBody DeckFolderAssignmentRequest request) {
        deckFolderService.assignToDeck(deckId, request.folderId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{deckId}/tags")
    public ResponseEntity<Void> assignTags(
            @PathVariable long deckId, @Valid @RequestBody DeckTagAssignmentRequest request) {
        deckTagService.assignToDeck(deckId, request.tagIds());
        return ResponseEntity.noContent().build();
    }
}
