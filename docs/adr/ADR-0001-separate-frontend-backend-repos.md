# ADR-0001: Use separate frontend and backend repositories

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble project requires a web interface and a backend API. We need to decide how to organize the codebase for these two components. The development team wants to ensure that both components can evolve independently.

We want to avoid coupling the deployment of the user interface with the backend services. The frontend will be a static web application, while the backend will be a Java API.

## Decision

We will maintain `deck-assemble-web` and `deck-assemble-api` as separate repositories. This separation allows independent deployment cadences and clear ownership of each codebase. The frontend can be deployed to edge networks, while the API can scale independently.

## Alternatives Considered

### Alternative 1: Monorepo
- **Pros**: Single repository to manage, easier to share types or configuration.
- **Cons**: Coupled deployment pipelines, complex CI configuration, shared commit history.
- **Why rejected**: We want independent deployment cadences and clear separation of concerns.

### Alternative 2: Single full-stack application
- **Pros**: Simple initial setup, no API boundary needed.
- **Cons**: Harder to add future mobile or admin clients, limits scaling options.
- **Why rejected**: We plan to support multiple client applications in the future.

## Consequences

### Positive
- Independent deployment pipelines.
- Clear ownership boundaries for frontend and backend developers.
- Optimized hosting for each tier.

### Negative
- Need to manage two separate repositories.
- API changes require coordination across repositories.

### Neutral
- API contract must be explicitly defined and versioned.

## References

- Project Roadmap
