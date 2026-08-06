# M4 Community and Physical Workflows Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe collaboration and discovery plus physical inventory, trade, scan-review, and tournament-registration workflows without weakening DeckAssemble’s ownership or revision guarantees.

**Architecture:** Collaboration uses explicit roles and optimistic expected-revision commands before any real-time transport. Social resources reference public/unlisted published revisions and pass through one visibility/moderation policy. Physical inventory augments collection-card records with normalized metadata and transactional allocations; scanner sessions submit reviewed recognition candidates rather than processing images. Tournament registrations pin existing immutable deck revisions.

**Tech Stack:** Existing Java/Spring/PostgreSQL stack, Liquibase release `015`, Spring scheduling/application events for notifications, optional SSE only after HTTP collaboration semantics pass.

## Global Constraints

- Require M3 complete and follow portfolio constraints.
- Collaborator roles: `VIEWER|EDITOR`; owners alone manage visibility, collaborators, deletion, and ownership-affecting physical operations.
- Every deck mutation from a collaborator requires `expectedRevision`; stale commands return 409 with current revision.
- No custom WebSocket protocol in the first collaboration slice; SSE presence/events are optional and read-only.
- Comments/follows/favorites/discovery apply only to visible published decks.
- Notification payloads contain resource IDs and reason codes, not copied private content.
- Physical quantity available = owned collection quantity - allocated quantity; it may never be negative.
- Scanner endpoints accept reviewed candidate rows/metadata only; computer vision is out of scope.
- Tournament registration is validation/export, not event management, pairings, standings, or payments.

---

## File Map

- `decks/domain/collaboration/*`, `application/collaboration/*`, `api/collaboration/*`.
- New top-level `community` module with `api/application/domain/infrastructure` for comments, follows, favorites, discovery, notifications, moderation.
- `collections/domain/physical/*` and related application/API packages for metadata, locations, allocations, scans, trades.
- `decks/application/registration/*` for tournament registration snapshots/exports.
- `015-community-physical.yaml` for all milestone persistence and indexes.

### Task 1: Persist Collaboration and Community Foundations

**Files:**
- Create: `src/main/resources/db/changelog/releases/015-community-physical.yaml`
- Modify: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/collaboration/DeckCollaborator.java`
- Create: `src/main/java/com/deckassemble/decks/domain/collaboration/DeckCollaboratorRole.java`
- Create: `src/main/java/com/deckassemble/decks/domain/collaboration/DeckCollaboratorRepository.java`
- Create: `src/main/java/com/deckassemble/community/domain/DeckComment.java`
- Create: `src/main/java/com/deckassemble/community/domain/DeckCommentRepository.java`
- Create: `src/main/java/com/deckassemble/community/domain/ProfileFollow.java`
- Create: `src/main/java/com/deckassemble/community/domain/ProfileFollowRepository.java`
- Create: `src/main/java/com/deckassemble/community/domain/DeckFavorite.java`
- Create: `src/main/java/com/deckassemble/community/domain/DeckFavoriteRepository.java`
- Create: `src/main/java/com/deckassemble/community/domain/Notification.java`
- Create: `src/main/java/com/deckassemble/community/domain/NotificationRepository.java`
- Create: `src/main/java/com/deckassemble/community/domain/ModerationReport.java`
- Create: `src/main/java/com/deckassemble/community/domain/ModerationReportRepository.java`
- Modify test: `src/test/java/com/deckassemble/MigrationIntegrationTest.java`
- Create test: `src/test/java/com/deckassemble/decks/domain/collaboration/DeckCollaboratorRepositoryIntegrationTest.java`
- Create test: `src/test/java/com/deckassemble/community/domain/CommunityRepositoryIntegrationTest.java`.

**Schema:** deck collaborators, comments, follows, deck favorites, notifications, moderation reports; all UUID keyed with uniqueness/indexes for collaborator, follower/followee, favorite, unread notification, and visible published deck queries.

- [ ] Write failing migration tests for constraints, cascade/retention semantics, and indexes.
- [ ] Run focused tests; expected: FAIL.
- [ ] Add release `015`, entities, repositories, and master include.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(community): add collaboration and social persistence`.

### Task 2: Manage Deck Collaborators

