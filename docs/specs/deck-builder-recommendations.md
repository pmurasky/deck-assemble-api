# Spec: Recommendations & Auto Deck Builder (Commander)

Status: PLANNING — no implementation yet.
Module: new `recommendations` module (hexagonal: `api / application / domain / infrastructure`), mirroring existing modules.

## 1. Goal

Three deck-building flows over the existing collection + card catalog:

1. **UC1 — Build from owned:** pick a commander → generate the best 100-card deck using only cards the user owns.
2. **UC2 — Commander suggestions:** rank commanders by how well the user's collection supports them ("you own 68% of this deck").
3. **UC3 — Optimal build + wishlist:** generate the best deck ignoring ownership; unowned cards are flagged `WISHLIST` and priced.

## 2. Existing assets (do not rebuild)

- `decks/domain/Deck.java` — already has `commanderCardId`, `secondaryCommanderCardId`, `useOwnedCardsOnly`, `budgetLimit`, `desiredPowerLevel`, `playStyle`.
- `decks/application/CommanderLegalityEvaluator.java` — full validation (100 cards, singleton, color identity, partner pairing, legality). Reused as the builder's validator.
- `DeckCard.Section` — `COMMANDER, MAIN_DECK, SIDEBOARD, COMPANION, MAYBE_BOARD`.
- `collections` — `CollectionCard(cardPrintingId, regularQuantity, foilQuantity)`: ownership inventory, printing-level.
- `cards` — Scryfall import pipeline (`RestClientScryfallClient`, rate limiter, `CardImportService`). `Card` has `colorIdentity`, `keywords`, `manaValue`, and an unpopulated `commanderRank` field (populated by this feature).

## 3. External data sources

| Source | Status | Use | Policy |
|---|---|---|---|
| Scryfall | Official API, already integrated | Prices (`usd`, `usd_foil`, `eur`, `tix`), legalities, oracle data | Extend `ScryfallCard` DTO with `prices`; daily price snapshot |
| EDHREC | Unofficial JSON (`json.edhrec.com/pages/commanders/{slug}.json`) | Per-commander synergy scores, inclusion %, themes, average decklists; top-commanders list → `cards.commander_rank` | Cache aggressively (weekly refresh), ~1 req/s, fallback: stale cache → generic template |
| Commander Spellbook | Official open API (Swagger at `backend.commanderspellbook.com/schema/swagger/`) | `find-my-combos` — combos present in a generated deck | Phase 2 |
| Archidekt | Unofficial read-only (~80 req/min) | Meta decklist sampling | Phase 2, optional |
| Moxfield | Private, Cloudflare-protected | — | **Do not use** |

**Deck template heuristics** (community consensus): ~35–38 lands (curve/ramp-adjusted), ~10 ramp, ~10 draw, ~8–10 spot removal, 3–5 board wipes, remainder synergy/theme cards. EDHREC synergy fills the theme slots.

## 4. Decisions (locked)

| # | Decision | Rationale |
|---|---|---|
| D1 | `deck_cards.ownership_status` enum `OWNED \| WISHLIST \| PROXY`, NOT NULL default `OWNED`, set at insert time (builder or manual add) via one collection lookup | Derived-only can't express PROXY, requires oracle-id anti-joins on every read, and gives no stable deck intent |
| D2 | `POST /decks/{id}/sync-ownership` recomputes flags from current collection on demand; response reports changed cards | Live accuracy on demand without per-read join cost |
| D3 | Ownership matched by **oracle id** (any printing counts as owned); foil+regular pooled | Printing-level matching produces false "not owned" |
| D4 | Card quantity sharing across decks is allowed; no allocation tracking in v1 | YAGNI; revisit if users ask |

## 5. Defaults (pending user override)

| # | Default | Override? |
|---|---|---|
| D5 | Ship order: UC1 → UC3 → UC2 | |
| D6 | Commander format only in v1 | |
| D7 | Builder picks cheapest printing for unowned cards; owned printing for owned cards | |
| D8 | Prices: daily snapshot, USD (EUR column stored, UI later) | |
| D9 | EDHREC failure fallback: stale cache, then generic template build without synergy scores | |
| D10 | `powerLevel` v1: label only. Phase 2: filter WotC Game Changers at low power | |
| D11 | `playStyle` v1: fixed chip set (aggro/control/combo/midrange/tribal), label only. Phase 2: adjusts category quotas | |
| D12 | Builds synchronous (target < 5s; EDHREC cache hit makes this realistic). Async job only if profiling misses target | |
| D13 | Builder always fills exactly 100: pads with basic lands, then reports unfillable slots as gaps | |
| D14 | Swap alternatives ranked by EDHREC synergy | |
| D15 | Generated decks auto-save as `DRAFT`; `deck_builds` row records config + score for reproducibility | |
| D16 | Wishlist: per-deck flag (cross-deck shopping view is a free query: `WHERE ownership_status='WISHLIST'`). "Mark as acquired" adds card to collection + flips flag | |

