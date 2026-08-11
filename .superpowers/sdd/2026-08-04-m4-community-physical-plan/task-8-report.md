# Task 8 Report: Allocate Physical Cards to Decks

## Status

DONE_WITH_CONCERNS — adopted the stranded implementation, found one ownership-sync gap, fixed it, verified the generated Gradle reports are clean after the single full verification command completed.

## Adopted Work Review

Solid pieces retained:

- `PhysicalCardAllocation` persists deck-to-collection-card reservations with deck/deck-card/collection-card foreign keys and a unique `(deck_card_id, collection_card_id)` constraint.
- `PhysicalCardAllocationService` owns allocation, update, release, deck release, list, unavailable/missing, and availability calculations.
- `PhysicalCardAllocationController` exposes allocate/list/unavailable/update/release under `/decks/{deckId}/physical-cards`.
- `CollectionCardRepository` has exact-printing-first compatible-card lookup and the locked variant needed for mutation paths.
- `DeckService.delete()` releases allocations before deleting the deck.
- `OwnershipSyncResponse` is additive: existing `changedCount` and `changes` remain first, and the existing two-argument constructor still returns empty physical-availability defaults.

## Fix Applied

The stranded implementation changed ownership sync to use physical availability, but still required exact printing ownership before marking a deck card `OWNED`. That contradicted Task 8's alternate-printing rule: an available alternate physical printing should satisfy the deck card.

Changed `DeckOwnershipService` so sync status is driven by physical availability (`availableQuantity >= deck quantity`) rather than exact printing id membership. Added `DeckOwnershipServiceTest.shouldMarkWishlistOwnedWhenAlternatePhysicalPrintingIsAvailable()` to lock the behavior.

## Locking and Availability Invariant

Invariant: `available = owned - allocated`, never negative.

- Mutation entrypoints are transactional because `PhysicalCardAllocationService` is class-level `@Transactional`.
- Allocation locks the deck and then the compatible collection-card rows before checking availability: `PhysicalCardAllocationService.java:41-51`.
- Update/release/deck-release also lock the deck; update locks the affected compatible collection-card row before recalculating availability: `PhysicalCardAllocationService.java:63-82` and `PhysicalCardAllocationService.java:218-226`.
- The row lock is real JPA pessimistic locking: `CollectionCardRepository.java:40-52` uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on the compatible-card query.
- Availability checks subtract existing allocations before insert/update: `PhysicalCardAllocationService.java:168-180`.
- Deck-card capacity is also enforced before insert/update: `PhysicalCardAllocationService.java:184-191`.

The concurrent-allocation test uses two executor threads, a start latch, and two different deck cards competing for the same one owned physical copy. It asserts exactly one allocation and one conflict: `PhysicalCardAllocationServiceTest.java:99-135`. Because both calls go through the transactional service and locked repository query, this proves the known check-then-insert race is serialized at the collection-card row.

## Printing Rule Implemented

Compatible cards are all owned printings with the same oracle id as the requested deck printing. Both read and locked queries order exact printing first, then alternates by collection-card id: `CollectionCardRepository.java:27-52`.

Allocation picks the first compatible row with enough unallocated quantity, so exact printing wins when available, and alternate printing is allowed when exact printing is unavailable: `PhysicalCardAllocationService.java:46-51` and `PhysicalCardAllocationService.java:168-172`.

## Brief Checklist Mapping

- Exact-printing-first allocation: `PhysicalCardAllocationServiceTest.shouldAllocateExactPrintingBeforeAlternatePrinting()` (`PhysicalCardAllocationServiceTest.java:58-67`).
- Allowed alternate printing: `PhysicalCardAllocationServiceTest.shouldAllocateAlternatePrintingWhenExactPrintingIsUnavailable()` (`PhysicalCardAllocationServiceTest.java:69-78`).
- Over-allocation rejection: `PhysicalCardAllocationServiceTest.shouldRejectOverAllocationAndRollback()` (`PhysicalCardAllocationServiceTest.java:80-96`).
- Transaction rollback: same test asserts no allocation row remains after conflict (`PhysicalCardAllocationServiceTest.java:95`).
- Concurrent allocation: `PhysicalCardAllocationServiceTest.shouldPreventConcurrentAllocationBeyondAvailability()` (`PhysicalCardAllocationServiceTest.java:98-136`).
- Deck disassembly releases allocations: `PhysicalCardAllocationServiceTest.shouldReleaseAllocationsWhenDeckIsDeleted()` (`PhysicalCardAllocationServiceTest.java:138-147`) plus hook at `DeckService.java:169-171`.
- Ownership status sync: `DeckOwnershipServiceTest.shouldMarkWishlistOwnedWhenAlternatePhysicalPrintingIsAvailable()` (`DeckOwnershipServiceTest.java:97-111`) and unavailable regression (`DeckOwnershipServiceTest.java:113-127`).

## Verification

- LSP diagnostics: attempted for changed Java files, but no language server is configured in this environment (`No language server found`).
- Full verification command launched once: `./gradlew test check`.
- The MCP call timed out while the Gradle process continued; I waited for the wrapper process to exit, then inspected generated reports.
- Test reports: 109 XML files, 832 tests, 0 failures, 0 errors, 0 skipped.
- Static-analysis reports: PMD main/test no violations, Checkstyle main no violations, SpotBugs main no bug instances.

## Files Changed

- `src/main/java/com/deckassemble/collections/domain/physical/PhysicalCardAllocation.java`
- `src/main/java/com/deckassemble/collections/domain/physical/PhysicalCardAllocationRepository.java`
- `src/main/java/com/deckassemble/collections/application/physical/PhysicalCardAllocationService.java`
- `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationController.java`
- `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationRequest.java`
- `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationResponse.java`
- `src/main/java/com/deckassemble/collections/application/physical/PhysicalDeckLookup.java`
- `src/main/java/com/deckassemble/decks/application/PhysicalDeckLookupAdapter.java`
- `src/main/java/com/deckassemble/collections/domain/CollectionCardRepository.java`
- `src/main/java/com/deckassemble/decks/application/DeckOwnershipService.java`
- `src/main/java/com/deckassemble/decks/application/DeckService.java`
- `src/main/java/com/deckassemble/decks/application/OwnershipSyncResponse.java`
- `src/main/resources/db/changelog/db.changelog-master.yaml`
- `src/main/resources/db/changelog/releases/020-physical-card-allocations.yaml`
- `src/test/java/com/deckassemble/collections/application/physical/PhysicalCardAllocationServiceTest.java`
- `src/test/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckOwnershipServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java`

## Concerns

- I could not capture the final Gradle exit code because the context-mode call timed out before the long-running Gradle process finished; report artifacts after completion were clean.
- The API allocates one collection-card row per request. If a caller wants to split a multi-copy deck card across exact and alternate printings, it should send multiple allocation requests with explicit smaller quantities.