package com.deckassemble.decks.application.history;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.collaboration.DeckRevisionConflictException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.history.DeckChangeType;
import com.deckassemble.decks.domain.history.DeckRevision;
import com.deckassemble.decks.domain.history.DeckRevisionRepository;
import java.time.Instant;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Records an immutable, append-only {@link DeckRevision} for a meaningful deck mutation, in the
 * same transaction as the mutation itself. Callers decide *whether* a change is meaningful (no-op
 * detection lives with the mutation, not here); this service allocates the sequential revision
 * number, delegates canonical-snapshot assembly to {@link DeckSnapshotBuilder}, and persists the
 * result.
 *
 * <p>Concurrent mutations to the same deck are serialized by taking a {@code PESSIMISTIC_WRITE} row
 * lock on the {@code Deck} row before computing the next revision number, the same locking
 * primitive {@code DeckAccessGuard.ownedLocked} already uses for other per-deck check-then-act
 * races in this module.
 */
@Service
public class DeckRevisionService {

    private static final ThreadLocal<Boolean> SUPPRESSED = ThreadLocal.withInitial(() -> false);

    private final DeckRevisionRepository revisionRepository;
    private final DeckRepository deckRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final DeckSnapshotBuilder snapshotBuilder;

    public DeckRevisionService(
            DeckRevisionRepository revisionRepository,
            DeckRepository deckRepository,
            DeckAccessGuard deckAccessGuard,
            DeckSnapshotBuilder snapshotBuilder) {
        this.revisionRepository = revisionRepository;
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.snapshotBuilder = snapshotBuilder;
    }

    /**
     * Records a revision for the given deck unless recording is currently suppressed by {@link
     * #withoutRecording}. No-op detection is the caller's responsibility.
     */
    public void record(long deckId, long profileId, DeckChangeType changeType) {
        if (Boolean.TRUE.equals(SUPPRESSED.get())) {
            return;
        }
        Deck deck =
                deckRepository
                        .findLockedByIdAndProfileId(deckId, profileId)
                        .orElseThrow(DeckNotFoundException::new);
        persist(deck, profileId, changeType);
    }

    /**
     * Records a revision for an already-locked/loaded deck. Used by the collaborative-edit paths
     * that took the row lock through {@code DeckAccessGuard.editableLocked}: those may be driven by
     * a non-owner editor (so the owner-filtered {@link #record(long, long, DeckChangeType)} would
     * not find the row) and already hold the lock (so re-fetching it is redundant). {@code
     * profileId} is the actor who made the change.
     */
    public void record(Deck deck, long profileId, DeckChangeType changeType) {
        if (Boolean.TRUE.equals(SUPPRESSED.get())) {
            return;
        }
        persist(deck, profileId, changeType);
    }

    private void persist(Deck deck, long profileId, DeckChangeType changeType) {
        long deckId = deck.getId();
        int nextRevisionNumber = nextRevisionNumber(deckId);
        Integer baseRevisionNumber = nextRevisionNumber == 1 ? null : nextRevisionNumber - 1;
        revisionRepository.save(
                new DeckRevision(
                        deckId,
                        profileId,
                        nextRevisionNumber,
                        baseRevisionNumber,
                        new DeckRevision.Content(changeType, null, snapshotBuilder.toJson(deck))));
    }

    /**
     * The deck's current revision number without any access check — the caller must already have
     * authorized and (for the write paths) locked the deck via {@code
     * DeckAccessGuard.editableLocked}.
     */
    public int currentRevisionNumberUnchecked(long deckId) {
        return nextRevisionNumber(deckId) - 1;
    }

    /**
     * Optimistic-concurrency gate for collaborative edits: if {@code expectedRevision} is non-null
     * and does not match the deck's current revision, fails with {@link
     * DeckRevisionConflictException} (HTTP 409). A null {@code expectedRevision} skips the check,
     * so owner-only callers that don't send one keep working unchanged. Must be called while
     * holding the {@code editableLocked} row lock so the read-check-mutate sequence is atomic
     * against other editors.
     */
    public void assertExpectedRevision(long deckId, @Nullable Integer expectedRevision) {
        if (expectedRevision == null) {
            return;
        }
        int current = currentRevisionNumberUnchecked(deckId);
        if (expectedRevision != current) {
            throw new DeckRevisionConflictException(current);
        }
    }

    /**
     * Runs {@code action} with revision recording suppressed, so a caller that composes several
     * already-instrumented mutations (e.g. deck import: create + N card adds) can record exactly
     * one revision itself afterward instead of one per composed call.
     */
    public <T> T withoutRecording(Supplier<T> action) {
        boolean previouslySuppressed = SUPPRESSED.get();
        SUPPRESSED.set(true);
        try {
            return action.get();
        } finally {
            SUPPRESSED.set(previouslySuppressed);
        }
    }

    /** Paginated, most-recent-first revision history for an owned deck. */
    public Page<RevisionView> list(long deckId, Pageable pageable) {
        deckAccessGuard.owned(deckId);
        return revisionRepository
                .findByDeckIdOrderByRevisionNumberDesc(deckId, pageable)
                .map(this::viewOf);
    }

    /** One specific revision of an owned deck, or a 404 if that revision number doesn't exist. */
    public RevisionView get(long deckId, int revisionNumber) {
        deckAccessGuard.owned(deckId);
        return viewOf(revisionFor(deckId, revisionNumber));
    }

    /** The deserialized snapshot stored on one revision of an owned deck. */
    public DeckSnapshot snapshotAt(long deckId, int revisionNumber) {
        deckAccessGuard.owned(deckId);
        return readSnapshot(revisionFor(deckId, revisionNumber));
    }

    /**
     * Same as {@link #snapshotAt} but skips the ownership check. For callers — the shared-deck and
     * fork flows — that have already authorized access to {@code deckId} through a different gate
     * (share-slug visibility, not ownership), where the caller may be an anonymous or stranger
     * requester rather than the deck's owner.
     */
    public DeckSnapshot snapshotAtForSharedAccess(long deckId, int revisionNumber) {
        return readSnapshot(revisionFor(deckId, revisionNumber));
    }

    /** The deck's current (most recent) revision number, or 0 if none has been recorded yet. */
    public int currentRevisionNumber(long deckId) {
        deckAccessGuard.owned(deckId);
        return nextRevisionNumber(deckId) - 1;
    }

    private DeckRevision revisionFor(long deckId, int revisionNumber) {
        return revisionRepository
                .findByDeckIdAndRevisionNumber(deckId, revisionNumber)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Revision not found"));
    }

    private RevisionView viewOf(DeckRevision revision) {
        return new RevisionView(
                revision.getRevisionNumber(),
                revision.getBaseRevisionNumber(),
                revision.getChangeType(),
                revision.getMetadata(),
                readSnapshot(revision),
                revision.getCreatedAt(),
                revision.getCreatedBy());
    }

    private DeckSnapshot readSnapshot(DeckRevision revision) {
        return snapshotBuilder.fromJson(revision.getSnapshot());
    }

    /** Read-only view of a revision, with its snapshot already deserialized. */
    public record RevisionView(
            int revisionNumber,
            @Nullable Integer baseRevisionNumber,
            DeckChangeType changeType,
            @Nullable String metadata,
            DeckSnapshot snapshot,
            Instant createdAt,
            @Nullable String createdBy) {}

    private int nextRevisionNumber(long deckId) {
        return revisionRepository
                .findFirstByDeckIdOrderByRevisionNumberDesc(deckId)
                .map(revision -> revision.getRevisionNumber() + 1)
                .orElse(1);
    }
}
