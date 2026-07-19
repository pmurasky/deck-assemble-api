---
description: Enforce strict test-first TDD sequencing with hard gates
agent: standards-build
---

**Canonical Contract**: Implements the workflow defined in `docs/archived-workflows/tdd-enforcement.md`.

Enforce strict TDD for this scope: $ARGUMENTS

<HARD-GATE>
Follow TDD sequencing defined in canonical contract: RED → VERIFY RED → GREEN → VERIFY GREEN → REFACTOR → VERIFY REFACTOR.
Never write production code before failing test evidence.
Block progression for any TDD violations per canonical contract.
</HARD-GATE>

Execute TDD workflow steps per canonical contract. Apply rationalization defense patterns and must-watch-it-fail verification as defined.

**References**:
- `docs/archived-workflows/tdd-enforcement.md` (authoritative workflow)
- `docs/AI_AGENT_WORKFLOW.md` (rationalization defense table)
