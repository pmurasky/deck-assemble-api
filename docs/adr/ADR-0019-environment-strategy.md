# ADR-0019: Maintain local, dev, and prod environments

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application needs a deployment and environment strategy. We must ensure that developers can test changes locally, verify integrations in a staging environment, and run the application securely in production.

Deploying directly from local machines to production introduces high risks of downtime, configuration errors, and data loss.

## Decision

We will maintain three distinct environments: local, dev, and prod.

1. **Local**: Docker Compose PostgreSQL database with the Spring Boot application running on the host machine.
2. **Dev**: Managed PostgreSQL and API hosted on Railway, with the frontend deployed as Vercel preview deployments.
3. **Prod**: Managed PostgreSQL and API hosted on Railway, with the frontend deployed as Vercel production deployments.

This strategy ensures local parity, safe integration testing, and production isolation.

## Alternatives Considered

### Alternative 1: Only local and production environments
- **Pros**: Lower hosting costs, fewer environments to manage.
- **Cons**: No staging environment to verify integrations, API contracts, or migrations before they reach production.
- **Why rejected**: Verifying changes directly in production is too risky and can lead to user-facing downtime.

### Alternative 2: More environments (e.g., local, dev, QA, staging, prod)
- **Pros**: More granular testing phases, isolated QA environments.
- **Cons**: High operational overhead, increased hosting costs, complex deployment pipelines.
- **Why rejected**: The complexity of managing five environments is unnecessary for our current team size and scale.

## Consequences

### Positive
- Safe verification of database migrations and API changes in the dev environment.
- Isolated production environment with restricted access.
- Clear path for continuous integration and deployment.

### Negative
- Need to manage configuration and secrets across three environments.
- Slightly higher hosting costs compared to a two-environment setup.

### Neutral
- Environment-specific configurations must be managed via Spring profiles and Vercel/Railway environment variables.

## References

- ADR-0013: Use Docker for local development
- ADR-0017: Use Vercel for frontend hosting
- ADR-0018: Use Railway for managed PostgreSQL and API hosting
