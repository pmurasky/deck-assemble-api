---
name: gortex-cards-domain-16-dirs
description: "Work in the cards/domain +16 dirs area — 537 symbols across 37 files (74% cohesion)"
---

# cards/domain +16 dirs

537 symbols | 37 files | 74% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- `src/main/java/com/deckassemble/cards/application/CardFaceResponse.java`
- `src/main/java/com/deckassemble/cards/domain/Card.java`
- `src/main/java/com/deckassemble/cards/domain/CardAttributes.java`
- `src/main/java/com/deckassemble/cards/domain/CardLegality.java`
- `src/main/java/com/deckassemble/cards/domain/CardLegalityRepository.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrinting.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrintingFace.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrintingFaceRepository.java`
- `src/main/java/com/deckassemble/cards/domain/CardPrintingRepository.java`
- `src/main/java/com/deckassemble/cards/domain/CardRepository.java`
- `src/main/java/com/deckassemble/cards/domain/MagicSet.java`
- `src/main/java/com/deckassemble/cards/domain/MagicSetRepository.java`
- `src/main/java/com/deckassemble/collections/domain/CardCollection.java`
- `src/main/java/com/deckassemble/imports/application/CardImportService.java`
- `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java`
- `src/main/java/com/deckassemble/recommendations/application/EdhrecCommanderService.java`
- `src/main/java/com/deckassemble/recommendations/domain/EdhrecClient.java`
- `src/main/java/com/deckassemble/recommendations/domain/EdhrecCommanderCache.java`
- `src/test/java/com/deckassemble/cards/api/CardControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/cards/api/PrintingControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/cards/application/CardCatalogServiceTest.java`
- `src/test/java/com/deckassemble/cards/application/CardSearchPredicatesTest.java`
- `src/test/java/com/deckassemble/cards/application/CardSummaryResponseTest.java`
- `src/test/java/com/deckassemble/collections/api/CollectionControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/collections/api/CollectionImportControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/DeckImportCommitControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/DeckImportControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/DeckUpgradePlansIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/history/DeckHistoryControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/api/simulation/DeckSimulationControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/simulation/DeckSimulationServiceTest.java`
- `src/test/java/com/deckassemble/imports/application/CardImportServiceTest.java`
- `src/test/java/com/deckassemble/recommendations/api/RecommendationControllerIntegrationTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/application/CardCatalogService.java` | partnerCandidatesSpec, card, primary, initializeAssociations, cardSpec |
| `src/main/java/com/deckassemble/cards/application/CardFaceResponse.java` | face, from |
| `src/main/java/com/deckassemble/cards/domain/Card.java` | setLayout, reserved, reserved, name, createdAt, ... |
| `src/main/java/com/deckassemble/cards/domain/CardAttributes.java` | typeLine, oracleText, setTypeLine, toughness, setColors, ... |
| `src/main/java/com/deckassemble/cards/domain/CardLegality.java` | updateStatus, legalityStatus, updatedAt, getUpdatedAt, CardLegality, ... |
| `src/main/java/com/deckassemble/cards/domain/CardLegalityRepository.java` | findByCardId, cardId |
| `src/main/java/com/deckassemble/cards/domain/CardPrinting.java` | CardPrinting.<init>, releasedAt, rarity, setReleasedAt, setCollectorNumber, ... |
| `src/main/java/com/deckassemble/cards/domain/CardPrintingFace.java` | getId, getFaceOrder, imageUri, CardPrintingFace.<init>, cardPrinting, ... |
| `src/main/java/com/deckassemble/cards/domain/CardPrintingFaceRepository.java` | cardPrintingId, CardPrintingFaceRepository, deleteByCardPrintingId |
| `src/main/java/com/deckassemble/cards/domain/CardPrintingRepository.java` | magicSetId, findByMagicSetIdOrderByCollectorNumberAsc |
| `src/main/java/com/deckassemble/cards/domain/CardRepository.java` | query, pageable, scryfallOracleId, findByNameContainingIgnoreCaseAndActiveTrue, findByScryfallOracleId |
| `src/main/java/com/deckassemble/cards/domain/MagicSet.java` | setDigital, setSetType, releaseDate, foilOnly, nonfoilOnly, ... |
| `src/main/java/com/deckassemble/cards/domain/MagicSetRepository.java` | findByScryfallSetId, setCode, findBySetCode, MagicSetRepository, scryfallSetId |
| `src/main/java/com/deckassemble/collections/domain/CardCollection.java` | getId |
| `src/main/java/com/deckassemble/imports/application/CardImportService.java` | replaceFaces, UPDATED, values, currentUser, card, ... |
| `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java` | index, applyTagsToCards, cardsByOracleId |
| `src/main/java/com/deckassemble/recommendations/application/EdhrecCommanderService.java` | fetchAndStore, commanderName, commanderOracleId, existing |
| `src/main/java/com/deckassemble/recommendations/domain/EdhrecClient.java` | fetchCommanderData, commanderSlug |
| `src/main/java/com/deckassemble/recommendations/domain/EdhrecCommanderCache.java` | newPayload, newFetchedAt, refresh |
| `src/test/java/com/deckassemble/cards/api/CardControllerIntegrationTest.java` | shouldRejectAnUnknownFunctionalCategory, shouldAllowAnonymousCardBrowsing, profileRepository, magicSetRepository, shouldFilterByCompoundOracleTextManaValueAndFormatLegality, ... |
| `src/test/java/com/deckassemble/cards/api/PrintingControllerIntegrationTest.java` | cardPrintingRepository, cardRepository, magicSetRepository, mockMvc, PrintingControllerIntegrationTest, ... |
| `src/test/java/com/deckassemble/cards/application/CardCatalogServiceTest.java` | shouldMapPrintingsToImmutableAnalysisViews |
| `src/test/java/com/deckassemble/cards/application/CardSearchPredicatesTest.java` | printingFilter, shouldFilterByPrintingRarityCollectorNumberLanguageAndFinish, shouldFilterByGameChanger, printingFilter, card, ... |
| `src/test/java/com/deckassemble/cards/application/CardSummaryResponseTest.java` | CardSummaryResponseTest, shouldTolerateDuplicateLegalityRowsLeftByConcurrentImports |
| `src/test/java/com/deckassemble/collections/api/CollectionControllerIntegrationTest.java` | setName, identifier, identifier, identifier, identifier, ... |
| `src/test/java/com/deckassemble/collections/api/CollectionImportControllerIntegrationTest.java` | name, setCode, createPrinting, name, collectorNumber, ... |
| `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java` | subject, printingId, createDeck, DeckControllerIntegrationTest, deckId, ... |
| `src/test/java/com/deckassemble/decks/api/DeckImportCommitControllerIntegrationTest.java` | setCode, collectorNumber, createPrinting, name |
| `src/test/java/com/deckassemble/decks/api/DeckImportControllerIntegrationTest.java` | collectorNumber, createPrinting, name, setCode, shouldResolveFlavorNamedTextImportToExactPrinting |
| `src/test/java/com/deckassemble/decks/api/DeckUpgradePlansIntegrationTest.java` | ownPrinting, createUpgradePrinting, name, name, createOffColorPrinting, ... |
| `src/test/java/com/deckassemble/decks/api/history/DeckHistoryControllerIntegrationTest.java` | createPrinting, identifier |
| `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java` | identifier, createPrinting |
| `src/test/java/com/deckassemble/decks/api/simulation/DeckSimulationControllerIntegrationTest.java` | createPrinting, identifier |
| `src/test/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeServiceTest.java` | oracleId, oracleId, id, name, card, ... |
| `src/test/java/com/deckassemble/decks/application/simulation/DeckSimulationServiceTest.java` | manaValue, id, spellCard, name |
| `src/test/java/com/deckassemble/imports/application/CardImportServiceTest.java` | magicSetRepository, legalities, scryfallClient, cardPrintingFaceRepository, shouldMergeLegalitiesInPlaceWhenReimportingAnExistingCard, ... |
| `src/test/java/com/deckassemble/recommendations/api/RecommendationControllerIntegrationTest.java` | cardLegalityRepository, card, magicSetRepository, name, colorIdentity, ... |

