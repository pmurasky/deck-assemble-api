---
description: Verify test coverage before refactoring
agent: standards-review
subtask: true
---

I want to refactor code and need to verify test coverage first.

Target: $ARGUMENTS

Before any refactoring can proceed, check:

1. **Test existence** - Do tests exist for the code being refactored?
2. **Coverage level** - Is there at least 80% line coverage? (100% for critical paths)
3. **Test quality** - Do tests follow Given-When-Then structure?
4. **Test names** - Are test names descriptive?
5. **All tests passing** - Are all current tests green?

If coverage is insufficient:
- List what tests need to be written BEFORE refactoring can begin
- Suggest test cases using Given-When-Then structure
- Remind that TDD (RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT) must be followed

If coverage is sufficient:
- Confirm refactoring can proceed
- Suggest the micro-commit breakdown for the refactoring
- Remind to run tests after EVERY refactoring step

Reference `docs/CODING_PRACTICES.md` for the full TDD workflow.
