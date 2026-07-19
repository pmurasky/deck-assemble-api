# Skill Behavioral Test Invariants (Phase 1 Minimal Contract)

**Date**: 2026-03-13
**Status**: Approved
**Parent Epic**: #264 (Phase 1 — Skill Behavioral Testing Foundation)
**Issue**: #278

---

## Purpose

This document defines a **small, high-signal, machine-checkable invariant contract** for Phase 1.

The goal is to catch meaningful skill regressions while keeping the smoke harness deterministic and low maintenance. Assertions are scoped to semantic anchors (sections/frontmatter), not full-paragraph wording.

---

## Phase 1 Scope Constraints

1. Focus only on policies explicitly required by Epic #264 and child issues #278, #279, #280.
2. Prefer fewer invariants with stronger semantics over broad keyword presence checks.
3. Anchor all policy checks to a specific section (except frontmatter/file-level checks).
4. Treat canonical docs as source-of-truth and skill files as mirrors per `docs/STANDARDS_OWNERSHIP_MATRIX.md`.

---

## Assertion Model (Phase 1)

Use only these five assertion types in #279:

1. `frontmatter_fields_present`
2. `section_anchor_present`
3. `section_contains_terms`
4. `section_contains_numeric_policy`
5. `file_forbids_redirect_behavior`

### Semantics

- **Section-scoped by default**: `section_*` assertions run only inside the anchored section body.
- **Case-insensitive matching**: all textual checks are case-insensitive.
- **No exact paragraph matching**: tests verify term groups and numeric policy markers.
- **No ordering assertions in Phase 1**: editorial reorder alone must not fail smoke tests.

---

## Ownership & Parity Rules

From `docs/STANDARDS_OWNERSHIP_MATRIX.md`:

- Canonical pre-commit policy: `docs/PRE_COMMIT_CHECKLIST.md`
- Canonical testing policy: `docs/TESTING_STANDARDS.md`
- Canonical skill-authoring policy: environment skill `writing-skills`; `CONTRIBUTING.md` is a mirror.

### Required parity behavior

Phase 1 smoke tests MUST include canonical↔mirror parity checks for critical policy markers:

1. `docs/PRE_COMMIT_CHECKLIST.md` ↔ `skills/pre-commit-checklist/SKILL.md`
2. `docs/TESTING_STANDARDS.md` ↔ `skills/testing-standards/SKILL.md`

Parity checks should compare critical semantics (thresholds + mandatory gates), not prose identity.

---

## Target A — `pre-commit-checklist` skill invariants (6)

**Mirror file**: `skills/pre-commit-checklist/SKILL.md`
**Canonical file**: `docs/PRE_COMMIT_CHECKLIST.md`

| ID | Assertion Type | Anchor | Required Policy |
|---|---|---|---|
| PCC-1 | `section_anchor_present` | heading contains `pre-commit` and `checklist` | Mandatory checklist anchor exists |
| PCC-2 | `section_contains_terms` | quick pre-commit checklist section | Gate trio present in checklist context: (`test` + `pass`), (`build` + `succeed`), (`lint` + `no`/`error`) |
| PCC-3 | `section_contains_terms` | red-light / do-not-commit section | Blocking conditions include failing tests, build failure, lint errors |
| PCC-4 | `section_contains_terms` | refactoring prerequisites section | Contains both semantics: `never refactor without tests` and stop/write-tests-first when below threshold |
| PCC-5 | `section_contains_numeric_policy` | TDD/refactor/checklist sections | Coverage policy markers: `80` + (`unit` + `coverage`) and `100` + `critical` |
| PCC-6 | `file_forbids_redirect_behavior` | full file (excluding code fences/examples) | No directive that tells the agent to load external docs for required behavior |

### PCC parity requirement

`PCC-PAR-1` (parity check): canonical and mirror both contain the same critical policy markers:
- tests pass gate
- build succeeds gate
- no lint errors gate
- 80% unit coverage
- 100% critical path coverage

---

## Target B — `testing-standards` skill invariants (7)

