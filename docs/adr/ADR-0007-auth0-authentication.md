# ADR-0007: Use Auth0 for authentication

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application requires secure user authentication. We need to support user registration, login, and session management. Storing passwords locally introduces security risks and compliance overhead.

We want to support social login providers and ensure that our authentication mechanism follows industry standards like OpenID Connect (OIDC).

## Decision

We will use Auth0 for user authentication. The frontend will use Auth0 Universal Login to authenticate users. The backend API will validate the resulting JSON Web Token (JWT) by checking its signature, issuer, audience, and expiration.

The backend will map the Auth0 `sub` claim to a local user profile to associate application data with the authenticated user.

## Alternatives Considered

### Alternative 1: Spring Security form login with local passwords
- **Pros**: Full control over the authentication flow, no external dependencies.
- **Cons**: High security liability, requires managing password hashing, reset flows, and multi-factor authentication.
- **Why rejected**: Storing passwords locally introduces unnecessary security risks and maintenance overhead.

### Alternative 2: AWS Cognito
- **Pros**: Integrates well with AWS services, cost-effective at scale.
- **Cons**: Less team familiarity, complex configuration, vendor lock-in to AWS.
- **Why rejected**: The team has more experience with Auth0, and Cognito's configuration is more complex for our needs.

### Alternative 3: Clerk
- **Pros**: Modern developer experience, excellent React integration.
- **Cons**: Newer service with less documentation and support for JVM-based backends.
- **Why rejected**: Auth0 has more mature documentation and libraries for Spring Boot integration.

## Consequences

### Positive
- No local password storage, reducing security liability.
- Out-of-the-box support for social login and multi-factor authentication.
- Standardized OIDC-compliant token validation on the backend.

### Negative
- Dependency on an external identity provider.
- Potential latency during authentication redirects.

### Neutral
- Local user profiles must be synchronized with Auth0 identifiers.

## References

- Auth0 Spring Boot SDK Documentation
- OpenID Connect Specification
