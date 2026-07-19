# Skill Test Promotion Rubric

**Date**: 2026-03-13
**Status**: Approved
**Parent Epic**: #265 (Phase 2 — Promotion of Stable Lanes)
**Issue**: #272

---

## Purpose

Define objective, lane-specific criteria for promoting skill tests from **informational** (non-blocking) CI to **required** (blocking) status checks. Separate policies for deterministic and empirical lanes prevent conflating fundamentally different noise profiles.

---

## Lane Definitions

### Deterministic Lane (Invariant/Parity Checks)

Tests that parse skill files and assert structural/policy invariants. These are fully deterministic — same input always produces same output.

**Workflow**: `skill-behavioral-smoke.yml`
**Test location**: `tests/skills/` (excluding `tests/skills/empirical/`)

### Empirical Lane (Trigger/Outcome Checks)

Tests that evaluate LLM behavior — whether a skill triggers correctly and produces expected outcomes. These are inherently non-deterministic.

**Workflow**: `skill-empirical-lane.yml`
**Test location**: `tests/skills/empirical/`

---

## Deterministic Lane Promotion Criteria

A deterministic test suite is eligible for promotion to **required** when ALL of the following are met:

| Criterion | Threshold | Measurement |
|---|---|---|
| Flakiness | 0% across ≥ 10 CI runs | Count of non-cancelled failures / total runs |
| False positives | Zero false positives during probation | No test failure caused by a valid, non-breaking edit |
| Stability window | ≥ 10 consecutive successful CI runs | Exclude cancelled runs from count |
| Maintenance burden | No test churn required during probation | Zero fixes needed for non-regression reasons |
| Regression value | ≥ 1 meaningful regression caught or prevented | Documented in PR or issue |

### Why stricter than the rollout plan's 5% / 20-run threshold

The rollout plan proposed 5% flakiness across 20 runs as a general guideline. For the deterministic lane, we tighten this to 0% because:

1. These tests are fully deterministic — any failure is a real signal, never noise.
2. Flakiness in a deterministic test indicates a bug in the test itself, not acceptable variance.
3. The 10-run minimum is sufficient because deterministic tests have no variance to average out.

### Current Status (2026-03-13)

| Criterion | Status | Evidence |
|---|---|---|
| Flakiness | ✅ 0% (13/13 successful) | `gh run list --workflow=skill-behavioral-smoke.yml` |
| False positives | ✅ Zero | No false alarms during Phase 1 probation |
| Stability window | ✅ 13 consecutive successes | All runs from feat/264 + feat/274 + main |
| Maintenance burden | ✅ Zero churn | No test fixes needed post-delivery |
| Regression value | ✅ Met | Parity checks validate canonical↔mirror drift |

**Decision**: Deterministic lane is **eligible for promotion**.

---

## Empirical Lane Promotion Criteria

An empirical test suite is eligible for promotion to **required** when ALL of the following are met:

| Criterion | Threshold | Measurement |
|---|---|---|
| Trigger false-positive rate | ≤ 10% over ≥ 20 runs | should-not-trigger prompts that incorrectly trigger |
| Trigger false-negative rate | ≤ 10% over ≥ 20 runs | should-trigger prompts that fail to trigger |
| Task pass rate | ≥ 80% over ≥ 20 runs | task-outcome prompts that meet expected criteria |
| Stability window | ≥ 20 consecutive non-noisy runs | No sustained signal degradation over 2+ runs |
| Actionability | Signal leads to concrete fixes | Team confirms failures map to real skill issues |

### Current Status (2026-03-13)

| Criterion | Status | Evidence |
|---|---|---|
| FP/FN rates | ⏳ Insufficient data | Only 7 successful runs; need 20 |
| Task pass rate | ⏳ Insufficient data | Need 20-run window |
| Stability window | ⏳ In probation | Runs started same day as delivery |
| Actionability | ⏳ Not yet evaluated | No empirical failures to assess |

**Decision**: Empirical lane **remains informational**. Re-evaluate after 20+ runs with trigger/outcome data.

---

## Decision Owners

| Decision | Owner |
|---|---|
| Promote deterministic lane to required | Repository maintainers (≥ 1 approval) |
| Promote empirical lane to required | Repository maintainers (≥ 1 approval) + data review |
| Rollback any promoted lane | Any repository maintainer (unilateral) |

---

## Promotion Process

### To promote a lane:

1. Verify all promotion criteria are met (document evidence in a PR).
2. Remove `continue-on-error: true` from the workflow file.
3. Add the workflow's job name as a required status check in branch protection.
4. Update this rubric's status section.
5. Monitor for 5 subsequent PRs — rollback if any false positive occurs.

### To add the deterministic lane as a required status check:

```bash
# The GitHub API call to add the check (requires admin access):
gh api repos/{owner}/{repo}/branches/main/protection/required_status_checks \
  --method PATCH \
  --field strict=true \
  --field contexts[]="Cycode: Secrets" \
  --field contexts[]="smoke-tests"
```

> **Note**: The job name in the workflow is `smoke-tests`. This is the context name GitHub uses for the status check.

---

## Rollback Policy

### Triggers for rollback

Any of these conditions triggers immediate rollback to informational:

1. **False positive**: A test fails on a PR that contains no actual policy regression.
2. **Sustained noise**: 2+ consecutive failures that are not real regressions.
3. **Maintenance burden**: Test requires fixes more than once per quarter for non-regression reasons.
4. **Merge blocking**: Test prevents merging of a time-sensitive fix with no workaround.

### Rollback process

1. Any maintainer can rollback unilaterally.
2. Re-add `continue-on-error: true` to the workflow.
3. Remove the job from required status checks.
4. Open an issue documenting the rollback reason and proposed fix.
5. Fix the root cause before re-promoting.

### Emergency bypass

If a promoted check blocks a critical fix and rollback is too slow:

- Maintainers with admin access can temporarily disable the required status check.
- Document the bypass in the PR description.
- Re-enable within 24 hours.

---

## Review Checklist

- [x] Separate criteria for deterministic vs. empirical lanes
- [x] Decision owners identified
- [x] Rollback criteria documented
- [x] Emergency bypass procedure included
- [x] Current status assessed with evidence
- [x] Promotion process is step-by-step actionable

---

*End of promotion rubric — deterministic lane eligible, empirical lane in probation.*