**Files:**
- Create: `src/main/java/com/deckassemble/decks/application/collaboration/DeckCollaborationService.java`
- Create: `src/main/java/com/deckassemble/decks/application/collaboration/DeckCollaborationPolicy.java`
- Create: `src/main/java/com/deckassemble/decks/api/collaboration/DeckCollaborationController.java`
- Create: `src/main/java/com/deckassemble/decks/api/collaboration/DeckCollaboratorRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/collaboration/DeckCollaboratorResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckAccessGuard.java`
- Create test: `src/test/java/com/deckassemble/decks/application/collaboration/DeckCollaborationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/decks/application/collaboration/DeckCollaborationPolicyTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/collaboration/DeckCollaborationControllerIntegrationTest.java`.

**APIs:** CRUD `/decks/{deckId}/collaborators`; invitation identifies an existing profile and role.

- [ ] Test owner-only management, duplicate invite idempotency, self-invite rejection, editor/viewer permissions, revoke, private deck access, and archived deck restrictions.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement centralized access decisions while preserving owner behavior.
- [ ] Run all deck authorization tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): manage deck collaborators`.

### Task 3: Enforce Optimistic Collaborative Editing

**Files:**
- Modify: `src/main/java/com/deckassemble/decks/application/DeckUpdateRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckCardAddRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckCardUpdateRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/api/organization/DeckCategoryRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/api/organization/DeckCategoryAssignmentRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/api/organization/DeckFolderRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/api/organization/DeckTagRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/api/publishing/DeckPrimerRequest.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckCardService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckCategoryService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckFolderService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/organization/DeckTagService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/publishing/DeckPrimerService.java`
- Create: `src/main/java/com/deckassemble/decks/application/collaboration/DeckRevisionConflictException.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/DeckServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/DeckCardServiceTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/DeckControllerIntegrationTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/DeckOrganizationControllerIntegrationTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/api/publishing/DeckPublishingControllerIntegrationTest.java`.

**Contract:** collaborator mutation commands carry `expectedRevision`; response returns the resulting revision.

- [ ] Add concurrent editor tests proving exactly one command wins from the same base revision and owner no-op behavior remains deterministic.
- [ ] Run focused tests; expected: FAIL.
- [ ] Check and increment revision inside the mutation transaction; map stale state to HTTP 409 with current revision.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): protect collaborative edits with revisions`.

### Task 4: Add Comments and Moderation Controls

**Files:**
- Create: `src/main/java/com/deckassemble/community/application/CommentService.java`
- Create: `src/main/java/com/deckassemble/community/application/ModerationService.java`
- Create: `src/main/java/com/deckassemble/community/api/CommunityCommentController.java`
- Create: `src/main/java/com/deckassemble/community/api/ModerationController.java`
- Create: `src/main/java/com/deckassemble/community/api/CommentRequest.java`
- Create: `src/main/java/com/deckassemble/community/api/CommentResponse.java`
- Create: `src/main/java/com/deckassemble/community/api/ModerationReportRequest.java`
- Create: `src/main/java/com/deckassemble/community/api/ModerationReportResponse.java`
- Create test: `src/test/java/com/deckassemble/community/application/CommentServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/application/ModerationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/api/CommunityCommentControllerIntegrationTest.java`
- Create test: `src/test/java/com/deckassemble/community/api/ModerationControllerIntegrationTest.java`.

**APIs:** list/create/edit/delete `/shared/decks/{slug}/comments`; owner controls `commentsEnabled`; report endpoint `/community/reports`.

