# ADR-0010: Use deterministic legality rules

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application must validate the legality of user-created decks. Legality rules include deck size limits, singleton constraints, commander color identity, and banned or restricted card lists. We need to decide how to implement this validation engine.

Validation results must be accurate, explainable, and reproducible. Users need to know exactly why a deck is illegal.

## Decision

We will implement the legality engine using deterministic Java code rather than artificial intelligence or external rules engines. The engine will validate deck size, singleton rules, commander identity, and banned or restricted cards using standard object-oriented patterns.

This approach ensures that validation logic is testable, fast, and completely predictable.

## Alternatives Considered

### Alternative 1: LLM-based legality validation
- **Pros**: Can handle natural language queries, flexible rule interpretation.
- **Cons**: High risk of hallucinations, non-deterministic results, slow execution times, API costs.
- **Why rejected**: Legality validation requires absolute correctness and reproducibility, which LLMs cannot guarantee.

### Alternative 2: Rules engine framework (e.g., Drools)
- **Pros**: Separates rules from application code, allows non-developers to edit rules.
- **Cons**: Adds complexity, requires learning a new rule language, harder to debug and test.
- **Why rejected**: The rules for Magic: The Gathering formats are well-defined and stable enough that plain Java code is simpler and easier to maintain.

## Consequences

### Positive
- Predictable and reproducible validation results.
- Fast execution times with zero external API dependencies.
- Easy to write unit tests for edge cases.

### Negative
- Changes to format rules require code updates and deployments.
- Developers must manually translate game rules into Java code.

### Neutral
- The legality engine must be kept up to date with official format changes.

## References

- Magic: The Gathering Tournament Rules
