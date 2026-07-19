package com.deckassemble.users.api;

import com.deckassemble.users.application.ProfileMapper;
import com.deckassemble.users.application.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final ProfileService profileService;

  public ProfileController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping
  public ResponseEntity<ProfileResponse> getCurrentProfile(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(
        ProfileMapper.toResponse(profileService.getOrCreate(jwt.getSubject())));
  }

  @PatchMapping
  public ResponseEntity<ProfileResponse> updateCurrentProfile(
      @AuthenticationPrincipal Jwt jwt,
      @Valid @RequestBody ProfileUpdateRequest request) {
    return ResponseEntity.ok(
        ProfileMapper.toResponse(profileService.update(jwt.getSubject(), request)));
  }
}
