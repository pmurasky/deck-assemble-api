---
name: test-coverage
description: Assess changed behavior for unit-test completeness and report concrete coverage gaps before closure.
---

# Test Coverage

## Use when

- User requests coverage-focused validation
- Refactoring/feature work needs targeted gap analysis
- You must prove changed paths are unit-test backed

## Not for

- General code-quality audits across all dimensions
- Substituting for running tests/build
- Relying only on aggregate percentages

## Hard Gates

1. Map changed behavior to existing tests
2. Identify uncovered branches and error paths
3. Flag critical-path gaps with priority
4. Provide explicit recommendation before completion

## Status Vocabulary

- `SUFFICIENT`
- `GAPS FOUND`
- `BLOCKED`

## Example

Input: "check coverage for these changes".

Output: file-level gap report, missing test scenarios, and next tests to add.

## Anti-patterns

- Reporting only a single coverage number
- Ignoring branch/edge-case coverage
- Declaring complete without gap disclosure
