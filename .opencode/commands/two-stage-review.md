---
description: Run staged review flow (spec compliance first, code quality second)
agent: standards-build
---

Run a strict two-stage review for this target: $ARGUMENTS

## Stage Order (Mandatory)

1. Stage 1: Spec compliance review
2. Stage 2: Code quality review (only if stage 1 passes)

<HARD-GATE>
Do not run stage 2 when stage 1 is FAIL.
If stage 1 reports requirement mismatch, stop and return FAIL with fix actions.
</HARD-GATE>

## Stage 1 - Spec Compliance

Validate requirement/acceptance-criteria coverage only:
- Requested behaviors implemented
- No requirement contradictions
- No out-of-scope behavior that masks missing requirements

## Stage 2 - Code Quality

After stage 1 PASS, review quality gates:
- SOLID and maintainability
- Method/class/parameter limits
- Testing and micro-commit quality signals

## Fail/Fix/Re-Review Loop

1. If stage 1 FAIL: fix requirement mismatches, rerun stage 1
2. If stage 1 PASS and stage 2 FAIL: fix quality issues
3. Rerun stage 1 and stage 2 after any fixes
4. Mark READY only when both stages PASS

## Requirement Mismatch Scenario

Example:
- Requirement: "Add hard gate to block production code before a failing test"
- Found: command documents TDD but still allows implementation-first path
- Stage 1 result: FAIL (spec mismatch)
- Action: enforce hard gate text, rerun stage 1, then run stage 2

Reference:
- `docs/AI_AGENT_WORKFLOW.md`
- `docs/PRE_COMMIT_CHECKLIST.md`
