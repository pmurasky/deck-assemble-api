# Design Document Standards

## Overview

A design document is a short, written record of a proposed solution — what you plan to build, why you chose that approach, and what alternatives you considered. It is written **before** implementation begins and reviewed by at least one other person.

Design docs are not bureaucracy. A well-written design doc prevents wasted work, surfaces blind spots early, and creates a record that future team members can read to understand *why* the system is shaped the way it is.

For capturing a single architectural decision (e.g., "we chose PostgreSQL"), use an [ADR](./ADR_STANDARDS.md) instead. Design docs cover broader features or initiatives that span multiple decisions.

---

## When to Write a Design Doc

Write a design doc when **any** of the following are true:

- **Touches 3+ files** with non-trivial logic changes
- **Introduces a new abstraction** — new interface, new service, new data model
- **Has architectural impact** — changes module boundaries, data flow, or deployment topology
- **Has multiple viable approaches** — if there's only one obvious solution, a design doc adds little value
- **Crosses team or system boundaries** — impacts other teams' APIs, contracts, or pipelines
- **Is risky or irreversible** — hard to undo, or failure would have significant consequences
- **Requires buy-in** — stakeholders need to understand the approach before work starts

### Do NOT Write a Design Doc For

- Small bug fixes (< 3 files, no design decision)
- Trivial refactors (rename, reorder, extract with no behavior change)
- Configuration-only changes
- Work already fully specified in an existing approved design doc
- Purely mechanical conversions with no decisions to make

**Rule of thumb**: If you could explain the full approach in one sentence with no trade-offs, skip the design doc and go straight to an ADR or a task plan.

---

## Required Sections

Every design doc must include all of the following sections. See `docs/templates/DESIGN_TEMPLATE.md` for the fill-in-the-blank template.

### 1. Background

*Why are we doing this?*

Describe the problem or opportunity. Include:
- Current state and its pain points
- Why this matters now (urgency or opportunity)
- Any constraints inherited from history

Keep it to 2–5 sentences. Link to tickets, incidents, or data where relevant.

### 2. Goal

*What does success look like?*

State the desired outcome in measurable or verifiable terms. Use bullet points if there are multiple goals. Distinguish **primary goals** (must achieve) from **secondary goals** (nice to have).

### 3. Non-Goals

*What are we explicitly NOT solving?*

List things that are out of scope. This prevents scope creep and makes the review conversation more focused.

### 4. Constraints

*What can't we change?*

List hard boundaries: technology choices already made, performance requirements, compatibility obligations, regulatory requirements, or team capacity limits.

### 5. Options Considered

*What approaches did you evaluate?*

List at least **two options**, including the one you chose. For each option, state:
- What it is (1–2 sentences)
- Pros
- Cons

Do not omit options you rejected — rejected options help future readers understand what was already tried.

### 6. Decision

*What did you choose, and why?*

State the chosen approach clearly in one or two sentences. Then explain the reasoning: why this option over the alternatives, given the constraints and goals.

### 7. Implementation Plan

*How will you build it?*

A high-level task breakdown. Not a full spec — just enough for a reviewer to understand sequencing and scope. Reference an implementation plan file (e.g., `docs/plans/YYYY-MM-DD-topic.md`) if one exists.

### 8. Open Questions

*What is still unresolved?*

List anything that needs a decision before or during implementation. Assign an owner and a target date if possible. Remove items from this list as they are resolved (update the doc, don't delete history).

---

## Design Doc Lifecycle

```
Draft → In Review → Approved → Superseded / Archived
```

| Status | Meaning |
|--------|---------|
| **Draft** | Author is still writing; not ready for review |
| **In Review** | Shared for feedback; implementation has not started |
| **Approved** | At least one reviewer has signed off; implementation may begin |
| **Superseded** | A newer design doc replaces this one (link to successor) |
| **Archived** | Work was cancelled or no longer relevant |

Update the `Status` field at the top of the document as it moves through the lifecycle. Never delete a design doc — archive it.

---

## Review Process

### Who Reviews

- **At minimum**: one peer familiar with the affected system
- **Recommended**: one person unfamiliar with the system (catch unclear explanations)
- **For cross-team changes**: a representative from each affected team

### What Reviewers Look For

Reviewers should ask:

1. **Is the problem clearly stated?** Would a new team member understand why this work is needed?
2. **Are the options genuinely distinct?** Or is one option a strawman?
3. **Is the decision justified?** Does the reasoning connect to the stated goals and constraints?
4. **Are the non-goals explicit?** Could this design accidentally solve something it shouldn't?
5. **Are the open questions real blockers?** Or are they deferred decisions that should be made now?
6. **Is the implementation plan realistic?** Does it match the complexity of the decision?

### Review Turnaround

- Authors should request review with at least **2 business days** lead time
- Reviewers should respond within **2 business days**
- Stale reviews (no response after 3 business days) may be treated as tacit approval with a note in the doc

---

## File Naming and Storage

Store design docs in `docs/plans/` using the naming convention:

```
docs/plans/YYYY-MM-DD-<short-slug>-design.md
```

Examples:
```
docs/plans/2026-03-06-payment-retry-design.md
docs/plans/2026-04-15-search-index-design.md
```

Use the date the doc was first created, not the date it was approved.

---

## Relationship to Other Documents

| Document type | Purpose | When to use |
|---------------|---------|-------------|
| **Design doc** | Explore options, justify a decision, plan a feature | New feature, significant change, cross-team impact |
| **ADR** | Record a single architectural decision | Technology choice, pattern adoption, one-off decision |
| **Implementation plan** | Task breakdown for executing an approved design | After design doc is approved |
| **Spike / research note** | Explore unknowns with a time-boxed investigation | When the design space is unclear before writing a design doc |

---

## Quick Reference

**Write a design doc when**: 3+ files, new abstraction, architectural impact, multiple approaches, cross-team.

**Required sections**: Background · Goal · Non-Goals · Constraints · Options Considered · Decision · Implementation Plan · Open Questions.

**Lifecycle**: Draft → In Review → Approved → Superseded / Archived.

**Template**: `docs/templates/DESIGN_TEMPLATE.md`

---

**Last Updated**: March 2026
**Version**: 1.0
