# M2 Intelligence and Organization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make DeckAssemble recommendations transparent and actionable while adding typed discovery and durable organization tools.

**Architecture:** Reuse score contributions as explanation evidence instead of creating a second explanation engine. Add read-only alternative/comparison/upgrade services over existing candidate scoring and M1 analysis. Extend card search through typed predicates and persist deck categories, tags, folders, and category templates within the deck module.

**Tech Stack:** Existing Java/Spring/PostgreSQL stack, JPA Specifications, Liquibase release `013`, JUnit 5, Mockito, Testcontainers.

## Global Constraints

- Follow `docs/plans/2026-08-04-product-enhancements-roadmap.md` and require M1 complete.
- No opaque free-form Scryfall query language; expose typed, allow-listed filters only.
- Explanation reason codes are stable API values; human text is presentation data, not business logic.
- Alternatives and upgrade plans never mutate a deck until a separate existing deck-card command is called.
- Default functional categories are system-owned; user categories may override presentation, not legality or recommendation facts.

---

## File Map

- `recommendations/application/explanation/*`: score contribution and reason mapping.
- `decks/application/alternatives/*`, `comparison/*`, `upgrades/*`: read-only intelligence workflows.
- `cards/application/CardSearchFilter.java`, `CardSearchPredicates.java`, and `CardCatalogService.java`: typed filters and query composition.
- `decks/domain/organization/*`: categories, tags, folders, templates and assignments.
- `013-intelligence-organization.yaml`: organization tables and search indexes.

### Task 1: Make Candidate Scores Explainable

**Files:**
- Create: `src/main/java/com/deckassemble/recommendations/application/ScoreContribution.java`
- Create: `src/main/java/com/deckassemble/recommendations/application/RecommendationReasonCode.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/DeckCandidateSelector.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/CardScore.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/DeckCandidate.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/DeckBuildResult.java`
- Test: `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java`
- Test: `src/test/java/com/deckassemble/recommendations/application/DeckDraftPickerTest.java`
- Create test: `src/test/java/com/deckassemble/recommendations/api/RecommendationControllerIntegrationTest.java`

**Interfaces:**
```java
public record ScoreContribution(RecommendationReasonCode code, BigDecimal points, Map<String, String> evidence) {}
public record ScoredCandidate(UUID cardId, BigDecimal total, List<ScoreContribution> contributions) {}
```

- [ ] Write tests proving contribution totals equal the final score and reason codes cover owned, commander synergy, category need, play style, combo, budget, and game-changer policy.
- [ ] Run focused tests; expected: FAIL.
- [ ] Refactor scorer output without changing ranking order; map contributions directly into build responses.
- [ ] Run recommendation regression tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(recommendations): explain candidate scores`.

### Task 2: Explain Commander Suggestions

**Files:**
- Modify: `src/main/java/com/deckassemble/recommendations/application/CommanderRankService.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/CommanderSuggestion.java`
- Test: `src/test/java/com/deckassemble/recommendations/application/CommanderRankServiceTest.java`

**Produces:** commander suggestions include ordered reason codes/evidence for collection coverage, missing count, completion cost, color support, and synergy data freshness.

- [ ] Add ranking tests asserting explanations match the exact factors that determine order.
- [ ] Run focused test; expected: FAIL.
- [ ] Return structured factors from ranking and preserve existing fields for compatibility.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(recommendations): explain commander rankings`.

### Task 3: Add Owned-First Card Alternatives

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeService.java`
- Create: `src/main/java/com/deckassemble/decks/api/alternatives/DeckCardAlternativeResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/alternatives/DeckCardAlternativeReason.java`
- Modify: `src/main/java/com/deckassemble/decks/api/DeckController.java`
- Test: `src/test/java/com/deckassemble/decks/application/alternatives/DeckCardAlternativeServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`

**API:** `GET /decks/{deckId}/cards/{deckCardId}/alternatives?limit=10&ownedFirst=true`.

**Ranking:** legality and color identity are hard filters; then ownership, functional category overlap, mana-value distance, commander synergy, combo dependency safety, and price.

- [ ] Test illegal/color-invalid exclusion, owned-first ordering, combo-piece warnings, missing prices, and deterministic ties.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement with existing candidate/scoring/category/card repositories and return reasons for every alternative.
- [ ] Add authorization, limit bounds, and unknown-card integration tests.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): suggest owned-first card alternatives`.

### Task 4: Compare Decks

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/comparison/DeckComparisonService.java`
- Create: `src/main/java/com/deckassemble/decks/api/comparison/DeckComparisonResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/api/DeckController.java`
- Test: `src/test/java/com/deckassemble/decks/application/comparison/DeckComparisonServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`

**API:** `GET /decks/{deckId}/comparison/{otherDeckId}` returns added/removed/quantity-changed cards and deltas for ownership, value, missing cost, curve, categories, legality, game changers, and combos.

- [ ] Write tests for exact printing changes versus card-identity equivalence and analysis deltas.
- [ ] Run focused tests; expected: FAIL.
- [ ] Compose M1 analysis and canonical card maps; enforce access to both decks.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): compare deck composition and metrics`.

### Task 5: Generate Bounded Upgrade Plans

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/upgrades/DeckUpgradeService.java`
- Create: `src/main/java/com/deckassemble/decks/api/upgrades/DeckUpgradeRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/upgrades/DeckUpgradeObjective.java`
- Create: `src/main/java/com/deckassemble/decks/api/upgrades/DeckUpgradePlanResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/api/DeckController.java`
- Test: `src/test/java/com/deckassemble/decks/application/upgrades/DeckUpgradeServiceTest.java`

**API:** `POST /decks/{deckId}/upgrade-plans` with objective `REPLACE_PROXIES_WITH_OWNED`, `IMPROVE_UNDER_BUDGET`, or `CLOSE_CATEGORY_GAPS`, budget/currency, maximum changes.

- [ ] Test objective constraints, budget ceiling, legality preservation, unchanged source deck, deterministic proposal order, and no-solution result.
- [ ] Run focused tests; expected: FAIL.
- [ ] Build plans from the alternatives service and analysis gaps; return before/after metrics and reasoned substitutions.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): generate bounded upgrade plans`.

