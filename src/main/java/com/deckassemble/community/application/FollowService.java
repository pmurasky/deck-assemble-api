package com.deckassemble.community.application;

import com.deckassemble.community.domain.ProfileFollow;
import com.deckassemble.community.domain.ProfileFollowRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.users.domain.ProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FollowService {

    private final DeckAccessGuard deckAccessGuard;
    private final ProfileFollowRepository followRepository;
    private final ProfileRepository profileRepository;

    public FollowService(
            DeckAccessGuard deckAccessGuard,
            ProfileFollowRepository followRepository,
            ProfileRepository profileRepository) {
        this.deckAccessGuard = deckAccessGuard;
        this.followRepository = followRepository;
        this.profileRepository = profileRepository;
    }

    public FollowResult follow(long followeeId) {
        long followerId = deckAccessGuard.profileId();
        assertValidFollowee(followerId, followeeId);
        return followRepository
                .findByFollowerIdAndFolloweeId(followerId, followeeId)
                .map(follow -> new FollowResult(follow, false))
                .orElseGet(() -> new FollowResult(saveFollow(followerId, followeeId), true));
    }

    public void unfollow(long followeeId) {
        long followerId = deckAccessGuard.profileId();
        followRepository
                .findByFollowerIdAndFolloweeId(followerId, followeeId)
                .ifPresent(followRepository::delete);
    }

    private void assertValidFollowee(long followerId, long followeeId) {
        if (followerId == followeeId) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }
        if (!profileRepository.existsById(followeeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile not found");
        }
    }

    private ProfileFollow saveFollow(long followerId, long followeeId) {
        return followRepository.save(new ProfileFollow(followerId, followeeId));
    }

    public record FollowResult(ProfileFollow follow, boolean created) {}
}
