---
name: context-budget
description: >
  Use when mid-session on a multi-step task — monitors context health, provides
  commit-count triggers, context exhaustion protocol (step-by-step graceful handoff),
  clean exit criteria, and session handoff template so the next session can resume cleanly.
triggers:
  - "checking context health during a long session"
  - "deciding whether to continue after many commits"
  - "responses feel compressed or context was compacted"
  - "writing a clean handoff before ending session"
not_for:
  - "planning what issue or task to start"
  - "writing commit messages or commit strategy"
---

# Context Budget — Active Mid-Session Checklist

## Use when

- When mid-session on a multi-step task and you need to check context health
- When you have made 8+ commits in a session and need to decide whether to continue
- When responses feel compressed or a compaction event occurred
- When finishing a session and need to write a clean handoff summary

## Not for

- Planning what to work on next — use `issue-workflow` instead
- Writing commit messages — use `micro-commit-workflow` instead

---

## Commit Count Trigger

| Commits in session | Action |
|--------------------|--------|
| 1–9 | Continue normally |
| **10+** | **Finish current task, then stop** |
| 15+ | Hard stop — do not start any new task |

---

## Other Exhaustion Triggers

Stop immediately if any of these fire:

- [ ] 10+ commits in this session
- [ ] 100+ tool calls in this session
- [ ] Cannot confidently fit the next feature within remaining budget
- [ ] Responses getting shorter or context feels compressed
- [ ] A compaction event occurred

---

## Context Exhaustion Protocol

1. Finish the current micro-task — complete RED → GREEN → COMMIT.
2. Do NOT start the next task.
3. Verify build passes — run unit tests.
4. Push all commits — `git push`.
5. Write handoff summary (see template below).
6. Stop.

---

## Clean Exit Criteria

- [ ] All commits pushed (`git status` clean)
- [ ] All unit tests pass
- [ ] Build succeeds
- [ ] No lint errors
- [ ] Handoff summary written with a specific NEXT ACTION
- [ ] Progress tracker committed and pushed

---

## Session Handoff Template

```
## Session Handoff — <DATE>

### What was completed
- <hash>: <description>

### Current state
- Branch: <name> | Tests: PASS/FAIL | Build: PASS/FAIL

### NEXT ACTION
<Exactly what to do first — one specific sentence>

### Open issues / blockers
- <anything the next session needs to know>

### Session metrics
- Commits: <N> | Files changed: <N> | Tests added: <N>
```

---

## Resume Protocol

1. Read progress tracker / handoff summary first.
2. `git status` and `git log --oneline -10` to verify state.
3. Run unit tests to confirm green.
4. Reset commit counter to 0.
5. Continue from NEXT ACTION — skip re-reading standards or re-exploring known files.
6. Use direct tools only — do not launch background agents on resume.

---

## Mechanical Limits

- Define a **commit cap** per work phase (e.g., 7 for implementation, 10 for scaffolding).
- Count commits, not tokens — increment after every commit.
- When you hit the cap, execute the exhaustion protocol immediately.
- For phase-based budgets, load the `multi-session-execution` skill.

---

## Anti-Patterns

- ❌ Starting a new feature at 10+ commits
- ❌ Ending a session on a failing build
- ❌ Vague NEXT ACTION ("continue the work")
- ❌ Skipping the handoff summary
- ❌ Re-reading standards or re-exploring known files at session start
- ❌ Launching background agents on resume — they burn context
- ❌ Estimating remaining context subjectively — count commits instead
