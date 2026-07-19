# Context Budget Standards

**PURPOSE**: Standards for managing AI agent context windows during long-running tasks. Covers token estimation, budget allocation, graceful session handoff, and multi-session resilience. Prevents the failure mode where an agent runs out of context mid-task, leaving uncommitted work, stale progress trackers, and broken builds.

---

## 1. Why Context Budgets Matter

Every AI model has a finite context window (e.g., ~200K tokens for Claude Opus). This window holds **everything**: system prompt, agent config, tool calls and results, all file reads, all code written, and all conversation history. Once the window fills up, the model is forcibly stopped — often mid-sentence, mid-file, or mid-commit.

**Without budget management**, agents:
- Run out of context mid-feature, leaving uncommitted code
- Leave progress trackers (boulder files, todo lists) stale
- Exit on a failing build that the next session must debug
- Waste context re-reading files already consumed in the same session

**With budget management**, agents:
- Stop cleanly between commits, never mid-work
- Hand off to the next session with an exact recovery point
- Always exit on a green build
- Allocate context deliberately to maximize productive output

---

## 2. Token Estimation

### 2.1 Rules of Thumb

| Content Type | Size | Approximate Tokens |
|-------------|------|-------------------|
| 1 KB of code/text | 1 KB | ~250 tokens |
| 1 KB of minified JSON | 1 KB | ~330 tokens |
| Typical source file read (tool call + result) | 8–15 KB | 2–4K tokens |
| System prompt + agent config | 5–15 KB | 1.5–4K tokens |
| Standards doc (e.g., CODING_PRACTICES.md) | 20–40 KB | 5–10K tokens |
| Large file read (build.gradle, config) | 3–8 KB | 1–2K tokens |
| Tool call overhead (request + response framing) | ~1 KB | ~250 tokens per call |
| Writing a Kotlin/Java/Python file | 3–8 KB | 1–2K tokens |
| One TDD micro-commit cycle (read source → write test → write impl → run tests → commit) | 15–30 KB | 4–8K tokens |

### 2.2 Measuring Your Budget

For a model with a 200K-token context window:

