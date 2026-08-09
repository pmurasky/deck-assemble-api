package com.deckassemble.decks.application.publishing;

import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.application.DeckNotFoundException;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.decks.domain.publishing.DeckVisibility;
import java.security.SecureRandom;
import java.util.Base64;
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
    private final SecureRandom secureRandom = new SecureRandom();

    public DeckPublishingService(
            DeckRepository deckRepository,
            DeckAccessGuard deckAccessGuard,
            DeckVisibilityPolicy visibilityPolicy) {
        this.deckRepository = deckRepository;
        this.deckAccessGuard = deckAccessGuard;
        this.visibilityPolicy = visibilityPolicy;
    }

    public Deck updateVisibility(long deckId, DeckVisibility visibility) {
        // Locked: read-then-conditionally-write the share slug, same reasoning as
        // DeckAccessGuard#ownedLocked's other lazy first-touch-seeding callers.
        Deck deck = deckAccessGuard.ownedLocked(deckId);
        deck.setVisibility(visibility);
        ensureShareSlug(deck);
        return deckRepository.save(deck);
    }

    public Deck getShared(String slug) {
        Deck deck = deckRepository.findByShareSlug(slug).orElseThrow(DeckNotFoundException::new);
        if (!visibilityPolicy.isSharedViewAllowed(deck.getVisibility())) {
            throw new DeckNotFoundException();
        }
        return deck;
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
}
