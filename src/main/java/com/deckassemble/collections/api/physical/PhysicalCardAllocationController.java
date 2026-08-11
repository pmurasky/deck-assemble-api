package com.deckassemble.collections.api.physical;

import com.deckassemble.collections.application.physical.PhysicalCardAllocationService;
import com.deckassemble.collections.application.physical.PhysicalCardAllocationService.AllocationCommand;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhysicalCardAllocationController {

    private final PhysicalCardAllocationService allocationService;

    public PhysicalCardAllocationController(PhysicalCardAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/decks/{deckId}/physical-cards")
    public ResponseEntity<PhysicalCardAllocationResponse> allocate(
            @PathVariable long deckId, @Valid @RequestBody PhysicalCardAllocationRequest request) {
        var allocation =
                PhysicalCardAllocationResponse.from(
                        allocationService.allocate(deckId, command(request)));
        return ResponseEntity.created(
                        URI.create("/decks/" + deckId + "/physical-cards/" + allocation.id()))
                .body(allocation);
    }

    @GetMapping("/decks/{deckId}/physical-cards")
    public List<PhysicalCardAllocationResponse> list(@PathVariable long deckId) {
        return allocationService.list(deckId).stream()
                .map(PhysicalCardAllocationResponse::from)
                .toList();
    }

    @GetMapping("/decks/{deckId}/physical-cards/unavailable")
    public List<PhysicalCardAllocationResponse> unavailable(@PathVariable long deckId) {
        return allocationService.unavailable(deckId).stream()
                .map(PhysicalCardAllocationResponse::from)
                .toList();
    }

    @PatchMapping("/decks/{deckId}/physical-cards/{allocationId}")
    public PhysicalCardAllocationResponse update(
            @PathVariable long deckId,
            @PathVariable long allocationId,
            @Valid @RequestBody PhysicalCardAllocationRequest request) {
        return PhysicalCardAllocationResponse.from(
                allocationService.update(deckId, allocationId, command(request)));
    }

    @DeleteMapping("/decks/{deckId}/physical-cards/{allocationId}")
    public ResponseEntity<Void> release(
            @PathVariable long deckId, @PathVariable long allocationId) {
        allocationService.release(deckId, allocationId);
        return ResponseEntity.noContent().build();
    }

    private AllocationCommand command(PhysicalCardAllocationRequest request) {
        return new AllocationCommand(
                request.deckCardId(), request.collectionCardId(), request.quantity());
    }
}
