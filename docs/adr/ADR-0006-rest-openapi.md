# ADR-0006: Use REST and OpenAPI

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble frontend needs to communicate with the backend API. We need to define the communication protocol, serialization format, and API documentation strategy. The API must support web clients, and potentially mobile or third-party clients in the future.

We want to ensure that the API is easy to consume, well-documented, and supports type generation for client applications.

## Decision

We will use a versioned REST API (`/api/v1`) with camelCase JSON payloads. The OpenAPI specification will be generated directly from the backend code using Springdoc OpenAPI. This specification will serve as the contract, allowing the frontend to generate TypeScript types automatically.

## Alternatives Considered

### Alternative 1: GraphQL
- **Pros**: Clients can request exactly the data they need, reduces over-fetching.
- **Cons**: Adds significant complexity to the backend, harder to cache, requires more setup.
- **Why rejected**: The application is currently CRUD-heavy, making the complexity of GraphQL unnecessary.

### Alternative 2: gRPC
- **Pros**: High performance, binary serialization, contract-first design.
- **Cons**: Poor native browser support, requires proxy layers for web clients.
- **Why rejected**: Web browser compatibility is a primary requirement, and gRPC introduces too much friction for web clients.

### Alternative 3: JSON-RPC
- **Pros**: Simple protocol, easy to implement.
- **Cons**: Lacks standard tooling, less ecosystem support compared to REST.
- **Why rejected**: REST has much better tooling, documentation standards, and developer familiarity.

## Consequences

### Positive
- Broad client support and developer familiarity.
- Automated API documentation that stays in sync with the code.
- Frontend type safety via generated TypeScript clients.

### Negative
- Potential for over-fetching or under-fetching data compared to GraphQL.
- Multiple round trips may be required for complex views.

### Neutral
- API changes must respect versioning guidelines to avoid breaking clients.

## References

- OpenAPI Specification
- Springdoc OpenAPI Documentation
