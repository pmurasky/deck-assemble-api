package com.deckassemble.shared.persistence;

import com.deckassemble.shared.security.CurrentUser;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditorAware implements AuditorAware<String> {

  private final CurrentUser currentUser;

  public SecurityAuditorAware(CurrentUser currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  public Optional<String> getCurrentAuditor() {
    return currentUser.subject().or(() -> Optional.of("system"));
  }
}
