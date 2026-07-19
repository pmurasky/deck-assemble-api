# ADR-0008: Use Scryfall as the initial card-data provider

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble application requires a comprehensive database of Magic: The Gathering cards. This data must include card names, sets, printings, legalities, and image URIs. We need to choose a reliable external data source to populate our database.

The data provider must offer accurate, up-to-date information and be accessible without restrictive licensing or high costs.

## Decision

We will use Scryfall as the canonical import source for card definitions, sets, printings, legalities, and images. Our import processes will use Scryfall's bulk data downloads and REST API to populate and update our local database.

## Alternatives Considered

### Alternative 1: MTGJSON
- **Pros**: Comprehensive offline data files, community-driven.
- **Cons**: Lacks a hosted API for real-time queries, does not host card images directly.
- **Why rejected**: Scryfall provides better image URIs and a robust API for real-time lookups when bulk data is not enough.

### Alternative 2: Manual entry
- **Pros**: Complete control over data quality and formatting.
- **Cons**: Extremely slow, error-prone, and impossible to scale for tens of thousands of cards.
- **Why rejected**: Manual entry is not scalable for the volume of card data required.

### Alternative 3: Wizards of the Coast official API
- **Pros**: Official source of truth.
- **Cons**: Not publicly available or actively maintained for general developer use.
- **Why rejected**: The official API is not accessible for third-party developers.

## Consequences

### Positive
- Access to high-quality, community-validated card data and images.
- Well-documented API and bulk data formats.
- Free tier that fits our current operational budget.

### Negative
- Dependency on Scryfall's API availability and rate limits.
- Changes in Scryfall's data schema may require updates to our import logic.

### Neutral
- We must respect Scryfall's rate limits and API guidelines.

## References

- Scryfall API Documentation
