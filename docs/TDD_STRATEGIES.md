# TDD Strategies

## Overview

This document covers **how to evolve code** during TDD — the problem-solving strategies and heuristics for writing tests and production code incrementally. It complements the [AI_AGENT_WORKFLOW.md](./AI_AGENT_WORKFLOW.md) and the `micro-commit-workflow` skill, which cover **when to commit** (the discipline side).

The micro-commit workflow tells you: RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT.
This document tells you: **how to get from RED to GREEN, and what to do during REFACTOR.**

---

## Incremental Scaffolding

Incremental scaffolding is a step-by-step approach for building new code from nothing. Instead of writing a complete test and then a complete implementation, you build both the test and the production code in small, verifiable increments.

### The Workflow

```
Step 1: RED   — Write a test referencing a class/function that doesn't exist
                Compilation failure = RED (this counts as a valid RED phase)

Step 2: GREEN — Create the empty class or function skeleton
                Code compiles = GREEN

Step 3: RED   — Add a method call in the test (method doesn't exist yet)
                Compilation failure = RED

Step 4: GREEN — Create the method; return a fake value (null, 0, -1, "")
                Code compiles, no assertions yet = GREEN
                IMPORTANT: Choose a fake value you WON'T expect in assertions

Step 5: ADD ASSERTIONS — Write only assert/verify statements
                No additional setup or arrange code at this point
                If you need more setup code, that belongs in a new test

Step 6: RED   — Run the test; assertions fail against the fake value
                Verify the failure is for the CORRECT reason (see below)

Step 7: GREEN — Write the real production code to make assertions pass
```

### Why Compilation Failure Counts as RED

A compilation error is a failing test in every meaningful sense: the code does not work, and you have a clear signal of what needs to change. Treating it as RED keeps the rhythm of the cycle consistent and prevents you from writing large chunks of untested code before running anything.

### Why Fake Values Should Be "Wrong," Not "Empty"

The fake value you return in Step 4 serves a purpose: when you add assertions in Step 5, the test should fail because the fake value doesn't match the expected result. If you return a value that accidentally matches what an assertion expects, you'll get a false GREEN and miss real bugs.

For example, if testing a method that returns a customer's full name:
- **Good fake**: return `null` or `""` — assertions expecting `"Jane Doe"` will fail clearly
- **Bad fake**: return `"Jane Doe"` — the test passes without real implementation, hiding bugs

---

## Verify the Failure

Before moving from RED to GREEN, always confirm **why** the test is failing. This is not optional — it catches subtle bugs in test setup, wrong assertions, and tests that pass for the wrong reason.

### What to Check

| Expected failure | Action |
|---|---|
| Assertion failure: got `null`, expected `"Customer Name"` | Correct — proceed to GREEN |
| Assertion failure: got `0`, expected `42` | Correct — proceed to GREEN |
| `NullPointerException` in production code | Wrong — fix the test setup or production skeleton |
| Wrong test method is failing | Wrong — investigate test isolation |
| No failure at all (test passes) | Wrong — your fake value matches the expectation; fix the assertion or fake |

### The Rule

**If the failure is unexpected, fix the test — don't fix the production code.** The test must fail for the reason you intended before you write the implementation.

---

## SRP for Tests

Once you start writing assertions in a test method, **add only assertions**. Do not add additional setup, arrange, or act code after the first assertion.

### Why This Matters

- Adding more logic after assertions makes tests harder to read and maintain
- It often means the test is verifying multiple behaviors — a sign it should be split
- Each test method should be readable top-to-bottom without scrolling

### The Rule

**One logical assertion group per test method.** If you need more arrange/act code to verify additional behavior, write a new test.

"One logical assertion group" means you can have multiple `assert` statements that verify different facets of the **same** result (e.g., checking both `firstName` and `lastName` on a returned object). What you should NOT do is add more setup/act code to test a **different** behavior in the same method.

### Anti-Pattern

```
// BAD: Two behaviors in one test
@Test
void testCustomerName() {
    var formatter = new NameFormatter();
    var result = formatter.format("jane", "doe");
    assertEquals("Jane Doe", result);          // First behavior

    // MORE setup after assertions — this should be a separate test
    var result2 = formatter.format("", "doe");
    assertEquals("Doe", result2);              // Second behavior
}
```

```
// GOOD: One behavior per test
@Test
void shouldFormatFullName_whenFirstAndLastNameProvided() {
    var formatter = new NameFormatter();
    var result = formatter.format("jane", "doe");
    assertEquals("Jane Doe", result);
}

@Test
void shouldReturnLastNameOnly_whenFirstNameIsEmpty() {
    var formatter = new NameFormatter();
    var result = formatter.format("", "doe");
    assertEquals("Doe", result);
}
```

---

## GREEN Strategies

When moving from RED to GREEN, you have three strategies. Choosing the right one depends on how confident you are in the correct implementation.

### Fake It Till You Make It

**What**: Return a hardcoded value that makes the test pass.

