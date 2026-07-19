# ADR-0003: Use PostgreSQL for persistence

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application needs a reliable database to store relational data. This data includes user profiles, deck lists, card definitions, and analysis payloads. We need a database that supports complex queries, full-text search, and JSON storage.

The database must run efficiently in local development environments and in production. We also need a solution that integrates well with Spring Boot and JPA.

## Decision

We will use PostgreSQL for all relational data persistence. PostgreSQL is a proven database that offers excellent JSON support for analysis payloads and strong full-text search capabilities for card queries. It works seamlessly in local Docker environments and as a managed service on Railway.

## Alternatives Considered

### Alternative 1: MySQL
- **Pros**: Widely used, simple setup, good performance.
- **Cons**: Less advanced JSON support and weaker full-text search capabilities compared to PostgreSQL.
- **Why rejected**: PostgreSQL provides superior JSON query capabilities and better text search features out of the box.

### Alternative 2: MongoDB
- **Pros**: Flexible schema, natural fit for document storage.
- **Cons**: Lacks strong relational integrity, complex joins are difficult to manage.
- **Why rejected**: The core domain model (users, decks, cards, sets) is highly relational, making a document database a poor fit.

### Alternative 3: SQLite
- **Pros**: Zero configuration, file-based, extremely fast for single-user scenarios.
- **Cons**: Not suitable for concurrent write operations in a production API.
- **Why rejected**: SQLite does not scale well for concurrent web applications and lacks advanced features like full-text search.

## Consequences

### Positive
- Strong relational integrity and transaction support.
- Rich JSON querying capabilities for flexible analysis data.
- Built-in full-text search for card lookup.

### Negative
- Requires managing database migrations and schema updates.
- Higher resource usage compared to SQLite.

### Neutral
- Database schema must be kept in sync with JPA entities.

## References

- PostgreSQL Documentation
