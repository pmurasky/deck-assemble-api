---
description: Guided TDD workflow for implementing a new feature
agent: standards-build
---

I need to implement a new feature following the TDD micro-commit workflow.

Feature: $ARGUMENTS

Guide me through the full TDD cycle for this feature:

## Step 1: Plan

Break the feature into the smallest possible increments. Each increment should be one testable behavior. Create a TodoWrite task list with the micro-commit breakdown.

Order of implementation:
1. Interfaces/abstractions first (if needed)
2. Core logic (one behavior at a time)
3. Edge cases and error handling
4. Integration points

## Step 2: For Each Increment, Follow TDD

### RED Phase
- Write ONE failing test using Given-When-Then structure
- Use a descriptive test name (e.g., `shouldCalculateTotalWhenItemsPresent`)
- Verify the test FAILS for the right reason
- Do NOT commit yet

### GREEN Phase
- Write the MINIMUM code to make the test pass
- No extra logic, no premature optimization
- Run ALL tests to verify nothing is broken
- COMMIT: `test(<scope>): add test for <behavior>` + `feat(<scope>): implement <behavior>`

### REFACTOR Phase
- Improve code structure (extract method, rename, simplify)
- Check SOLID compliance and code quality gates
- Run ALL tests to verify nothing broke
- COMMIT: `refactor(<scope>): <what was improved>`

## Step 3: Repeat

Move to the next increment until the feature is complete.

## Quality Gates (Every Commit)

- All tests pass
- Build succeeds
- No lint errors
- Methods within language-specific limit (typically 15-20 lines; Go: 25 lines)
- Parameters <= 5
- SOLID principles followed

Reference `docs/AI_AGENT_WORKFLOW.md` for the full workflow and `docs/CODING_PRACTICES.md` for TDD details.
