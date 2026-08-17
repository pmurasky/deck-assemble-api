---
name: gortex-decks-domain-6-dirs
description: "Work in the decks/domain +6 dirs area — 80 symbols across 7 files (45% cohesion)"
---

# decks/domain +6 dirs

80 symbols | 7 files | 45% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/decks/domain/Deck.java`
- `src/main/java/com/deckassemble/decks/domain/publishing/DeckVisibility.java`
- `src/test/java/com/deckassemble/DeckAssembleApplicationTests.java`
- `src/test/java/com/deckassemble/administration/api/CardImportControllerSecurityTest.java`
- `src/test/java/com/deckassemble/cards/application/CardExportViewTest.java`
- `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/users/api/ProfileControllerIntegrationTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/decks/domain/Deck.java` | getSourceDeckId |
| `src/main/java/com/deckassemble/decks/domain/publishing/DeckVisibility.java` | PRIVATE, DeckVisibility, PUBLIC, UNLISTED |
| `src/test/java/com/deckassemble/DeckAssembleApplicationTests.java` | postgres, contextLoads, DeckAssembleApplicationTests |
| `src/test/java/com/deckassemble/administration/api/CardImportControllerSecurityTest.java` | ADMIN, mockMvc, shouldForbidHistoryForNonAdministrators, CardImportControllerSecurityTest, cardImportTrigger, ... |
| `src/test/java/com/deckassemble/cards/application/CardExportViewTest.java` | Test |
| `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java` | shouldRejectPrimerMarkdownSourceExceedingTheSizeLimit, printingRepository, deckId, rename, slug, ... |
| `src/test/java/com/deckassemble/users/api/ProfileControllerIntegrationTest.java` | shouldCreateAndReturnCurrentProfile, shouldRejectUnauthenticatedRequests, mockMvc, ProfileControllerIntegrationTest, shouldUpdateCurrentProfile |

## Entry Points

- `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java::DeckPublishingControllerIntegrationTest.shouldForkAPublishedDeckCopyingThePinnedSnapshotContent`

## Connected Communities

- **decks/application +13 dirs** (1 cross-edges)
- **cards/domain +16 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-124"
smart_context with task: "understand decks/domain +6 dirs", format: "gcx"
find_usages with id: "src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java::DeckPublishingControllerIntegrationTest.shouldForkAPublishedDeckCopyingThePinnedSnapshotContent", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
