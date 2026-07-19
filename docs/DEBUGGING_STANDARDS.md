# Debugging Standards

## Overview

This document defines the systematic debugging protocol used by this project. The goal is to prevent the most common debugging failure: jumping to a fix before understanding the problem.

The protocol has four phases. They must be followed in order. Skipping phases — especially Phase 1 and Phase 3 — is the root cause of debugging thrash.

---

## The Four-Phase Protocol

### Phase 1: Investigate Without Fixing

**Read. Reproduce. Gather evidence. Do not touch code yet.**

- Read the full error output — not a summary, the actual text
- Identify the exact file, line number, and error type
- Reproduce the failure consistently before doing anything else
- Gather all relevant evidence: stack trace, logs, test output, recent git changes
- Write down what you observe (not what you think it means — what you see)

**Exit criterion**: You can reproduce the failure and you have the raw evidence in front of you.

**What goes wrong when this phase is skipped**: The agent fixes a symptom instead of the cause, the fix appears to work, the real problem resurfaces later.

---

### Phase 2: Identify the Failure Pattern

**Classify the problem before diagnosing it.**

Ask: what kind of failure is this?

| Pattern | Description | Example signals |
|---|---|---|
| **Typo / syntax error** | Trivial textual mistake | Compiler error, obvious misspelling |
| **Logic error** | Correct syntax, wrong behavior | Test assertion mismatch, off-by-one |
| **Missing dependency** | Something expected isn't present | Import error, null reference, missing file |
| **Configuration error** | Environment or build misconfiguration | Works locally, fails in CI; wrong path |
| **Architectural issue** | Root cause is a design problem | Same failure recurs across multiple fixes |
| **Test error** | The test itself is wrong | Assertion checks wrong value; wrong setup |

**Exit criterion**: You have named the pattern. You know which category this failure belongs to.

**Why this matters**: Architectural issues cannot be fixed with local patches. Naming the pattern early prevents wasted fix attempts.

---

### Phase 3: Form a Specific Hypothesis

**State exactly what you think is wrong and why — before changing any code.**

A hypothesis must be specific enough to be falsifiable. Vague hypotheses produce vague fixes.

| Not a hypothesis | A real hypothesis |
|---|---|
| "Something is wrong with the parser" | "Line 47 of XmlParser passes a null string to trim() when the input has no root element" |
| "The test might be failing because of state" | "The test shares a mutable list with the previous test; the second test sees leftover items" |
| "The build might be misconfigured" | "The classpath does not include the test resources directory, so the fixture file isn't found at runtime" |

Write the hypothesis down. If you cannot write it in one sentence, keep investigating.

**Exit criterion**: You have a written, specific, falsifiable hypothesis.

---

### Phase 4: Targeted Fix

**Fix only the thing your hypothesis identified. Nothing more.**

- Make the smallest change that tests your hypothesis
- If the fix works, verify with full test suite + build before declaring done
- If the fix does not work, go back to Phase 2 — do not pile on additional changes

**What "targeted" means**:
- ✅ Change the one line your hypothesis pointed to
- ❌ Refactor the surrounding code while you're in there
- ❌ Add defensive null checks "just in case" unrelated to your hypothesis
- ❌ Fix two suspected issues at once

---

## The 3-Fix Escalation Rule

**After 3 failed fix attempts on the same issue: STOP.**

Do not make a 4th attempt without re-diagnosing from Phase 2.

Three failed fixes in a row is a strong signal that:
- The hypothesis was wrong each time
- The failure pattern was misclassified (likely an architectural issue, not a local one)
- More information is needed before attempting another fix

### Escalation procedure

After attempt 3 fails, produce an Escalation Report before doing anything else:

```
## Escalation Report — 3 Fix Attempts Failed

**Issue**: [one sentence describing what is failing]

**Attempt 1**: [what was changed and why]
**Result 1**: [what the output showed]

**Attempt 2**: [what was changed and why]
**Result 2**: [what the output showed]

**Attempt 3**: [what was changed and why]
**Result 3**: [what the output showed]

**My hypothesis now**: [what I think is actually wrong after reviewing all three attempts]

**Question**: [specific decision or input needed to proceed]

Possible explanations:
- [ ] Root cause is different from what I have been targeting
- [ ] The test expectation itself is wrong
- [ ] This is an architectural issue that local fixes cannot resolve
- [ ] There is a configuration or environment issue I cannot see
```

This report is not a failure — it is the correct response to a hard problem. Attempting a 4th fix without it almost always makes the situation worse.

---

## Common Debugging Mistakes

| Mistake | Why it causes thrash |
|---|---|
| Fixing before reproducing | You may be fixing the wrong occurrence of the problem |
| Skipping hypothesis formation | Without a hypothesis, fixes are guesses; guesses compound |
| Making multiple changes in one fix attempt | You cannot tell which change (if any) resolved the problem |
| Continuing past 3 failed attempts without re-diagnosing | The diagnosis is wrong; more fixes will not help |
| Fixing symptoms instead of the root cause | The same failure resurfaces under different conditions |
| Trusting memory of the error instead of re-reading it | Memory distorts; always re-read the actual output |

---

## Quick Reference

```
Phase 1 — Investigate without fixing
  → Read the error. Reproduce it. Gather evidence.

Phase 2 — Identify the failure pattern
  → Typo? Logic? Missing dep? Config? Architecture? Test?

Phase 3 — Form a specific hypothesis
  → One falsifiable sentence. Write it down before touching code.

Phase 4 — Targeted fix
  → Fix only what your hypothesis identified. Run tests. Verify.

3-Fix Rule
  → Three failed attempts → STOP → Escalation Report → Re-diagnose from Phase 2.
```

---

## Relationship to Other Standards

- **Verification Before Completion** (`verification-before-completion` skill, environment-provided via [superpowers](https://github.com/obra/superpowers)): After a successful fix in Phase 4, follow the iron law — run tests, run build, run `git status`, paste all three outputs before declaring done.
- **Micro-Commit Workflow** (`micro-commit-workflow` skill): Once the fix is verified, commit it as a focused, single-change commit.
- **CODING_PRACTICES.md**: The "Red Flags — STOP and Ask" section aligns with the 3-fix escalation rule — both say to escalate when the problem scope is unclear or when repeated attempts are failing.

---

**Last Updated**: March 2026
**Version**: 1.0
