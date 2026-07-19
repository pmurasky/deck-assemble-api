# ADR-0016: Use PATCH for partial resource updates

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble API needs to support resource updates, such as modifying a deck's name, description, or card list. We need to define the HTTP methods used for these updates.

Using `PUT` for all updates forces clients to send the entire resource representation, which is inefficient and prone to overwriting concurrent changes.

## Decision

We will use the HTTP `PATCH` method for partial resource updates. The HTTP `POST` method will be used for resource creation and complex operations. Retrievals will use the HTTP `GET` method, while removals will use `DELETE`.

We will avoid using `PUT` unless a complete replacement of the resource is explicitly intended.

## Alternatives Considered

### Alternative 1: PUT-only updates
- **Pros**: Simpler backend implementation, idempotent by definition.
- **Cons**: Forces clients to send the entire resource representation, increases payload size, increases risk of overwriting concurrent updates.
- **Why rejected**: `PUT` is inefficient for large resources like decks or collections where only a single field might change.

### Alternative 2: POST-only for all updates
- **Pros**: Simple routing, avoids issues with client support for PATCH.
- **Cons**: Violates REST semantics, makes the API less intuitive for external developers.
- **Why rejected**: We want to follow standard REST semantics to ensure the API is intuitive and easy to integrate with.

## Consequences

### Positive
- Efficient client updates with smaller payloads.
- Clear REST semantics that align with industry standards.
- Reduced risk of accidental data overwrites.

### Negative
- Backend implementation is slightly more complex as it must handle partial updates.
- Requires careful handling of null values vs omitted fields in JSON payloads.

### Neutral
- API documentation must clearly specify which fields are patchable.

## References

- RFC 5789: PATCH Method for HTTP
- ADR-0006: Use REST and OpenAPI
