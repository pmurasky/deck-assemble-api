---
name: gortex-imports-application-7-dirs
description: "Work in the imports/application +7 dirs area — 102 symbols across 11 files (87% cohesion)"
---

# imports/application +7 dirs

102 symbols | 11 files | 87% cohesion

## When to Use

Use this skill when working on files in:
- `src/main/java/com/deckassemble/administration/api/CardImportController.java`
- `src/main/java/com/deckassemble/imports/api/ImportStatusController.java`
- `src/main/java/com/deckassemble/imports/application/ImportRunRecorder.java`
- `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java`
- `src/main/java/com/deckassemble/imports/domain/CardImportRun.java`
- `src/main/java/com/deckassemble/imports/domain/CardImportRunRepository.java`
- `src/main/java/com/deckassemble/recommendations/application/ManaBaseCheck.java`
- `src/test/java/com/deckassemble/administration/api/CardImportControllerSecurityTest.java`
- `src/test/java/com/deckassemble/cards/api/CardControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/imports/application/ImportRunRecorderTest.java`
- `src/test/java/com/deckassemble/imports/application/OracleTagImportServiceTest.java`

## Key Files

| File | Symbols |
|------|---------|
| `src/main/java/com/deckassemble/administration/api/CardImportController.java` | from, run |
| `src/main/java/com/deckassemble/imports/api/ImportStatusController.java` | from, latest, ImportStatusController, importRunRecorder, run, ... |
| `src/main/java/com/deckassemble/imports/application/ImportRunRecorder.java` | complete, repository, ImportRunRecorder, runId, run, ... |
| `src/main/java/com/deckassemble/imports/application/OracleTagImportService.java` | importTags, runId |
| `src/main/java/com/deckassemble/imports/domain/CardImportRun.java` | CardImportRun.<init>, recordsCreated, getCompletedAt, FAILED, recordsFailed, ... |
| `src/main/java/com/deckassemble/imports/domain/CardImportRunRepository.java` | findTopByStatusOrderByCompletedAtDesc, findTop20ByOrderByStartedAtDesc, status, CardImportRunRepository |
| `src/main/java/com/deckassemble/recommendations/application/ManaBaseCheck.java` | card, actual, countProducedSources |
| `src/test/java/com/deckassemble/administration/api/CardImportControllerSecurityTest.java` | shouldReturnHistoryForAdministrators |
| `src/test/java/com/deckassemble/cards/api/CardControllerIntegrationTest.java` | shouldReturnLatestImportRun |
| `src/test/java/com/deckassemble/imports/application/ImportRunRecorderTest.java` | shouldReturnLatestCompleted, shouldStartRunAndReturnId, repository, ImportRunRecorderTest, shouldFailRunWithTruncatedErrorSummary, ... |
| `src/test/java/com/deckassemble/imports/application/OracleTagImportServiceTest.java` | shouldRecordRunFailureWhenFetchFails |

## Connected Communities

- **cards/domain +16 dirs** (8 cross-edges)
- **imports/application +4 dirs** (4 cross-edges)
- **com/deckassemble · has** (2 cross-edges)
- **recommendations/application +2 dirs · EdhrecCommanderService** (2 cross-edges)
- **cards/application +3 dirs** (2 cross-edges)
- **com/deckassemble · name** (1 cross-edges)
- **api/importing +8 dirs** (1 cross-edges)

## How to Explore

```
get_communities with id: "community-78"
smart_context with task: "understand imports/application +7 dirs", format: "gcx"
```

_`format: "gcx"` returns the [GCX1 compact wire format](../../docs/wire-format.md) — round-trippable, ~27% fewer tokens than JSON. Drop it for JSON output; agents using `@gortex/wire` or the Go `github.com/gortexhq/gcx-go` package decode either._
