# M1 Interoperability and Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users safely migrate decks and collections into and out of DeckAssemble and obtain a complete, actionable deck analysis.

**Architecture:** Introduce a canonical import-row model shared only inside each owning module, with format adapters at the API boundary and existing card/printing repositories as resolvers. Imports use preview tokens and idempotent commit commands; exports stream deterministic text/CSV. Deck analysis is a read-only application service composed from existing deck, card, price, legality, and combo data.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring MVC multipart/streaming responses, Spring Data JPA, PostgreSQL, Liquibase, JUnit 5, Mockito, Testcontainers.

## Global Constraints

- Follow the portfolio constraints in `docs/plans/2026-08-04-product-enhancements-roadmap.md`.
- Supported deck formats: DeckAssemble text, generic CSV, Moxfield CSV, Archidekt CSV, Arena text, MTGO text.
- Collection ingestion is CSV only in this milestone; exact Scryfall ID wins, then `(name, set code, collector number)`, then an explicit ambiguous/unmatched result.
- Import preview must not write collection or deck cards.
- Commit requests require `Idempotency-Key`; a repeated key returns the original result.
- All money values use `BigDecimal` and explicit currency codes.

---

## File Map

- `com.deckassemble.imports` remains card-catalog import status only; user imports live in the owning `decks` and `collections` modules.
- `decks/api/importing/*`: deck upload/preview/commit/export contracts and controller.
- `decks/application/importing/*`: parsing orchestration, preview storage, idempotent commit, exporters.
- `collections/api/importing/*` and `collections/application/importing/*`: equivalent collection workflow.
- `cards/application/CardReferenceResolver.java`: the single cross-workflow card/printing resolution service.
- `decks/application/analysis/*`: read-only deck analysis calculators and response assembler.
- `012-interoperability-analytics.yaml`: import previews, idempotency results, and supporting indexes.

### Task 1: Resolve External Card References

**Files:**
- Create: `src/main/java/com/deckassemble/cards/application/CardReference.java`
- Create: `src/main/java/com/deckassemble/cards/application/CardReferenceResolution.java`
- Create: `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java`
- Modify: `src/main/java/com/deckassemble/cards/domain/CardRepository.java`
- Modify: `src/main/java/com/deckassemble/cards/domain/CardPrintingRepository.java`
- Test: `src/test/java/com/deckassemble/cards/application/CardReferenceResolverTest.java`

**Interfaces:**
```java
public record CardReference(UUID scryfallId, String name, String setCode, String collectorNumber) {}
public sealed interface CardReferenceResolution {
    record Matched(Long cardId, Long printingId) implements CardReferenceResolution {}
    record Ambiguous(List<Long> printingIds) implements CardReferenceResolution {}
    record Unmatched() implements CardReferenceResolution {}
}
public CardReferenceResolution resolve(CardReference reference);
```

- [ ] Write tests for exact Scryfall ID, exact name/set/collector number, ambiguous name-only, and unmatched input.
- [ ] Run `./gradlew test --tests '*CardReferenceResolverTest'`; expected: compilation or assertion failure because the resolver does not exist.
- [ ] Implement repository queries and the resolver using exact normalized comparisons; do not add fuzzy matching.
- [ ] Run the focused test; expected: PASS.
- [ ] Run `./gradlew check`; expected: exit `0`.
- [ ] Commit implementation and test together: `feat(cards): resolve external card references`.

### Task 2: Add Import Persistence and Idempotency

**Files:**
- Create: `src/main/resources/db/changelog/releases/012-interoperability-analytics.yaml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/DeckImportPreview.java`
- Create: `src/main/java/com/deckassemble/decks/domain/DeckImportPreviewRepository.java`
- Create: `src/main/java/com/deckassemble/collections/domain/CollectionImportPreview.java`
- Create: `src/main/java/com/deckassemble/collections/domain/CollectionImportPreviewRepository.java`
- Test: `src/test/java/com/deckassemble/MigrationIntegrationTest.java`

**Produces:** preview records keyed by token/profile with expiry, source SHA-256, canonical JSON rows, status, idempotency key, and committed resource/result IDs.