- [ ] Test visibility, comment-disabled, author edit/delete, deck-owner moderation, pagination, length/rate limits, soft deletion, and private-content non-disclosure.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement comments against pinned published deck ID/revision and minimal report status workflow `OPEN|RESOLVED|DISMISSED`.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(community): add moderated deck comments`.

### Task 5: Add Follows, Favorites, and Discovery

**Files:**
- Create: `src/main/java/com/deckassemble/community/application/FollowService.java`
- Create: `src/main/java/com/deckassemble/community/application/FavoriteService.java`
- Create: `src/main/java/com/deckassemble/community/application/DeckDiscoveryService.java`
- Create: `src/main/java/com/deckassemble/community/api/ProfileFollowController.java`
- Create: `src/main/java/com/deckassemble/community/api/DeckFavoriteController.java`
- Create: `src/main/java/com/deckassemble/community/api/DeckDiscoveryController.java`
- Create: `src/main/java/com/deckassemble/community/api/DeckDiscoveryQuery.java`
- Create: `src/main/java/com/deckassemble/community/api/DeckDiscoveryResponse.java`
- Create test: `src/test/java/com/deckassemble/community/application/FollowServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/application/FavoriteServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/application/DeckDiscoveryServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/api/CommunityDiscoveryControllerIntegrationTest.java`.

**APIs:** follow/unfollow profiles, favorite/unfavorite shared decks, list favorites/feed, `GET /community/decks` with commander/colors/tags/category/updated/favorite filters and bounded sorting.

- [ ] Test idempotency, self-follow rejection, visibility removal, public-only discovery, unlisted exclusion, deterministic pagination, and aggregate counts.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement repository projections over published revisions and existing organization metadata.
- [ ] Run query-count/performance integration tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(community): add follows favorites and discovery`.

### Task 6: Deliver Deduplicated Notifications

**Files:**
- Create: `src/main/java/com/deckassemble/community/application/NotificationService.java`
- Create: `src/main/java/com/deckassemble/community/application/CommunityEventListener.java`
- Create: `src/main/java/com/deckassemble/community/application/CommunityEvent.java`
- Create: `src/main/java/com/deckassemble/community/api/NotificationController.java`
- Create: `src/main/java/com/deckassemble/community/api/NotificationResponse.java`
- Create test: `src/test/java/com/deckassemble/community/application/NotificationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/community/application/CommunityEventListenerTest.java`
- Create test: `src/test/java/com/deckassemble/community/api/NotificationControllerIntegrationTest.java`.

**Events:** collaborator added/removed, comment/reply, follow, favorite, fork; dedupe by recipient/event/resource/actor in a short configured window.

**APIs:** `GET /notifications`, `POST /notifications/{id}/read`, `POST /notifications/read-all`.

- [ ] Test recipient isolation, self-action suppression, deduplication, unread count, read/read-all idempotency, and private resource payload minimization.
- [ ] Run focused tests; expected: FAIL.
- [ ] Publish after-commit application events and persist concise notifications; no email/push transport yet.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(community): add in-app notifications`.

### Task 7: Add Physical Card Metadata and Locations

**Files:**
- Modify: `src/main/resources/db/changelog/releases/015-community-physical.yaml`
- Create: `src/main/java/com/deckassemble/collections/domain/physical/StorageLocation.java`
- Create: `src/main/java/com/deckassemble/collections/domain/physical/StorageLocationRepository.java`
- Create: `src/main/java/com/deckassemble/collections/domain/physical/CollectionCardPhysicalMetadata.java`
- Create: `src/main/java/com/deckassemble/collections/domain/physical/CollectionCardPhysicalMetadataRepository.java`
- Create: `src/main/java/com/deckassemble/collections/application/physical/StorageLocationService.java`
- Create: `src/main/java/com/deckassemble/collections/application/physical/PhysicalCardMetadataService.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCollectionController.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/StorageLocationRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/StorageLocationResponse.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardMetadataRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardMetadataResponse.java`
- Create test: `src/test/java/com/deckassemble/collections/application/physical/StorageLocationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/application/physical/PhysicalCardMetadataServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/api/physical/PhysicalCollectionControllerIntegrationTest.java`.

**Metadata:** condition, language, finish/treatment, purchase price/currency/date, notes, storage location; location supports parent UUID and ordered name.

**APIs:** CRUD `/collection-locations`; patch physical metadata on collection cards; query by location/metadata.

- [ ] Test valid enum values, price scale/currency, location tree cycle prevention, deletion with contained cards, owner isolation, and default metadata.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement normalized metadata and locations without changing card catalog printing facts.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): track physical card metadata and locations`.

### Task 8: Allocate Physical Cards to Decks