## 6. Schema changes (one Liquibase release: `007-recommendations.yaml`)

- `deck_cards.ownership_status` varchar(20) NOT NULL DEFAULT 'OWNED'
- `card_price_snapshots`: id, card_printing_id FK, usd, usd_foil, eur, fetched_at; latest-per-printing lookup
- `edhrec_commander_cache`: commander_oracle_id, payload jsonb, fetched_at
- `deck_builds`: id, deck_id FK, config jsonb, score numeric, created_at/created_by
- Backfill job populates `cards.commander_rank` from EDHREC top commanders

## 7. Endpoints (sketch)

```
POST /recommendations/builds          { commanderCardId, secondaryCommanderCardId?,
                                        useOwnedCardsOnly, budgetLimit?, powerLevel?, playStyle? }
                                      → 201 DeckResponse (status DRAFT) + build summary
GET  /recommendations/commanders      → ranked list: commander, coverage%, missing staples,
                                        est. cost-to-complete, commanderRank
GET  /decks/{id}/wishlist             → WISHLIST cards + latest prices + total cost
POST /decks/{id}/sync-ownership       → recompute flags; returns changed cards
POST /decks/{id}/cards/{deckCardId}/acquire → add to collection, flip WISHLIST→OWNED
GET  /decks/{id}/cards                → now includes ownershipStatus per card
```

## 8. Builder algorithm (UC1/UC3 core)

1. Resolve commander(s) → color identity; reuse `CommanderLegalityEvaluator` helpers.
2. Load EDHREC commander data (cache → fetch → fallback template).
3. Candidate pool: owned-only = collection printings (oracle-id match); optimal = all legal cards in color identity.
4. Fill category quotas in order: lands → ramp → draw → removal → wipes → synergy/theme.
   Score = EDHREC synergy × inclusion% (× owned-bonus in UC3 so owned cards win ties).
5. Choose printings: owned printing if owned, else cheapest (D7). Set `ownership_status` (D1).
6. Pad to exactly 100 (D13); run `CommanderLegalityEvaluator`; persist Deck (DRAFT) + `deck_builds`.

## 9. UI brief (for UI agent — copy from here)

**Screen 1 — Commander Suggestions ("What can I build?")**
Ranked grid of commander tiles: card art, name, color-identity pips, **coverage %** ("you own 68% of this deck"), missing-staples count, est. cost-to-complete, popularity rank. Filters: colors, budget, owned-only. CTA per tile: **Build Deck**.

**Screen 2 — Build Config (modal/drawer)**
Commander summary + inputs: **Owned cards only** toggle, **budget limit** ($), **power level** slider (1–10), **play style** chips (aggro/control/combo/midrange/tribal). CTA **Generate** → loading state → Screen 3.

**Screen 3 — Generated Deck View**
Decklist grouped by function (Commander, Lands, Ramp, Draw, Removal, Wipes, Theme). Per-card row: **ownership badge** — ✅ owned / 🛒 wishlist + price / 🖨 proxy (color + icon, never color alone). Header stats: card count, owned %, **wishlist total cost**, mana-curve chart. Card hover: synergy score + "why this card" tooltip. Row actions: **swap** (ranked alternatives), move to maybeboard, remove. Legality violations as inline warnings. **Sync ownership** button; show "N cards changed" after sync.

**Screen 4 — Wishlist Panel**
Unowned cards grouped by priority (highest synergy first), individual + total price, export (text/CSV), **Mark as acquired** flow (adds to collection, badge flips to ✅).

**Screen 5 — Deck Comparison**
2–3 builds side-by-side: owned %, wishlist cost, power level, commander. Supports "various decks with different commanders."

## 10. Phasing

- **Phase 1 — Data infra:** prices (DTO + snapshots + refresh), EDHREC client + cache, commander_rank backfill, `ownership_status` + sync endpoint, acquire endpoint.
- **Phase 2 — UC1 builder:** algorithm + `POST /recommendations/builds` (owned-only) + deck view badges.
- **Phase 3 — UC3:** optimal builds, wishlist endpoint + panel, pricing totals, budget enforcement.
- **Phase 4 — UC2:** commander suggestions + comparison view.
- **Phase 5 — Polish:** Spellbook combos, power-level/game-changers filtering, play-style quotas, Archidekt sampling.
