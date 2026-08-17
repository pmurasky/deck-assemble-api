---
name: gortex-api-history-5-dirs
description: "Work in the api/history +5 dirs area — 93 symbols across 9 files (75% cohesion)"
---

# api/history +5 dirs

93 symbols | 9 files | 75% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/DeckController.java`
- `src/main/java/com/deckassemble/decks/api/comparison/DeckComparisonResponse.java`
- `src/main/java/com/deckassemble/decks/api/history/DeckHistoryController.java`
- `src/main/java/com/deckassemble/decks/api/history/DeckRevisionDiffResponse.java`
- `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java`
- `src/main/java/com/deckassemble/decks/application/DeckCardService.java`
- `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonDiffer.java`
- `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonService.java`
- `src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/DeckController.java` | deckId, otherDeckId, listCards, deckId, comparison |
| `src/main/java/com/deckassemble/decks/api/comparison/DeckComparisonResponse.java` | from, comparison |
| `src/main/java/com/deckassemble/decks/api/history/DeckHistoryController.java` | deckId, DeckHistoryController.<init>, restore, deckId, deckRevisionService, ... |
| `src/main/java/com/deckassemble/decks/api/history/DeckRevisionDiffResponse.java` | from, diff |
| `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java` | deckId, owned |
| `src/main/java/com/deckassemble/decks/application/DeckCardService.java` | deckId, listCards |
| `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonDiffer.java` | base, change, identity, other, added, ... |
| `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonService.java` | DeckComparisonService, otherDeckId, DeckComparisonService.<init>, deckAnalysisService, deckCardService, ... |
| `src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java` | name, section, shouldFallBackToPrintingIdentityWhenCardSummaryMissing, oracleId, shouldReportAddedRemovedAndQuantityChangedCards, ... |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java::DeckComparisonServiceTest.shouldComputeLegalityGameChangerAndComboDeltas`
- `src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java::DeckComparisonServiceTest.shouldComputeMetricDeltasAsOtherMinusBase`

## Connected Communities

- **decks/application +18 dirs** (6 cross-edges)
- **application/analysis +5 dirs** (5 cross-edges)
- **api/organization +19 dirs** (5 cross-edges)
- **application/upgrades +2 dirs** (2 cross-edges)
- **decks/application +13 dirs** (2 cross-edges)
- **deckassemble/decks · of** (2 cross-edges)
- **application/importing +3 dirs** (1 cross-edges)
- **application/comparison** (1 cross-edges)
- **application/history +1 dirs** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-136"
smart_context with task: "understand api/history +5 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java::DeckComparisonServiceTest.shouldComputeLegalityGameChangerAndComboDeltas", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
