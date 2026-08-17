---
name: gortex-recommendations-application-5-dirs
description: "Work in the recommendations/application +5 dirs area — 137 symbols across 11 files (81% cohesion)"
---

# recommendations/application +5 dirs

137 symbols | 11 files | 81% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/domain/Card.java`
- `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java`
- `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java`
- `src/main/java/com/deckassemble/recommendations/application/CardCategorizer.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestion.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckCandidate.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckDraftPicker.java`
- `src/test/java/com/deckassemble/recommendations/application/DeckDraftPickerTest.java`
- `src/test/java/com/deckassemble/recommendations/application/PlayStyleQuotasTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/domain/Card.java` | getCommanderRank |
| `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java` | from, rows |
| `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java` | rows, from |
| `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java` | size |
| `src/main/java/com/deckassemble/recommendations/application/CardCategorizer.java` | SYNERGY, LAND, category, WIPE, toCategory, ... |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestion.java` | withExplanations, newExplanations |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java` | fetchedAt, missing, totalCards, response, prices, ... |
| `src/main/java/com/deckassemble/recommendations/application/DeckCandidate.java` | scoreValue, DeckCandidate.<init>, totalScore |
| `src/main/java/com/deckassemble/recommendations/application/DeckDraftPicker.java` | CURVE_WEIGHT, curve, pick, CURVE_SOFT_CAPS, pickedOracles, ... |
| `src/test/java/com/deckassemble/recommendations/application/DeckDraftPickerTest.java` | name, shouldPreserveScoreExplanationsWhenPicking, shouldReserveSlotsForBasicLandsWhenCandidateLandsFallShort, category, synergy, ... |
| `src/test/java/com/deckassemble/recommendations/application/PlayStyleQuotasTest.java` | shouldBoostInteractionForControl, shouldBoostFinishersForAggro |

## Connected Communities

- **cards/application +3 dirs** (5 cross-edges)
- **com/deckassemble · add · Card** (4 cross-edges)
- **recommendations/application +7 dirs** (4 cross-edges)
- **api/organization +19 dirs** (3 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (2 cross-edges)
- **recommendations/application +1 dirs · forStyle** (2 cross-edges)
- **cards/application +10 dirs** (2 cross-edges)
- **application/upgrades +2 dirs** (1 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)
- **com/deckassemble · getColorIdentity** (1 cross-edges)
- **recommendations/application · usd** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-157"
smart_context with task: "understand recommendations/application +5 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
