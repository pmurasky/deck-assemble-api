---
name: multi-session-execution
description: >
  Use when a task requires more than one LLM session to complete — mechanical commit
  caps instead of soft context monitoring, phase-based session budgets, context exhaustion
  protocol, session boundary hygiene, and resume anti-patterns.
triggers:
  - "planning work that will span multiple sessions"
  - "defining commit caps for each task phase"
  - "approaching a session commit cap mid work"
  - "setting clean resume rules for future sessions"
not_for:
  - "single session tasks under ten commits"
  - "progress tracker file structure design"
  - "mid session context health monitoring only"
---

# Multi-Session Execution

LLM agents cannot accurately estimate remaining context. Replace soft monitoring with **hard mechanical limits**.

## Use when

- Any task expected to span multiple sessions
- When the plan has defined phases with estimated commit counts
- When an orchestrator provides a session budget table
- Mid-session when approaching a commit cap

## Not for

- Single-session tasks (< 10 commits total)
- Progress tracking file structure — use `progress-tracker` instead
- Mid-session context health monitoring — use `context-budget` instead

---

## Core Rule: Count Commits, Not Tokens

Define a **hard commit cap** per session. Count commits after every commit. When you hit the cap, execute the exhaustion protocol — no judgment calls, no exceptions.

---

## Session Budget Table

| Task Type | Default Cap |
|-----------|-------------|
| Boilerplate / scaffolding | 10 commits/session |
| Feature implementation | 7 commits/session |
| Refactoring | 8 commits/session |
| Bug fixing | 5 commits/session |

Define custom budgets in the plan when phases have specific requirements.

---

## Context Exhaustion Protocol

When you hit the commit cap:

1. Finish the current micro-task — complete RED-GREEN-COMMIT in progress.
2. Do NOT start the next task.
3. Run the build — verify all tests pass.
4. `git status` must show clean — commit any uncommitted work.
5. Update the progress tracker with metrics and NEXT ACTION.
6. `git push`
7. Stop.

**Emergency:** If you see compression signals (shorter responses, forgotten context, imprecise tool calls) before hitting the cap, execute the protocol immediately.

---

## Session Boundary Hygiene

**Clean start:** Read progress tracker → `git status` → `git log --oneline -5` → run build → reset commit counter → note phase cap.

**Clean end:** All work committed → tests pass → build green → progress tracker updated with specific NEXT ACTION → all commits pushed.

---

## Resume Protocol

1. Read the progress tracker — it has your exact position and NEXT ACTION.
2. `git status` and `git log --oneline -5` to verify state.
3. Run the build — confirm green before making any changes.
4. Reset session commit counter to 0.
5. Continue from NEXT ACTION — do not re-explore or re-read.

**Skip on resume:** standards documents, codebase exploration, source material already analyzed, plan file deep-read.

---

## Anti-Patterns

| Never | Always |
|---|---|
| Launch background agents on resume | Direct tools only — they burn context |
| Re-read all standards docs | Standards are in your skills — skip |
| Re-explore the codebase | Read boulder, continue from NEXT ACTION |
| Estimate remaining context subjectively | Count commits against the cap |
| Start a new phase near the commit cap | End session, start fresh |
| Skip build check on resume | Always verify green before proceeding |
| End session with uncommitted work | Clean working tree, all pushed |
