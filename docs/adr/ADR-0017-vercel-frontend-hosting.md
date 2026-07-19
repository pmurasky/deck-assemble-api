# ADR-0017: Use Vercel for frontend hosting

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble frontend application (`deck-assemble-web`) is built using Next.js. We need to choose a hosting platform that supports Next.js features, provides fast global content delivery, and integrates with our development workflow.

We want to minimize operational overhead for frontend deployments while maintaining high performance and reliability.

## Decision

We will deploy the frontend application on Vercel. Vercel provides native support for Next.js features, automated preview deployments for pull requests, global edge CDN hosting, and simple integration with GitHub.

This platform allows the frontend team to deploy changes quickly and safely without managing infrastructure.

## Alternatives Considered

### Alternative 1: Railway static hosting
- **Pros**: Consolidates hosting on a single platform alongside the backend API and database.
- **Cons**: Less optimized for Next.js-specific features like Server-Side Rendering (SSR) or Incremental Static Regeneration (ISR).
- **Why rejected**: Vercel offers superior native integration and performance optimization for Next.js applications.

### Alternative 2: Self-hosted virtual private server (VPS)
- **Pros**: Full control over the hosting environment, potentially lower direct costs.
- **Cons**: High operational overhead, manual setup of web servers, SSL certificates, and deployment pipelines.
- **Why rejected**: The operational overhead of managing a VPS is not a good use of team resources.

## Consequences

### Positive
- Native support for all Next.js features and optimizations.
- Automated preview deployments for every pull request.
- Global edge network delivery with zero configuration.

### Negative
- Dependency on an additional hosting provider.
- Potential vendor lock-in to Vercel's platform features.

### Neutral
- Frontend environment variables must be configured in the Vercel dashboard.

## References

- Vercel Documentation
- ADR-0001: Use separate frontend and backend repositories
