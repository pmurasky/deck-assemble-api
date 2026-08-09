package com.deckassemble.decks.application;

import com.deckassemble.decks.application.collaboration.DeckCollaborationPolicy;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import org.springframework.stereotype.Component;

/** Resolves the current profile and guards deck access for its owner and collaborators. */
@Component
public class DeckAccessGuard {

    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final DeckRepository deckRepository;
    private final DeckCollaborationPolicy deckCollaborationPolicy;

    public DeckAccessGuard(
            CurrentUser currentUser,
            ProfileService profileService,
            DeckRepository deckRepository,
            DeckCollaborationPolicy deckCollaborationPolicy) {
        this.currentUser = currentUser;
        this.profileService = profileService;
        this.deckRepository = deckRepository;
        this.deckCollaborationPolicy = deckCollaborationPolicy;
    }

    public long profileId() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return profileService.getOrCreate(subject).getId();
    }

    public long lockedProfileId() {
        long profileId = profileId();
        return profileService.lock(profileId).getId();
    }

    public Deck owned(long deckId) {
        return deckRepository
                .findByIdAndProfileId(deckId, profileId())
                .orElseThrow(DeckNotFoundException::new);
    }

    /**
     * Same as {@link #owned(long)} but takes a row lock on the deck, serializing concurrent
     * check-then-act operations scoped to this deck (e.g. lazy first-touch seeding) the same way
     * {@link #lockedProfileId()} serializes profile-scoped ones.
     */
    public Deck ownedLocked(long deckId) {
        return deckRepository
                .findLockedByIdAndProfileId(deckId, profileId())
                .orElseThrow(DeckNotFoundException::new);
    }

    /**
     * The deck if the current profile owns it or is a collaborator on it (any role), else throws
     * {@link DeckNotFoundException} — same not-found-not-forbidden shape as {@link #owned(long)},
     * so a stranger can't distinguish "doesn't exist" from "exists but I can't see it".
     */
    public Deck viewable(long deckId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(DeckNotFoundException::new);
        if (!deckCollaborationPolicy.canView(deck, profileId())) {
            throw new DeckNotFoundException();
        }
        return deck;
    }

    /**
     * Same as {@link #viewable(long)} but requires write access: the owner, or a collaborator with
     * the EDITOR role.
     */
    public Deck editable(long deckId) {
        Deck deck = deckRepository.findById(deckId).orElseThrow(DeckNotFoundException::new);
        if (!deckCollaborationPolicy.canEdit(deck, profileId())) {
            throw new DeckNotFoundException();
        }
        return deck;
    }
}
