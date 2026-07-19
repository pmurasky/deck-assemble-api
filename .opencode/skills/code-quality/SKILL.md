---
name: code-quality
description: Review changes for maintainability, SOLID alignment, and repository quality gates after requirement compliance.
---

# Code Quality

## Use when

- Requirement acceptance is already validated
- User requests maintainability/SOLID review
- You need a quality-gate pass before merge/ship

## Not for

- Determining feature scope or acceptance criteria
- Security-only or performance-only audits
- Skipping required verification evidence

## Hard Gates

1. Confirm scope and changed files
2. Evaluate SOLID/DRY and structural boundaries
3. Check method/class size and coupling signals
4. Report concrete findings with file references

## Status Vocabulary

- `PASS`
- `NEEDS FIXES`
- `BLOCKED`

## Example

Input: "run code quality review on staged changes".

Output: prioritized findings with fix guidance and explicit pass/fail verdict.

## Anti-patterns

- Mixing spec compliance and quality review stages
- Vague findings without code locations
- Completion claims without verification output
