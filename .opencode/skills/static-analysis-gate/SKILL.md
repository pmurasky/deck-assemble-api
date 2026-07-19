---
name: static-analysis-gate
description: Execute static-analysis gates (PMD, detekt, Checkstyle) and block commit readiness on violations.
---

# Static Analysis Gate

## Use when

- Commit readiness requires static-analysis evidence
- Java/Kotlin code or shared quality infrastructure changed
- User asks for hard quality gate checks

## Not for

- Replacing functional test verification
- Architecture/spec compliance reviews
- Waiving failures without explicit user direction

## Hard Gates

1. Run configured static-analysis commands
2. Record tool-specific pass/fail outcomes
3. Summarize blocking findings with locations
4. Return fail verdict if any required gate fails

## Status Vocabulary

- `PASS`
- `FAIL`
- `BLOCKED`

## Example

Input: "run static-analysis gate".

Output: PMD/detekt/Checkstyle verdicts and actionable blockers.

## Anti-patterns

- Claiming pass without command output
- Ignoring failing required gates
- Conflating warnings with documented waivers
