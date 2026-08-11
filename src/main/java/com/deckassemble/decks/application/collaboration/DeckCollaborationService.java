package com.deckassemble.decks.application.collaboration;

import com.deckassemble.community.application.CommunityEvent;
import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.decks.domain.Deck;
import com.deckassemble.decks.domain.collaboration.DeckCollaborator;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRepository;
import com.deckassemble.decks.domain.collaboration.DeckCollaboratorRole;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Owner-only CRUD of a deck's collaborators. Only the deck owner may call any method here — a
 * collaborator, even an EDITOR, never manages who else has access (see {@link DeckAccessGuard}'s
 * {@code viewable}/{@code editable} for the read/write access decision collaborators do get on the
 * deck's own content).
 */
@Service
@Transactional
public class DeckCollaborationService {

    private final DeckAccessGuard deckAccessGuard;
    private final DeckCollaboratorRepository deckCollaboratorRepository;
    private final ProfileRepository profileRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DeckCollaborationService(
            DeckAccessGuard deckAccessGuard,
            DeckCollaboratorRepository deckCollaboratorRepository,
            ProfileRepository profileRepository,
            ApplicationEventPublisher eventPublisher) {
        this.deckAccessGuard = deckAccessGuard;
        this.deckCollaboratorRepository = deckCollaboratorRepository;
        this.profileRepository = profileRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<DeckCollaborator> list(long deckId) {
        deckAccessGuard.owned(deckId);
        return deckCollaboratorRepository.findByDeckId(deckId);
    }

    /**
     * Adds a collaborator, or changes their role if already invited — repeat invitations of the
     * same profile are always safe to retry rather than erroring on the unique (deck, profile)
     * constraint.
     */
    public DeckCollaborator invite(long deckId, long profileId, DeckCollaboratorRole role) {
        Deck deck = deckAccessGuard.owned(deckId);
        assertValidInvitee(deck, profileId);
        Optional<DeckCollaborator> existing =
                deckCollaboratorRepository.findByDeckIdAndProfileId(deckId, profileId);
        if (existing.isPresent()) {
            return saveWithRole(existing.get(), role);
        }
        DeckCollaborator collaborator =
                deckCollaboratorRepository.save(new DeckCollaborator(deckId, profileId, role));
        publish(deck, profileId, Reason.COLLABORATOR_ADDED);
        return collaborator;
    }

    private void assertValidInvitee(Deck deck, long profileId) {
        if (profileId == deck.getProfileId()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot invite the deck owner as a collaborator");
        }
        if (!profileRepository.existsById(profileId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found");
        }
    }

    private DeckCollaborator saveWithRole(
            DeckCollaborator collaborator, DeckCollaboratorRole role) {
        collaborator.changeRole(role);
        return deckCollaboratorRepository.save(collaborator);
    }

    public void revoke(long deckId, long profileId) {
        Deck deck = deckAccessGuard.owned(deckId);
        DeckCollaborator collaborator =
                deckCollaboratorRepository
                        .findByDeckIdAndProfileId(deckId, profileId)
                        .orElseThrow(DeckCollaboratorNotFoundException::new);
        deckCollaboratorRepository.delete(collaborator);
        publish(deck, profileId, Reason.COLLABORATOR_REMOVED);
    }

    private void publish(Deck deck, long recipientId, Reason reason) {
        eventPublisher.publishEvent(
                new CommunityEvent(
                        reason, deck.getProfileId(), recipientId, String.valueOf(deck.getId())));
    }
}
