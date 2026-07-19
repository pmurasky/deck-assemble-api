# ADR-0014: Use Testcontainers for database integration tests

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application relies on PostgreSQL-specific features, including JSON querying and full-text search. We need to write integration tests that validate database queries, Liquibase migrations, and repository methods.

Using a different database engine for tests can hide SQL syntax errors, dialect differences, and migration issues until the code is deployed to production.

## Decision

We will use Testcontainers to spin up a real PostgreSQL database container for all database integration tests. Our test suites will avoid H2 or any other in-memory database for testing repository layers or database migrations.

This ensures that all integration tests run against the exact same database engine used in production.

## Alternatives Considered

### Alternative 1: H2 in-memory database for tests
- **Pros**: Fast test execution, no Docker dependency during test runs.
- **Cons**: Does not support PostgreSQL-specific SQL syntax, JSON functions, or full-text search indexes.
- **Why rejected**: H2 dialect differences would prevent us from testing our actual PostgreSQL queries, leading to false positives in tests.

### Alternative 2: Shared test database instance
- **Pros**: Fast test execution, no container startup overhead.
- **Cons**: High risk of test pollution, requires network access, difficult to run tests in parallel or offline.
- **Why rejected**: Shared databases introduce flakiness and test pollution, making CI builds unreliable.

## Consequences

### Positive
- High confidence that database queries and migrations work correctly.
- Tests run against a clean, isolated database instance every time.
- No dialect differences between test and production environments.

### Negative
- Test execution is slower due to container startup overhead.
- Running tests requires Docker to be installed and running on the host machine.

### Neutral
- Test configuration must be managed via Spring Boot test profiles.

## References

- Testcontainers Documentation
- ADR-0003: Use PostgreSQL for persistence
- ADR-0004: Use Liquibase YAML migrations
