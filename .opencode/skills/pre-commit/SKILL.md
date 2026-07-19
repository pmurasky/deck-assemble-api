---
name: pre-commit
description: Run pre-commit readiness checks and report blockers before any commit is created.
---

# Pre-Commit

## Use when

- User asks if changes are ready to commit
- You need a final readiness gate after implementation
- Commit should be blocked on unresolved quality issues

## Not for

- Creating commits directly
- Replacing feature acceptance validation
- Assuming readiness without fresh evidence

## Hard Gates

1. Run required unit tests
2. Run build/compile validation
3. Run lint/static checks in scope
4. Summarize blockers and readiness verdict

## Status Vocabulary

- `READY`
- `NOT READY`
- `BLOCKED`

## Example

Input: "are we pre-commit ready?".

Output: checklist-style pass/fail report and exact blockers to resolve.

## Anti-patterns

- Marking ready when required checks failed
- Omitting command output evidence
- Treating stale earlier runs as current evidence
