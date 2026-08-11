# Task 6 Report: Deliver Deduplicated Notifications

## Status

DONE_WITH_CONCERNS: in-app notifications are implemented and verified. Concern: the current comment model/service has no parent-comment/reply concept, so `COMMENT_REPLY` was added as a notification reason for compatibility but no reply event can be emitted until a reply source exists.

## Implemented

- Added Spring application event notification flow:
  - `CommunityEvent` immutable record carrying reason, actor id, recipient id, and resource id.
  - `CommunityEventListener` with `@TransactionalEventListener(phase = AFTER_COMMIT)` so notifications persist only after the source transaction commits.
  - `NotificationService` to create/list/read/read-all notifications.
- Extended notification reasons:
  - `COMMENT_REPLY`
  - `COLLABORATOR_REMOVED`
  - `DECK_FORKED`
- Added dedupe behavior:
  - Configured by `community.notifications.dedupe-window` in `application.yml`.
  - Default/property value: `PT5M`.
  - Dedupe skips an unread notification with the same recipient, reason, resource id, and actor id created inside the window.
- Added self-action suppression:
  - `actorId == recipientId` returns without persisting.
- Added notification APIs:
  - `GET /notifications` returns `{ notifications, unreadCount }` for the current profile only.
  - `POST /notifications/{id}/read` marks a current-profile notification read, idempotently.
  - `POST /notifications/read-all` marks all current-profile unread notifications read, idempotently.
- Wired event sources:
  - Collaborator invite: recipient = new collaborator, actor = deck owner, resource = deck id.
  - Collaborator revoke: recipient = removed collaborator, actor = deck owner, resource = deck id.
  - New comment: recipient = deck owner, actor = commenter, resource = deck id.
  - Follow: recipient = followed profile, actor = follower, resource = follower profile id.
  - Favorite: recipient = deck owner, actor = favoriter, resource = deck id.
  - Fork: recipient = source deck owner, actor = forker, resource = source deck id.
- No schema migration added: `notifications` already exists in `015-community-physical.yaml` with string `reason`, nullable `actor_id`, generic `resource_id`, and read/created fields.

## TDD Evidence

### RED

Command:

```bash
./gradlew --console=plain test \
  --tests 'com.deckassemble.community.application.NotificationServiceTest' \
  --tests 'com.deckassemble.community.application.CommunityEventListenerTest' \
  --tests 'com.deckassemble.community.api.NotificationControllerIntegrationTest'
```

Result: failed as expected during `compileTestJava` because the new notification service/event/API classes and repository methods did not exist yet. Representative output:

```text
exit 1
> Task :compileTestJava FAILED
NotificationServiceTest.java:34: error: cannot find symbol
CommunityEventListenerTest.java:14: error: cannot find symbol
NotificationServiceTest.java:81: error: package NotificationService does not exist
BUILD FAILED
```

### GREEN / Focused

Command:

```bash
./gradlew --console=plain test \
  --tests 'com.deckassemble.community.application.NotificationServiceTest' \
  --tests 'com.deckassemble.community.application.CommunityEventListenerTest' \
  --tests 'com.deckassemble.community.api.NotificationControllerIntegrationTest' \
  --tests 'com.deckassemble.community.application.FollowServiceTest' \
  --tests 'com.deckassemble.community.application.FavoriteServiceTest' \
  --tests 'com.deckassemble.community.application.CommentServiceTest' \
  --tests 'com.deckassemble.decks.application.collaboration.DeckCollaborationServiceTest' \
  --tests 'com.deckassemble.decks.application.publishing.DeckForkServiceTest'
```

Result:

```text
exit 0
BUILD SUCCESSFUL in 31s
```

### Full Verification

Baseline before changes:

```text
./gradlew check
BUILD SUCCESSFUL in 796ms
```

Final command:

```bash
./gradlew --console=plain check
```

Result:

```text
exit 0
> Task :check
BUILD SUCCESSFUL in 1m 22s
```

LSP diagnostics were attempted for `NotificationService.java`; no Java language server was available in the session (`No language server found`). Gradle compile/check acted as the Java diagnostics gate.

## Files Changed

- `src/main/java/com/deckassemble/community/application/CommunityEvent.java`
- `src/main/java/com/deckassemble/community/application/CommunityEventListener.java`
- `src/main/java/com/deckassemble/community/application/NotificationService.java`
- `src/main/java/com/deckassemble/community/api/NotificationController.java`
- `src/main/java/com/deckassemble/community/api/NotificationInboxResponse.java`
- `src/main/java/com/deckassemble/community/api/NotificationResponse.java`
- `src/main/java/com/deckassemble/community/domain/Notification.java`
- `src/main/java/com/deckassemble/community/domain/NotificationRepository.java`
- `src/main/java/com/deckassemble/community/application/CommentService.java`
- `src/main/java/com/deckassemble/community/application/FavoriteService.java`
- `src/main/java/com/deckassemble/community/application/FollowService.java`
- `src/main/java/com/deckassemble/decks/application/collaboration/DeckCollaborationService.java`
- `src/main/java/com/deckassemble/decks/application/publishing/DeckForkService.java`
- `src/main/resources/application.yml`
- `src/test/java/com/deckassemble/community/application/NotificationServiceTest.java`
- `src/test/java/com/deckassemble/community/application/CommunityEventListenerTest.java`
- `src/test/java/com/deckassemble/community/api/NotificationControllerIntegrationTest.java`
- Updated existing service tests for event publication expectations.

## Judgment Calls / Resolutions

- Enum names: chose `COMMENT_REPLY`, `COLLABORATOR_REMOVED`, and `DECK_FORKED`, matching existing all-caps reason style.
- Dedupe default: `PT5M`, documented in `application.yml` as `community.notifications.dedupe-window`.
- ArchitectureTest: no layer/cycle violation after wiring decks services to publish `CommunityEvent`; final `./gradlew check` passed.
- Read ownership failure: `POST /notifications/{id}/read` returns 404 for non-owner/missing notifications, matching the repo's not-found-not-forbidden owner isolation pattern.
- Comment reply: actual `DeckComment`/`CommentService` has no parent id or reply API. I did not invent reply persistence/API in this notification task; `COMMENT_REPLY` is reserved for the future event source.
- New comment resource id: used deck id so repeated comments by the same actor on the same deck dedupe within the configured window.
- Follow resource id: used follower profile id; favorite/collaborator/fork use deck/source deck id.

## Self-Review

- Recipient isolation: covered by `NotificationControllerIntegrationTest` and service owner lookup.
- Self-action suppression: covered by `NotificationServiceTest`.
- Deduplication: covered by `NotificationServiceTest`; unread duplicate inside window is skipped.
- Unread count: covered by service and controller tests.
- Read/read-all idempotency: covered by service and controller tests.
- Payload minimization: responses include only id, actorId, reason, resourceId, unread, createdAt; integration test asserts no copied `body` field.
- YAGNI: no email/push transport, no copied content payloads, no migration, no new abstraction beyond Spring's built-in event listener.
- Test quality: focused unit tests cover business logic and event delegation; integration test covers API ownership and serialization shape.

## Concerns

- Reply notification emission is pending the existence of a comment reply model/API/source. The enum is present, but no current code path can publish it without adding non-notification product behavior.
