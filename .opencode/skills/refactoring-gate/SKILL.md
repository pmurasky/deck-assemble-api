---
name: refactoring-gate
description: Block refactoring when test coverage or test-health prerequisites are not met. Use before starting non-trivial refactoring work.
---

# Refactoring Gate

Verify refactoring preconditions before any behavior-preserving structural change.

## Use when

- The user requests refactoring of existing code
- You are about to change structure without intended behavior changes
- You need explicit readiness evidence for coverage and test health

## Not for

- New feature work that intentionally changes behavior
- Declaring readiness without coverage/test evidence
- Using integration/E2E coverage to satisfy unit-test thresholds

## Hard Gates

1. Unit-test coverage for target code is >= 80%.
2. Critical-path unit-test coverage is 100%.
3. Relevant tests are currently passing before refactoring starts.
4. If coverage is insufficient, stop and add tests first.

## Workflow

1. Identify exact refactoring target.
2. Collect coverage and test-health evidence.
3. Determine if change is exempt (format-only or trivial rename/move).
4. Return `GO`, `STOP`, or `EXEMPT` with required next action.

## Status Vocabulary

- `GO`: Refactoring prerequisites are satisfied.
- `STOP`: Coverage/test blockers prevent safe refactoring.
- `EXEMPT`: Change is formatting-only or trivial rename/move.

## Example

User asks: "Refactor this service class."

Expected outcome:
- Coverage check result for target code
- Test-health result for relevant suites
- `GO`, `STOP`, or `EXEMPT` verdict
- Concrete next step (proceed or add tests first)

## Anti-patterns

- Refactoring first and adding tests later
- Mixing bug-fix behavior changes with refactoring-only steps
- Continuing while relevant tests are failing
