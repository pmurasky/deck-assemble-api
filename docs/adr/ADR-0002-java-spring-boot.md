# ADR-0002: Use Java and Spring Boot for the API

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble backend API needs a language and framework that supports rapid development, type safety, and reliable database integration. We need to choose a technology stack that matches the team's skills and provides long-term support.

The API will handle complex domain logic, including deck validation, card imports, and recommendation algorithms. We need a framework with mature libraries for database access, testing, and API documentation.

## Decision

We will use Java 25 LTS and Spring Boot 4.1.0 for the backend API. This stack offers team familiarity, a mature ecosystem, and excellent support for PostgreSQL, JPA, and Testcontainers. It also simplifies OpenAPI generation.

## Alternatives Considered

### Alternative 1: Node.js or Next.js API
- **Pros**: Shared language with the frontend, fast startup times.
- **Cons**: Mixes frontend and backend concerns, weaker support for complex multi-module domain logic.
- **Why rejected**: We want to keep frontend and backend concerns separate and prefer a strongly typed backend language.

### Alternative 2: Go
- **Pros**: Fast execution, low memory usage, simple deployment.
- **Cons**: Smaller ecosystem for enterprise patterns, lacks a framework as comprehensive as Spring Boot.
- **Why rejected**: The team has less familiarity with Go, and the ecosystem lacks the mature ORM and testing tools we need.

### Alternative 3: Python and FastAPI
- **Pros**: Great for data analysis, fast to write.
- **Cons**: Dynamic typing by default, weaker concurrency model compared to the JVM.
- **Why rejected**: We prefer the strong type system and mature ecosystem of the JVM for our core business logic.

## Consequences

### Positive
- Access to mature libraries for database access and testing.
- Strong type safety across the domain model.
- Long-term support from the Java and Spring Boot communities.

### Negative
- Higher memory footprint compared to Go or Node.js.
- Slower startup times during local development and deployment.

### Neutral
- Requires JVM knowledge for maintenance and operations.

## References

- Spring Boot Documentation
