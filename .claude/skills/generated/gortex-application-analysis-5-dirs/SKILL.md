---
name: gortex-application-analysis-5-dirs
description: "Work in the application/analysis +5 dirs area — 94 symbols across 9 files (72% cohesion)"
---

# application/analysis +5 dirs

94 symbols | 9 files | 72% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- `src/main/java/com/deckassemble/cards/application/CardPriceService.java`
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/api/upgrades/DeckUpgradePlanResponse.java`
- `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisResponse.java`
- `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisService.java`
- `src/main/java/com/deckassemble/decks/application/analysis/DeckCompositionCalculator.java`
- `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java`
- `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/application/CardCatalogService.java` | getAnalysisViewsByPrintingIds, cardPrintingIds |
| `src/main/java/com/deckassemble/cards/application/CardPriceService.java` | cardPrintingIds, latestPrices |
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | analysis, deckId |
| `src/main/java/com/deckassemble/decks/api/upgrades/DeckUpgradePlanResponse.java` | from, metrics |
| `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisResponse.java` | entries, ownershipBreakdown |
| `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisService.java` | deckComboService, views, includedInAnalysis, views, DeckAnalysisService, ... |
| `src/main/java/com/deckassemble/decks/application/analysis/DeckCompositionCalculator.java` | explicitCategoryNames, functionalCategories, entries |
| `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java` | beforeMetrics, before, gaps, PlanSelection.<init>, before, ... |
| `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java` | shouldReturnEmptyAnalysisForEmptyDeck, deckComboService, gameChanger, section, deckCategoryService, ... |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java::DeckAnalysisServiceTest.shouldComposeAnalysisAcrossCollaborators`
- `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java::DeckAnalysisServiceTest.shouldReturnEmptyAnalysisForEmptyDeck`
- `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java::DeckAnalysisServiceTest.shouldExcludeSideboardCompanionAndMaybeBoard`

## Connected Communities

- **application/analysis · entry** (7 cross-edges)
- **application/analysis +1 dirs · DeckCompositionCalculator** (6 cross-edges)
- **api/history +5 dirs** (6 cross-edges)
- **decks/application +18 dirs** (6 cross-edges)
- **cards/application +10 dirs** (5 cross-edges)
- **application/analysis +1 dirs · entry · AnalysisEntry** (4 cross-edges)
- **domain/organization +6 dirs** (3 cross-edges)
- **recommendations/application +7 dirs** (2 cross-edges)
- **application/analysis +1 dirs · entry · ManaProductionCalculator** (2 cross-edges)
- **api/organization +19 dirs** (2 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)
- **application/analysis · appendLowercased** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-130"
smart_context with task: "understand application/analysis +5 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java::DeckAnalysisServiceTest.shouldComposeAnalysisAcrossCollaborators", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
