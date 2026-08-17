---
name: gortex-decks-application-13-dirs
description: "Work in the decks/application +13 dirs area — 395 symbols across 29 files (79% cohesion)"
---

# decks/application +13 dirs

395 symbols | 29 files | 79% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/api/publishing/DeckPrimerResponse.java`
- `src/main/java/com/deckassemble/decks/api/publishing/DeckPublishingController.java`
- `src/main/java/com/deckassemble/decks/api/publishing/SharedDeckResponse.java`
- `src/main/java/com/deckassemble/decks/application/DeckNotFoundException.java`
- `src/main/java/com/deckassemble/decks/application/DeckResponse.java`
- `src/main/java/com/deckassemble/decks/application/DeckService.java`
- `src/main/java/com/deckassemble/decks/application/DeckStateReplacer.java`
- `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckRevisionService.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java`
- `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java`
- `src/main/java/com/deckassemble/decks/application/publishing/DeckPublishingService.java`
- `src/main/java/com/deckassemble/decks/domain/Deck.java`
- `src/main/java/com/deckassemble/decks/domain/DeckCardRepository.java`
- `src/main/java/com/deckassemble/decks/domain/DeckRepository.java`
- `src/main/java/com/deckassemble/decks/domain/history/DeckChangeType.java`
- `src/main/java/com/deckassemble/decks/domain/history/DeckRevision.java`
- `src/main/java/com/deckassemble/decks/domain/history/DeckRevisionRepository.java`
- `src/test/java/com/deckassemble/decks/api/DeckFolderControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/history/DeckHistoryControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckRevisionServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java`
- `src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/publishing/DeckPrimerServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/publishing/DeckPublishingServiceTest.java`
- `src/test/java/com/deckassemble/decks/domain/history/DeckRevisionRepositoryIntegrationTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | archive, deckId, getById, deckId, list, ... |
| `src/main/java/com/deckassemble/decks/api/publishing/DeckPrimerResponse.java` | from, deck |
| `src/main/java/com/deckassemble/decks/api/publishing/DeckPublishingController.java` | slug, fork |
| `src/main/java/com/deckassemble/decks/api/publishing/SharedDeckResponse.java` | from, pinnedSnapshot, deck |
| `src/main/java/com/deckassemble/decks/application/DeckNotFoundException.java` | DeckNotFoundException |
| `src/main/java/com/deckassemble/decks/application/DeckResponse.java` | deck, commander, cardCount, from, commanderName |
| `src/main/java/com/deckassemble/decks/application/DeckService.java` | deck, deckAccessGuard, deckId, latestPrintingId, deckAccessGuard, ... |
| `src/main/java/com/deckassemble/decks/application/DeckStateReplacer.java` | DeckStateReplacer, DeckStateReplacer.<init>, deckRevisionService, deckRepository, deckRevisionService, ... |
| `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java` | commanders, deck |
| `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java` | expected, restore, expectedCurrentRevision, deckId, revisionNumber, ... |
| `src/main/java/com/deckassemble/decks/application/history/DeckRevisionService.java` | revision, deckRepository, action, changeType, revisionNumber, ... |
| `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java` | json, build, toJson, deckCategoryRepository, deckTagAssignmentRepository, ... |
| `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java` | deckCategoryService, deckRepository, deckPublishingService, deckService, deckCardService, ... |
| `src/main/java/com/deckassemble/decks/application/publishing/DeckPublishingService.java` | publish, getShared, deckId, pinnedSnapshotOrNull, deck, ... |
| `src/main/java/com/deckassemble/decks/domain/Deck.java` | id, sourceDeckId, playStyle, primerTitle, profileId, ... |
| `src/main/java/com/deckassemble/decks/domain/DeckCardRepository.java` | deckId, findByDeckId |
| `src/main/java/com/deckassemble/decks/domain/DeckRepository.java` | findByShareSlug, DeckRepository, id, shareSlug, findByProfileIdOrderByNameAsc, ... |
| `src/main/java/com/deckassemble/decks/domain/history/DeckChangeType.java` | CATEGORY_CHANGED, TAG_CHANGED, FOLDER_CHANGED, IMPORTED, FORKED, ... |
| `src/main/java/com/deckassemble/decks/domain/history/DeckRevision.java` | baseRevisionNumber, getProfileId, getBaseRevisionNumber, getCreatedAt, getRevisionNumber, ... |
| `src/main/java/com/deckassemble/decks/domain/history/DeckRevisionRepository.java` | DeckRevisionRepository, findByDeckIdAndRevisionNumber, deckId, pageable, revisionNumber, ... |
| `src/test/java/com/deckassemble/decks/api/DeckFolderControllerIntegrationTest.java` | shouldRecordFolderChangedRevisionReflectingClearedFolderIdAfterFolderDeletion |
| `src/test/java/com/deckassemble/decks/api/history/DeckHistoryControllerIntegrationTest.java` | shouldClearNullableMetadataFieldsOnRestore |
| `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java` | shouldNotRecordRevisionWhenArchivingAlreadyArchivedDeck, shouldDelegateLegalityToEvaluator, deckRevisionService, deck, id, ... |
| `src/test/java/com/deckassemble/decks/application/history/DeckRevisionServiceTest.java` | existingRevision, deckRepository, deckAccessGuard, shouldThrowWhenDeckNotFoundUnderLock, shouldResumeRecordingAfterWithoutRecordingReturns, ... |
| `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java` | shouldRoundTripASnapshotThroughJson |
| `src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java` | categoryName, id, minimalSnapshotWithTag, deckRevisionService, service, ... |
| `src/test/java/com/deckassemble/decks/application/publishing/DeckPrimerServiceTest.java` | shouldStoreTitleAndMarkdownSourceOnTheOwnedDeck |
| `src/test/java/com/deckassemble/decks/application/publishing/DeckPublishingServiceTest.java` | shouldServeThePinnedSnapshotContentWhenTheDeckHasBeenPublished, shouldRejectPublishingADeckWithNoRecordedRevisions, shouldPinTheCurrentRevisionNumberAndATimestampWhenPublishing, shouldFallBackToLiveDeckStateWhenNeverPublished |
| `src/test/java/com/deckassemble/decks/domain/history/DeckRevisionRepositoryIntegrationTest.java` | profileRepository, shouldRejectDuplicateRevisionNumberForSameDeck, revisionRepository, shouldLoadPersistedSnapshotUnchangedAfterReload, saveDeck, ... |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java::DeckForkServiceTest.shouldForkPinnedSnapshotContentIntoANewDeckAndRecordExactlyOneForkedRevision`
- `src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java::DeckForkServiceTest.shouldNotRecreateACategoryTheSnapshotAlreadyHasByName`
- `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java::DeckServiceTest.shouldDuplicateDeckWithCards`

## Connected Communities

- **cards/domain +16 dirs** (30 cross-edges)
- **decks/application +18 dirs** (19 cross-edges)
- **api/organization +19 dirs** (9 cross-edges)
- **com/deckassemble · name** (7 cross-edges)
- **api/history +5 dirs** (5 cross-edges)
- **application/publishing +3 dirs** (5 cross-edges)
- **recommendations/application +7 dirs** (5 cross-edges)
- **domain/organization +6 dirs** (4 cross-edges)
- **decks/domain +1 dirs · createFolder** (4 cross-edges)
- **cards/application +10 dirs** (3 cross-edges)
- **shared/security +6 dirs** (3 cross-edges)
- **com/deckassemble · add · CardSearchPredicates** (3 cross-edges)
- **decks/domain +6 dirs** (1 cross-edges)
- **deckassemble/decks · attributeSource** (1 cross-edges)
- **application/publishing +2 dirs** (1 cross-edges)
- **api/history +1 dirs** (1 cross-edges)
- **application/importing +3 dirs** (1 cross-edges)
- **application/publishing +1 dirs · shouldAllowSharedViewForUnliste…** (1 cross-edges)
- **api/publishing** (1 cross-edges)
- **application/history +1 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-66"
smart_context with task: "understand decks/application +13 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java::DeckForkServiceTest.shouldForkPinnedSnapshotContentIntoANewDeckAndRecordExactlyOneForkedRevision", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