**When to use**: When the behavior is complex or unclear. Faking lets you defer the real implementation until you have enough test cases to see the pattern.

**Example**:
```
// Test expects: format("jane", "doe") -> "Jane Doe"
// Fake It:
String format(String first, String last) {
    return "Jane Doe";  // Hardcoded — we'll generalize later
}
```

You then add more test cases (Triangulation) to force the code to become general.

### Obvious Implementation

**What**: Write the real implementation directly.

**When to use**: When the correct code is trivially clear and you're confident it's right. Don't fake it just for ceremony.

**Example**:
```
// Test expects: add(2, 3) -> 5
// Obvious Implementation:
int add(int a, int b) {
    return a + b;  // Clearly correct — no need to fake it
}
```

**Caution**: If the obvious implementation turns out to be wrong (test fails unexpectedly), fall back to Fake It and work incrementally.

### Triangulation

**What**: Add multiple test cases with different inputs to force the code to become general.

**When to use**: When Fake It leaves ambiguity about what the general solution should be. Each new test case constrains the solution further until only the correct general implementation satisfies all tests.

**Example**:
```
// Test 1: format("jane", "doe") -> "Jane Doe"     (could fake with hardcoded value)
// Test 2: format("john", "smith") -> "John Smith"  (forces generalization)
// Test 3: format("", "doe") -> "Doe"               (forces edge case handling)
```

After test 2, the hardcoded `"Jane Doe"` no longer works, forcing you to write real capitalization and concatenation logic. Test 3 adds the edge case.

---

## The REFACTOR Phase

After reaching the final GREEN — when all tests pass and the behavior is correct — **always refactor before considering the feature done.** The REFACTOR phase applies to **both production code and test code**.

### Production Code Refactoring

Look for:
- Methods exceeding the line limit (15-20 lines) — extract methods
- Duplicated logic — extract to shared methods
- Poor naming — rename for clarity
- SOLID violations — apply appropriate patterns
- Dead code from incremental scaffolding — delete it

### Test Code Refactoring

Test code is production code. It deserves the same quality attention:
- **Extract shared setup** to helper methods, builders, or fixtures (e.g., `createDefaultCustomer()`)
- **Improve test names** — test names are documentation. Rename `test1` to `shouldReturnFormattedName_whenFirstAndLastNameProvided`
- **Remove duplication** across test methods — shared arrange steps, repeated assertion patterns
- **Improve assertion readability** — use assertion helpers or custom matchers if available

### Commit Heuristic

Prefer a single refactor commit covering both production and test code. Split into separate commits only when the refactoring is large or touches unrelated concerns — follow the same micro-commit rule: if you can't describe it in one sentence, split it.

### What Refactoring Is NOT

- Refactoring **must not change behavior** — all tests must stay GREEN
- If refactoring reveals missing test coverage, that's a **new RED-GREEN cycle**, not part of this refactor
- If you find a bug during refactoring, **stop refactoring**, write a failing test (RED), fix it (GREEN), commit, then resume refactoring

---

## Putting It All Together

Here is the complete flow for building a new `NameFormatter` class from scratch:

```
1. RED    — Write test: new NameFormatter()           → compile error (class doesn't exist)
2. GREEN  — Create empty NameFormatter class           → compiles
3. RED    — Add test: formatter.format("jane", "doe")  → compile error (method doesn't exist)
4. GREEN  — Create format() method, return null         → compiles, no assertions yet
5. ASSERT — Add: assertEquals("Jane Doe", result)       → assertions only, no other code
6. RED    — Run test: got null, expected "Jane Doe"     → correct failure, proceed
7. GREEN  — Implement real format() logic               → test passes
8. (Optional) Add more test cases via Triangulation     → repeat RED-GREEN for each
9. REFACTOR — Clean up production code AND test code   → tests still pass
10. COMMIT
```

Steps 1-2 and 3-4 are the incremental scaffolding. Steps 5-7 are the core RED-GREEN with verified failure. Step 8 uses Triangulation if needed. Step 9-10 is the mandatory REFACTOR phase.

---

## Anti-Patterns

### Writing All Tests Before Any Production Code

Writing 10 tests upfront and then implementing all of them violates the TDD rhythm. Write ONE test, make it pass, then write the next. Each RED-GREEN cycle gives you feedback.

### Skipping Failure Verification

Going straight from "test fails" to "write production code" without checking why it fails. This leads to tests that pass for the wrong reason or production code that fixes the wrong problem.

### Refactoring Only Production Code

Neglecting test code during the REFACTOR phase. Tests with duplicated setup, poor names, or unclear assertions become a maintenance burden and reduce the value of the test suite as documentation.

### Faking When Obvious, or Implementing When Unclear

Using Fake It when the answer is clearly `return a + b` wastes time. Using Obvious Implementation when you're uncertain leads to debugging instead of incremental discovery. Match the strategy to your confidence level.

---

**Last Updated**: March 2026
**Version**: 1.0