- [ ] Add a migration integration assertion that Liquibase reaches release `012` and creates `deck_import_previews` and `collection_import_previews` with unique `(profile_id, idempotency_key)` constraints.
- [ ] Run `./gradlew test --tests '*MigrationIntegrationTest'`; expected: FAIL because release `012` is absent.
- [ ] Add the two tables, expiry/status indexes, and master include; map focused entities/repositories.
- [ ] Run the migration test and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(imports): persist idempotent import previews`.

### Task 3: Preview Deck Imports

**Files:**
- Create: `src/main/java/com/deckassemble/decks/api/importing/DeckImportController.java`
- Create: `src/main/java/com/deckassemble/decks/api/importing/DeckImportFormat.java`
- Create: `src/main/java/com/deckassemble/decks/api/importing/DeckImportPreviewResponse.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/DeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/DeckAssembleTextDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/GenericCsvDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/MoxfieldCsvDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/ArchidektCsvDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/ArenaTextDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/MtgoTextDeckImportParser.java`
- Create: `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- Test: `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckImportControllerIntegrationTest.java`
- Fixture: `src/test/resources/fixtures/deck-imports/deckassemble.txt`
- Fixture: `src/test/resources/fixtures/deck-imports/generic.csv`
- Fixture: `src/test/resources/fixtures/deck-imports/moxfield.csv`
- Fixture: `src/test/resources/fixtures/deck-imports/archidekt.csv`
- Fixture: `src/test/resources/fixtures/deck-imports/arena.txt`
- Fixture: `src/test/resources/fixtures/deck-imports/mtgo.txt`

**API:** `POST /decks/imports/preview?format={format}` with multipart file; returns token, detected metadata, resolved rows, ambiguous rows, unmatched rows, invalid rows, and totals.

- [ ] Add one fixture and parser test per supported format, including commander/main/sideboard/maybeboard mapping and quantity parsing.
- [ ] Run focused tests; expected: FAIL because parsers/endpoints are absent.
- [ ] Implement the smallest format-specific parsers behind `DeckImportParser`, resolve references, persist a 30-minute preview, and enforce file-size/row-count limits.
- [ ] Add integration cases for valid preview, unsupported format, oversized file, and no database deck/card mutation.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): preview external deck imports`.

### Task 4: Commit Deck Imports Idempotently

**Files:**
- Create: `src/main/java/com/deckassemble/decks/api/importing/CommitDeckImportRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/importing/DeckImportResultResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/api/importing/DeckImportController.java`
- Modify: `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- Test: `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckImportControllerIntegrationTest.java`

**API:** `POST /decks/imports` with `Idempotency-Key` and `{ "previewToken": UUID, "name": String }`; returns the created deck plus imported/skipped counts.

- [ ] Write tests proving unresolved rows block commit unless explicitly excluded, expired/foreign previews return 404, and duplicate idempotency keys create one deck.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement a transaction that validates ownership/status, creates the deck through existing services, adds selected resolved cards, and stores the result before returning.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): commit deck imports safely`.

### Task 5: Export Decks

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/exporting/DeckExportFormat.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/DeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/DeckAssembleTextDeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/GenericCsvDeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/MoxfieldCsvDeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/ArchidektCsvDeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/ArenaTextDeckExporter.java`
- Create: `src/main/java/com/deckassemble/decks/application/exporting/MtgoTextDeckExporter.java`
- Modify: `src/main/java/com/deckassemble/decks/api/DeckController.java`
- Test: `src/test/java/com/deckassemble/decks/application/exporting/DeckExporterTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`

**API:** `GET /decks/{deckId}/exports?format={format}`; returns deterministic attachment content.

