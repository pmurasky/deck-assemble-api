package com.deckassemble.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deckassemble.community.domain.Notification.Reason;
import com.deckassemble.community.domain.ProfileFollow;
import com.deckassemble.community.domain.ProfileFollowRepository;
import com.deckassemble.decks.application.DeckAccessGuard;
import com.deckassemble.users.domain.ProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private DeckAccessGuard deckAccessGuard;
    @Mock private ProfileFollowRepository followRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FollowService service;

    @BeforeEach
    void setUp() {
        service =
                new FollowService(
                        deckAccessGuard, followRepository, profileRepository, eventPublisher);
    }

    @Test
    void shouldCreateFollowOnceAndReturnExistingOnRetry() {
        when(deckAccessGuard.profileId()).thenReturn(10L);
        when(profileRepository.existsById(20L)).thenReturn(true);
        when(followRepository.findByFollowerIdAndFolloweeId(10L, 20L))
                .thenReturn(Optional.empty(), Optional.of(new ProfileFollow(10L, 20L)));
        when(followRepository.save(any(ProfileFollow.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProfileFollow created = service.follow(20L).follow();
        ProfileFollow retried = service.follow(20L).follow();

        assertThat(created.getFollowerId()).isEqualTo(10L);
        assertThat(retried.getFolloweeId()).isEqualTo(20L);
        verify(followRepository).save(any(ProfileFollow.class));
        verify(eventPublisher)
                .publishEvent(new CommunityEvent(Reason.NEW_FOLLOWER, 10L, 20L, "10"));
    }

    @Test
    void shouldRejectSelfFollowAndUnknownProfile() {
        when(deckAccessGuard.profileId()).thenReturn(10L);

        assertThatThrownBy(() -> service.follow(10L)).isInstanceOf(ResponseStatusException.class);

        when(profileRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.follow(99L)).isInstanceOf(ResponseStatusException.class);
        verify(followRepository, never()).save(any(ProfileFollow.class));
    }

    @Test
    void shouldUnfollowIdempotently() {
        ProfileFollow follow = new ProfileFollow(10L, 20L);
        when(deckAccessGuard.profileId()).thenReturn(10L);
        when(followRepository.findByFollowerIdAndFolloweeId(10L, 20L))
                .thenReturn(Optional.of(follow), Optional.empty());

        service.unfollow(20L);
        service.unfollow(20L);

        verify(followRepository).delete(follow);
    }
}