## Entry Points

- `src/test/java/com/deckassemble/imports/application/CardImportServiceTest.java::CardImportServiceTest.shouldImportAValidScryfallCard`
- `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java::DeckControllerIntegrationTest.shouldCompareDeckCompositionAndMetrics`
- `src/test/java/com/deckassemble/imports/application/CardImportServiceTest.java::CardImportServiceTest.shouldMergeLegalitiesInPlaceWhenReimportingAnExistingCard`
- `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java::DeckControllerIntegrationTest.shouldReturnAnalysisForOwnedDeck`
- `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java::DeckControllerIntegrationTest.shouldCreateUpdateDuplicateArchiveAndDeleteDeck`

## Connected Communities

- **cards/application +10 dirs** (22 cross-edges)
- **cards/domain +1 dirs · CardSearchPredicatesTest** (8 cross-edges)
- **recommendations/application +7 dirs** (4 cross-edges)
- **imports/application +4 dirs** (4 cross-edges)
- **decks/application +13 dirs** (4 cross-edges)
- **shared/security +6 dirs** (4 cross-edges)
- **decks/api · DeckUpgradePlansIntegrationTest** (3 cross-edges)
- **api/organization +19 dirs** (3 cross-edges)
- **cards/application +3 dirs** (2 cross-edges)
- **com/deckassemble · add · CardSearchPredicates** (2 cross-edges)
- **users/application +4 dirs** (2 cross-edges)
- **application/analysis +5 dirs** (2 cross-edges)
- **infrastructure/scryfall +1 dirs · imageUris** (1 cross-edges)
- **recommendations/application +5 dirs** (1 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (1 cross-edges)
- **com/deckassemble · getColorIdentity** (1 cross-edges)
- **com/deckassemble · withinGameChangerLimit** (1 cross-edges)
- **com/deckassemble · has** (1 cross-edges)
- **com/deckassemble · add · Card** (1 cross-edges)
- **imports/application +7 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-100"
smart_context with task: "understand cards/domain +16 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/imports/application/CardImportServiceTest.java::CardImportServiceTest.shouldImportAValidScryfallCard", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
