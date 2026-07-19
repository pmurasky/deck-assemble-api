# ADR-0004: Use Liquibase YAML migrations

## Status

Accepted

## Date

2026-07-19

## Context

As the Deck Assemble database schema evolves, we need a reliable way to manage database migrations. We must ensure that schema changes are version-controlled, testable, and safe to apply across different environments.

Manual schema updates are error-prone and difficult to track. We need a tool that integrates with Spring Boot and supports automated schema updates during deployment.

## Decision

We will use Liquibase with YAML changelogs located in `src/main/resources/db/changelog/` to manage database migrations. This approach provides version-controlled schema changes, rollback support, CI validation, and explicit change sets.

## Alternatives Considered

### Alternative 1: Flyway
- **Pros**: Simple to use, uses raw SQL scripts by default.
- **Cons**: Lacks built-in support for YAML-based changelog organization, which the team prefers for structured metadata.
- **Why rejected**: While Flyway is a strong tool, the team prefers Liquibase's YAML format for its structure and rollback capabilities.

### Alternative 2: Raw SQL scripts
- **Pros**: No dependency on external migration tools, full control over SQL syntax.
- **Cons**: Lacks automated tracking, validation, and rollback support.
- **Why rejected**: Raw SQL scripts are difficult to manage at scale and do not provide safety checks during deployment.

### Alternative 3: Hibernate auto-DDL (ddl-auto=update)
- **Pros**: Zero configuration, automatic schema generation from JPA entities.
- **Cons**: Unsafe for production environments, can lead to accidental data loss or inconsistent schemas.
- **Why rejected**: Auto-DDL is not deterministic and poses a high risk of data corruption in production.

## Consequences

### Positive
- Explicit, version-controlled database schema history.
- Automated validation of migrations during CI builds.
- Rollback support for database changes.

### Negative
- YAML syntax can be verbose compared to raw SQL.
- Developers must learn Liquibase-specific XML/YAML tags.

### Neutral
- Migration files must be created and reviewed for every schema change.

## References

- Liquibase Documentation
