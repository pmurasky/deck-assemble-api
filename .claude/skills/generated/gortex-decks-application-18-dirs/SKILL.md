---
name: gortex-decks-application-18-dirs
description: "Work in the decks/application +18 dirs area — 314 symbols across 36 files (73% cohesion)"
---

# decks/application +18 dirs

314 symbols | 36 files | 73% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java`
- `src/main/java/com/deckassemble/decks/application/DeckCardResponse.java`
- `src/main/java/com/deckassemble/decks/application/DeckCardService.java`
- `src/main/java/com/deckassemble/decks/application/DeckComboService.java`
- `src/main/java/com/deckassemble/decks/application/DeckOwnershipService.java`
- `src/main/java/com/deckassemble/decks/application/DeckService.java`
- `src/main/java/com/deckassemble/decks/application/DeckWishlistService.java`
- `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java`
- `src/main/java/com/deckassemble/decks/application/exporting/GenericCsvDeckExporter.java`
- `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportParser.java`
- `src/main/java/com/deckassemble/decks/application/importing/GenericCsvDeckImportParser.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java`
- `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- `src/main/java/com/deckassemble/decks/domain/DeckCard.java`
- `src/main/java/com/deckassemble/decks/domain/DeckCardRepository.java`
- `src/main/java/com/deckassemble/decks/domain/DeckRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckFolderRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignmentRepository.java`
- `src/main/java/com/deckassemble/decks/domain/organization/DeckTagRepository.java`
- `src/main/java/com/deckassemble/recommendations/domain/CommanderSpellbookClient.java`
- `src/main/java/com/deckassemble/users/application/ProfileService.java`
- `src/test/java/com/deckassemble/decks/api/DeckUpgradePlansIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckComboServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckOwnershipServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/DeckWishlistServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/exporting/DeckExporterTest.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/publishing/DeckPrimerServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | deckComparisonService, updateCard, deckId, deckCardId, deckUpgradeService, ... |
| `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java` | profileId, profileService, currentUser, deckRepository, deckRepository, ... |
| `src/main/java/com/deckassemble/decks/application/DeckCardResponse.java` | from, card, deckCard |
| `src/main/java/com/deckassemble/decks/application/DeckCardService.java` | section, mergeOrNew, ownershipChecker, deckId, deckCardId, ... |
| `src/main/java/com/deckassemble/decks/application/DeckComboService.java` | card, deckId, cards, deckCardRepository, getCombos, ... |
| `src/main/java/com/deckassemble/decks/application/DeckOwnershipService.java` | deckId, ownershipChecker, collectionService, DeckOwnershipService, deckAccessGuard, ... |
| `src/main/java/com/deckassemble/decks/application/DeckService.java` | deckId, card, copyCard |
| `src/main/java/com/deckassemble/decks/application/DeckWishlistService.java` | deckCard, cardCatalogService, toWishlistItem, cardPriceService, deckCardRepository, ... |
| `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java` | categorizer, DeckCardAlternativeService.<init>, cardCatalogService, deckComboService, ownershipChecker, ... |
| `src/main/java/com/deckassemble/decks/application/exporting/GenericCsvDeckExporter.java` | section |
| `src/main/java/com/deckassemble/decks/application/history/DeckSnapshotBuilder.java` | deckTagAssignmentRepository, DeckSnapshotBuilder.<init>, deckTagRepository, deckCardRepository, deckId, ... |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportParser.java` | DeckImportParser |
| `src/main/java/com/deckassemble/decks/application/importing/GenericCsvDeckImportParser.java` | section, value |
| `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java` | DeckFolderService.<init>, deckAccessGuard, deckFolderRepository, deckRepository, deckRevisionService |
| `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java` | assignmentRepository, deckTagRepository, deckAccessGuard, DeckTagService.<init>, deckRevisionService |
| `src/main/java/com/deckassemble/decks/domain/DeckCard.java` | getOwnershipStatus, DeckCard.<init>, cardPrintingId, deckId, SIDEBOARD, ... |
| `src/main/java/com/deckassemble/decks/domain/DeckCardRepository.java` | id, deckId, deckSection, deckId, DeckCardRepository, ... |
| `src/main/java/com/deckassemble/decks/domain/DeckRepository.java` | profileId, id, findByIdAndProfileId |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryRepository.java` | DeckCategoryRepository |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckFolderRepository.java` | DeckFolderRepository |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagAssignmentRepository.java` | DeckTagAssignmentRepository |
| `src/main/java/com/deckassemble/decks/domain/organization/DeckTagRepository.java` | DeckTagRepository |
| `src/main/java/com/deckassemble/recommendations/domain/CommanderSpellbookClient.java` | deckList, findCombos |
| `src/main/java/com/deckassemble/users/application/ProfileService.java` | getOrCreate, subject |
| `src/test/java/com/deckassemble/decks/api/DeckUpgradePlansIntegrationTest.java` | deckCardId, markProxy, deckId |
| `src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java` | deckRepository, shouldMarkNewCardAsOwnedWhenPrintingInCollection, currentUser, DeckCardServiceTest, shouldUpdateCardFields, ... |
| `src/test/java/com/deckassemble/decks/application/DeckComboServiceTest.java` | DeckComboServiceTest, stubUser, PROFILE_ID, currentUser, deckRepository, ... |
| `src/test/java/com/deckassemble/decks/application/DeckOwnershipServiceTest.java` | DeckOwnershipServiceTest, shouldNotResaveWhenAcquiringAlreadyOwnedCard, stubUser, ownershipChecker, cardCatalogService, ... |
| `src/test/java/com/deckassemble/decks/application/DeckWishlistServiceTest.java` | DeckWishlistServiceTest, shouldReturnEmptyWishlistWhenNoWishlistCards, service, deckRepository, profileService, ... |
| `src/test/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeServiceTest.java` | stubTarget, card, setUp |
| `src/test/java/com/deckassemble/decks/application/exporting/DeckExporterTest.java` | cards |
| `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java` | shouldSnapshotCardsOrderedByIdRegardlessOfRepositoryOrder, id, deckCard, printingId, quantity, ... |
| `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java` | shouldParseQuantitiesAndSectionsForEverySupportedFormat, parser, fixture |
| `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java` | stubCommonCollaborators |
| `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java` | stubCommonCollaborators |
| `src/test/java/com/deckassemble/decks/application/publishing/DeckPrimerServiceTest.java` | BeforeEach |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java::DeckCardServiceTest.shouldSynthesizeCommanderEntryWhenRowMissing`
- `src/test/java/com/deckassemble/decks/application/DeckWishlistServiceTest.java::DeckWishlistServiceTest.shouldReturnWishlistWithPricesAndTotal`

