package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.application.history.DeckRevisionService;
import com.deckassemble.decks.application.history.DeckSnapshot;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeckPublishingService {

    // ponytail: 9 random bytes -> 12 URL-safe base64 chars, no padding; plenty of entropy for a
    // share slug and small enough to be a friendly URL segment.
    private static final int SLUG_BYTES = 9;
    private static final int MAX_SLUG_ATTEMPTS = 5;

    private final DeckRepository deckRepository;
    private final DeckAccessGuard deckAccessGuard;
    private final DeckVisibilityPolicy visibilityPolicy;
    private final DeckRevisionService deckRevisionService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeckPublishingService(
            DeckRepository deckRepository,
            DeckAccessGuard deckAccessGuard,
            DeckVisibilityPolicy visibilityPolicy,
            DeckRevisionService deckRevisionService) {
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.visibilityPolicy = visibilityPolicy;
        this.deckRevisionService = deckRevisionService;
    }

    public Deck updateVisibility(long deckId, DeckVisibility visibility) {
        // Locked: read-then-conditionally-write the share slug, same reasoning as
        // DeckAccessGuard#ownedLocked's other lazy first-touch-seeding callers.
        Deck deck = deckAccessGuard.ownedLocked(deckId);
        deck.setVisibility(visibility);
        ensureShareSlug(deck);
        return deckRepository.save(deck);
    }

    /** Owner-only toggle for whether new comments may be posted on this deck's shared view. */
    public Deck setCommentsEnabled(long deckId, boolean enabled) {
        Deck deck = deckAccessGuard.owned(deckId);
        deck.setCommentsEnabled(enabled);
        return deckRepository.save(deck);
    }

    /**
     * Pins the deck's current revision as its shared/fork representation. Until this is called
     * (again), the shared view keeps serving whatever was pinned last — later private edits do not
     * change it. Primer content is intentionally excluded from the pin: it stays live, editable
     * independently of publish state (see SharedDeckResponse).
     */
    public Deck publish(long deckId) {
        Deck deck = deckAccessGuard.ownedLocked(deckId);
        int currentRevisionNumber = deckRevisionService.currentRevisionNumber(deckId);
        if (currentRevisionNumber == 0) {
            // Every deck-creation path records a CREATED revision at creation time, so this should
            // be unreachable through the API — but 0 is also nextRevisionNumber's "never recorded"
            // sentinel, and pinning it would make pinnedSnapshotOrNull call
            // snapshotAtForSharedAccess(deckId, 0), which 404s (revisions start at 1). Fail loudly
            // here instead of leaving the shared page permanently broken.
            throw new IllegalStateException(
                    "Cannot publish deck " + deckId + ": it has no recorded revisions");
        }
        deck.setPublishedRevisionNumber(currentRevisionNumber);
        deck.setPublishedAt(Instant.now());
        return deckRepository.save(deck);
    }

    /**
     * Resolves a share slug through the same visibility gate every shared-deck access (view, fork)
     * must go through — a deck that once was shared can return to PRIVATE while keeping its slug
     * (see Deck's shareSlug comment), and this is what stops that stale slug from still resolving.
     */
    public SharedDeckView getShared(String slug) {
        Deck deck = deckRepository.findByShareSlug(slug).orElseThrow(DeckNotFoundException::new);
        if (!visibilityPolicy.isSharedViewAllowed(deck.getVisibility())) {
            throw new DeckNotFoundException();
        }
        return new SharedDeckView(deck, pinnedSnapshotOrNull(deck));
    }

    private @Nullable DeckSnapshot pinnedSnapshotOrNull(Deck deck) {
        Integer publishedRevisionNumber = deck.getPublishedRevisionNumber();
        if (publishedRevisionNumber == null) {
            // Never published: reasonable minimal fallback is to keep showing live current state,
            // same as before this task — Task 6 never gated shared-view access on publish state,
            // and requiring publish-before-shareable would be new, unrequested scope.
            return null;
        }
        return deckRevisionService.snapshotAtForSharedAccess(deck.getId(), publishedRevisionNumber);
    }

    private void ensureShareSlug(Deck deck) {
        if (deck.getVisibility() == DeckVisibility.PRIVATE || deck.getShareSlug() != null) {
            return;
        }
        deck.setShareSlug(generateUniqueSlug());
    }

    private String generateUniqueSlug() {
        for (int attempt = 1; attempt <= MAX_SLUG_ATTEMPTS; attempt++) {
            String candidate = randomSlug();
            if (!deckRepository.existsByShareSlug(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Unable to generate a unique share slug after " + MAX_SLUG_ATTEMPTS + " attempts");
    }

    private String randomSlug() {
        byte[] bytes = new byte[SLUG_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * A gated shared-deck lookup result: the deck row (always live — visibility, slug, primer) plus
     * its pinned content snapshot, or {@code null} if the deck has never been published.
     */
    public record SharedDeckView(Deck deck, @Nullable DeckSnapshot pinnedSnapshot) {}
}
