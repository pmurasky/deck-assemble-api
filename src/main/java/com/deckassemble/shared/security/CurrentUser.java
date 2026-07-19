package com.deckassemble.shared.security;

import java.util.Optional;

public interface CurrentUser {

  Optional<String> subject();

  boolean isAuthenticated();
}
