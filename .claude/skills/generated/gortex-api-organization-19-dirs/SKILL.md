---
name: gortex-api-organization-19-dirs
description: "Work in the api/organization +19 dirs area — 149 symbols across 27 files (54% cohesion)"
---

# api/organization +19 dirs

149 symbols | 27 files | 54% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/administration/api/CardImportController.java`
- `src/main/java/com/deckassemble/cards/api/SetPrintingController.java`
- `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java`
- `src/main/java/com/deckassemble/cards/domain/OracleTagIndex.java`
- `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java`
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/api/organization/CategoryTemplateController.java`
- `src/main/java/com/deckassemble/decks/api/organization/DeckFolderController.java`
- `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationAssignmentController.java`
- `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java`
- `src/main/java/com/deckassemble/decks/api/organization/DeckTagController.java`
- `src/main/java/com/deckassemble/decks/application/DeckWishlistService.java`
- `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java`
- `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonDiffer.java`
- `src/main/java/com/deckassemble/decks/application/exporting/DeckExporter.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportCommitService.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignment.java`
- `src/main/java/com/deckassemble/recommendations/application/BasicLandAllocation.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java`
- `src/test/java/com/deckassemble/cards/domain/OracleTagIndexTest.java`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/administration/api/CardImportController.java` | history |
| `src/main/java/com/deckassemble/cards/api/SetPrintingController.java` | GetMapping |
| `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java` | findByName, name |
| `src/main/java/com/deckassemble/cards/domain/OracleTagIndex.java` | MAPPER, jsonl, OracleTagIndex.<init>, parse, OracleTagIndex |
| `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java` | rows, excludedCount, rejectUnresolved, excluded, rows, ... |
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | limit, ownedFirst, wishlist, alternatives, deckCardId, ... |
| `src/main/java/com/deckassemble/decks/api/organization/CategoryTemplateController.java` | list |
| `src/main/java/com/deckassemble/decks/api/organization/DeckFolderController.java` | list |
| `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationAssignmentController.java` | assignTags, deckId, request, PutMapping |
| `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java` | list, deckId |
| `src/main/java/com/deckassemble/decks/api/organization/DeckTagController.java` | list |
| `src/main/java/com/deckassemble/decks/application/DeckWishlistService.java` | items, getWishlist, wishlistTotal, deckId |
| `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java` | targetCombos, deckId, breaksAny, names, deckId, ... |
| `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonDiffer.java` | changed, base, other |
| `src/main/java/com/deckassemble/decks/application/exporting/DeckExporter.java` | cards, sorted |
| `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java` | deckId, DeckRevisionRestoreService, deckId, of, card, ... |
| `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java` | tagNames, deckId |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportCommitService.java` | excluded, excluded, excludedCount, rejectUnresolved, rows, ... |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java` | MAX_ROWS, accessGuard, resolver, DeckImportService, PREVIEW_TTL, ... |
| `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java` | deckRevisionService, deckTagRepository, list, deckId, deckId, ... |
| `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java` | snapshot, applyCategories, deckRepository, applyTags, applyCards, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignment.java` | getTagId |
| `src/main/java/com/deckassemble/recommendations/application/BasicLandAllocation.java` | weights, basicsNeeded, largestRemainder |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java` | pricesFor, missingLists |
| `src/test/java/com/deckassemble/cards/domain/OracleTagIndexTest.java` | OracleTagIndexTest, content, stream, shouldParseAnEmptyStream, shouldSkipLinesWithoutTaggings, ... |
| `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java` | parsedRows, references |
| `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java` | shouldRejectAssigningToForeignDeck |

## Connected Communities

- **decks/application +18 dirs** (11 cross-edges)
- **decks/application +13 dirs** (6 cross-edges)
- **cards/application +3 dirs** (6 cross-edges)
- **domain/organization +6 dirs** (4 cross-edges)
- **application/importing +3 dirs** (3 cross-edges)
- **application/analysis +5 dirs** (3 cross-edges)
- **api/history +5 dirs** (3 cross-edges)
- **domain/organization +4 dirs** (3 cross-edges)
- **deckassemble/decks · of** (2 cross-edges)
- **domain/organization +1 dirs** (2 cross-edges)
- **cards/application +10 dirs** (2 cross-edges)
- **api/organization +4 dirs** (2 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)
- **cards/domain +4 dirs · asString** (1 cross-edges)
- **domain/organization +3 dirs** (1 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (1 cross-edges)
- **recommendations/application +7 dirs** (1 cross-edges)
- **imports/application +7 dirs** (1 cross-edges)
- **application/exporting +1 dirs** (1 cross-edges)
- **application/alternatives +1 dirs** (1 cross-edges)
- **cards/application +2 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-106"
smart_context with task: "understand api/organization +19 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
