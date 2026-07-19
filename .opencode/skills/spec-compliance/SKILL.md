---
name: spec-compliance
description: Verify requirement and acceptance-criteria compliance before code quality review. Use as the first review stage for feature or bug-fix work.
---

# Specification Compliance Review

Review whether implementation satisfies requested behavior before any code-quality judgment.

## Use when

- The user asks to review if a change meets requirements
- You are validating acceptance criteria before code-quality review
- You need to separate behavior gaps from style or maintainability feedback

## Not for

- Replacing detailed code-quality review or security review
- Approving work without verification evidence
- Mixing requirement failures with minor stylistic suggestions

## Hard Gates

1. Compare implementation against stated requirements and acceptance criteria.
2. Flag missing required behavior, edge cases, or user-visible outcomes.
3. Separate requirement compliance outcomes from quality recommendations.
4. Do not mark `PASS` without explicit evidence.

## Workflow

1. Restate expected behavior from task/spec/tests.
2. Inspect implementation and verification evidence.
3. Map each acceptance criterion to observed proof or gap.
4. Produce a `PASS`, `FLAG`, or `BLOCK` verdict before stage-2 quality review.

## Status Vocabulary

- `PASS`: Requirements are satisfied with evidence.
- `FLAG`: Core behavior exists, but ambiguity or follow-up remains.
- `BLOCK`: Required behavior is missing, contradicted, or unverified.

## Example

User asks: "Check if this feature meets acceptance criteria before quality review."

Expected outcome:
- Restated acceptance criteria
- Criterion-by-criterion compliance verdict
- Clear `PASS`/`FLAG`/`BLOCK` status
- Specific behavioral gaps (if any), separate from code-style notes

## Anti-patterns

- Saying `PASS` based on intuition without tests or evidence
- Combining requirement violations with low-priority formatting comments
- Skipping edge-case criteria because happy-path tests pass
