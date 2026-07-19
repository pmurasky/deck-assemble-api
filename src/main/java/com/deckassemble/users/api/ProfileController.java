package com.deckassemble.users.api;

import com.deckassemble.shared.security.CurrentProfile;
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

  private final CurrentProfile currentProfile;
  private final ProfileService profileService;

  public ProfileController(CurrentProfile currentProfile, ProfileService profileService) {
    this.currentProfile = currentProfile;
    this.profileService = profileService;
  }

  @GetMapping
  public ResponseEntity<ProfileResponse> getCurrentProfile() {
    return ResponseEntity.ok(
        ProfileMapper.toResponse(profileService.getOrCreate(currentProfile.requireProfile().getAuthProviderSubject())));
  }

  @PatchMapping
  public ResponseEntity<ProfileResponse> updateCurrentProfile(
      @Valid @RequestBody ProfileUpdateRequest request) {
    return ResponseEntity.ok(
        ProfileMapper.toResponse(profileService.update(currentProfile.requireProfile().getAuthProviderSubject(), request)));
  }
}
