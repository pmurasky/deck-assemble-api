package com.deckassemble.authentication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class Auth0RoleConverterTest {

    private final Auth0RoleConverter converter = new Auth0RoleConverter();

    @Test
    void shouldReturnEmptyWhenPermissionsClaimMissing() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").claim("sub", "user").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void shouldMapPermissionsToUppercaseRoleAuthorities() {
        Jwt jwt =
                Jwt.withTokenValue("token")
                        .header("alg", "none")
                        .claim("permissions", List.of("read:cards", "write:decks"))
                        .build();

        assertThat(converter.convert(jwt))
                .containsExactly(
                        new SimpleGrantedAuthority("ROLE_READ:CARDS"),
                        new SimpleGrantedAuthority("ROLE_WRITE:DECKS"));
    }
}
