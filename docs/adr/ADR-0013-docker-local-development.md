# ADR-0013: Use Docker for local development

## Status

Accepted

## Date

2026-07-19

## Context

Developers working on the Deck Assemble project need a consistent, reproducible local development environment. We need to ensure that the local database matches the production database in version, configuration, and behavior.

Setting up databases manually on developer machines leads to version drift, configuration discrepancies, and installation issues.

## Decision

We will use Docker Compose to manage the local PostgreSQL database. The Spring Boot application can run either directly on the host machine (for faster debugging and hot-reloading) or inside a Docker container.

A `docker-compose.yml` file will define the PostgreSQL service, ensuring that all developers run the same database version and configuration.

## Alternatives Considered

### Alternative 1: Locally installed PostgreSQL
- **Pros**: Slightly lower resource usage, no Docker dependency.
- **Cons**: High risk of version drift, manual installation overhead, configuration differences across developer operating systems.
- **Why rejected**: Local installations are difficult to keep in sync across the team and often lead to environment-specific bugs.

### Alternative 2: H2 in-memory database for local development
- **Pros**: Zero installation, extremely fast startup, no Docker dependency.
- **Cons**: Does not support PostgreSQL-specific features like JSON querying or full-text search.
- **Why rejected**: H2 does not match production behavior, which would prevent testing PostgreSQL-specific queries locally.

## Consequences

### Positive
- Reproducible local database environment matching production.
- Simple onboarding for new developers (single command to start the database).
- Isolation of database state from the host operating system.

### Negative
- Developers must install and run Docker on their machines.
- Docker containers consume system memory and CPU resources.

### Neutral
- Database connection settings must be configured via Spring profiles.

## References

- `docker-compose.yml`
- ADR-0003: Use PostgreSQL for persistence
