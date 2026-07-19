---
name: prompt-engineering
description: >
  Use when writing, reviewing, or debugging complex agent prompts — proven
  frameworks for Forbidden Actions tables, Completion Criteria checklists,
  autonomy directives, and anti-pattern encoding. Prevents premature completion,
  scope creep, and uncontrolled agent behavior.
triggers:
  - "writing prompts for autonomous agent tasks"
  - "debugging a prompt that gives poor results"
  - "designing reusable prompt templates for others"
  - "hardening prompts for multi session quality"
not_for:
  - "simple one shot prompts or quick questions"
  - "interactive prompting where user steers each step"
  - "model selection or fine tuning decisions"
---

# Prompt Engineering for Agent Tasks

Complex agent prompts fail in predictable ways. These frameworks address the most common failure classes.

## Use when

- Writing prompts for autonomous tasks (ulw-loop, ralph-loop, etc.)
- Reviewing or debugging an existing prompt that produces bad results
- Creating multi-session templates that others will reuse

## Not for

- Simple one-shot prompts (single file edit, quick question)
- Interactive sessions where you steer each step
- Model selection or fine-tuning decisions

---

## 1 — Forbidden Actions Table

Verbal warnings are ignored. Numbered tables with concrete consequences are obeyed.

```markdown
| # | Forbidden Action | Why It Fails | What To Do Instead |
|---|-----------------|--------------|-------------------|
| 1 | **[Name]** — [description] | [Measurable consequence] | [Verifiable alternative] |
```

**Rules:** Only encode observed failures. 8–15 rows ordered by severity. "Why It Fails" must be specific. "What To Do Instead" must be verifiable.

---

## 2 — Completion Criteria Checklist

```markdown
You are done ONLY when ALL are true:
- [ ] [Deliverable exists and works — define "works"]
- [ ] [Quality gates pass — name tools and thresholds]
- [ ] [Tests pass — coverage ≥ X%]
- [ ] [Build succeeds — exact command]
If you cannot meet these, document the blocker — do NOT declare complete.
```

**Rules:** Checkbox format agents parse literally. Specific thresholds (not "tests pass"). Name verification commands. Include the "cannot meet" escape clause.

---

## 3 — Autonomy Directive

```markdown
Work continuously until all completion criteria are met OR you are blocked after 3 attempts.
Do NOT ask "Should I continue?" between phases.
If blocked, document in [progress tracker] and stop.
```

Omit for interactive sessions or exploratory tasks.

---

## 4 — Scope Fence

```markdown
ALL new code goes in `[target]`. `[source]` is READ-ONLY. Do NOT explore outside listed directories.
```

Name directories explicitly. Distinguish read-only from read-write. Cap exploration steps.

---

## 5 — Soft → Hard Rule Conversion

| Soft (ignored) | Hard (followed) |
|----------------|-----------------|
| "Keep commits small" | "Max 300 lines per commit. Verify with `git diff --cached --stat`." |
| "Monitor context" | "Max 5 commits per session. Count your commits." |
| "Write tests when possible" | "Every production-code commit MUST include unit tests." |
| "Be careful with quality" | "Zero detekt violations. Run `./gradlew detekt` before each commit." |

Pattern: remove hedging → add a number → add a verification command → add a consequence.

---

## 6 — Prompt Assembly Order

| # | Section | Required? |
|---|---------|-----------|
| 1 | **Mission** — one sentence | Always |
| 2 | **Paths** — dirs with read/write labels | When files involved |
| 3 | **Scope Fence** | When autonomy > 0 |
| 4 | **Steps** — ordered phases | Multi-phase tasks |
| 5 | **Forbidden Actions** | When autonomy > 0 |
| 6 | **Completion Criteria** | Always |
| 7 | **Autonomy Directive** | Continuation loops |
| 8 | **Lessons from Failures** | After first iteration |

---

## 7 — Iterative Hardening

After each run: identify failure → trace root cause → add fix (new Forbidden Action, Criteria item, or soft→hard rule) → document in Lessons table → bump version.

- **Patch**: wording fixes · **Minor**: new sections · **Major**: structural changes
