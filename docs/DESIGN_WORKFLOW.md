# Design Workflow

## Overview

This document explains the design-first philosophy and the design gate process used in this project. The goal is to prevent wasted implementation effort caused by jumping into code before the problem is fully understood.

## The Problem with Jumping Straight to Code

Agents (and humans) have a natural tendency to begin implementing as soon as a request arrives. This creates several problems:

- **Misunderstood requirements** — the implementation solves the wrong problem
- **Over-engineering** — building for hypothetical futures that never materialize (YAGNI violations)
- **Throwaway work** — hours of implementation discarded because the approach was wrong
- **Design debt** — code that works but is structured around an unconsidered design

A few minutes of structured design exploration prevents these problems.

## The Design-First Principle

**No implementation should begin until the design is explicitly confirmed by the user.**

This is a hard gate — not a guideline. The brainstorming skill enforces it.

## When to Use the Design Workflow

Use the `brainstorming` skill (environment-provided via [superpowers](https://github.com/obra/superpowers)) when:

- A new feature, system, or architectural component is requested
- The requirements are ambiguous or the scope is unclear
- Multiple approaches are plausible and trade-offs need evaluation
- The task involves integrating with external systems or APIs
- The user asks "how should we..." or "what's the best way to..."

You do NOT need the design workflow for:
- Single-line bug fixes with clear root cause
- Pure refactoring within an already-designed system
- Documentation-only changes
- Mechanical tasks (rename, move file, update version)

## The Four-Phase Process

The brainstorming skill runs through four phases before any code is written:

### Phase 1: Clarify Goals and Constraints

The agent asks Socratic questions to understand:
- What problem is being solved?
- Who is the user / consumer?
- What does "done" look like?
- What constraints apply (technology, performance, backward compatibility)?
- What is out of scope?

### Phase 2: Explore Options and Trade-offs

The agent presents **at least two options**, each with:
- A name and 1-2 sentence summary
- Pros and cons
- Whether it fits the stated constraints

Trade-off dimensions include: simplicity vs. flexibility, build vs. reuse, short-term vs. long-term, and reversibility.

### Phase 3: Recommend and Confirm

The agent states a recommendation and its rationale, then explicitly asks the user to confirm before proceeding.

**The agent will not write any code until the user explicitly confirms the design.**

### Phase 4: Commit the Design Doc

Once the user confirms, the agent:
1. Creates a design doc at `docs/plans/YYYY-MM-DD-<topic>-design.md`
2. Commits it to git
3. Then begins implementation

The design doc is a permanent record of what was decided and why.

## Design Doc Location and Format

All design docs live in `docs/plans/` and follow this naming convention:

```
docs/plans/YYYY-MM-DD-<topic>-design.md
```

Examples:
- `docs/plans/2026-03-05-user-auth-design.md`
- `docs/plans/2026-03-12-report-export-design.md`

The design doc template is embedded in the brainstorming skill. Required sections:
- Problem
- Goal
- Constraints
- Options Considered (with pros/cons per option)
- Decision (which option and why)
- Implementation Plan (high-level steps, not code)
- Open Questions

## How to Invoke

Load the `brainstorming` skill in OpenCode (environment-provided via [superpowers](https://github.com/obra/superpowers)):

```
skill(name="brainstorming")
```

The skill enforces design-first workflow with `disable-model-invocation: true`, which ensures the skill is loaded without model pre-processing.

## The Hard Gate — What It Means in Practice

"Hard gate" means the brainstorming skill will refuse to write code until confirmation is received. If a user pressures the agent to skip the design phase, the agent is instructed to push back:

> I understand the urgency, but skipping the design phase tends to create more work, not less.
> It will only take a few minutes to confirm the approach. Can we proceed with the quick
> clarification questions?

The gate exists because the cost of a few minutes of design exploration is always lower than the cost of throwing away an implementation.

## Relationship to Other Workflows

| Workflow | When to Use | Gate |
|---|---|---|
| `brainstorming` skill | New features, ambiguous scope, architectural decisions | Hard gate: no code before confirmation |
| `micro-commit-workflow` skill | Clear requirements, scope well understood | No design gate; follows TDD micro-commit workflow |
| Refactoring | Improving existing, well-understood code | No design gate; requires 80% test coverage |

For implementation after the design is confirmed, use the `micro-commit-workflow` skill, which follows the TDD micro-commit workflow documented in `docs/AI_AGENT_WORKFLOW.md`.

## See Also

- `brainstorming` skill (environment-provided via [superpowers](https://github.com/obra/superpowers)) — the skill that enforces this workflow
- `docs/AI_AGENT_WORKFLOW.md` — the TDD micro-commit workflow used after design confirmation
- `docs/CODING_PRACTICES.md` — YAGNI and design principles

---

*Last updated: 2026-03-05*
