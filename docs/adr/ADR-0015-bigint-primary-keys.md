# ADR-0015: Use bigint primary keys and database sequences

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble database schema contains multiple tables representing application-owned entities (such as users, decks, and collections) and imported entities (such as cards and sets). We need to decide on the primary key strategy for these tables.

Primary keys must support fast joins, efficient indexing, and remain stable even if external data formats change.

## Decision

We will use PostgreSQL `bigint` generated primary keys with database sequences for all application-owned entities. External identifiers, such as Scryfall's `oracle_id` or `id` UUIDs, will be stored in separate columns and indexed for lookups, but they will not serve as primary keys.

This approach ensures that our internal database relationships remain compact and independent of external data formats.

## Alternatives Considered

### Alternative 1: UUID primary keys
- **Pros**: Globally unique, can be generated in application code without database round trips.
- **Cons**: Larger storage size (128 bits vs 64 bits), slower join performance, index fragmentation in B-Tree indexes.
- **Why rejected**: The performance and storage overhead of UUIDs as primary keys is unnecessary for our relational model.

### Alternative 2: Scryfall identifiers as primary keys
- **Pros**: Eliminates the need for a separate mapping column, simplifies imports.
- **Cons**: Couples our database schema to an external provider's ID format, makes it harder to support other card data providers.
- **Why rejected**: Using external IDs as primary keys introduces a risky dependency on third-party data structures.

## Consequences

### Positive
- Compact database joins and efficient index usage.
- Stable internal identifiers that are independent of external data providers.
- Standardized primary key strategy across all tables.

### Negative
- Requires managing database sequences.
- Exposes sequential IDs in the database, which may require obfuscation if exposed to clients.

### Neutral
- External IDs must be mapped to internal `bigint` keys during imports.

## References

- PostgreSQL Documentation on Data Types
- ADR-0003: Use PostgreSQL for persistence
