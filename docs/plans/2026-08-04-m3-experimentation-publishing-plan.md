# M3 Experimentation and Publishing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give users safe deck experimentation, reproducible statistical playtesting, and durable published deck pages.

**Architecture:** Capture meaningful deck mutations as append-only revisions with immutable card snapshots. Simulations consume a revision snapshot and a supplied/generated seed, never live mutable rows. Publishing exposes explicit private/unlisted/public visibility, stable slugs, Markdown primers, forks, and immutable published revision references.

**Tech Stack:** Existing Java/Spring/PostgreSQL stack, Liquibase release `014`, `java.util.random`, CommonMark only if already installed or after a separately approved dependency check; otherwise store Markdown and return source.

## Global Constraints

- Require M2 complete and follow portfolio constraints.
- History records user-visible mutations only: deck metadata, commander, card, category, tag, folder, and restore changes.
- Restore creates a new revision; it never deletes history.
- Simulation is statistical goldfishing, not a Magic rules engine.
- Every simulation accepts a seed and returns it; same revision/config/seed must produce the same result.
- Private decks never resolve through public slugs; unlisted decks resolve only by exact slug; public decks may appear in discovery later.
- Published deck pages pin an immutable revision.

---

## File Map

- `decks/domain/history/*`, `application/history/*`, `api/history/*`: revisions, changes, snapshots, restore.
- `decks/application/simulation/*`, `api/simulation/*`: sample hands and Monte Carlo summaries.
- `decks/domain/publishing/*`, `application/publishing/*`, `api/publishing/*`: visibility, slugs, primers, published revisions, forks.
- `014-experimentation-publishing.yaml`: history and publishing persistence.

### Task 1: Add Revision Persistence

**Files:**
- Create: `src/main/resources/db/changelog/releases/014-experimentation-publishing.yaml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/history/DeckRevision.java`
- Create: `src/main/java/com/deckassemble/decks/domain/history/DeckRevisionRepository.java`
- Create: `src/main/java/com/deckassemble/decks/domain/history/DeckChangeType.java`
- Create: `src/main/java/com/deckassemble/decks/application/history/DeckSnapshot.java`
- Test: `src/test/java/com/deckassemble/MigrationIntegrationTest.java`
- Create test: `src/test/java/com/deckassemble/decks/domain/history/DeckRevisionRepositoryIntegrationTest.java`

**Schema:** `deck_revisions` stores deck/profile IDs, sequential revision number, base revision, change type, actor, timestamp, metadata JSON, and complete canonical deck snapshot JSON; unique `(deck_id, revision_number)`.

- [ ] Add failing migration/entity tests for sequence uniqueness, immutable snapshot load, and profile/deck indexes.
- [ ] Run focused tests; expected: FAIL.
- [ ] Add release `014`, master include, entities, and repository methods.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): persist immutable deck revisions`.

### Task 2: Record Meaningful Deck Changes

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/history/DeckRevisionService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckCardService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckCategoryService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/importing/DeckImportService.java`
- Create test: `src/test/java/com/deckassemble/decks/application/history/DeckRevisionServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/organization/DeckCategoryServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/importing/DeckImportServiceTest.java`.

**Interface:** `DeckRevision recordRevision(UUID deckId, DeckChangeType type, UUID actorProfileId)` after successful mutation, in the same transaction.

- [ ] Write tests for create, metadata, commander, card add/update/remove, category/tag/folder, import, and no-op commands.
- [ ] Run focused tests; expected: FAIL.
- [ ] Snapshot canonical ordered state after successful meaningful changes; no revision for reads or idempotent no-ops.
- [ ] Run all deck mutation tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): record meaningful deck history`.

### Task 3: List, Diff, and Restore Revisions

**Files:**
- Create: `src/main/java/com/deckassemble/decks/api/history/DeckHistoryController.java`
- Create: `src/main/java/com/deckassemble/decks/api/history/DeckRevisionResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/history/DeckRevisionDiffResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/history/RestoreDeckRevisionRequest.java`
- Create: `src/main/java/com/deckassemble/decks/application/history/DeckRevisionDiffService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/history/DeckRevisionService.java`
- Create test: `src/test/java/com/deckassemble/decks/application/history/DeckRevisionDiffServiceTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/history/DeckHistoryControllerIntegrationTest.java`.

**APIs:**
- `GET /decks/{deckId}/revisions`
- `GET /decks/{deckId}/revisions/{revisionNumber}`
- `GET /decks/{deckId}/revisions/{revisionNumber}/diff/{otherRevisionNumber}`
- `POST /decks/{deckId}/revisions/{revisionNumber}/restore` with expected current revision.

- [ ] Test metadata/card/category diffs, pagination, foreign access, stale expected revision (409), and restore producing a new revision.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement canonical snapshot diff and transactional restore through existing mutation primitives.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): inspect and restore deck revisions`.

### Task 4: Generate Seeded Sample Hands

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSampleHandRequest.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSampleHandResponse.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSampleHandService.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/MulliganStrategy.java`
- Create: `src/main/java/com/deckassemble/decks/api/simulation/DeckSimulationController.java`
- Create test: `src/test/java/com/deckassemble/decks/application/simulation/DeckSampleHandServiceTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/simulation/DeckSimulationControllerIntegrationTest.java`.

**API:** `POST /decks/{deckId}/sample-hands` with revision, hand count 1-100, hand size 7, mulligan strategy `NONE|LONDON_LAND_RANGE`, minimum/maximum lands, seed.

- [ ] Write tests for deterministic shuffle, quantities, commanders excluded from library, London mulligan bottoming, invalid deck size, and exact same seed output.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement with `RandomGeneratorFactory` and revision snapshots; return seed, hands, mulligan counts, card IDs/names.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): generate reproducible sample hands`.