| Component | Typical Cost | Notes |
|-----------|-------------|-------|
| **Fixed overhead** (system prompt, agent config, skills) | 10–20K tokens | Always consumed; cannot reduce without changing config |
| **Standards/plan/boulder reads** (one-time per session) | 15–40K tokens | Minimize by reading only what's needed |
| **Working budget** (what's left for actual work) | 140–175K tokens | This is your productive capacity |

**Working budget formula:**

```
Working Budget = Context Window − Fixed Overhead − One-Time Reads
```

For a 200K-token model with typical overhead:
- Fresh session: ~155K tokens available for work
- Resume session (skip standards reads): ~170K tokens available for work

### 2.3 Estimating Task Size

Before starting a large task, estimate total tokens needed:

```
Total Tokens = (Number of Features) × (Tokens per Feature) + Fixed Overhead
```

| Task Type | Tokens per Unit | Example |
|-----------|----------------|---------|
| Simple file creation | 2–4K tokens | Config file, data class |
| Endpoint with TDD | 8–15K tokens | REST endpoint + tests + model |
| Complex service with TDD | 15–25K tokens | Service class + strategy pattern + tests |
| Full microservice conversion | 800K–1.2M tokens | 15+ endpoints, 200+ files |

If total tokens exceed the context window, the task **will** require multiple sessions. Plan for it.

---

## 3. Budget Allocation

### 3.1 Session Budget (Commits per Session)

A practical proxy for token consumption is **commits per session**. Each TDD micro-commit cycle (test → implement → refactor → commit) costs roughly 4–8K tokens.

| Session Type | Expected Commits | Rationale |
|-------------|-----------------|-----------|
| Fresh start (scaffold + explore + code) | 5–10 | Fixed overhead is higher |
| Resume session (skip exploration) | 8–15 | More budget for actual work |
| Refactoring-only session | 10–20 | Refactoring commits are smaller |

**Use commit count as your primary budget gauge.** If you've made 10+ commits in a session, start evaluating whether to wrap up.

### 3.2 Phase Budgets (for Multi-Phase Tasks)

For tasks with distinct phases (e.g., conversions, large features), allocate budgets per phase:

| Phase | Budget (% of working budget) | Guidance |
|-------|------------------------------|----------|
| Exploration/planning | 10–15% | Read only what's needed; cap tool calls |
| Scaffolding/setup | 5–10% | Mechanical work; should be fast |
| Core implementation | 60–70% | Bulk of productive work |
| Quality/verification | 10–15% | Tests, linting, build verification |
| Handoff/cleanup | 5% | Always reserve for clean exit |

**Key rule**: Always reserve 5–10% of your budget for a clean exit. Never spend your last tokens writing code — spend them committing, updating the progress tracker, and verifying the build.

---

## 4. Context Exhaustion Protocol

When you detect you are approaching the end of your context budget, execute this protocol. **Do not try to squeeze in one more feature.** An incomplete feature with uncommitted code is worse than stopping cleanly.

### 4.1 Trigger Conditions

Execute the protocol when **any** of these conditions are met:

| Trigger | How to Detect |
|---------|--------------|
| Commit count exceeded | You have made 10+ commits this session (for fresh starts) or 12+ commits (for resume sessions) |
| Tool call count high | You have made 100+ tool calls this session |
| Responses getting slower | Model responses are noticeably slower or shorter (sign of approaching context limit) |
| Cannot fit next feature | You estimate the next feature will require more tokens than you have remaining |

### 4.2 Protocol Steps

Execute these steps in order. Do not skip any.

1. **Stop writing new code.** Do not start a new feature, test, or refactoring.
2. **Commit all outstanding work.** Run `git status` — if anything is modified or untracked, commit it with appropriate messages.
3. **Verify the build passes.** Run the project's build/test command. If it fails, make only the minimal fixes required to restore green. Commit the fixes. **Never leave a session on a red build.**
4. **Update the progress tracker.** Write the current state to your boulder/progress file:
   - What was completed this session (list commits or features)
   - Current metrics (test count, coverage, file count)
   - **Specific NEXT ACTION** — not "continue Phase 2" but "wire up GET /segments/{id} route in SegmentRoutes.kt, then add unit tests for SegmentService.getById()"
5. **Update the plan.** Check off completed items in the plan file.
6. **Stop.** The next session will resume from the progress tracker's NEXT ACTION.

### 4.3 Clean Exit Criteria

A session exit is "clean" when ALL of these are true:

- [ ] All work committed (`git status` shows clean working tree)
- [ ] Build passes (exit code 0)
- [ ] Progress tracker updated with specific NEXT ACTION
- [ ] Plan updated with completed items checked off
- [ ] Latest commit is on the correct branch

---

## 5. Multi-Session Resilience

### 5.1 Progress Tracker (Boulder Pattern)

For multi-session tasks, maintain a progress tracker file (commonly called a "boulder file") outside the project's git repo. This file is the agent's external memory — it survives context resets.

**Required fields:**

```markdown
# Project Boulder

## Phase Status
| Phase | Status | Notes |
|-------|--------|-------|
| Phase 1 | ✅ Complete | ... |
| Phase 2 | 🔄 In Progress | ... |
| Phase 3 | 🔲 Not Started | ... |

## Current Metrics
- Commits: N on feat/branch
- Production files: N
- Test files: N
- Unit tests: N
- Acceptance tests: N

## NEXT ACTION
[Extremely specific next step — not "continue Phase 2" but "implement UserService.create() with validation, add unit tests, commit"]

## Session Log
| Session | Commits | Key Actions |
|---------|---------|-------------|
| 1 | 1–8 | Scaffold, explore, first 3 endpoints |
| 2 | 9–18 | Remaining endpoints, error handling |
```

### 5.2 Resume Protocol

When resuming from a previous session:

1. **Read the progress tracker first.** It has your exact position and NEXT ACTION.
2. **Check git state.** Run `git log --oneline -10` and `git status` to verify commits and detect uncommitted work.
3. **Commit uncommitted work.** If `git status` shows changes, commit them before doing anything else.
4. **Verify the build.** Run the build/test command. Fix any failures before continuing.
5. **Read the plan.** Find the first unchecked item to confirm alignment with the boulder's NEXT ACTION.
6. **Continue from NEXT ACTION.** Do not re-explore, re-read standards, or re-scaffold.

**What NOT to do on resume:**
- Do NOT re-read standards docs already encoded in your agent config
- Do NOT re-explore source/reference projects for information already in the plan
- Do NOT re-scaffold or re-initialize anything
- Do NOT start from the beginning — the progress tracker tells you where you are

**Token savings on resume**: By skipping exploration and standards reads, a resume session saves ~30–50K tokens (~15–25% of context window), leaving more budget for actual work.

### 5.3 Compaction Resilience

Some AI platforms perform "context compaction" — summarizing earlier messages to free up context space. If you detect a compaction event (sudden loss of prior conversation context):

1. **Read the progress tracker immediately.** It is your external memory.
2. **Read the plan.** Find your position (first unchecked item).
3. **Check git history.** Run `git log --oneline` to verify your commits are intact.
4. **Check for uncommitted work.** Run `git status`.
5. **Continue from NEXT ACTION.** The progress tracker has everything you need.

---

## 6. Token-Efficient Practices

### 6.1 Minimize Reads

- Read files **on demand**, not preemptively. Don't load a file until you need it.
- For standards docs, read only the **language-specific** file relevant to your task — skip general docs already encoded in your agent config.
- Use targeted reads (specific line ranges, grep) instead of reading entire large files.
- Don't re-read a file you've already read in this session unless it has been modified.

### 6.2 Minimize Exploration

- Set a **tool call budget** for exploration phases (e.g., 15 calls max).
- Stop exploring when you have enough context to proceed confidently.
- Document findings in the plan file — this is cheaper than re-exploring later.

### 6.3 Minimize Output

- Write code directly — don't narrate what you're about to write.
- Use focused commit messages, not essays.
- Update progress trackers with specifics, not summaries of summaries.

---

## 7. Anti-Patterns

| Anti-Pattern | Why It's a Problem | What To Do Instead |
|-------------|-------------------|-------------------|
| Reading all standards docs at session start | Consumes 30–60K tokens before any work begins | Read only what's needed; most rules are in agent config |
| Re-exploring on resume | Wastes 20–40K tokens on information already in the plan | Read the progress tracker and plan; skip exploration |
| No progress tracker | Next session has no recovery point; must re-explore everything | Always maintain a boulder file for multi-session tasks |
| Stopping mid-feature | Uncommitted code, failing build, stale progress tracker | Execute the context exhaustion protocol; stop between commits |
| Ignoring commit count | Agent runs until forcibly stopped, losing unsaved work | Track commits; start wrapping up after 10+ |
| "Just one more feature" | Runs out of context mid-implementation, leaves broken state | Stop cleanly when triggers fire; the next session will handle it |
| Stale NEXT ACTION | Progress tracker says "continue Phase 2" — too vague to resume from | NEXT ACTION must be specific enough for a new session to execute immediately |
| Exiting on a red build | Next session wastes budget debugging the previous session's failures | Always verify build passes before stopping |

---

## 8. Quick Reference

### Fresh Session Checklist
1. Read progress tracker (if resuming)
2. Read plan file
3. Verify git state
4. Note your commit count baseline
5. Work through tasks using TDD micro-commits
6. Monitor commit count (trigger at 10+)
7. Execute context exhaustion protocol when triggered
8. Exit clean

### Context Exhaustion Protocol (Summary)
1. Stop writing new code
2. Commit all outstanding work
3. Verify build passes
4. Update progress tracker with specific NEXT ACTION
5. Update plan with completed items
6. Stop

---

**Last Updated**: March 2026
**Version**: 1.0
