---
name: gortex-application-importing-6-dirs
description: "Work in the application/importing +6 dirs area — 153 symbols across 12 files (88% cohesion)"
---

# application/importing +6 dirs

153 symbols | 12 files | 88% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreviewResponse.java`
- `src/main/java/com/deckassemble/decks/api/importing/DeckImportPreviewResponse.java`
- `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportCommitService.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportParser.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- `src/main/java/com/deckassemble/decks/domain/Deck.java`
- `src/main/java/com/deckassemble/decks/domain/DeckImportPreview.java`
- `src/main/java/com/deckassemble/decks/domain/DeckImportPreviewRepository.java`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportCommitServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`
- `src/test/java/com/deckassemble/decks/domain/DeckImportPreviewTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreviewResponse.java` | CollectionImportPreviewResponse.<init> |
| `src/main/java/com/deckassemble/decks/api/importing/DeckImportPreviewResponse.java` | DeckImportPreviewResponse.<init> |
| `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java` | lockedProfileId |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportCommitService.java` | preview, idempotencyKey, idempotencyKey, accessGuard, reason, ... |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportParser.java` | format |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java` | previewRepository, resolver, objectMapper, DeckImportService.<init>, accessGuard, ... |
| `src/main/java/com/deckassemble/decks/domain/Deck.java` | Status, ARCHIVED, DRAFT, ACTIVE |
| `src/main/java/com/deckassemble/decks/domain/DeckImportPreview.java` | storeCanonicalRows, status, committedDeckId, getStatus, markCommitted, ... |
| `src/main/java/com/deckassemble/decks/domain/DeckImportPreviewRepository.java` | token, DeckImportPreviewRepository, findLockedByTokenAndProfileId, profileId, profileId, ... |
| `src/test/java/com/deckassemble/decks/application/importing/DeckImportCommitServiceTest.java` | committedPreview, previewRepository, rows, id, pendingPreview, ... |
| `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java` | previewService, shouldReturnInvalidRowForMalformedTextQuantity, parser, resolver, identifierHeader, ... |
| `src/test/java/com/deckassemble/decks/domain/DeckImportPreviewTest.java` | DeckImportPreviewTest, shouldRecordCommittedDeck |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/importing/DeckImportCommitServiceTest.java::DeckImportCommitServiceTest.shouldCommitSelectedCardsAndReturnRefreshedDeck`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportCommitServiceTest.java::DeckImportCommitServiceTest.shouldLeavePreviewPendingWhenCardAdditionFails`

## Connected Communities

- **decks/application +13 dirs** (9 cross-edges)
- **cards/domain +16 dirs** (3 cross-edges)
- **decks/application +18 dirs** (3 cross-edges)
- **api/organization +19 dirs** (2 cross-edges)
- **cards/application +3 dirs** (1 cross-edges)
- **cards/application +10 dirs** (1 cross-edges)
- **deckassemble/decks · preview** (1 cross-edges)
- **application/importing · row** (1 cross-edges)
- **users/application +4 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-70"
smart_context with task: "understand application/importing +6 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/importing/DeckImportCommitServiceTest.java::DeckImportCommitServiceTest.shouldCommitSelectedCardsAndReturnRefreshedDeck", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
