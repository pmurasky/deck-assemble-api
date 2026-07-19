---
name: micro-commit
description: Create one production-ready micro-commit with mandatory verification evidence and conventional commit messaging.
---

# Micro-Commit

## Use when

- User asks to commit current logical unit
- You need one scoped, reviewable commit
- Verification must be run immediately before commit

## Not for

- Bundling unrelated changes
- Skipping tests/build/lint gates
- Rewriting history or force-push workflows

## Hard Gates

1. Validate commit scope is one logical change
2. Run required tests/build/lint for this scope
3. Stage only relevant files
4. Commit with conventional message describing why

## Status Vocabulary

- `READY TO COMMIT`
- `NOT READY`
- `BLOCKED`

## Example

Input: "create a micro-commit for this fix".

Output: staged-file list, verification evidence, commit message, and commit hash.

## Anti-patterns

- Committing with failing required checks
- Including incidental/unrelated files
- Using vague commit messages
