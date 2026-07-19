---
name: tdd-enforcement
description: Enforce strict RED→GREEN→REFACTOR sequencing with proof before progression or completion claims.
---

# TDD Enforcement

## Use when

- User requests strict TDD discipline
- New behavior must be introduced test-first
- You need gate checks across RED/GREEN/REFACTOR

## Not for

- Post-hoc testing after implementation is complete
- Commit readiness without phase evidence
- Multi-test batching before implementation

## Hard Gates

1. RED: introduce one failing test only
2. Capture RED evidence
3. GREEN: implement minimum fix
4. Capture GREEN evidence
5. REFACTOR: improve without behavior change
6. Re-run tests and confirm still green

## Status Vocabulary

- `READY FOR GREEN`
- `READY FOR REFACTOR`
- `READY FOR NEXT RED`
- `BLOCKED`

## Example

Input: "enforce TDD for this task".

Output: current phase, required evidence, and explicit next smallest step.

## Anti-patterns

- Skipping RED failure demonstration
- Writing broad implementations for multiple tests
- Declaring done before REFACTOR verification
