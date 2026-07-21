package com.deckassemble.shared.security;

import com.deckassemble.users.domain.Profile;
import java.util.Optional;

public interface CurrentProfile {

    Optional<Profile> profile();

    Profile requireProfile();
}
