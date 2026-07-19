---
name: progress-tracker
description: >
  Use when working on any multi-session task — defines the boulder/progress tracker
  pattern for maintaining state across LLM sessions: file structure, update protocol,
  NEXT ACTION specificity, compaction resilience, and resume protocol.
triggers:
  - "creating a progress tracker for multi session work"
  - "updating a boulder file after each commit"
  - "resuming a large task from previous session"
  - "writing a specific next action for handoff"
not_for:
  - "single session tasks under ten commits"
  - "planning what to work on next"
  - "monitoring context health during active session"
---

# Progress Tracker (Boulder Pattern)

An external progress file — the **boulder** — is the agent's persistent memory across sessions.

## Use when

- Any task expected to span multiple sessions (more than ~10 commits)
- Large refactors, migrations, feature epics, conversion projects
- When resuming work started in a previous session

## Not for

- Single-session tasks (< 10 commits expected)
- Planning what to work on — use `issue-workflow` instead
- Monitoring context health mid-session — use `context-budget` instead

---

## Boulder File Structure

Create at project root (e.g., `PROJECT-BOULDER.md`) with four sections:

**Phase Table** — overall progress:

| Phase | Description | Status | Commits | Notes |
|-------|-------------|--------|---------|-------|
| 0 | Scaffolding | DONE | 4 | |
| 1 | Implementation | IN PROGRESS | 12/20 | |
| 2 | Quality | NOT STARTED | 0 | |

**Metrics** — key quality metrics: unit tests passing, coverage %, total commits.

**Commit History** — table of every commit: `# | Hash | Description`

**NEXT ACTION** — the most critical section (see below).

---

## NEXT ACTION Specificity

Must pass this test: **could a different agent with no prior context read ONLY the NEXT ACTION and know exactly what to do?**

| Bad (vague) | Good (specific) |
|---|---|
| "Continue phase 2" | "Implement PaymentService.processRefund() with unit test for successful refund and insufficient-balance edge case" |
| "Fix remaining issues" | "Fix detekt MaxLineLength violation in RouteConfig.kt lines 42, 67 — extract route blocks into extension functions" |

Template:
```markdown
## NEXT ACTION
[ACTION VERB] `[Exact class/function]` with [specific details].
[CONTEXT: what already exists that this builds on].
[ACCEPTANCE: how to know this step is done].
```

---

## Update Protocol

**Update the boulder after EVERY commit.** Increment phase commit count, update metrics, add commit to history, rewrite NEXT ACTION. Then commit the boulder separately (`docs: update boulder — [progress]`). Boulder commits do NOT count toward your session commit budget.

---

## Compaction Resilience

- **Boulder = memory** (current position, next step, metrics)
- **Plan = roadmap** (phase structure — read once at session start)
- **Git log = safety net** (authoritative but verbose)

If compaction occurs mid-session: re-read the boulder only — it's self-sufficient.

---

## Resume Protocol

1. Read the boulder file first — exact position, metrics, next step.
2. Skim the plan file for phase structure.
3. `git status` and `git log --oneline -5`
4. Run the build — confirm green.
5. Reset session commit counter to 0.
6. Begin from NEXT ACTION — do not re-explore.

**Skip on resume:** standards docs, codebase re-exploration, background agents, source material already analyzed.

---

## Anti-Patterns

| Never | Always |
|---|---|
| Skip boulder update after a commit | Update after every commit |
| Write a vague NEXT ACTION | Be hyper-specific (class, method, test, edge case) |
| Store architectural decisions in the boulder | Put those in the plan file or ADRs |
| Treat boulder as append-only log | Rewrite NEXT ACTION and metrics each time |
| Forget to commit the boulder | Commit as a separate docs commit |
