---
name: commit-review
description: Review staged changes and draft a standards-compliant commit message. Use when user asks for a commit review or wants help wording a production-ready commit.
---

# Commit Review

Review staged changes and propose a conventional commit message with readiness guidance.

## Use when

- The user asks for commit-message drafting help
- You need to validate staged changes are one logical commit
- You want to identify blockers before creating a commit

## Not for

- Auto-creating commits
- Reviewing unstaged work as if it were commit-ready
- Ignoring test/build/lint failures while marking readiness

## Hard Gates

1. Review staged changes only.
2. Do not approve commits that include secrets, credentials, or unrelated changes.
3. Require production-readiness evidence (tests/build/lint where applicable).
4. Require single logical change boundaries.

## Workflow

1. Inspect staged diff and recent commit style.
2. Summarize logical scope.
3. Draft concise conventional-commit message focused on why.
4. Report blockers and split recommendations when needed.

## Status Vocabulary

- `READY`: Staged change is coherent and commit-ready.
- `NOT READY`: Staged change requires cleanup before commit.
- `SPLIT REQUIRED`: Multiple logical changes must be separated.

## Example

User asks: "Review my staged changes and propose a commit message."

Expected outcome:
- Readiness verdict (`READY`, `NOT READY`, or `SPLIT REQUIRED`)
- Commit message draft in Conventional Commits format
- Blocker list and recommended next actions

## Anti-patterns

- Writing vague commit messages that omit intent
- Endorsing one commit for unrelated staged changes
- Proposing commit when quality gates are failing
