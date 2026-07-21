package com.deckassemble.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class SecurityCurrentUserTest {

    private final SecurityCurrentUser currentUser = new SecurityCurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnEmptySubjectAndUnauthenticatedWhenNoAuthentication() {
        assertThat(currentUser.subject()).isEmpty();
        assertThat(currentUser.isAuthenticated()).isFalse();
    }

    @Test
    void shouldReturnSubjectFromJwtAuthentication() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject("auth0|123").build();
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));

        assertThat(currentUser.subject()).contains("auth0|123");
        assertThat(currentUser.isAuthenticated()).isTrue();
    }

    @Test
    void shouldReturnEmptySubjectForNonJwtAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("user", "password"));

        assertThat(currentUser.subject()).isEmpty();
    }
}
