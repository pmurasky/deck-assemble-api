package com.deckassemble.community.api;

import com.deckassemble.community.application.FollowService;
import com.deckassemble.community.domain.ProfileFollow;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/community/profiles/{profileId}/follow")
public class ProfileFollowController {

    private final FollowService followService;

    public ProfileFollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping
    public ResponseEntity<ProfileFollow> follow(@PathVariable long profileId) {
        FollowService.FollowResult result = followService.follow(profileId);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.follow());
    }

    @DeleteMapping
    public ResponseEntity<Void> unfollow(@PathVariable long profileId) {
        followService.unfollow(profileId);
        return ResponseEntity.noContent().build();
    }
}
