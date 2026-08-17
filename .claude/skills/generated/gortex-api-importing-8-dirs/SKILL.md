---
name: gortex-api-importing-8-dirs
description: "Work in the api/importing +8 dirs area — 173 symbols across 13 files (89% cohesion)"
---

# api/importing +8 dirs

173 symbols | 13 files | 89% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/collections/api/importing/CollectionColumnMapping.java`
- `src/main/java/com/deckassemble/collections/api/importing/CollectionImportController.java`
- `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreset.java`
- `src/main/java/com/deckassemble/collections/api/importing/CollectionImportResultResponse.java`
- `src/main/java/com/deckassemble/collections/application/CollectionAccessGuard.java`
- `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java`
- `src/main/java/com/deckassemble/collections/domain/CollectionImportPreview.java`
- `src/main/java/com/deckassemble/collections/domain/CollectionImportPreviewRepository.java`
- `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java`
- `src/main/java/com/deckassemble/users/domain/ProfileRepository.java`
- `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java`
- `src/test/java/com/deckassemble/collections/domain/CollectionImportPreviewTest.java`
- `src/test/java/com/deckassemble/decks/application/exporting/DeckExportRoundTripTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/collections/api/importing/CollectionColumnMapping.java` | toLayout |
| `src/main/java/com/deckassemble/collections/api/importing/CollectionImportController.java` | errors, idempotencyKey, file, importService, CollectionImportController.<init>, ... |
| `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreset.java` | MANABOX, GENERIC, defaultMapping, ARCHIDEKT, MOXFIELD, ... |
| `src/main/java/com/deckassemble/collections/api/importing/CollectionImportResultResponse.java` | CollectionImportResultResponse.<init> |
| `src/main/java/com/deckassemble/collections/application/CollectionAccessGuard.java` | currentUser, lockedProfileId, CollectionAccessGuard.<init>, profileService, CollectionAccessGuard, ... |
| `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java` | ownedLockedPreview, collectionService, commit, snapshotFrom, source, ... |
| `src/main/java/com/deckassemble/collections/domain/CollectionImportPreview.java` | token, canonicalRows, markCommitted, getSourceSha256, storeCanonicalRows, ... |
| `src/main/java/com/deckassemble/collections/domain/CollectionImportPreviewRepository.java` | idempotencyKey, findByProfileIdAndIdempotencyKey, findLockedByTokenAndProfileId, token, profileId, ... |
| `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java` | applyTags, runId, index, skipped, cardsByOracleId, ... |
| `src/main/java/com/deckassemble/users/domain/ProfileRepository.java` | Lock |
| `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java` | shouldExportErrorsForPendingAndCommittedPreviews, previewRows, previewRepository, shouldReplayCommittedResultForSameIdempotencyKey, scryfallPresets, ... |
| `src/test/java/com/deckassemble/collections/domain/CollectionImportPreviewTest.java` | shouldRecordCommittedCollection, CollectionImportPreviewTest |
| `src/test/java/com/deckassemble/decks/application/exporting/DeckExportRoundTripTest.java` | MethodSource |

## Entry Points

- `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java::CollectionImportServiceTest.shouldCommitSelectedRowsAndStoreSnapshot`

## Connected Communities

- **cards/domain +16 dirs** (3 cross-edges)
- **api/organization +19 dirs** (3 cross-edges)
- **application/importing +3 dirs** (2 cross-edges)
- **collections/application +4 dirs** (2 cross-edges)
- **cards/application +3 dirs** (2 cross-edges)
- **application/importing · append** (1 cross-edges)
- **recommendations/application +5 dirs** (1 cross-edges)
- **users/application +4 dirs** (1 cross-edges)
- **application/importing · MutableRows** (1 cross-edges)
- **shared/security +6 dirs** (1 cross-edges)
- **decks/application +18 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **com/deckassemble · searchPrintings** (1 cross-edges)
- **application/importing +6 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-112"
smart_context with task: "understand api/importing +8 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java::CollectionImportServiceTest.shouldCommitSelectedRowsAndStoreSnapshot", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
