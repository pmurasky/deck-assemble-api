---
description: Review recent commits against engineering standards
agent: standards-review
subtask: true
---

Review recent commits against the engineering standards.

Target: $ARGUMENTS (number of commits to review, default: 5)

First, retrieve the recent commit history:
!`git log --oneline -${ARGUMENTS:-5}`

For each commit, evaluate:

### 1. Micro-Commit Compliance
- Does each commit represent ONE logical change?
- Are refactoring and feature changes in separate commits?
- Are test changes properly paired with implementation?

### 2. Commit Message Quality
- Follows Conventional Commits: `<type>(<scope>): <description>`
- Valid type: feat, fix, refactor, test, docs, perf, chore
- Description is concise and explains WHY, not WHAT
- No vague messages ("fix bug", "update code", "WIP")

### 3. Production-Readiness (spot check)
- Do the changes look complete (no half-implemented features)?
- Are there any obvious test gaps in the diffs?
- Any signs of bundled changes that should have been separate commits?

### 4. Code Quality (from diffs)
Review the actual diffs for quality issues:
!`git log --format="%H" -${ARGUMENTS:-5} | head -1 | xargs -I {} git diff {}^..HEAD --stat`

Output a structured report:

```
## Commit Review Report

### Commits Reviewed: X

| Commit | Message | Micro-Commit | Message Quality | Issues |
|--------|---------|:------------:|:---------------:|--------|
| abc123 | ... | PASS/FAIL | PASS/FAIL | ... |

### Violations
1. [COMMIT] Description of violation
   - Standard: Which standard is violated
   - Fix: How to fix it

### Recommendations
- Overall patterns to improve
```

Reference `docs/AI_AGENT_WORKFLOW.md` for micro-commit standards and `docs/CODING_PRACTICES.md` for commit message format.
