# ADR-0011: Start with Commander format

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application needs to support deck building and validation for Magic: The Gathering formats. Supporting all formats from day one would require a massive development effort and complicate the user experience. We need to choose a single format to focus on initially.

The chosen format should have clear rules, a large active player base, and be a good fit for our planned recommendation engine.

## Decision

We will start by supporting the Commander format as our first and primary format. Other formats will be added later. Commander has clear constraints, such as a 100-card deck size, singleton rules, and commander color identity.

This format is highly popular among casual players and provides a structured domain model that fits our recommendation engine well.

## Alternatives Considered

### Alternative 1: Standard
- **Pros**: Small card pool, official competitive format.
- **Cons**: Rotates frequently, requiring constant updates to card legality and recommendation logic.
- **Why rejected**: Standard's frequent rotation makes it high-maintenance for a starting project.

### Alternative 2: Modern
- **Pros**: Large card pool, highly popular competitive format.
- **Cons**: Complex card interactions, high barrier to entry for beginners, less focus on casual deck building.
- **Why rejected**: Modern has a very wide card pool and complex meta, making recommendation algorithms harder to design initially.

### Alternative 3: Pioneer
- **Pros**: Non-rotating format, smaller card pool than Modern.
- **Cons**: Less beginner-friendly, smaller player base compared to Commander.
- **Why rejected**: Pioneer has less casual appeal and a smaller audience than Commander.

## Consequences

### Positive
- Focused development scope for deck validation and recommendations.
- High appeal to a large and active casual player base.
- Clear, structured rules that simplify the initial domain model.

### Negative
- Users who play other formats will not be able to use the application initially.
- The database must still store all cards, even if they are not legal in Commander.

### Neutral
- The domain model must be designed to allow adding other formats in the future.

## References

- Commander Format Rules
