# ADR-0012: Use rule-based recommendations before generative AI

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application will provide card recommendations to users building decks. We need to decide on the architecture of the recommendation engine. The recommendations must be relevant, explainable, and free from hallucinated card suggestions.

We want to ensure that the system is easy to debug and does not rely on expensive or slow external AI APIs for core functionality.

## Decision

We will build the recommendation engine using deterministic, rule-based scoring algorithms. Generative AI will only be used to generate natural language explanations or summaries of the recommendations.

The core recommendation logic will analyze card synergies, mana curves, and color identities using local database queries and scoring rules.

## Alternatives Considered

### Alternative 1: Pure LLM-based recommendations
- **Pros**: Easy to set up initially, can handle natural language prompts.
- **Cons**: High risk of recommending non-existent or illegal cards, slow response times, high API costs.
- **Why rejected**: LLMs are prone to hallucinations and cannot guarantee the validity or legality of recommended cards.

### Alternative 2: Hybrid recommendation engine from day one
- **Pros**: Combines the strengths of rules and AI immediately.
- **Cons**: Harder to debug, complex architecture, difficult to isolate issues during early development.
- **Why rejected**: We want to establish a solid, deterministic baseline before introducing the complexity of generative AI.

## Consequences

### Positive
- Complete control over recommendation logic and scoring.
- High performance with zero external API latency for core recommendations.
- No risk of recommending hallucinated or non-existent cards.

### Negative
- Developers must manually define and tune scoring rules.
- Recommendations may feel less conversational or dynamic initially.

### Neutral
- AI integration is deferred to a later phase and limited to explanation generation.

## References

- ADR-0010: Use deterministic legality rules
