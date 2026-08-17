---
name: gortex-domain-organization-4-dirs
description: "Work in the domain/organization +4 dirs area — 76 symbols across 9 files (78% cohesion)"
---

# domain/organization +4 dirs

76 symbols | 9 files | 78% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/organization/DeckTagController.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckTagNotFoundException.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTag.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignment.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignmentRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagRepository.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/organization/DeckTagController.java` | delete, DeleteMapping, tagId |
| `src/main/java/com/deckassemble/decks/application/organization/DeckTagNotFoundException.java` | DeckTagNotFoundException |
| `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java` | delete, ownedTag, tagId, profileId, tagId |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTag.java` | name, DeckTag, profileId, DeckTag.<init>, DeckTag.<init> |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignment.java` | deckId, createdAt, tagId, DeckTagAssignment.<init>, tagId, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignmentRepository.java` | tagId, tagId, deleteByDeckId, deckId, findByTagId, ... |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagRepository.java` | profileId, findByProfileIdOrderByNameAsc, id, findByIdAndProfileId, profileId |
| `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java` | deckTagAssignmentRepository, deck, PROFILE_ID, id, tag, ... |
| `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java` | TAG_ID_B, shouldAssignManyTagsToOneDeck, shouldRecordTagChangedRevisionForEachDeckAffectedByTagDeletion, shouldRejectAssigningTagNotOwnedByProfile, deckRepository, ... |

## Connected Communities

- **api/organization +19 dirs** (6 cross-edges)
- **decks/application +13 dirs** (2 cross-edges)
- **cards/domain +16 dirs** (2 cross-edges)
- **domain/organization +1 dirs** (2 cross-edges)
- **decks/application +18 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-142"
smart_context with task: "understand domain/organization +4 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