**Files:**
- Create: `src/main/java/com/deckassemble/collections/domain/physical/PhysicalCardAllocation.java`
- Create: `src/main/java/com/deckassemble/collections/domain/physical/PhysicalCardAllocationRepository.java`
- Create: `src/main/java/com/deckassemble/collections/application/physical/PhysicalCardAllocationService.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationController.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationResponse.java`
- Modify: `src/main/java/com/deckassemble/decks/application/DeckOwnershipService.java`
- Modify: `src/main/java/com/deckassemble/decks/application/OwnershipSyncResponse.java`
- Create test: `src/test/java/com/deckassemble/collections/application/physical/PhysicalCardAllocationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/api/physical/PhysicalCardAllocationControllerIntegrationTest.java`
- Modify test: `src/test/java/com/deckassemble/decks/application/DeckOwnershipServiceTest.java`.

**APIs:** allocate/release/update `/decks/{deckId}/physical-cards`; list unavailable/missing physical cards.

- [ ] Test exact-printing-first allocation, allowed alternate printing, over-allocation rejection, transaction rollback, concurrent allocation, deck disassembly, and ownership status sync.
- [ ] Run focused tests; expected: FAIL.
- [ ] Lock affected collection-card rows transactionally and maintain availability invariant.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): allocate physical cards to decks`.

### Task 9: Add Trade Lists and Matching

**Files:**
- Modify: `src/main/resources/db/changelog/releases/015-community-physical.yaml`
- Create: `src/main/java/com/deckassemble/collections/domain/trading/TradeList.java`
- Create: `src/main/java/com/deckassemble/collections/domain/trading/TradeListRepository.java`
- Create: `src/main/java/com/deckassemble/collections/domain/trading/TradeListItem.java`
- Create: `src/main/java/com/deckassemble/collections/domain/trading/TradeListItemRepository.java`
- Create: `src/main/java/com/deckassemble/collections/application/trading/TradeListService.java`
- Create: `src/main/java/com/deckassemble/collections/application/trading/TradeMatchService.java`
- Create: `src/main/java/com/deckassemble/collections/api/trading/TradeListController.java`
- Create: `src/main/java/com/deckassemble/collections/api/trading/TradeListRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/trading/TradeListResponse.java`
- Create: `src/main/java/com/deckassemble/collections/api/trading/TradeMatchResponse.java`
- Create test: `src/test/java/com/deckassemble/collections/application/trading/TradeListServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/application/trading/TradeMatchServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/api/trading/TradeListControllerIntegrationTest.java`.

**APIs:** CRUD owned offered/wanted lists; compare two visible lists; generate non-binding match with quantity/value deltas.

- [ ] Test owner controls, public/unlisted/private list visibility, available-quantity bounds, exact/alternate printing policy, missing prices, currency grouping, and no inventory mutation.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement read-only matching from collection availability and current price snapshots; no checkout or reservation.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): match physical trade lists`.

### Task 10: Ingest Reviewed Scanner Sessions

**Files:**
- Modify: `src/main/resources/db/changelog/releases/015-community-physical.yaml`
- Create: `src/main/java/com/deckassemble/collections/domain/scanning/ScannerSession.java`
- Create: `src/main/java/com/deckassemble/collections/domain/scanning/ScannerSessionRepository.java`
- Create: `src/main/java/com/deckassemble/collections/domain/scanning/ScannerCandidate.java`
- Create: `src/main/java/com/deckassemble/collections/domain/scanning/ScannerCandidateRepository.java`
- Create: `src/main/java/com/deckassemble/collections/application/scanning/ScannerSessionService.java`
- Create: `src/main/java/com/deckassemble/collections/api/scanning/ScannerSessionController.java`
- Create: `src/main/java/com/deckassemble/collections/api/scanning/CreateScannerSessionRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/scanning/ScannerCandidateRequest.java`
- Create: `src/main/java/com/deckassemble/collections/api/scanning/ScannerSessionResponse.java`
- Modify: `src/main/java/com/deckassemble/cards/application/CardReferenceResolver.java`
- Modify: `src/main/java/com/deckassemble/collections/application/importing/CollectionImportService.java`
- Create test: `src/test/java/com/deckassemble/collections/application/scanning/ScannerSessionServiceTest.java`
- Create test: `src/test/java/com/deckassemble/collections/api/scanning/ScannerSessionControllerIntegrationTest.java`.