- [ ] Write golden-file tests for all six formats, section ordering, quantities, flavor names, and exact printing identifiers where the target supports them.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement exporters with deterministic section/card sorting and content disposition.
- [ ] Add owner-isolation and unsupported-format integration tests.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): export decks to common formats`.

### Task 6: Preview and Commit Collection CSV Imports

**Files:**
- Create: `src/main/java/com/deckassemble/collections/api/importing/CollectionImportController.java`
- Create: `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreset.java`
- Create: `src/main/java/com/deckassemble/collections/api/importing/CollectionColumnMapping.java`
- Create: `src/main/java/com/deckassemble/collections/api/importing/CollectionImportPreviewResponse.java`
- Create: `src/main/java/com/deckassemble/collections/api/importing/CommitCollectionImportRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/importing/CollectionImportResultResponse.java`
- Create: `src/main/java/com/deckassemble/collections/application/importing/CollectionCsvParser.java`
- Create: `src/main/java/com/deckassemble/collections/application/importing/CollectionImportErrorExporter.java`
- Create: `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java`
- Test: `src/test/java/com/deckassemble/collections/application/importing/CollectionImportServiceTest.java`
- Test: `src/test/java/com/deckassemble/collections/api/CollectionImportControllerIntegrationTest.java`
- Fixture: `src/test/resources/fixtures/collection-imports/deckassemble.csv`
- Fixture: `src/test/resources/fixtures/collection-imports/moxfield.csv`
- Fixture: `src/test/resources/fixtures/collection-imports/archidekt.csv`
- Fixture: `src/test/resources/fixtures/collection-imports/manabox.csv`
- Fixture: `src/test/resources/fixtures/collection-imports/generic.csv`

**APIs:**
- `POST /collections/imports/preview` accepts CSV plus a column map/preset.
- `POST /collections/imports` commits selected rows with `Idempotency-Key`.
- `GET /collections/imports/{token}/errors` downloads rejected rows with reason codes.

- [ ] Test DeckAssemble, Moxfield, Archidekt, ManaBox, and generic column-map fixtures; include duplicate rows, malformed quantities, ambiguity, and UTF-8 names.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement streaming CSV parsing with JDK facilities or already-installed CSV support only; aggregate duplicate exact printings in the preview.
- [ ] Commit through existing collection services in one transaction, preserving row failures and retry result.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): import collections from CSV`.

### Task 7: Export Collections

**Files:**
- Create: `src/main/java/com/deckassemble/collections/application/exporting/CollectionCsvExporter.java`
- Modify: `src/main/java/com/deckassemble/collections/api/CollectionController.java`
- Test: `src/test/java/com/deckassemble/collections/application/exporting/CollectionCsvExporterTest.java`
- Test: `src/test/java/com/deckassemble/collections/api/CollectionControllerIntegrationTest.java`

**API:** `GET /collections/{collectionId}/export`; columns: Scryfall ID, card name, set, collector number, quantity, printing ID.

- [ ] Write deterministic CSV escaping/order tests and an owner-isolation integration test.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement streaming CSV export and attachment headers.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): export collection CSV files`.

### Task 8: Add Deck Analysis

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisService.java`
- Create: `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisResponse.java`
- Create: `src/main/java/com/deckassemble/decks/application/analysis/DeckCompositionCalculator.java`
- Create: `src/main/java/com/deckassemble/decks/application/analysis/ManaCurveCalculator.java`
- Create: `src/main/java/com/deckassemble/decks/application/analysis/ManaProductionCalculator.java`
- Create: `src/main/java/com/deckassemble/decks/application/analysis/DeckValueCalculator.java`
- Modify: `src/main/java/com/deckassemble/decks/api/DeckController.java`
- Test: `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`

**API:** `GET /decks/{deckId}/analysis` returns curve, type distribution, color demand/production, land count, average mana value, ownership breakdown, value by currency, missing cost, functional categories, tokens, game changers, legality summary, and combo summary.

- [ ] Write Given-When-Then tests for split cards, X costs, lands, missing prices, multiple quantities, proxies/wishlist, and empty decks.
- [ ] Run focused tests; expected: FAIL.
- [ ] Compose existing legality/combo/price data and focused pure calculators; keep each calculator independently unit tested.
- [ ] Add endpoint authorization and response-shape integration tests.
- [ ] Run `./gradlew test` and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): expose actionable deck analysis`.

## Milestone Manual QA

1. Start PostgreSQL-backed application with `./gradlew bootRun`.
2. Preview and commit one Moxfield deck; retry with the same idempotency key and confirm the same deck ID.
3. Preview a collection CSV containing one exact, one ambiguous, and one invalid row; confirm no mutation before commit and download the error CSV.
4. Export the resulting deck and collection, then re-preview the exports.
5. Call `/decks/{id}/analysis` and reconcile totals against deck cards, prices, legality, and combos.
6. Repeat owner-scoped calls as a second user; expect 404/403 according to existing policy.

## Completion Gate

```bash
./gradlew test
./gradlew check
```

Expected: exit `0`; migration `012` is included exactly once; no generated file or fixture leaks into production artifacts.
