# Implementation Planning Standards

## Overview

Before writing code, write a plan. This document explains when a plan is required, what
it must contain, and how it fits into the development workflow.

Plans live at `docs/plans/YYYY-MM-DD-<feature>-plan.md` and are committed to git before
implementation begins. The plan is the source of truth for spec-compliance review.

---

## When to Write a Plan

| Situation | Plan required? | Tier |
|---|---|---|
| Single-file change, no architectural decisions | Optional | Tier 1 (todo list) |
| Change touches 1–2 files, behavior is obvious | Optional | Tier 1 (todo list) |
| Change touches 3+ files | **Yes** | Tier 1 or 2 |
| New feature with architectural decisions | **Yes** | Tier 2 (task spec) |
| Refactoring that crosses class or package boundaries | **Yes** | Tier 2 (task spec) |
| Epic-level work or multiple sub-features | **Yes** | Tier 2 (task spec) |
| Unclear acceptance criteria | **Yes** (clarify first) | Tier 2 (task spec) |

**When in doubt, write a plan.** A 10-minute plan prevents hours of rework.

---

## Two-Tier Format

### Tier 1 — Simple Todo List

Use when the scope is small and the approach is unambiguous.

```markdown
## Tasks

- [ ] Add `generateTimestamp()` helper to `ReportGenerator`
- [ ] Update `buildReportPath()` to include timestamp in filename
- [ ] Update test assertions to match new filename pattern
```

No additional structure required. Commit the todo list and start working.

### Tier 2 — Structured Task Spec

Use when the scope is larger, involves architectural decisions, or has non-obvious
acceptance criteria.

See `docs/templates/PLAN_TEMPLATE.md` for the full template.

Each task in a Tier 2 plan includes:

| Field | Description |
|---|---|
| **Task name** | Short imperative label (e.g., "Extract `TimestampGenerator` class") |
| **Files affected** | Explicit list of files to create or modify |
| **Expected behavior change** | What will be different after this task |
| **Done when** | Verifiable acceptance criteria |
| **Execution mode** | `sequential` (depends on prior task) or `parallel` (independent) |

---

## Plan Lifecycle

### 1. Write the Plan

Create the plan file at:
```
docs/plans/YYYY-MM-DD-<feature>-plan.md
```

Use today's date. Use a short, descriptive kebab-case label for `<feature>`.

For Tier 2 plans: use the template at `docs/templates/PLAN_TEMPLATE.md`.

### 2. Commit the Plan

```bash
git add docs/plans/YYYY-MM-DD-<feature>-plan.md
git commit -m "docs(plan): add implementation plan for <feature>"
```

**Do NOT begin implementation until this commit succeeds.**

### 3. Implement

Execute tasks in order. For each task:
- Mark it `in_progress` in your task tracker
- Make the change
- Run unit tests
- Commit
- Mark it `completed`

After every 3 completed tasks, stop and verify all 3 passed quality gates before
continuing (batch checkpoint).

### 4. Close the Plan

When all tasks are complete, update the plan status to `Completed` and commit:

```bash
git add docs/plans/YYYY-MM-DD-<feature>-plan.md
git commit -m "docs(plan): mark <feature> plan as completed"
```

---

## Plan as Source of Truth

The plan is the source of truth for spec-compliance review. A reviewer checking
whether the implementation matches the plan should be able to answer:

- Were all planned files created or modified?
- Does the actual behavior change match the expected behavior change?
- Is every "done when" criterion satisfied?

**Do not trust the implementer's report — read the code and the plan side by side.**

---

## Relationship to Design Docs

A design doc (`docs/plans/YYYY-MM-DD-<topic>-design.md`, produced by the
`brainstorming` skill) captures the architectural decision — *what* to build and *why*.

An implementation plan (`docs/plans/YYYY-MM-DD-<feature>-plan.md`) captures the
execution steps — *how* to build it and *in what order*.

For large features:
1. Design doc first (`brainstorming` skill → confirm → commit; environment-provided via superpowers)
2. Implementation plan second (`writing-plans` skill → commit; environment-provided via superpowers)
3. Implementation third

For small features, a Tier 1 plan is sufficient — no separate design doc needed.

---

## Invoke the Skill

Use the `writing-plans` skill (environment-provided via [superpowers](https://github.com/obra/superpowers)) interactively in OpenCode:

```
skill(name="writing-plans")
```

The skill guides you through selecting the right tier, filling in the task fields,
and committing the plan before implementation begins.

---

*Last updated: March 2026*
*See also: `docs/templates/PLAN_TEMPLATE.md`, `docs/AI_AGENT_WORKFLOW.md`*