**Mirror file**: `skills/testing-standards/SKILL.md`
**Canonical file**: `docs/TESTING_STANDARDS.md`

| ID | Assertion Type | Anchor | Required Policy |
|---|---|---|---|
| TS-1 | `section_anchor_present` | heading contains `coverage` | Coverage section exists |
| TS-2 | `section_contains_numeric_policy` | coverage section | Coverage semantics include: `80` overall, `75` branch, `100` critical |
| TS-3 | `section_contains_terms` | coverage section | Explicitly states coverage is from unit tests only and integration/E2E do not count |
| TS-4 | `section_anchor_present` | heading contains `test execution tiers` | Execution-tier section exists |
| TS-5 | `section_contains_terms` | test execution tiers section | Tier policy includes: before commit = unit; before push = unit+integration; CI = unit+integration+E2E; CI hard gate |
| TS-6 | `section_contains_terms` | flaky test policy section | Flaky policy includes quarantine/non-blocking handling and fix window (`within 2 sprints`) |
| TS-7 | `file_forbids_redirect_behavior` | full file (excluding code fences/examples) | No directive that tells the agent to load external docs for required behavior |

### TS parity requirement

`TS-PAR-1` (parity check): canonical and mirror both contain the same critical policy markers:
- coverage from unit tests only
- integration/E2E excluded from coverage accounting
- 80/75/100 thresholds
- execution-tier policy (before commit / before push / CI)

---

## Target C — `CONTRIBUTING.md` skill-authoring mirror invariants (4)

**Mirror file**: `CONTRIBUTING.md`
**Ownership note**: this file is a mirror for skill-authoring policy; canonical owner is `writing-skills` per ownership matrix.

| ID | Assertion Type | Anchor | Required Policy |
|---|---|---|---|
| CTB-1 | `section_contains_terms` | "Writing a Skill" section | CSO rule explicitly defines complete + self-contained + opinionated |
| CTB-2 | `section_contains_numeric_policy` | skill checklist/token budget area | Token budget guidance includes ~800 tokens / ~600 words and verification via `wc -w` |
| CTB-3 | `section_contains_terms` | no-doc-loading guidance | Explicitly states skills must not include doc-loading redirects |
| CTB-4 | `section_contains_terms` | required frontmatter section | Required metadata includes all: `name`, `description`, `disable-model-invocation` |

### Ownership alignment requirement

`CTB-OWN-1`: `CONTRIBUTING.md` must not claim canonical ownership for skill-authoring policy and must remain compatible with ownership matrix wording.

---

## Redirect-behavior rule definition

`file_forbids_redirect_behavior` fails when a file contains imperative guidance directing the agent to external docs as required behavior (e.g., "read docs/...", "load X.md first", "see docs/... for required steps").

Allowed:
- Canonical owner notes without imperative loading instruction.
- Mentions inside fenced code blocks/examples quoted as anti-patterns.

---

## Invariant Count Summary (Phase 1)

| Target | Count |
|---|---:|
| pre-commit-checklist | 6 + parity check |
| testing-standards | 7 + parity check |
| CONTRIBUTING.md mirror rules | 4 + ownership check |
| **Total** | **17 core + 3 governance checks** |

---

## Implementation Notes for Issue #279

1. Build section parser helpers first (heading map + section extraction).
2. Implement reusable term-group assertions (AND groups within section scope).
3. Implement numeric policy assertions (number + semantic keyword group in same section).
4. Implement redirect-behavior detector that ignores fenced code blocks.
5. Add parity helper comparing critical marker sets between canonical and mirror files.

---

## Review Checklist

- [ ] Invariants stay within Phase 1 minimal scope
- [ ] All policy checks are section-scoped unless file-level by definition
- [ ] Canonical↔mirror parity checks included for pre-commit and testing standards
- [ ] Skill-authoring checks respect ownership matrix (mirror, not canonical)
- [ ] No invariant depends on exact sentence wording

---

*End of revised invariant contract — ready for #279 harness implementation.*
