package com.deckassemble.decks.api.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deckassemble.decks.application.match.Match;
import com.deckassemble.decks.application.match.MatchActionRequest;
import com.deckassemble.decks.application.match.MatchActionRequest.MatchActionType;
import com.deckassemble.decks.application.match.MatchRequest;
import com.deckassemble.decks.application.match.MatchResponse;
import com.deckassemble.decks.application.match.MatchService;
import com.deckassemble.decks.application.match.StackObject;
import com.deckassemble.shared.security.CurrentProfile;
import com.deckassemble.users.domain.Profile;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    private static final long CALLER_PROFILE_ID = 42L;
    private static final UUID MATCH_ID = UUID.randomUUID();

    @Mock private MatchService matchService;

    @Mock private CurrentProfile currentProfile;

    @Mock private Profile profile;

    private MatchController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchController(matchService, currentProfile);
    }

    @Test
    void shouldStartMatchForAuthenticatedCaller() {
        MatchRequest request = mock(MatchRequest.class);
        Match match = mock(Match.class);
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(match.id()).thenReturn(MATCH_ID);
        when(matchService.start(request, CALLER_PROFILE_ID)).thenReturn(match);
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);

        assertThat(controller.start(request)).isSameAs(response);
        verify(matchService).start(request, CALLER_PROFILE_ID);
    }

    @Test
    void shouldReturnViewForAuthenticatedCaller() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);

        assertThat(controller.view(MATCH_ID)).isSameAs(response);
    }

    @Test
    void shouldDelegatePlayLandAndReturnUpdatedView() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);
        MatchActionRequest request =
                new MatchActionRequest(MatchActionType.PLAY_LAND, 55L, null, null, null, null, null);

        assertThat(controller.act(MATCH_ID, request)).isSameAs(response);
        verify(matchService).playLand(MATCH_ID, CALLER_PROFILE_ID, 55L);
    }

    @Test
    void shouldDelegateDeclareBlockersWithEmptyAssignmentsWhenOmitted() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);
        MatchActionRequest request =
                new MatchActionRequest(MatchActionType.DECLARE_BLOCKERS, null, null, null, null, null, null);

        assertThat(controller.act(MATCH_ID, request)).isSameAs(response);
        verify(matchService).declareBlockers(MATCH_ID, CALLER_PROFILE_ID, Map.of());
    }

    @Test
    void shouldDelegateConcede() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);
        MatchActionRequest request =
                new MatchActionRequest(MatchActionType.CONCEDE, null, null, null, null, null, null);

        assertThat(controller.act(MATCH_ID, request)).isSameAs(response);
        verify(matchService).concede(MATCH_ID, CALLER_PROFILE_ID);
    }

    @Test
    void shouldRejectSpellActionWithoutPrintingId() {
        stubCallerProfile();
        MatchActionRequest request =
                new MatchActionRequest(MatchActionType.CAST_SPELL, null, null, null, null, null, null);

        assertThatThrownBy(() -> controller.act(MATCH_ID, request))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getStatusCode())
                                        .isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(matchService);
    }

    @Test
    void shouldDelegateCastSpellWithPermanentTarget() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);
        MatchActionRequest request =
                new MatchActionRequest(
                        MatchActionType.CAST_SPELL, 9L, null, null, 7L, null, null);

        assertThat(controller.act(MATCH_ID, request)).isSameAs(response);
        verify(matchService)
                .castSpell(
                        MATCH_ID,
                        CALLER_PROFILE_ID,
                        9L,
                        new StackObject.Target.PermanentTarget(7L));
    }

    @Test
    void shouldRejectCastSpellWithTwoTargets() {
        stubCallerProfile();
        MatchActionRequest request =
                new MatchActionRequest(
                        MatchActionType.CAST_SPELL, 9L, null, null, 7L, UUID.randomUUID(), null);

        assertThatThrownBy(() -> controller.act(MATCH_ID, request))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception ->
                                assertThat(exception.getReason()).isEqualTo("at most one target"));
        verifyNoInteractions(matchService);
    }

    @Test
    void shouldDelegateSetAutoPass() {
        MatchResponse response = mock(MatchResponse.class);
        stubCallerProfile();
        when(matchService.view(MATCH_ID, CALLER_PROFILE_ID)).thenReturn(response);
        MatchActionRequest request =
                new MatchActionRequest(
                        MatchActionType.SET_AUTO_PASS, null, null, null, null, null, true);

        assertThat(controller.act(MATCH_ID, request)).isSameAs(response);
        verify(matchService).setAutoPass(MATCH_ID, CALLER_PROFILE_ID, true);
    }

    @Test
    void shouldRejectSetAutoPassWithoutFlag() {
        stubCallerProfile();
        MatchActionRequest request =
                new MatchActionRequest(
                        MatchActionType.SET_AUTO_PASS, null, null, null, null, null, null);

        assertThatThrownBy(() -> controller.act(MATCH_ID, request))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(matchService);
    }

    private void stubCallerProfile() {
        when(currentProfile.requireProfile()).thenReturn(profile);
        when(profile.getId()).thenReturn(CALLER_PROFILE_ID);
    }
}
