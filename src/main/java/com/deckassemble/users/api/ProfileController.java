package com.deckassemble.users.api;

import com.deckassemble.shared.security.CurrentUser;
import com.deckassemble.users.application.ProfileMapper;
import com.deckassemble.users.application.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

    private final CurrentUser currentUser;
    private final ProfileService profileService;

    public ProfileController(CurrentUser currentUser, ProfileService profileService) {
        this.currentUser = currentUser;
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> getCurrentProfile() {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return ResponseEntity.ok(ProfileMapper.toResponse(profileService.getOrCreate(subject)));
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> updateCurrentProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        String subject =
                currentUser
                        .subject()
                        .orElseThrow(() -> new IllegalStateException("No authenticated user"));
        return ResponseEntity.ok(ProfileMapper.toResponse(profileService.update(subject, request)));
    }
}
