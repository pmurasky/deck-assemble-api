# Task 5 Report: Add Follows, Favorites, and Discovery

## Implemented

- Added profile follow/unfollow support:
  - `POST /community/profiles/{profileId}/follow`
  - `DELETE /community/profiles/{profileId}/follow`
  - Idempotent follow and unfollow.
  - Rejects self-follow and unknown followee profiles with the same HTTP 400 shape used by collaborator invitation validation.
- Added deck favorite/unfavorite support:
  - `POST /shared/decks/{slug}/favorite`
  - `DELETE /shared/decks/{slug}/favorite`
  - `GET /community/favorites`
  - Favorite create/delete resolves decks through `DeckPublishingService#getShared`, so the favorite gate is PUBLIC + UNLISTED.
  - Listing favorites re-checks `DeckVisibilityPolicy.isSharedViewAllowed`, so favorites stop resolving after a deck goes PRIVATE.
- Added public deck discovery:
  - `GET /community/decks`
  - Discovery is PUBLIC-only and requires `publishedRevisionNumber != null`; UNLISTED decks are excluded.
  - Supports commander name, commander color identity, tags, category, updatedAfter, updatedBefore, and favorited filters.
  - Tags match by owner-scoped tag name via deck owner + tag assignment.
  - Category matches current `DeckCategory` entity shape by deck-local category name.
  - Sort fields are bounded to `name`, `updated`/`updatedAt`, and `published`/`publishedAt`; unsupported sort fields return HTTP 400.
  - Favorite counts and viewer-favorited flags are batch-fetched for page deck IDs.
- Added `CardCatalogService#getActiveCardIdsByColorIdentity` so discovery color filtering goes through the card catalog service rather than inventing a deck field.
- Added `Deck#getUpdatedAt` and `DeckRepository extends JpaSpecificationExecutor<Deck>` for bounded discovery queries.
- Added `GET /community/decks` to public security allowlist.

## Tests and Results

### RED evidence

Command:

```bash
./gradlew test --tests 'com.deckassemble.community.application.FollowServiceTest' \
  --tests 'com.deckassemble.community.application.FavoriteServiceTest' \
  --tests 'com.deckassemble.community.application.DeckDiscoveryServiceTest' \
  --tests 'com.deckassemble.community.api.CommunityDiscoveryControllerIntegrationTest'
```

Relevant failing output:

```text
FollowServiceTest.java:29: error: cannot find symbol
    private FollowService service;
FavoriteServiceTest.java:31: error: cannot find symbol
    private FavoriteService service;
DeckDiscoveryServiceTest.java:9: error: cannot find symbol
import com.deckassemble.community.api.DeckDiscoveryQuery;
DeckDiscoveryServiceTest.java:41: error: cannot find symbol
    private DeckDiscoveryService service;
```

Why expected: the focused tests were written first against the required Task 5 services, controllers, query DTO, response DTO, and repository methods before implementation existed.

### GREEN evidence

Focused command:

```bash
./gradlew test --tests 'com.deckassemble.community.application.FollowServiceTest' \
  --tests 'com.deckassemble.community.application.FavoriteServiceTest' \
  --tests 'com.deckassemble.community.application.DeckDiscoveryServiceTest' \
  --tests 'com.deckassemble.community.api.CommunityDiscoveryControllerIntegrationTest'
```

Result:

```text
BUILD SUCCESSFUL in 30s
5 actionable tasks: 2 executed, 3 up-to-date
```

Full static-analysis gate command:

```bash
./gradlew spotlessApply check
```

Result:

```text
BUILD SUCCESSFUL in 1m 13s
15 actionable tasks: 11 executed, 4 up-to-date
```

Diagnostics:

- `lsp_diagnostics` could not run because `jdtls` is not installed in this environment.
- `gsd_lsp diagnostics` also reported no Java language server.
- Compile, tests, Checkstyle, PMD, CPD, SpotBugs, ArchUnit, Spotless, and Jacoco passed via `./gradlew check`.

## Files Changed

- `src/main/java/com/deckassemble/authentication/infrastructure/config/SecurityConfig.java`
- `src/main/java/com/deckassemble/cards/application/CardCatalogService.java`
- `src/main/java/com/deckassemble/community/api/DeckDiscoveryController.java`
- `src/main/java/com/deckassemble/community/api/DeckDiscoveryQuery.java`
- `src/main/java/com/deckassemble/community/api/DeckDiscoveryResponse.java`
- `src/main/java/com/deckassemble/community/api/DeckFavoriteController.java`
- `src/main/java/com/deckassemble/community/api/ProfileFollowController.java`
- `src/main/java/com/deckassemble/community/application/DeckDiscoveryService.java`
- `src/main/java/com/deckassemble/community/application/FavoriteService.java`
- `src/main/java/com/deckassemble/community/application/FollowService.java`
- `src/main/java/com/deckassemble/community/domain/DeckFavoriteRepository.java`
- `src/main/java/com/deckassemble/community/domain/ProfileFollowRepository.java`
- `src/main/java/com/deckassemble/decks/domain/Deck.java`
- `src/main/java/com/deckassemble/decks/domain/DeckRepository.java`
- `src/test/java/com/deckassemble/community/api/CommunityDiscoveryControllerIntegrationTest.java`
- `src/test/java/com/deckassemble/community/application/DeckDiscoveryServiceTest.java`
- `src/test/java/com/deckassemble/community/application/FavoriteServiceTest.java`
- `src/test/java/com/deckassemble/community/application/FollowServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckTagServiceTest.java`