**APIs:** create session, submit candidate rows with confidence/external IDs/image reference, review/correct rows, preview totals/value, commit with idempotency key.

- [ ] Test no raw image processing, candidate ambiguity, review-required rule, metadata defaults/overrides, duplicate submission, commit idempotency, and error export.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement scanner sessions as a reviewed import source and delegate final mutation to collection import behavior.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(collections): ingest reviewed scanner sessions`.

### Task 11: Register Tournament Deck Revisions

**Files:**
- Modify: `src/main/resources/db/changelog/releases/015-community-physical.yaml`
- Create: `src/main/java/com/deckassemble/decks/domain/registration/TournamentRegistration.java`
- Create: `src/main/java/com/deckassemble/decks/domain/registration/TournamentRegistrationRepository.java`
- Create: `src/main/java/com/deckassemble/decks/application/registration/TournamentRegistrationService.java`
- Create: `src/main/java/com/deckassemble/decks/application/registration/TournamentRegistrationExporter.java`
- Create: `src/main/java/com/deckassemble/decks/api/registration/TournamentRegistrationController.java`
- Create: `src/main/java/com/deckassemble/decks/api/registration/TournamentRegistrationRequest.java`
- Create: `src/main/java/com/deckassemble/decks/api/registration/TournamentRegistrationResponse.java`
- Create test: `src/test/java/com/deckassemble/decks/application/registration/TournamentRegistrationServiceTest.java`
- Create test: `src/test/java/com/deckassemble/decks/application/registration/TournamentRegistrationExporterTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/registration/TournamentRegistrationControllerIntegrationTest.java`.

**API:** `POST /decks/{deckId}/registrations` pins a published/current revision, event name/date/format/player identity; validates legality and exports text/CSV/PDF only if an existing PDF dependency is available (otherwise text/CSV).

- [ ] Test legal-only registration, immutable pinned snapshot, owner-only identity, event-date validation, repeated export, later deck edits, and archived deck behavior.
- [ ] Run focused tests; expected: FAIL.
- [ ] Implement validation with existing legality service and deterministic registration export; do not build event scheduling or standings.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit: `feat(decks): register immutable tournament deck lists`.

### Task 12: Optional Read-Only Collaboration Events

**Files (create only when the consuming-client gate below passes):**
- Create: `src/main/java/com/deckassemble/decks/application/collaboration/DeckCollaborationEventBroadcaster.java`
- Create: `src/main/java/com/deckassemble/decks/api/collaboration/DeckCollaborationEventController.java`
- Create test: `src/test/java/com/deckassemble/decks/application/collaboration/DeckCollaborationEventBroadcasterTest.java`
- Create test: `src/test/java/com/deckassemble/decks/api/collaboration/DeckCollaborationEventControllerIntegrationTest.java`.

**API:** `GET /decks/{deckId}/events` emits revision-created, collaborator-presence, and comment-created notifications; mutation remains ordinary HTTP with expected revisions.

- [ ] Confirm a consuming client requirement before implementation; otherwise mark this task skipped in execution notes with no code.
- [ ] If required, write failing SSE authorization/reconnect tests.
- [ ] Implement bounded in-memory live delivery backed by revision history for reconnect; no custom conflict protocol.
- [ ] Run focused tests and `./gradlew check`; expected: PASS.
- [ ] Commit only if implemented: `feat(decks): stream collaborative deck events`.

## Milestone Manual QA

- Add viewer/editor collaborators and exercise the complete authorization matrix.
- Submit two edits from the same revision and observe one 409 without lost changes.
- Publish/comment/follow/favorite/fork and verify deduplicated recipient notifications.
- Confirm private/unlisted decks do not leak into discovery or notification payloads.
- Create locations and metadata, allocate all copies, verify over-allocation fails, then disassemble.
- Compare trade lists with missing prices and ensure no inventory changes.
- Submit/review/commit a scanner session and retry its idempotency key.
- Register a legal revision, edit the deck, and verify registration export remains unchanged.

## Completion Gate

```bash
./gradlew test
./gradlew check
```

Expected: exit `0`; release `015` follows `014`; authorization and concurrent-allocation tests pass repeatedly.
