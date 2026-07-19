---
description: Check test coverage for target files and identify gaps
agent: standards-review
subtask: true
---

Analyze test coverage for the specified target.

Target: $ARGUMENTS

Perform these checks:

1. **Find tests** - Locate test files that correspond to the target (e.g., `FooTest.java` for `Foo.java`, `foo.test.ts` for `foo.ts`)
2. **Coverage assessment** - Estimate line coverage from test content:
   - Which public methods are tested?
   - Which branches/edge cases are covered?
   - Are error paths tested?
3. **Coverage threshold** - Does it meet the minimum?
   - 80% minimum for standard code
   - 100% for critical paths (auth, payments, data integrity)
4. **Test quality** - Are existing tests well-structured?
   - Given-When-Then format
   - Descriptive names (e.g., `shouldReturnEmptyListWhenNoResultsFound`)
   - Proper assertions (not just `assertNotNull`)
   - Independent (no shared mutable state)
5. **Gap analysis** - What's missing?
   - Untested methods
   - Missing edge cases (null, empty, boundary values)
   - Missing error/exception paths
   - Missing integration tests

Output a structured report:

```
## Test Coverage Report

### Target: <file or directory>

### Coverage Summary
- Estimated coverage: X%
- Threshold: 80% / 100% (critical)
- Status: PASS / FAIL

### Tested
- List of methods/paths with test coverage

### Gaps (Missing Tests)
1. [Priority: High/Medium/Low] Description of missing test
   - Suggested test: `shouldDoXWhenY`
   - Structure: Given... When... Then...

### Recommendations
- Prioritized list of tests to write
```

Reference `docs/CODING_PRACTICES.md` for testing standards.
