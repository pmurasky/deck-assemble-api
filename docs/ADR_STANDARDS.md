# Architecture Decision Records (ADR)

## Overview

Architecture Decision Records capture significant architectural decisions along with their context and consequences. They provide a lightweight, version-controlled log of why the system is shaped the way it is.

ADRs are not bureaucracy -- they are a communication tool. A well-written ADR saves hours of "why did we do it this way?" conversations.

## When to Write an ADR

Write an ADR when a decision:

- **Affects structure**: changes module boundaries, introduces a new layer, or alters data flow between components
- **Is hard to reverse**: switching databases, choosing a framework, adopting a messaging protocol
- **Has multiple viable options**: if there was no real alternative, there is nothing to record
- **Crosses team boundaries**: impacts other teams' codebases, APIs, or deployment pipelines
- **Sets a precedent**: establishes a pattern that future work should follow (e.g., "all new services use gRPC")

### Do NOT Write an ADR For

- Routine implementation details (variable naming, loop vs. stream)
- Bug fixes or small refactorings
- Decisions already mandated by organizational policy (unless you are recording the rationale for adopting that policy locally)
- Temporary experiments or spikes (document these in spike tickets instead)

## ADR Template

Store ADRs in a `docs/adr/` directory at the repository root. Use sequential numbering with a short slug:

```
docs/adr/
  0001-use-postgresql-for-primary-datastore.md
  0002-adopt-event-driven-architecture.md
  0003-select-grpc-for-inter-service-communication.md
```

### Template

```markdown
# ADR-NNNN: <Short Decision Title>

## Status

Proposed | Accepted | Deprecated | Superseded by `ADR-NNNN` (`NNNN-slug.md`)

## Date

YYYY-MM-DD

## Context

What is the issue or problem that motivated this decision?
Include relevant constraints, requirements, and forces at play.
Be specific -- link to tickets, metrics, or incidents where appropriate.

## Decision

State the decision clearly in one or two sentences.
Then explain the approach in enough detail that a new team member
could understand what was chosen and how it works.

## Alternatives Considered

### Alternative 1: <Name>
- **Pros**: ...
- **Cons**: ...
- **Why rejected**: ...

### Alternative 2: <Name>
- **Pros**: ...
- **Cons**: ...
- **Why rejected**: ...

## Consequences

### Positive
- What becomes easier or better as a result of this decision?

### Negative
- What trade-offs are we accepting?
- What new risks or constraints does this introduce?

### Neutral
- What changes without being clearly better or worse?

## References

- Links to relevant RFCs, documentation, benchmarks, or prior art
```

## ADR Lifecycle

### Statuses

| Status | Meaning |
|--------|---------|
| **Proposed** | Under discussion, not yet approved |
| **Accepted** | Approved and in effect |
| **Deprecated** | No longer recommended but still present in the codebase |
| **Superseded** | Replaced by a newer ADR (link to it) |

### Rules

1. **ADRs are immutable once accepted.** Do not edit the body of an accepted ADR. If a decision changes, write a new ADR that supersedes the old one and update the old ADR's status.
2. **Number sequentially.** Never reuse or renumber ADRs. Gaps in numbering are acceptable (e.g., if a proposed ADR is withdrawn).
3. **Keep them short.** A good ADR is one page. If it exceeds two pages, the decision may need to be broken into smaller decisions.
4. **One decision per ADR.** Do not bundle multiple decisions into a single record.

## Maintaining ADRs Over Time

### Regular Review

- **Quarterly review**: During architecture review sessions, scan ADRs with `Accepted` status. Ask: "Is this still true? Has anything changed?"
- **On-incident review**: After a production incident caused by an architectural limitation, check if there is an ADR that predicted the trade-off. If not, write one retroactively.
- **On-boarding**: New team members should read all `Accepted` ADRs as part of onboarding. If any are confusing, that is a signal to improve them.

### Superseding an ADR

When a decision is reversed or significantly changed:

1. Write a new ADR explaining the new decision and why the old one no longer applies
2. In the new ADR's **Context** section, reference the old ADR
3. Update the old ADR's **Status** to `Superseded by ADR-NNNN (NNNN-slug.md)`
4. Do NOT modify any other content in the old ADR

### Deprecating an ADR

When a decision becomes irrelevant (e.g., the component it describes was decommissioned):

1. Update the status to `Deprecated`
2. Add a one-line note below the status explaining why (e.g., "Service X was decommissioned on YYYY-MM-DD")
3. Do NOT delete the ADR -- it remains part of the historical record

## Integration with Development Workflow

### When to Propose ADRs

- **Before starting work**: If the task requires an architectural decision, propose the ADR in the PR/MR before writing implementation code
- **During design review**: ADRs serve as the artifact for design discussions
- **Retroactively**: If an undocumented architectural decision is discovered, write an ADR to capture it

### Review Process

1. Author creates the ADR with status `Proposed` and opens a PR
2. Team reviews the ADR as part of the PR (treat it like a code review)
3. Once approved, update status to `Accepted` and merge
4. Implementation work can reference the ADR in commit messages (e.g., `feat(auth): implement OAuth2 flow (see ADR-0005)`)

### Commit Message Convention

When committing ADR documents, use:

```
docs(adr): add ADR-NNNN <short description>
```

When committing work that implements an ADR:

```
feat(scope): <description> (implements ADR-NNNN)
```

## Example ADR

```markdown
# ADR-0001: Use PostgreSQL for Primary Datastore

## Status

Accepted

## Date

2026-01-15

## Context

The application needs a primary datastore for user accounts, orders,
and product catalog. The data is highly relational with complex query
requirements (joins across 5+ tables for reporting). We expect
~10,000 concurrent users at peak and need ACID transactions for
order processing.

## Decision

Use PostgreSQL 16 as the primary relational datastore, deployed as
a managed instance (e.g., AWS RDS or GCP Cloud SQL).

## Alternatives Considered

### Alternative 1: MySQL 8
- **Pros**: Wide adoption, good tooling, lower memory footprint
- **Cons**: Weaker JSON support, less advanced indexing (no partial indexes)
- **Why rejected**: Our reporting queries benefit from PostgreSQL's
  query planner and partial index support

### Alternative 2: MongoDB
- **Pros**: Flexible schema, horizontal scaling
- **Cons**: No native joins, eventual consistency by default
- **Why rejected**: Data is inherently relational; document model
  would require denormalization that increases update complexity

## Consequences

### Positive
- Strong ACID guarantees simplify order processing logic
- Rich indexing options support complex reporting queries
- Large ecosystem of extensions (PostGIS, pg_trgm) for future needs

### Negative
- Vertical scaling limits may require read replicas at ~50K concurrent users
- Team has less PostgreSQL operational experience than MySQL

### Neutral
- Migration tooling (Flyway/Liquibase) works equally well with either RDBMS

## References

- PostgreSQL 16 release notes: https://www.postgresql.org/docs/16/release-16.html
- Internal load test results: JIRA-1234
```

## Quick Reference

| Question | Answer |
|----------|--------|
| Where do ADRs live? | `docs/adr/` in the repository root |
| Who can propose an ADR? | Any team member |
| Who approves an ADR? | The team, via PR review |
| Can I edit an accepted ADR? | No. Write a new one that supersedes it |
| Minimum content? | Status, Date, Context, Decision, Consequences |
| Maximum length? | Aim for one page; two pages is the upper bound |

---

**Last Updated**: February 19, 2026
**Version**: 1.0
