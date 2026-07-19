# ADR-0009: Store card data locally

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application needs to search and display Magic: The Gathering cards. We need to decide whether to query the external card data provider (Scryfall) in real-time for every user request or store the card data locally.

Querying an external API for every search request introduces latency, rate-limiting issues, and dependency on external service availability.

## Decision

We will import Scryfall card data into our local PostgreSQL database. The application will serve all card searches and lookups from the local database. Direct calls to the Scryfall API will only occur when necessary, such as during initial imports or when fetching specific real-time updates.

## Alternatives Considered

### Alternative 1: Proxy every search to Scryfall
- **Pros**: Zero local storage requirements, always up-to-date card data.
- **Cons**: Slow search performance, high risk of hitting rate limits, brittle dependency on Scryfall availability.
- **Why rejected**: Real-time proxying is too slow and unreliable for a responsive user experience.

### Alternative 2: Hybrid caching (cache on demand)
- **Pros**: Lower initial database size, only stores cards that users search for.
- **Cons**: Complex cache invalidation logic, first-time searches remain slow.
- **Why rejected**: Storing the entire card catalog locally is manageable in size and provides a much simpler architecture.

## Consequences

### Positive
- Fast search response times for users.
- Resilience against Scryfall API outages.
- Full control over database indexes and search optimization.

### Negative
- Increased local database storage requirements.
- Need to implement and maintain a background import process to keep data fresh.

### Neutral
- Local card data must be periodically synchronized with Scryfall bulk data.

## References

- ADR-0003: Use PostgreSQL for persistence
- ADR-0008: Use Scryfall as the initial card-data provider