### Task 6: Expand Typed Card Search

**Files:**
- Modify: `src/main/java/com/deckassemble/cards/application/CardSearchFilter.java`
- Modify: `src/main/java/com/deckassemble/cards/application/CardSearchPredicates.java`
- Modify: `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- Modify: `src/main/java/com/deckassemble/cards/api/CardController.java`
- Create: `src/main/resources/db/changelog/releases/013-intelligence-organization.yaml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Test: `src/test/java/com/deckassemble/cards/application/CardSearchPredicatesTest.java`
- Modify test: `src/test/java/com/deckassemble/cards/application/CardCatalogServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/cards/api/CardControllerIntegrationTest.java`

**Filters:** oracle text, mana value range, power/toughness range, rarity, keywords, format legality, price range/currency, owned quantity, functional category, game changer, set, collector number, language, finish.

- [ ] Add one focused predicate test per filter and compound-filter integration tests with pagination/sort bounds.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement only typed predicates, required joins/subqueries, and indexes justified by integration query plans.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(cards): add advanced typed search filters`.

### Task 7: Persist Categories and Card Assignments

**Files:**
- Modify: `src/main/resources/db/changelog/releases/013-intelligence-organization.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckCategory.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryRepository.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignment.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckCategoryAssignmentRepository.java`
- Create: `src/main/java/com/deckassemble/decks/application/organization/DeckCategoryService.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckCategoryRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckCategoryResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckCategoryAssignmentRequest.java`
- Test: `src/test/java/com/deckassemble/decks/application/organization/DeckCategoryServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/api/DeckOrganizationControllerIntegrationTest.java`

**APIs:** CRUD `/decks/{deckId}/categories`; bulk assignment `PUT /decks/{deckId}/categories/{categoryId}/cards`.

- [ ] Test default category seeding, unique names per deck, ordered display, assignment idempotency, bulk replace semantics, and owner isolation.
- [ ] Run focused tests; expected: FAIL.
- [ ] Persist deck categories and many-to-many deck-card assignments using UUID foreign keys and transactions.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): organize cards with deck categories`.

### Task 8: Add Tags, Folders, and Category Templates

**Files:**
- Modify: `src/main/resources/db/changelog/releases/013-intelligence-organization.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckFolder.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckFolderRepository.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckTag.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/DeckTagRepository.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/CategoryTemplate.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/CategoryTemplateRepository.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/CategoryTemplateItem.java`
- Create: `src/main/java/com/deckassemble/decks/domain/organization/CategoryTemplateItemRepository.java`
- Create: `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java`
- Create: `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- Create: `src/main/java/com/deckassemble/decks/application/organization/CategoryTemplateService.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckFolderRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckFolderResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckTagRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/DeckTagResponse.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/CategoryTemplateRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/organization/CategoryTemplateResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/api/organization/DeckOrganizationController.java`
- Modify: `src/main/java/com/deckassemble/decks/domain/Deck.java`
- Test: `src/test/java/com/deckassemble/decks/application/organization/DeckFolderServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/application/organization/CategoryTemplateServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/DeckOrganizationControllerIntegrationTest.java`

**APIs:**
- CRUD `/deck-folders`, `/deck-tags`, `/category-templates`.
- `PUT /decks/{deckId}/folder`, `PUT /decks/{deckId}/tags`.
- `POST /decks/{deckId}/categories/from-template`.

- [ ] Test one-folder-per-deck, many tags, normalized unique names per profile, reusable ordered templates, apply-without-duplicate behavior, and deletion semantics that retain decks.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement profile-scoped folders/tags/templates and batch application through the category service.
- [ ] Run `./gradlew test` and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): add folders tags and category templates`.

### Task 9: Feed Organization Back into Recommendations

**Files:**
- Modify: `src/main/java/com/deckassemble/recommendations/application/CardCategorizer.java`
- Modify: `src/main/java/com/deckassemble/recommendations/application/DeckCandidateSelector.java`
- Modify: `src/main/java/com/deckassemble/decks/application/analysis/DeckAnalysisService.java`
- Test: `src/test/java/com/deckassemble/recommendations/application/CardCategorizerTest.java`
- Test: `src/test/java/com/deckassemble/recommendations/application/DeckBuilderServiceTest.java`
- Test: `src/test/java/com/deckassemble/decks/application/analysis/DeckAnalysisServiceTest.java`

- [ ] Write tests showing explicit user category assignments override inferred presentation categories while recommendation quotas still use canonical functional categories.
- [ ] Run focused tests; expected: FAIL.
- [ ] Add the minimal read interface from deck organization to recommendation/analysis services without cross-module JPA relationships.
- [ ] Run full tests and check; expected: PASS.
- [ ] Commit: `feat(recommendations): incorporate deck organization metadata`.

## Milestone Manual QA

- Inspect build and commander explanation evidence and reconcile contribution totals.
- Request alternatives for a combo piece and a generic card; verify warning and owned-first ranking.
- Compare two decks, then request each upgrade-plan objective and verify the source is unchanged.
- Exercise compound advanced searches and invalid ranges.
- Create folder/tags/template/categories, bulk assign cards, apply template twice, and verify idempotency.

## Completion Gate

```bash
./gradlew test
./gradlew check
```

Expected: exit `0`; release `013` applies cleanly after `012`; existing search clients remain source-compatible.
