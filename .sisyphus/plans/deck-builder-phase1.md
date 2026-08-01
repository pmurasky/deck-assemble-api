# Plan: Deck Builder Phase 1 — Data Infra

Spec: `docs/specs/deck-builder-recommendations.md` (decisions D1–D16 approved by user).

## TODOs

- [x] Task 1: Liquibase changelog `007-recommendations.yaml` — `deck_cards.ownership_status` (varchar(20) NOT NULL DEFAULT 'OWNED'), `card_price_snapshots`, `edhrec_commander_cache`, `deck_builds`; register in master changelog
- [x] Task 2: `DeckCard` entity — `OwnershipStatus` enum (OWNED/WISHLIST/PROXY) + field; set flag on insert in `DeckService.addCard` via collection lookup (oracle-id match); include in `DeckCardResponse`
- [x] Task 3: Scryfall prices — extend `ScryfallCard` DTO with `prices` (usd/usd_foil/eur/tix); `CardPriceSnapshot` entity + repo; daily refresh job
- [x] Task 4: EDHREC client + cache — `EdhrecCommanderCache` entity/repo; `EdhrecClient` interface + RestClient impl (~1 req/s politeness); fetch commander JSON, store payload
- [x] Task 5: `POST /decks/{id}/sync-ownership` — recompute flags from collection; return changed cards
- [x] Task 6: `POST /decks/{id}/cards/{deckCardId}/acquire` — add to collection, flip WISHLIST→OWNED
- [x] Task 7: `cards.commander_rank` backfill from EDHREC top commanders

## Final Verification Wave

- [x] F1: Build passes (`./gradlew compileJava`)
- [x] F2: All tests pass (`./gradlew test`)
- [x] F3: Static analysis clean (`./gradlew check` — Checkstyle/PMD/CPD/SpotBugs/Spotless)
- [x] F4: Manual review — every changed file read, logic matches spec decisions

## Constraints

- Engineering standards: TDD micro-commits, Conventional Commits, googleJavaFormat AOSP, 80% coverage on new code, no JPA relations between modules (plain FKs).
- Liquibase changelogs 001–006 exist; follow their format exactly. Master: `db.changelog-master.yaml`.
- Endpoints return `ResponseEntity<?>`, use constructors, `@PatchMapping` with `Record` update DTOs.
