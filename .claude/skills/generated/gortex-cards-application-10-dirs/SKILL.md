---
name: gortex-cards-application-10-dirs
description: "Work in the cards/application +10 dirs area — 288 symbols across 41 files (76% cohesion)"
---

# cards/application +10 dirs

288 symbols | 41 files | 76% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/api/CardController.java`
- `src/main/java/com/deckassemble/cards/api/CardSearchRequest.java`
- `src/main/java/com/deckassemble/cards/application/CardAnalysisView.java`
- `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- `src/main/java/com/deckassemble/cards/application/CardDetailResponse.java`
- `src/main/java/com/deckassemble/cards/application/CardExportView.java`
- `src/main/java/com/deckassemble/cards/application/CardNotFoundException.java`
- `src/main/java/com/deckassemble/cards/application/CardOwnershipLookup.java`
- `src/main/java/com/deckassemble/cards/application/CardPriceService.java`
- `src/main/java/com/deckassemble/cards/application/CardPrintingResponse.java`
- `src/main/java/com/deckassemble/cards/application/CardReferenceResolution.java`
- `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java`
- `src/main/java/com/deckassemble/cards/application/CardSearchCandidateSpecifications.java`
- `src/main/java/com/deckassemble/cards/application/CardSearchFilter.java`
- `src/main/java/com/deckassemble/cards/application/CardSummaryResponse.java`
- `src/main/java/com/deckassemble/cards/application/GameChangerRefreshService.java`
- `src/main/java/com/deckassemble/cards/application/LegalityMapper.java`
- `src/main/java/com/deckassemble/cards/application/PriceRefreshJob.java`
- `src/main/java/com/deckassemble/cards/application/PrintingFields.java`
- `src/main/java/com/deckassemble/cards/domain/Card.java`
- `src/main/java/com/deckassemble/cards/domain/CardAttributes.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrice.java`
- `src/main/java/com/deckassemble/cards/domain/CardPriceSnapshotRepository.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrinting.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrintingRepository.java`
- `src/main/java/com/deckassemble/cards/domain/CardRepository.java`
- `src/main/java/com/deckassemble/cards/domain/MagicSet.java`
- `src/main/java/com/deckassemble/cards/domain/ManaPips.java`
- `src/main/java/com/deckassemble/decks/application/CommanderLegalityEvaluator.java`
- `src/main/java/com/deckassemble/decks/application/publishing/DeckPrimerService.java`
- `src/main/java/com/deckassemble/recommendations/application/ManaBaseCheck.java`
- `src/main/java/com/deckassemble/users/domain/ProfileRepository.java`
- `src/test/java/com/deckassemble/cards/application/CardCatalogServiceTest.java`
- `src/test/java/com/deckassemble/cards/application/CardExportViewTest.java`
- `src/test/java/com/deckassemble/cards/application/CardPriceServiceTest.java`
- `src/test/java/com/deckassemble/cards/application/CardReferenceResolverTest.java`
- `src/test/java/com/deckassemble/cards/application/CardSearchCandidateSpecificationsTest.java`
- `src/test/java/com/deckassemble/cards/application/GameChangerRefreshServiceTest.java`
- `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java`
- `src/test/java/com/deckassemble/decks/api/DeckExportControllerTest.java`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/api/CardController.java` | cardId, CardController.<init>, getById, cardCatalogService, cardId, ... |
| `src/main/java/com/deckassemble/cards/api/CardSearchRequest.java` | priceRange |
| `src/main/java/com/deckassemble/cards/application/CardAnalysisView.java` | from, printing |
| `src/main/java/com/deckassemble/cards/application/CardCatalogService.java` | cardPrintingRepository, setCode, getById, cardPrintingIds, foilQuantity, ... |
| `src/main/java/com/deckassemble/cards/application/CardDetailResponse.java` | latestPrinting, card, from |
| `src/main/java/com/deckassemble/cards/application/CardExportView.java` | displayName |
| `src/main/java/com/deckassemble/cards/application/CardNotFoundException.java` | CardNotFoundException |
| `src/main/java/com/deckassemble/cards/application/CardOwnershipLookup.java` | subject, ownedQuantitiesBySubject |
| `src/main/java/com/deckassemble/cards/application/CardPriceService.java` | trackedPrintingIds |
| `src/main/java/com/deckassemble/cards/application/CardPrintingResponse.java` | from, printing |
| `src/main/java/com/deckassemble/cards/application/CardReferenceResolution.java` | CardReferenceResolution |
| `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java` | cardRepository, CardReferenceResolver.<init>, reference, exactReference, resolve, ... |
| `src/main/java/com/deckassemble/cards/application/CardSearchCandidateSpecifications.java` | ownedQuantitySpec, includeUnmatched, cardPrintingRepository, matchingIds, cardOwnershipLookup, ... |
| `src/main/java/com/deckassemble/cards/application/CardSearchFilter.java` | isEmpty |
| `src/main/java/com/deckassemble/cards/application/CardSummaryResponse.java` | card, latestPrinting, from |
| `src/main/java/com/deckassemble/cards/application/GameChangerRefreshService.java` | refreshGameChangers, cardCatalogService, scryfallClient, GameChangerRefreshService.<init> |
| `src/main/java/com/deckassemble/cards/application/LegalityMapper.java` | byFormat, legalities, LegalityMapper.<init>, LegalityMapper |
| `src/main/java/com/deckassemble/cards/application/PriceRefreshJob.java` | refreshTrackedPrices, Scheduled |
| `src/main/java/com/deckassemble/cards/application/PrintingFields.java` | of, getter, PrintingFields.<init>, T, printing, ... |
| `src/main/java/com/deckassemble/cards/domain/Card.java` | getKeywords, getManaValue |
| `src/main/java/com/deckassemble/cards/domain/CardAttributes.java` | getPower, getColors, getLoyalty, getToughness, getManaCost |
| `src/main/java/com/deckassemble/cards/domain/CardPrice.java` | currency, forCurrency |
| `src/main/java/com/deckassemble/cards/domain/CardPriceSnapshotRepository.java` | findLatestByCardPrintingIds, printingIds, findTrackedPrintingIds |
| `src/main/java/com/deckassemble/cards/domain/CardPrinting.java` | getReleasedAt, setActive, getFlavorName, foilAvailable, setNonfoilAvailable, ... |
| `src/main/java/com/deckassemble/cards/domain/CardPrintingRepository.java` | collectorNumber, findByScryfallCardId, name, scryfallCardId, query, ... |
| `src/main/java/com/deckassemble/cards/domain/CardRepository.java` | Modifying, name, scryfallOracleIds, findByNameIgnoreCase, findByNameIn, ... |
| `src/main/java/com/deckassemble/cards/domain/MagicSet.java` | getSetCode |
| `src/main/java/com/deckassemble/cards/domain/ManaPips.java` | forColor, color |
| `src/main/java/com/deckassemble/decks/application/CommanderLegalityEvaluator.java` | cardPrintingRepository, pairingRules, cardRepository, CommanderLegalityEvaluator.<init> |
| `src/main/java/com/deckassemble/decks/application/publishing/DeckPrimerService.java` | Transactional |
| `src/main/java/com/deckassemble/recommendations/application/ManaBaseCheck.java` | required, raiseRequirements, card |
| `src/main/java/com/deckassemble/users/domain/ProfileRepository.java` | Query |
| `src/test/java/com/deckassemble/cards/application/CardCatalogServiceTest.java` | PAGEABLE, shouldMapPrintingsToCards, shouldRejectFoilQuantityWhenFoilUnavailable, shouldThrowWhenCardInactive, shouldThrowWhenCardMissing, ... |
| `src/test/java/com/deckassemble/cards/application/CardExportViewTest.java` | shouldUseCanonicalNameWhenFlavorNameIsBlank, CardExportViewTest |
| `src/test/java/com/deckassemble/cards/application/CardPriceServiceTest.java` | shouldReturnEmptyPricesWhenNoPrintingsGiven, shouldReturnLatestPricesMappedByPrintingId |
| `src/test/java/com/deckassemble/cards/application/CardReferenceResolverTest.java` | shouldFallBackToExactPrintingReferenceWhenScryfallIdIsUnknown, UNKNOWN_SCRYFALL_ID, setCode, printingRepository, CARD_ID, ... |
| `src/test/java/com/deckassemble/cards/application/CardSearchCandidateSpecificationsTest.java` | shouldLookUpOwnedQuantitiesForAuthenticatedSubject, CardSearchCandidateSpecificationsTest, cardOwnershipLookup, shouldFetchTrackedPricesForPriceRangeSpec, currentUser, ... |
| `src/test/java/com/deckassemble/cards/application/GameChangerRefreshServiceTest.java` | cardCatalogService, scryfallClient, GameChangerRefreshServiceTest, card, shouldKeepExistingFlagsWhenScryfallFails, ... |
| `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java` | shouldClassifyEveryResolutionAndCalculateTotals, shouldPersistProfileBoundPreview |
| `src/test/java/com/deckassemble/decks/api/DeckExportControllerTest.java` | shouldRejectMissingPrintingReadModel |
| `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java` | shouldClassifyEveryResolutionAndCalculateTotals |

## Entry Points

- `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java::CollectionImportServiceTest.shouldPersistProfileBoundPreview`
- `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java::DeckImportServiceTest.shouldClassifyEveryResolutionAndCalculateTotals`
- `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java::CollectionImportServiceTest.shouldClassifyEveryResolutionAndCalculateTotals`

## Connected Communities

- **cards/domain +16 dirs** (22 cross-edges)
- **api/organization +19 dirs** (15 cross-edges)
- **api/importing +8 dirs** (6 cross-edges)
- **recommendations/application +7 dirs** (5 cross-edges)
- **application/analysis +5 dirs** (4 cross-edges)
- **recommendations/application +5 dirs** (3 cross-edges)
- **com/deckassemble · isLand** (3 cross-edges)
- **com/deckassemble · getColorIdentity** (3 cross-edges)
- **shared/security +6 dirs** (3 cross-edges)
- **cards/domain +2 dirs · CardPriceSnapshot** (3 cross-edges)
- **com/deckassemble · has** (2 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (2 cross-edges)
- **com/deckassemble · withinGameChangerLimit** (2 cross-edges)
- **collections/application +4 dirs** (2 cross-edges)
- **com/deckassemble · add · Card** (2 cross-edges)
- **api/history +5 dirs** (1 cross-edges)
- **cards/domain +2 dirs · fromManaCost** (1 cross-edges)
- **recommendations/application +1 dirs · spell** (1 cross-edges)
- **decks/api +2 dirs · export** (1 cross-edges)
- **application/importing +3 dirs** (1 cross-edges)
- **decks/application +13 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **deckassemble/cards · addRelatedEntityFilters** (1 cross-edges)
- **cards/application · GameChangerRefreshService** (1 cross-edges)
- **cards/domain +1 dirs · CardSearchPredicatesTest** (1 cross-edges)
- **cards/domain +4 dirs · commander** (1 cross-edges)
- **cards/application +3 dirs** (1 cross-edges)
- **com/deckassemble · searchPrintings** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-13"
smart_context with task: "understand cards/application +10 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java::CollectionImportServiceTest.shouldPersistProfileBoundPreview", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
