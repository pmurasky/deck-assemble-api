package com.deckassemble.shared.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityCurrentUser implements CurrentUser {

  @Override
  public Optional<String> subject() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .filter(Authentication::isAuthenticated)
        .filter(JwtAuthenticationToken.class::isInstance)
        .map(JwtAuthenticationToken.class::cast)
        .map(token -> token.getToken().getSubject());
  }

  @Override
  public boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }
}