## Connected Communities

- **decks/application +13 dirs** (28 cross-edges)
- **cards/domain +16 dirs** (18 cross-edges)
- **cards/application +2 dirs** (8 cross-edges)
- **api/history +5 dirs** (8 cross-edges)
- **recommendations/application +7 dirs** (8 cross-edges)
- **shared/security +6 dirs** (7 cross-edges)
- **api/organization +19 dirs** (6 cross-edges)
- **com/deckassemble · name** (6 cross-edges)
- **cards/application +10 dirs** (4 cross-edges)
- **collections/application +4 dirs** (3 cross-edges)
- **application/importing +6 dirs** (1 cross-edges)
- **com/deckassemble · add · CardSearchPredicates** (1 cross-edges)
- **decks/application · commanderAt** (1 cross-edges)
- **cards/application +3 dirs** (1 cross-edges)
- **application/analysis +5 dirs** (1 cross-edges)
- **cards/domain +1 dirs · CardSearchPredicatesTest** (1 cross-edges)
- **recommendations/application +5 dirs** (1 cross-edges)
- **users/application +4 dirs** (1 cross-edges)
- **domain/organization +6 dirs** (1 cross-edges)
- **domain/organization +1 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-69"
smart_context with task: "understand decks/application +18 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java::DeckCardServiceTest.shouldSynthesizeCommanderEntryWhenRowMissing", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
