---
name: gortex-domain-organization-6-dirs
description: "Work in the domain/organization +6 dirs area — 171 symbols across 11 files (86% cohesion)"
---

# domain/organization +6 dirs

171 symbols | 11 files | 86% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java`
- `src/main/java/com/deckassemble/decks/application/DeckCardNotFoundException.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckCategoryService.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckCategory.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignment.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignmentRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryRepository.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckCategoryServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java` | deckId, categoryId, delete |
| `src/main/java/com/deckassemble/decks/application/DeckCardNotFoundException.java` | DeckCardNotFoundException |
| `src/main/java/com/deckassemble/decks/application/history/DeckRevisionRestoreService.java` | deckId, applyCategories, target |
| `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java` | deckId, categoryNames |
| `src/main/java/com/deckassemble/decks/application/organization/DeckCategoryService.java` | deckCardIdsByCategoryId, deckCardIds, assertCardsInDeck, assignCards, deckId, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckCategory.java` | setName, getId, createdBy, displayOrder, DeckCategory.<init>, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignment.java` | updatedAt, deckCardId, deckCardId, deckCategoryId, createdBy, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignmentRepository.java` | deleteByDeckCategoryId, deckCategoryId, findByDeckCategoryIdIn, deckCategoryIds, DeckCategoryAssignmentRepository |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryRepository.java` | existsByDeckIdAndName, findByIdAndDeckId, deckId, deckId, id, ... |
| `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java` | category, name, shouldSnapshotCategoriesInDisplayOrder, order |
| `src/test/java/com/deckassemble/decks/application/organization/DeckCategoryServiceTest.java` | shouldReplaceAssignmentsOnBulkAssign, PROFILE_ID, shouldDeleteUserCreatedCategory, deckRepository, shouldNotRecordRevisionWhenRenamingToSameName, ... |

## Connected Communities

- **cards/domain +16 dirs** (11 cross-edges)
- **decks/application +18 dirs** (10 cross-edges)
- **api/history +5 dirs** (6 cross-edges)
- **api/organization +19 dirs** (5 cross-edges)
- **decks/application +13 dirs** (3 cross-edges)
- **shared/security +6 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **application/publishing +3 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-141"
smart_context with task: "understand domain/organization +6 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
