# Verification Standards

## Overview

This document defines the canonical verification protocol for all code changes in this project. The core principle is simple: **evidence before claims, always.**

Every completion claim — whether for a commit, a task, a PR, or a feature — must include fresh verification evidence. No stale output. No cached results. No assertions without proof.

The goal is to prevent the most common failure mode in AI-assisted development: claiming work is done when it is not.

---

## The Evidence Principle

**Fresh evidence is mandatory for every completion claim.**

Fresh means:
- Test output from a run you just executed (not cached, not from 10 minutes ago)
- Build output from a compile you just triggered (not from memory, not from a previous session)
- Git status from a command you just ran (not assumed, not inferred from other signals)

Stale evidence is not evidence. If you did not run it just now, run it again.

---

## Required Evidence

Every completion claim MUST include ALL THREE of these outputs:

### 1. Fresh Test Output

Run the full test suite (or the relevant subset for the change) and paste the complete output.

**Required information**:
- Test execution summary (e.g., "137 tests passed")
- Execution time
- Coverage report (if applicable)
- Any warnings or skipped tests

**Not acceptable**:
- "All tests pass" without output
- Partial test run results (unless you explicitly explain why a subset is sufficient)
- Cached test results from a previous run

**Example (acceptable)**:
```
$ npm test

 PASS  src/auth/auth.test.ts
 PASS  src/parser/xml-parser.test.ts
 PASS  src/report/report-builder.test.ts

Test Suites: 12 passed, 12 total
Tests:       137 passed, 137 total
Snapshots:   0 total
Time:        4.823 s
```

---

### 2. Fresh Build Output

Run your project's build command and paste the output.

**Required information**:
- Build command used (e.g., `./gradlew build`, `npm run build`, `go build ./...`)
- Exit code (0 = success)
- Any warnings or deprecation notices

**Not acceptable**:
- "Build succeeds" without output
- Assumption that build passes because tests passed
- Build output from before your most recent code change

**Example (acceptable)**:
```
$ ./gradlew build

BUILD SUCCESSFUL in 8s
12 actionable tasks: 12 executed
```

---

### 3. Fresh Git Status

Run `git status` and paste the output to show the working directory state.

**Required information**:
- Branch name
- Staged vs. unstaged changes
- Untracked files (if any)
- Status relative to remote (ahead/behind/up-to-date)

**Not acceptable**:
- Skipping git status because "I know what changed"
- Describing the git state in prose instead of showing the command output

**Example (acceptable)**:
```
$ git status
On branch feature/add-oauth
Your branch is up to date with 'origin/feature/add-oauth'.

nothing to commit, working tree clean
```

---

## When Verification Is Required

### 1. Before Every Commit

Before running `git commit`, you MUST verify:
- [ ] All tests pass (run full suite, paste output)
- [ ] Build succeeds (run build command, paste output)
- [ ] Working directory is clean or only contains intended changes (`git status`)

See [PRE_COMMIT_CHECKLIST.md](./PRE_COMMIT_CHECKLIST.md) for the full pre-commit checklist.

---

### 2. Before Marking a Task Complete

When you finish a task (in a TODO list, GitHub issue, or user request), provide:
- [ ] Test output showing all tests pass
- [ ] Build output showing successful compilation
- [ ] Git status showing clean state (or explicit note of what remains uncommitted)

**Not acceptable**: "Task complete" with no evidence.

---

### 3. Before Pushing to Remote

Before running `git push`, verify:
- [ ] All unit tests pass
- [ ] All integration tests pass (run separately from unit tests)
- [ ] Build succeeds
- [ ] Git status shows branch is ahead of remote (or up-to-date if nothing to push)

See [AI_AGENT_WORKFLOW.md](./AI_AGENT_WORKFLOW.md) for test execution tier details.

---

### 4. After Fixing a Failure

After ANY fix attempt (bug fix, test fix, build fix), you MUST re-verify:
- [ ] Run tests again (paste new output)
- [ ] Run build again (paste new output)
- [ ] Check git status (paste output)

Do NOT assume the fix worked. Verify it worked.

---

## Failure Protocol

### What to Do When Verification Fails

If any verification step fails (tests fail, build fails, working directory has unexpected changes):

1. **DO NOT** claim the work is complete
2. **DO NOT** commit the code as-is
3. **DO** fix the failure in the same logical change (if it's related to your change)
4. **DO** re-verify after the fix (paste new output)

---

### The 3-Fix Escalation Rule

**After 3 consecutive failed fix attempts on the same issue: STOP.**

Do not make a 4th attempt without re-diagnosing the problem.

Three failed fixes in a row signals:
- The hypothesis was wrong
- The failure is likely architectural, not local
- More investigation is needed

**Escalation procedure**:
1. STOP all further edits immediately
2. REVERT to last known working state (if applicable)
3. DOCUMENT what was attempted and what failed (evidence for each attempt)
4. RE-DIAGNOSE the problem from Phase 2 (see [DEBUGGING_STANDARDS.md](./DEBUGGING_STANDARDS.md))
5. If still stuck after re-diagnosis, ASK the user before proceeding

See [DEBUGGING_STANDARDS.md](./DEBUGGING_STANDARDS.md) for the full 4-phase debugging protocol.

---

## Common Mistakes

| Anti-Pattern | Why It's Wrong | What to Do Instead |
|---|---|---|
| "All tests pass" (no output) | Stale or cached evidence; no proof | Run tests and paste output |
| "Build succeeds" (no output) | Assumed state; unverified | Run build and paste output |
| Running tests once at session start, reusing output later | Stale evidence doesn't reflect current code state | Re-run tests after every change |
| Skipping git status because "I know what changed" | Unexpected files may be staged; state may drift | Always run `git status` and paste output |
| Fixing a test failure and immediately claiming done | No verification that the fix worked | Re-run tests after the fix and paste output |
| Committing with failing tests "to fix in next commit" | Violates production-ready commit rule | Fix tests in same commit or revert change |
| Running subset of tests without explanation | May miss regressions in related code | Run full suite or explain why subset is sufficient |
| Batch-verifying multiple changes at once | Can't tell which change caused a failure | Verify after each atomic change |

---

## Relationship to Other Standards

This standard works together with:

- **[PRE_COMMIT_CHECKLIST.md](./PRE_COMMIT_CHECKLIST.md)** — Full checklist for production-ready commits; verification is one part of the larger quality gate
- **[AI_AGENT_WORKFLOW.md](./AI_AGENT_WORKFLOW.md)** — TDD micro-commit workflow; verification happens at the COMMIT step in RED → GREEN → COMMIT → REFACTOR → COMMIT
- **[TESTING_STANDARDS.md](./TESTING_STANDARDS.md)** — Test execution tiers; defines when to run unit-only vs. unit+integration vs. full suite
- **[DEBUGGING_STANDARDS.md](./DEBUGGING_STANDARDS.md)** — 4-phase debugging protocol; Phase 4 (Targeted Fix) requires re-verification after every fix attempt

---

## Quick Reference

**Before every commit:**
```bash
# 1. Run tests
npm test  # (or ./gradlew test, pytest, go test ./..., etc.)

# 2. Run build
npm run build  # (or ./gradlew build, go build ./..., etc.)

# 3. Check git status
git status

# 4. Paste all three outputs before claiming "ready to commit"
```

**Iron Law**: Evidence before claims, always. No exceptions.

---

**Last Updated**: 2026-03-12  
**Version**: 1.0.0
