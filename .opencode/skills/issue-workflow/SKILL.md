---
name: issue-workflow
description: >
  Use when selecting what to work on or completing a task — detects whether the
  project uses Beads (bd) or GitHub Issues and follows the appropriate workflow.
  Priority triage (P0-P1 before P2+), referencing issues in commits, and the
  mandatory issue-closing step after completion.
triggers:
  - "selecting which issue to work on next"
  - "starting work on an issue with proper workflow"
  - "closing completed work with tracker evidence"
  - "checking whether issues were left open after merge"
not_for:
  - "low level implementation or testing guidance"
  - "teams using jira instead of beads or github"
  - "pull request review triage or sprint reporting"
---

# Issue Workflow

**CRITICAL**: Always close the issue after completing work. Skipping this step is never acceptable.

---

## Tracker Detection

| Condition | Tracker | Commands |
|-----------|---------|----------|
| `.beads/` exists | Beads (`bd`) | `bd` CLI |
| `.beads/` does NOT exist | GitHub Issues | `gh issue` |

```bash
ls -d .beads/ 2>/dev/null && echo "TRACKER=beads" || echo "TRACKER=github"
```

---

## Use when

- Selecting which issue to work on next (priority triage)
- Starting work on an issue (branch naming, commit references)
- Completing work on an issue (closing with evidence)
- Auditing whether issues were closed after merging

## Not for

- Low-level coding, testing, or refactoring guidance inside the implementation itself
- Teams using Jira or another tracker instead of Beads or GitHub Issues
- Pull request review triage or sprint-board reporting

---

## Selecting Work

**Priority rule**: High priority before low. Always.

**Beads:** `bd ready --json` (unblocked, sorted by priority)

**GitHub:** `gh issue list --label "P1: should fix" --state open`

---

## Referencing Issues in Commits

- **Beads:** `feat(scope): add feature X (bd-42)`
- **GitHub:** `feat(scope): add feature X (closes #42)` — auto-closes on merge

---

## Closing Issues — MANDATORY

**Beads:**
```bash
bd close <id> --reason "Completed in commit <hash>. All tests pass." --json
```

**GitHub:**
```bash
gh issue close <number> --comment "Completed in commit <hash>. All tests pass ✅"
```

---

## Complete Lifecycle

**Beads:** ready → claim (`bd update <id> --claim`) → implement → commit → push → **close** → `bd dolt commit && bd dolt push`

**GitHub:** implement → commit (`closes #N`) → push → **close** (`gh issue close <N>`)

Never stop before the close step.

---

## Anti-Patterns

| Never | Always |
|---|---|
| Leave issue open after pushing | Close immediately after push |
| Skip closing comment/reason | Include summary + test status |
| Work on low-priority when high-priority exists | Drain high-priority first |
| Mix bd and gh tracking in the same project | Use one tracker consistently |