### Task 5: Add Monte Carlo Deck Statistics

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSimulationRequest.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSimulationResponse.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/DeckSimulationService.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/LandDropCalculator.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/ColorAvailabilityCalculator.java`
- Create: `src/main/java/com/deckassemble/decks/application/simulation/CastabilityCalculator.java`
- Modify: `src/main/java/com/deckassemble/decks/api/simulation/DeckSimulationController.java`
- Create test: `src/test/java/com/deckassemble/decks/application/simulation/DeckSimulationServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/simulation/DeckSimulationControllerIntegrationTest.java`.

**API:** `POST /decks/{deckId}/simulations` with revision, iterations 100-100000, turns 1-10, on-the-play flag, mulligan strategy, seed.

**Output:** land-drop probability by turn, color availability, cards seen, mana-value castability proxy, playable-spell count by turn, confidence metadata; no card-text execution.

- [ ] Test deterministic aggregates, iteration bounds, land/color production, zero-source colors, and stable percentages within exact seeded expectations.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement pure bounded simulation using parsed mana cost/color-production facts from M1 analysis.
- [ ] Add timeout/performance test for the maximum allowed workload and reject excessive input.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): simulate deck consistency statistics`.

### Task 6: Add Visibility and Stable Share Slugs

**Files:**
- Modify: `src/main/resources/db/changelog/releases/014-experimentation-publishing.yaml`
- Modify: `src/main/java/com/deckassemble/decks/domain/Deck.java`
- Create: `src/main/java/com/deckassemble/decks/domain/publishing/DeckVisibility.java`
- Create: `src/main/java/com/deckassemble/decks/application/publishing/DeckVisibilityPolicy.java`
- Create: `src/main/java/com/deckassemble/decks/application/publishing/DeckPublishingService.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/DeckPublishingRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/SharedDeckResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/DeckPublishingController.java`
- Modify: `src/main/java/com/deckassemble/authentication/infrastructure/config/SecurityConfig.java`
- Create test: `src/test/java/com/deckassemble/decks/application/publishing/DeckVisibilityPolicyTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`.

**Model:** `PRIVATE|UNLISTED|PUBLIC`, unique random slug, current published revision number, published timestamp.

**APIs:** `PATCH /decks/{deckId}/publishing`; `GET /shared/decks/{slug}` permits anonymous access according to visibility.

- [ ] Test owner/collaborator/authenticated stranger/anonymous access for every visibility value, slug collision retry, archive behavior, and no sequential identifiers.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement centralized `DeckVisibilityPolicy` and cryptographically random URL-safe slugs.
- [ ] Run security/integration tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): publish decks with controlled visibility`.

### Task 7: Add Markdown Primers

**Files:**
- Modify: `src/main/resources/db/changelog/releases/014-experimentation-publishing.yaml`
- Create: `src/main/java/com/deckassemble/decks/application/publishing/DeckPrimerService.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/DeckPrimerRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/DeckPrimerResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/api/publishing/DeckPublishingController.java`
- Create test: `src/test/java/com/deckassemble/decks/application/publishing/DeckPrimerServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`.

**API:** `PUT /decks/{deckId}/primer` with Markdown source and title; shared response includes source and sanitized/rendered HTML only if a vetted renderer already exists.

- [ ] Test size limits, UTF-8, dangerous raw HTML handling, update authorization, and pinned revision display.
- [ ] Run focused tests; expected: FAIL.
- [ ] Store Markdown source; disable/raw-escape HTML unless an approved sanitizer is present. Do not build a custom sanitizer.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): add Markdown deck primers`.

### Task 8: Publish Immutable Revisions and Fork Decks

**Files:**
- Modify: `src/main/resources/db/changelog/releases/014-experimentation-publishing.yaml`
- Modify: `src/main/java/com/deckassemble/decks/application/publishing/DeckPublishingService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/history/DeckRevisionService.java`
- Modify: `src/main/java/com/deckassemble/decks/api/publishing/SharedDeckResponse.java`
- Create: `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java`
- Create: `src/main/java/com/deckassemble/decks/api/publishing/DeckForkResponse.java`
- Create test: `src/test/java/com/deckassemble/decks/application/publishing/DeckPublishingServiceTest.java`
- Create test: `src/test/java/com/deckassemble/decks/application/publishing/DeckForkServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`.

**APIs:**
- `POST /decks/{deckId}/publish` pins the current revision.
- `POST /shared/decks/{slug}/fork` creates a private deck owned by caller and records source deck/revision.

- [ ] Test that later private edits do not change the shared representation until republished, forks copy the pinned snapshot, source attribution survives source deletion/privacy change, and private decks cannot be forked.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement publish pinning and fork creation through existing deck/card/category services.
- [ ] Run `./gradlew test` and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): publish revisions and fork shared decks`.

## Milestone Manual QA

- Make several deck mutations, inspect diffs, restore an old revision, and verify a new revision is created.
- Generate sample hands twice with the same seed and compare byte-equivalent outputs.
- Run a seeded simulation and reconcile land/color results with a controlled fixture deck.
- Exercise the full visibility matrix from anonymous and two authenticated users.
- Publish, edit privately, verify shared content remains pinned, republish, and fork.
- Submit primer content containing raw script/HTML and confirm it is not executable output.

## Completion Gate

```bash
./gradlew test
./gradlew check
```

Expected: exit `0`; release `014` follows `013`; seeded tests are deterministic across repeated local runs.
