# Refactoring Guidelines Reference

> Extracted from the micro-commit-workflow skill for detailed reference.
> Canonical owner: AI_AGENT_WORKFLOW (see STANDARDS_OWNERSHIP_MATRIX).

---

## Safe Refactoring Workflow

Before changing any production code for refactoring purposes, follow this workflow in order:

```
Step 1: ESTABLISH BASELINE
├─ Run the existing tests for the file(s) you will modify
├─ Verify all relevant tests pass (this is your behavioral baseline)
└─ Record which tests cover the production code under change

Step 2: VERIFY COVERAGE
├─ Check unit test coverage for the code being refactored
├─ Requirements: ≥ 80% unit test line coverage, 100% for critical paths
└─ If coverage is sufficient AND tests meaningfully verify behavior → proceed to Step 4

Step 3: ADD CHARACTERIZATION TESTS (when coverage is insufficient or behavior is unclear)
├─ Write characterization tests that capture current behavior — not correctness
├─ Use Feathers' algorithm: call the code, assert what fails, change assertion to match reality
├─ Cover all code paths you will touch during refactoring
├─ Commit separately: "test(<scope>): add characterization tests before refactoring"
└─ Verify all characterization tests pass

Step 4: REFACTOR (behavior-preserving changes only)
├─ Improve structure without changing observable behavior
├─ Target: code smells, compiler/linter warnings, unnecessary complexity, duplication
├─ One refactoring step per commit (see micro-commit rules below)
├─ Run ALL tests after each step — baseline + characterization tests must stay green
└─ Commit: "refactor(<scope>): <what was improved>"
```

## When Characterization Tests Are Required

Add characterization tests before refactoring when ANY of the following are true:

- Unit test coverage for the code being refactored is below 80%
- Existing tests only verify happy paths (no edge cases, error paths, or boundary conditions)
- The code has complex branching logic, external integrations, or implicit side effects
- You cannot confidently explain what the code does by reading the existing tests alone

## CRITICAL: Never Refactor Without Tests

Before refactoring ANY code:
- ✅ Run existing tests for the files you will modify — establish a behavioral baseline
- ✅ Minimum 80% unit test line coverage for code being refactored (unit tests only)
- ✅ 100% unit test coverage for critical paths
- ✅ All existing tests must PASS
- ✅ Tests meaningfully verify behavior (not just line execution)

If unit test coverage or behavioral confidence is insufficient:
- STOP refactoring
- Write characterization tests to capture current behavior (separate commit)
- Write additional unit tests to reach 80% coverage (separate commits using TDD cycle)
- Then proceed with refactoring

## Each Refactoring Step = One Commit

Examples of ONE refactoring step:
- Extract one method
- Rename one variable/class
- Move one method to another class
- Introduce one parameter object
- Extract one interface
- **Delete one obsolete class or dead code block**

## When to Refactor Proactively (Do Automatically)
- Method exceeds language-specific line limit (15-20 lines) → Extract methods
- Class exceeds 300 lines → Split responsibilities
- Duplicated code appears → Extract to shared method
- Hard-coded dependencies → Inject via constructor
- Long parameter list (>5 params) → Introduce parameter object
- Dead code exists → Delete it

## When to Ask First (Reactive Refactoring)
- Large architectural changes
- Moving code between packages
- Changing public APIs
