---
name: gortex-api-organization-4-dirs
description: "Work in the api/organization +4 dirs area — 80 symbols across 9 files (82% cohesion)"
---

# api/organization +4 dirs

80 symbols | 9 files | 82% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/organization/DeckFolderController.java`
- `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationAssignmentController.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckFolderNotFoundException.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java`
- `src/main/java/com/deckassemble/decks/domain/Deck.java`
- `src/main/java/com/deckassemble/decks/domain/DeckRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckFolder.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckFolderRepository.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/organization/DeckFolderController.java` | create, DeckFolderController.<init>, deckFolderService, DeckFolderController, folderId, ... |
| `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationAssignmentController.java` | assignFolder, deckId, request |
| `src/main/java/com/deckassemble/decks/application/organization/DeckFolderNotFoundException.java` | DeckFolderNotFoundException |
| `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java` | list, ownedFolder, folder, deckAccessGuard, folderId, ... |
| `src/main/java/com/deckassemble/decks/domain/Deck.java` | setFolderId, folderId |
| `src/main/java/com/deckassemble/decks/domain/DeckRepository.java` | folderId, findByFolderId, clearFolderId, folderId |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckFolder.java` | DeckFolder.<init>, profileId, DeckFolder, name, DeckFolder.<init> |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckFolderRepository.java` | profileId, id, profileId, findByProfileIdOrderByNameAsc, existsByProfileIdAndNameIgnoreCase, ... |
| `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java` | DECK_ID, shouldRecordFolderChangedRevisionForEachDeckAffectedByFolderDeletion, shouldAssignDeckToFolderReplacingAnyPriorFolder, DeckFolderServiceTest, shouldDeleteFolderAndClearReferencesButRetainDecks, ... |

## Connected Communities

- **decks/application +18 dirs** (7 cross-edges)
- **cards/domain +16 dirs** (4 cross-edges)
- **decks/domain +1 dirs · createFolder** (3 cross-edges)
- **decks/application +13 dirs** (2 cross-edges)
- **api/organization +19 dirs** (2 cross-edges)
- **api/history +5 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **com/deckassemble · from** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-73"
smart_context with task: "understand api/organization +4 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
