# DeckAssemble Product Enhancements Portfolio Roadmap

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this roadmap milestone-by-milestone. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn DeckAssemble into the strongest collection-aware Commander platform while adding the interoperability, analysis, experimentation, publishing, community, and physical-card workflows expected from mature deck applications.

**Architecture:** Preserve the existing Spring modular monolith and its `api / application / domain / infrastructure` package boundaries. Deliver vertical slices behind authenticated `/api/v1` endpoints, use plain UUID foreign keys between modules, and add persistence only when a user-visible workflow needs it. Each milestone is independently deployable and has its own executable plan.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Web, Spring Data JPA, PostgreSQL, Liquibase, Bean Validation, OAuth2 Resource Server/Auth0, JUnit 5, Mockito, Testcontainers, ArchUnit, JaCoCo, PIT, PMD, CPD, Checkstyle, SpotBugs, Error Prone, NullAway.

## Global Constraints

- Backend only; UI, camera image recognition, and native mobile implementation are out of repository scope.
- Follow STOP -> RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT; every logical change is one production-ready commit.
- Run `./gradlew test` before every commit and `./gradlew check` at each task checkpoint.
- Maintain at least 80% unit-test coverage for changed code and 100% for recommendation, pricing, legality, import matching, simulation, and authorization decisions.
- Keep methods to 15-20 lines, classes below 300 lines, no more than five parameters, and no speculative abstractions.
- Use constructor injection and existing persistence identifiers between modules (`Long` card/printing IDs where already established); cross-module contracts must use immutable DTOs/value objects, and no cross-module JPA relationships.
- Public APIs remain under `/api/v1`; breaking changes require a separate deprecation plan.
- Import endpoints must preview before mutation, report row-level failures, and be idempotent when the same idempotency key is retried.
- Every owner-scoped resource must enforce access with the existing current-profile and access-guard patterns.
- New persisted data is introduced through one numbered Liquibase release per milestone.

---

## Product Strategy

DeckAssemble should not clone Moxfield or Archidekt feature-for-feature. Its moat is answering: **“What is the best legal Commander deck I can build with the cards I own, what am I missing, and why?”** The roadmap first removes migration friction, then deepens that intelligence, then adds retention and community workflows.

## Milestone Sequence

| Milestone | Outcome | Depends on | Main risk | Executable plan |
|---|---|---|---|---|
| M1 Adoption Foundations | Users can migrate decks/collections and inspect actionable deck analytics | Existing cards, collections, decks, prices | Ambiguous card matching and partial imports | `docs/plans/2026-08-04-m1-interoperability-analytics-plan.md` |
| M2 Intelligence and Organization | Recommendations are explainable, swappable, comparable, searchable, and organized | M1 analytics and import identifiers | Ranking quality and query performance | `docs/plans/2026-08-04-m2-intelligence-organization-plan.md` |
| M3 Experimentation and Publishing | Users can restore deck history, simulate draws, and publish durable deck pages | M2 categories and analytics | Reproducibility and privacy leaks | `docs/plans/2026-08-04-m3-experimentation-publishing-plan.md` |
| M4 Community and Physical Workflows | Users can collaborate, discover decks, manage physical inventory/trades, ingest scan results, and register tournament lists | M3 immutable revisions and visibility | Authorization, abuse, concurrency, physical allocation consistency | `docs/plans/2026-08-04-m4-community-physical-plan.md` |

## Dependency Map

```text
M1 deck/collection import -> stable card resolution -> M2 advanced search
M1 analytics -> M2 explanations/comparisons -> M3 simulations
M2 categories/tags -> M3 history snapshots and published deck grouping
M3 revisions -> M4 optimistic collaboration and auditability
M3 visibility/share slugs -> M4 discovery, follows, comments, notifications
M4 physical locations/metadata -> scanner review and trade matching
M4 published immutable revisions -> tournament registration
```

## Vertical Slice Policy

Each slice must ship a complete backend behavior: migration, domain model, application service, endpoint, authorization, unit/integration tests, and API documentation where applicable. Do not create empty packages or generalized frameworks before a slice consumes them.

## Portfolio Acceptance Criteria

- [ ] Existing deck, collection, recommendation, legality, combo, and profile tests remain green throughout.
- [ ] A user can import a collection and deck from CSV/text, inspect failures, export them, and retry safely.
- [ ] A user can view deck value, missing-card cost, curve, mana production, type/category distributions, ownership, tokens, game changers, combos, and legality in one analysis response.
- [ ] Recommendation and owned-first alternative/swap responses explain their rankings with stable reason codes and evidence.
- [ ] Users can compare builds and request bounded upgrades without mutating the source deck.
- [ ] Cards and decks support typed discovery filters, categories, tags, folders, templates, and bulk assignment.
- [ ] Deck mutations create restorable history; simulations are reproducible with a seed and return statistical summaries.
- [ ] Decks support private/unlisted/public visibility, share slugs, primers, forks, collaborators, and immutable published revisions.
- [ ] Social interactions enforce visibility and moderation controls; notifications are deduplicated and markable as read.
- [ ] Physical inventory captures storage location, condition, language, finish, purchase price, and allocation without allowing negative availability.
- [ ] Trade matching, scanner-session ingestion, and tournament registration consume existing collection/deck primitives instead of duplicating card data.
- [ ] `./gradlew test` and `./gradlew check` pass at the end of every milestone.

## Program Risks and Controls

| Risk | Control |
|---|---|
| Large scope causes a long-lived branch | One milestone branch at a time; ship after each independently useful vertical slice. |
| Import formats drift | Canonical internal row model plus format-specific parsers; fixture tests for each supported export. |
| Search becomes an unbounded query language | Typed allow-listed filters only; indexed predicates and maximum page size. |
| Recommendation reasons diverge from ranking | Produce reasons from the same score contributions used by the ranker. |
| History storage grows indefinitely | Append meaningful domain changes only; snapshots at publish/restore boundaries, not every read. |
| Simulation promises a full rules engine | Statistical goldfishing only; no stack, priority, combat, or card-text execution. |
| Social features expose private data | Central visibility policy and integration tests for owner, collaborator, authenticated stranger, and anonymous caller. |
| Real-time editing corrupts decks | Start with optimistic revision checks; add transport-level real-time events only after conflict semantics work. |
| Physical allocation exceeds inventory | Database-backed availability invariant and transactional allocation commands. |
| Camera scanning expands backend scope | Backend accepts reviewed candidate rows; image recognition remains a separate mobile/service project. |

## Release Gates

After every three completed tasks in a milestone:

```bash
./gradlew test
./gradlew check
```

Expected: both commands exit `0`; no skipped required tests, quality suppressions, or architecture violations.

Before closing a milestone, manually drive its API surface against a running application and PostgreSQL Testcontainers-compatible database:

```bash
./gradlew bootRun
```

Expected: the milestone happy path succeeds, one invalid input returns the documented 4xx response, and owner isolation is observed with a second authenticated profile.

## Explicit Non-Goals

- No full Scryfall query-language parser; typed filters cover the current product need.
- No MTG rules engine or graphical playtest board.
- No custom WebSocket collaboration protocol before optimistic concurrency works over HTTP.
- No in-process computer vision or camera SDK in this backend.
- No marketplace purchasing, payment processing, or escrow.
- No automated tournament organizer platform; registration exports validated deck snapshots only.
