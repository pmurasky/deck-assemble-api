---
name: gortex-application-simulation-2-dirs-request
description: "Work in the application/simulation +2 dirs · request area — 77 symbols across 4 files (87% cohesion)"
---

# application/simulation +2 dirs · request

77 symbols | 4 files | 87% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/api/simulation/DeckSimulationController.java`
- `src/main/java/com/deckassemble/decks/application/simulation/DeckSampleHandService.java`
- `src/main/java/com/deckassemble/decks/application/simulation/MulliganStrategy.java`
- `src/test/java/com/deckassemble/decks/application/simulation/DeckSampleHandServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/api/simulation/DeckSimulationController.java` | request, generate, deckId |
| `src/main/java/com/deckassemble/decks/application/simulation/DeckSampleHandService.java` | mulliganCount, random, toHand, generate, deckId, ... |
| `src/main/java/com/deckassemble/decks/application/simulation/MulliganStrategy.java` | MulliganStrategy, NONE, LONDON_LAND_RANGE |
| `src/test/java/com/deckassemble/decks/application/simulation/DeckSampleHandServiceTest.java` | nonlandCards, secondaryCommanderCardId, cardsFor, snapshot, secondaryCommanderCardId, ... |

## Connected Communities

- **api/organization +19 dirs** (6 cross-edges)
- **application/simulation · resolveLibrary** (3 cross-edges)
- **application/history +1 dirs** (2 cross-edges)
- **recommendations/application +7 dirs** (2 cross-edges)
- **application/simulation · draw** (2 cross-edges)
- **decks/application +13 dirs** (1 cross-edges)
- **cards/domain +1 dirs · CardSearchPredicatesTest** (1 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-147"
smart_context with task: "understand application/simulation +2 dirs · request", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