## Judgment-call Resolutions

- No new migration was added: `015-community-physical.yaml` already created `profile_follows`, `deck_favorites`, and `notifications`; `016` was only comment controls.
- Added repository methods instead of new repositories because persistence entities already existed from Task 4/015.
- Discovery response uses live `Deck` row fields while requiring `publishedRevisionNumber != null`. This matches the existing schema and avoids a heavy JSONB projection; shared deck rendering remains the pinning authority for full deck content.
- Category filtering follows the actual code shape: `DeckCategory` is deck-scoped, not `ProfileOwnedNamedEntity`; matching is by deck-local category name.
- `DeckRepository` now extends `JpaSpecificationExecutor`; this caused one existing Mockito verification ambiguity, fixed by typing the `delete(any(Deck.class))` matcher in `DeckTagServiceTest`.
- ArchitectureTest registration was not needed: adding community to the bounded-context slice list was not required for this change. The architecture failure encountered was application → API DTO leakage, fixed by moving service contracts into `DeckDiscoveryService` and keeping API mapping in controllers.
- PMD suppressions were extended only where they were existing facade/query-service patterns: `CardCatalogService` already carried a facade-method-count warning; `DeckDiscoveryService` is a query facade with many small specification builders.

## Self-review

- Completeness vs checklist: follows/favorites/discovery APIs implemented; idempotency, self-follow rejection, visibility removal, PUBLIC-only discovery, UNLISTED exclusion, deterministic sorting, aggregate counts, and bounded query count are covered.
- YAGNI: reused existing tables/entities and Spring Data specifications; no new migration, no new dependencies, no custom query DSL.
- Names: service/controller names match brief; API DTOs stay in API, application query/result contracts stay in application.
- Test quality: tests exercise real controller + repository + visibility behavior through MockMvc/Postgres and unit-test batch decoration/rejection behavior; no mock-only coverage for the core integration gates.
- Concerns: LSP diagnostics unavailable due missing Java language server, but full Gradle compile/check passed.

## Review Fix Report

### What changed

- Fixed category owner scoping by adding `profile_id` to `deck_categories`, backfilling it from `decks.profile_id` in `017-community-discovery-owner-scope.yaml`, writing it from `DeckCategoryService`, and requiring `deckCategory.profileId = deck.profileId` in discovery category filters.
- Re-checked tag filtering: it already required `tag.profileId = deck.profileId`; added a regression test that rogue tag assignments from another owner do not match.
- Fixed favorites pagination by replacing page-then-filter logic with `DeckFavoriteRepository.findVisibleFavoriteDecks`, which pages only currently shared-view-visible favorites and preserves correct totals.
- Added minimal followed-decks feed API at `GET /community/feed`: authenticated, published PUBLIC decks owned by followed profiles only, sorted newest-published-first.
- Updated API/controller tests and category/history tests for the new `DeckCategory.profileId` constructor requirement.

### Covering tests

- `src/test/java/com/deckassemble/community/api/CommunityDiscoveryControllerIntegrationTest.java`
  - `shouldRejectTagAndCategoryAssignmentsOwnedByAnotherProfile`
  - `shouldPageFavoritesAfterFilteringPrivateDecks`
  - `shouldListFollowedPublicDecksNewestPublishedFirst`
  - existing discovery/favorite/follow integration coverage
- `src/test/java/com/deckassemble/community/application/DeckDiscoveryServiceTest.java`
- `src/test/java/com/deckassemble/community/application/FavoriteServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/organization/DeckCategoryServiceTest.java`
- `src/test/java/com/deckassemble/decks/application/history/DeckSnapshotBuilderTest.java`

### Commands and output

Focused covering tests:

```bash
./gradlew test --tests 'com.deckassemble.community.api.CommunityDiscoveryControllerIntegrationTest' \
  --tests 'com.deckassemble.community.application.DeckDiscoveryServiceTest' \
  --tests 'com.deckassemble.community.application.FavoriteServiceTest' \
  --tests 'com.deckassemble.decks.application.organization.DeckCategoryServiceTest' \
  --tests 'com.deckassemble.decks.application.history.DeckSnapshotBuilderTest'
```

Output:

```text
BUILD SUCCESSFUL in 33s
5 actionable tasks: 4 executed, 1 up-to-date
```

Full gate after formatting fix:

```bash
./gradlew spotlessApply check
```

Output:

```text
BUILD SUCCESSFUL in 1m 12s
15 actionable tasks: 11 executed, 4 up-to-date
```
