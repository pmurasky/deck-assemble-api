---
name: gortex-application-upgrades-2-dirs
description: "Work in the application/upgrades +2 dirs area — 92 symbols across 3 files (94% cohesion)"
---

# application/upgrades +2 dirs

92 symbols | 3 files | 94% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java`
- `src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java` | substitutions, currency, CLOSE_CATEGORY_GAPS, budget, Objective, ... |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java` | cost, suggestion |
| `src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java` | missing, shouldOnlyTargetMainDeckCards, alternatives, summary, usd, ... |

## Entry Points

- `src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java::DeckUpgradeServiceTest.shouldReplaceProxyWithOwnedAlternative`
- `src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java::DeckUpgradeServiceTest.shouldImproveWithinBudgetCeiling`

## Connected Communities

- **application/analysis +5 dirs** (10 cross-edges)
- **api/history +5 dirs** (2 cross-edges)
- **application/upgrades +1 dirs** (2 cross-edges)
- **cards/application +3 dirs** (1 cross-edges)
- **api/organization +19 dirs** (1 cross-edges)
- **decks/application · afterMetrics** (1 cross-edges)
- **application/alternatives +1 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-149"
smart_context with task: "understand application/upgrades +2 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java::DeckUpgradeServiceTest.shouldReplaceProxyWithOwnedAlternative", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
