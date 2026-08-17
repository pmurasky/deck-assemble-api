---
name: gortex-collections-application-4-dirs
description: "Work in the collections/application +4 dirs area — 192 symbols across 13 files (88% cohesion)"
---

# collections/application +4 dirs

192 symbols | 13 files | 88% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/application/CardOwnershipLookup.java`
- `src/main/java/com/deckassemble/cards/application/FinishUnavailableException.java`
- `src/main/java/com/deckassemble/collections/api/CollectionController.java`
- `src/main/java/com/deckassemble/collections/application/CollectionCardResponse.java`
- `src/main/java/com/deckassemble/collections/application/CollectionNotFoundException.java`
- `src/main/java/com/deckassemble/collections/application/CollectionOwnershipLookup.java`
- `src/main/java/com/deckassemble/collections/application/CollectionResponse.java`
- `src/main/java/com/deckassemble/collections/application/CollectionService.java`
- `src/main/java/com/deckassemble/collections/domain/CardCollection.java`
- `src/main/java/com/deckassemble/collections/domain/CardCollectionRepository.java`
- `src/main/java/com/deckassemble/collections/domain/CollectionCard.java`
- `src/main/java/com/deckassemble/collections/domain/CollectionCardRepository.java`
- `src/test/java/com/deckassemble/collections/application/CollectionServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/application/CardOwnershipLookup.java` | CardOwnershipLookup |
| `src/main/java/com/deckassemble/cards/application/FinishUnavailableException.java` | FinishUnavailableException, finish, FinishUnavailableException.<init> |
| `src/main/java/com/deckassemble/collections/api/CollectionController.java` | request, list, cardCatalogService, update, collectionId, ... |
| `src/main/java/com/deckassemble/collections/application/CollectionCardResponse.java` | from, card, summary |
| `src/main/java/com/deckassemble/collections/application/CollectionNotFoundException.java` | CollectionNotFoundException |
| `src/main/java/com/deckassemble/collections/application/CollectionOwnershipLookup.java` | CollectionOwnershipLookup, profileService, collectionCardRepository, collectionRepository, collectionCardRepository, ... |
| `src/main/java/com/deckassemble/collections/application/CollectionResponse.java` | from, collection |
| `src/main/java/com/deckassemble/collections/application/CollectionService.java` | request, delete, removeCard, listCards, collectionId, ... |
| `src/main/java/com/deckassemble/collections/domain/CardCollection.java` | name, createdBy, description, defaultCollection, getCreatedAt, ... |
| `src/main/java/com/deckassemble/collections/domain/CardCollectionRepository.java` | id, findByProfileIdOrderByNameAsc, profileId, findByIdAndProfileId, findByProfileIdAndDefaultCollectionTrue, ... |
| `src/main/java/com/deckassemble/collections/domain/CollectionCard.java` | createdAt, collectionId, regularQuantity, CollectionCard.<init>, getCollectionId, ... |
| `src/main/java/com/deckassemble/collections/domain/CollectionCardRepository.java` | cardPrintingId, id, findByCollectionIdIn, findByCollectionIdAndCardPrintingId, collectionId, ... |
| `src/test/java/com/deckassemble/collections/application/CollectionServiceTest.java` | collectionRepository, shouldThrowWhenCollectionNotOwned, collectionCardRepository, name, PROFILE_ID, ... |

## Connected Communities

- **cards/domain +16 dirs** (11 cross-edges)
- **cards/application +10 dirs** (6 cross-edges)
- **api/organization +19 dirs** (5 cross-edges)
- **shared/security +6 dirs** (3 cross-edges)
- **com/deckassemble · name** (3 cross-edges)
- **decks/application +18 dirs** (2 cross-edges)
- **collections/domain +1 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-110"
smart_context with task: "understand collections/application +4 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
