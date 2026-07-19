# Implementation Plan: <Feature Name>

**Date**: YYYY-MM-DD
**Status**: Draft | In Progress | Completed
**Author**: AI agent + <user name if known>
**Related design doc**: `docs/plans/YYYY-MM-DD-<topic>-design.md` *(if applicable)*
**Closes / relates to**: #<issue number> *(if applicable)*

---

## Tier 1 — Simple Todo List

*Use this tier for small changes (1–2 files, no architectural decisions, obvious scope).*
*Delete the Tier 2 section below if using Tier 1.*

- [ ] Task 1
- [ ] Task 2
- [ ] Task 3

---

## Tier 2 — Structured Task Spec

*Use this tier for larger changes (3+ files, architectural decisions, or non-obvious*
*acceptance criteria). Delete the Tier 1 section above if using Tier 2.*

### Context

[1–3 sentences: what problem does this plan solve? Why now?]

### Scope

**In scope:**
- [Item 1]
- [Item 2]

**Out of scope:**
- [Item 1]
- [Item 2]

### Tasks

---

#### Task 1: <Short imperative label>

| Field | Value |
|---|---|
| **Files affected** | `path/to/FileA.kt` (modify), `path/to/FileB.kt` (create) |
| **Expected behavior change** | [What will be different after this task completes] |
| **Done when** | [Verifiable acceptance criterion — e.g., "unit tests pass", "endpoint returns 200", "class has no more than 2 responsibilities"] |
| **Execution mode** | `sequential` — must run after Task N |

**Notes**: [Optional: any implementation hints, gotchas, or design notes]

---

#### Task 2: <Short imperative label>

| Field | Value |
|---|---|
| **Files affected** | `path/to/FileC.kt` (modify) |
| **Expected behavior change** | [What will be different after this task completes] |
| **Done when** | [Verifiable acceptance criterion] |
| **Execution mode** | `parallel` — independent of other tasks |

**Notes**: [Optional]

---

#### Task 3: <Short imperative label>

| Field | Value |
|---|---|
| **Files affected** | `path/to/FileD.kt` (modify), `README.md` (modify) |
| **Expected behavior change** | [What will be different after this task completes] |
| **Done when** | [Verifiable acceptance criterion] |
| **Execution mode** | `sequential` — must run after Task 2 |

**Notes**: [Optional]

---

### Acceptance Criteria (Plan-Level)

The plan is complete when ALL of the following are true:

- [ ] All tasks are marked completed
- [ ] All unit tests pass
- [ ] All integration tests pass (if applicable)
- [ ] No lint or static analysis violations
- [ ] All planned files exist and match the expected behavior

### Risks and Open Questions

- [Risk or open question 1]
- [Risk or open question 2]

---

*See `docs/IMPLEMENTATION_PLANNING.md` for guidance on when to use Tier 1 vs Tier 2.*
