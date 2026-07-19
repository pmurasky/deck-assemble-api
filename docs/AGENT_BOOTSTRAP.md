# Engineering Standards Bootstrap

Apply these rules from the first response in every coding session:

1. Follow micro-commit workflow: one logical change per commit.
2. Follow TDD cycle: STOP -> RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT.
3. Every commit must be production-ready: tests pass, build succeeds, no lint errors.
4. Do not refactor without test coverage (>=80% unit test coverage for changed code).
5. Run unit tests before every commit; run unit + integration tests before push.
6. Enforce SOLID, DRY, and method/class size limits from standards docs.
7. Use Conventional Commits: `<type>(<scope>): <description>`.
8. Stop and ask when scope is unclear, behavior is ambiguous, or API changes are breaking.

Load detailed guidance lazily from:
- `AGENTS.md`
- `docs/AI_AGENT_WORKFLOW.md`
- `docs/PRE_COMMIT_CHECKLIST.md`
- `docs/CODING_PRACTICES.md`

Do not preload all standards documents unless required for the current task.
