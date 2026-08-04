package com.deckassemble.decks.application;

import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.DeckRepository;
import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileService;
import org.springframework.stereotype.Component;

/** Resolves the current profile and guards deck ownership for deck collaborators. */
@Component
public class DeckAccessGuard {

    private final CurrentUser currentUser;
    private final ProfileService profileService;
    private final DeckRepository deckRepository;

    public DeckAccessGuard(
            CurrentUser currentUser, ProfileService profileService, DeckRepository deckRepository) {
        this.currentUser = currentUser;
        this.profileService = profileService;
        this.deckRepository = deckRepository;
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
}
