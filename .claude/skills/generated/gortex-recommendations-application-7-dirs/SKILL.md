---
name: gortex-recommendations-application-7-dirs
description: "Work in the recommendations/application +7 dirs area — 195 symbols across 18 files (76% cohesion)"
---

# recommendations/application +7 dirs

195 symbols | 18 files | 76% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- `src/main/java/com/deckassemble/cards/domain/Card.java`
- `src/main/java/com/deckassemble/collections/application/CollectionService.java`
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/application/DeckService.java`
- `src/main/java/com/deckassemble/recommendations/api/RecommendationController.java`
- `src/main/java/com/deckassemble/recommendations/application/BasicLandPadder.java`
- `src/main/java/com/deckassemble/recommendations/application/CardScore.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderResolver.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckBuildRecorder.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckBuilderService.java`
- `src/main/java/com/deckassemble/recommendations/application/EdhrecCommanderService.java`
- `src/main/java/com/deckassemble/recommendations/application/RecommendationReasonCode.java`
- `src/test/java/com/deckassemble/recommendations/application/CommanderSuggestionServiceTest.java`
- `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java`
- `src/test/java/com/deckassemble/recommendations/application/PlayStyleQuotasTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/application/CardCatalogService.java` | cardId, getCardsByNames, cardPrintingIds, getLatestPrintingIdByCardIds, names, ... |
| `src/main/java/com/deckassemble/cards/domain/Card.java` | setCommanderRank, gameChanger, setGameChanger, getActive, commanderRank |
| `src/main/java/com/deckassemble/collections/application/CollectionService.java` | profileId, getOwnedPrintingIds |
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | legality, deckId |
| `src/main/java/com/deckassemble/decks/application/DeckService.java` | legality, deckId |
| `src/main/java/com/deckassemble/recommendations/api/RecommendationController.java` | RecommendationController, request, deckBuilderService, build, commanders, ... |
| `src/main/java/com/deckassemble/recommendations/application/BasicLandPadder.java` | identity, basicLands |
| `src/main/java/com/deckassemble/recommendations/application/CardScore.java` | CardScore.<init> |
| `src/main/java/com/deckassemble/recommendations/application/CommanderResolver.java` | resolve, request |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java` | suggestion, missing |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java` | cardCatalogService, allScoreNames, cards, edhrecCommanderService, LOGGER, ... |
| `src/main/java/com/deckassemble/recommendations/application/DeckBuildRecorder.java` | scoredCandidates, finalCards |
| `src/main/java/com/deckassemble/recommendations/application/DeckBuilderService.java` | request, build |
| `src/main/java/com/deckassemble/recommendations/application/EdhrecCommanderService.java` | commanderOracleId, fetchedAt, commanderOracleId, commanderName, getCardScores |
| `src/main/java/com/deckassemble/recommendations/application/RecommendationReasonCode.java` | PLAY_STYLE, MISSING_COUNT, RecommendationReasonCode, COLOR_SUPPORT, OWNED, ... |
| `src/test/java/com/deckassemble/recommendations/application/CommanderSuggestionServiceTest.java` | profileService, service, setUp, shouldExplainFactorsDeterminingSuggestionOrder, suggestion, ... |
| `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java` | typeLine, pool, DeckBuilderServiceTest, commander, PROFILE_ID, ... |
| `src/test/java/com/deckassemble/recommendations/application/PlayStyleQuotasTest.java` | quotas, total |

## Entry Points

- `src/test/java/com/deckassemble/recommendations/application/CommanderSuggestionServiceTest.java::CommanderSuggestionServiceTest.shouldExplainFactorsDeterminingSuggestionOrder`
- `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java::DeckBuilderServiceTest.shouldBuildFullDeckWithBasicsPadding`
- `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java::DeckBuilderServiceTest.shouldCoverAllCandidateScoreReasonCodesAcrossScoredCandidates`
- `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java::DeckBuilderServiceTest.shouldExcludeUnownedCardsOverBudget`
- `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java::DeckBuilderServiceTest.shouldKeepOnlyThreeGameChangersAtMediumPower`

## Connected Communities

- **api/organization +19 dirs** (19 cross-edges)
- **cards/domain +16 dirs** (14 cross-edges)
- **decks/application +13 dirs** (13 cross-edges)
- **recommendations/application +1 dirs · card** (10 cross-edges)
- **cards/application +10 dirs** (9 cross-edges)
- **com/deckassemble · add · Card** (8 cross-edges)
- **application/analysis +5 dirs** (7 cross-edges)
- **recommendations/application +5 dirs** (6 cross-edges)
- **cards/application +3 dirs** (6 cross-edges)
- **decks/application +18 dirs** (4 cross-edges)
- **cards/domain +1 dirs · CardSearchPredicatesTest** (4 cross-edges)
- **shared/security +6 dirs** (2 cross-edges)
- **recommendations/domain +2 dirs** (2 cross-edges)
- **collections/application +4 dirs** (2 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (1 cross-edges)
- **application/importing +3 dirs** (1 cross-edges)
- **com/deckassemble · add · CardSearchPredicates** (1 cross-edges)
- **recommendations/application +1 dirs · forStyle** (1 cross-edges)
- **recommendations/application · DeckBuildRecorder** (1 cross-edges)
- **cards/domain +4 dirs · asString** (1 cross-edges)
- **com/deckassemble · getColorIdentity** (1 cross-edges)
- **recommendations/application +1 dirs · picked** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-156"
smart_context with task: "understand recommendations/application +7 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/recommendations/application/CommanderSuggestionServiceTest.java::CommanderSuggestionServiceTest.shouldExplainFactorsDeterminingSuggestionOrder", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
