# ADR-0005: Use a modular monolith

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble backend API needs to support multiple business domains. These domains include authentication, users, cards, sets, collections, decks, legality, analysis, recommendations, imports, administration, and shared utilities. We need to decide on the architectural style of the application.

We want to keep operational complexity low while ensuring that the codebase remains maintainable as it grows. The architecture should allow developers to work on different domains without causing merge conflicts or tight coupling.

## Decision

We will build the backend as a modular monolith. The application will be a single Spring Boot deployable organized into distinct business modules. These modules are: authentication, users, cards, sets, collections, decks, legality, analysis, recommendations, imports, administration, and shared.

Each module will have a well-defined API and package structure. We will enforce boundaries between modules to prevent circular dependencies.

## Alternatives Considered

### Alternative 1: Microservices
- **Pros**: Independent scaling, separate deployment units, technology flexibility per service.
- **Cons**: High operational overhead, complex distributed transactions, network latency.
- **Why rejected**: The operational complexity of microservices is unnecessary for our current scale and team size.

### Alternative 2: Layered package structure (controllers, services, repositories)
- **Pros**: Simple to set up, familiar to most Spring developers.
- **Cons**: Weak domain boundaries, leads to tight coupling across different business areas.
- **Why rejected**: A layered structure makes it difficult to isolate domains and extract them into separate services later if needed.

## Consequences

### Positive
- Simple deployment and operations with a single artifact.
- Clear module boundaries that make the codebase easier to navigate.
- Straightforward path to extract modules into microservices in the future.

### Negative
- The entire application must be redeployed for any change.
- Shared database schema requires careful coordination.

### Neutral
- Developers must discipline themselves to respect module boundaries.

## References

- Spring Modulith Documentation
