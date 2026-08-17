---
name: gortex-com-deckassemble-add-card
description: "Work in the com/deckassemble · add · Card area — 107 symbols across 9 files (71% cohesion)"
---

# com/deckassemble · add · Card

107 symbols | 9 files | 71% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/cards/domain/Card.java`
- `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java`
- `src/main/java/com/deckassemble/recommendations/application/CandidateScoreExplainer.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderResolver.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java`
- `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckCandidate.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckCandidateSelector.java`
- `src/main/java/com/deckassemble/recommendations/application/DeckDraftPicker.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/cards/domain/Card.java` | getScryfallOracleId |
| `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java` | target, excludedOracles, commanders |
| `src/main/java/com/deckassemble/recommendations/application/CandidateScoreExplainer.java` | budget, score, comboLists, owned, score, ... |
| `src/main/java/com/deckassemble/recommendations/application/CommanderResolver.java` | requireEligible, card |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionExplainer.java` | colorSupport, coverage, fetchedAt, freshness, CommanderSuggestionExplainer.<init>, ... |
| `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestionService.java` | scores, cardsByName, ownedOracleIds, missingCards, printingIds |
| `src/main/java/com/deckassemble/recommendations/application/DeckCandidate.java` | contribution, commanderOracles, isEligible, withContribution, identity, ... |
| `src/main/java/com/deckassemble/recommendations/application/DeckCandidateSelector.java` | edhrecCommanderService, withinBudget, ownedPrintingIds, collectCandidates, collectOptimalCandidates, ... |
| `src/main/java/com/deckassemble/recommendations/application/DeckDraftPicker.java` | add, draft, quotas, candidate |

## Connected Communities

- **recommendations/application +5 dirs** (7 cross-edges)
- **recommendations/application +7 dirs** (6 cross-edges)
- **cards/application +3 dirs** (6 cross-edges)
- **api/organization +19 dirs** (5 cross-edges)
- **com/deckassemble · getColorIdentity** (2 cross-edges)
- **recommendations/application +1 dirs · card** (2 cross-edges)
- **com/deckassemble · withinGameChangerLimit** (1 cross-edges)
- **application/upgrades +2 dirs** (1 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (1 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **com/deckassemble · has** (1 cross-edges)
- **recommendations/application · usd** (1 cross-edges)
- **application/analysis +5 dirs** (1 cross-edges)
- **recommendations/application +1 dirs · forStyle** (1 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)
- **com/deckassemble · add · CardSearchPredicates** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-84"
smart_context with task: "understand com/deckassemble · add · Card", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
