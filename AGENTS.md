# Engineering Standards Agent

## Overview

This project provides reusable engineering standards for AI coding agents (OpenCode, Cursor, Copilot, etc.). It enforces coding practices, SOLID principles, TDD micro-commit workflows, and code quality standards across any project.

## Core Principles

You MUST follow these principles for ALL code changes. No exceptions.

### 1. Micro-Commit Workflow
Every logical change = one commit. Never bundle multiple logical changes into one commit.

- One refactoring step per commit
- One feature implementation per commit
- One test update per commit
- One documentation update per commit

### 2. TDD Micro-Commit Cycle
For ALL code changes, follow the STOP -> RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT cycle. See `docs/AI_AGENT_WORKFLOW.md` for the full workflow.

### 3. Production-Ready Commits
Every commit MUST be production-ready:
- All tests pass
- Build succeeds
- No lint errors
- Code is deployable

### 4. Code Quality Rules
- Maximum method length: 15-20 lines (excluding blank lines and braces; see language-specific standards for exact limit)
- Maximum class length: 300 lines (consider refactoring if larger)
- Maximum 0-2 private methods per class (SRP guideline)
- Maximum 5 parameters per method (use parameter objects)
- No duplicated code (DRY principle)

### 5. SOLID Principles
- **SRP**: Each class has ONE reason to change
- **OCP**: Open for extension, closed for modification (use Strategy Pattern)
- **LSP**: Subtypes must be substitutable for base types
- **ISP**: Prefer focused interfaces over fat interfaces
- **DIP**: Depend on abstractions, not concrete classes (use dependency injection)

### 6. Commit Message Format
Use Conventional Commits:

Scope is recommended and may be omitted for trivial cross-cutting changes.

```
<type>(<scope>): <description>
# or
<type>: <description>

[body explaining WHY, not WHAT]
```

Types: feat, fix, refactor, test, docs, perf, chore

### 7. Testing Standards
- Minimum 80% unit test coverage overall (unit tests only -- integration/E2E tests do not count toward coverage)
- 100% unit test coverage for critical paths
- Use Given-When-Then structure
- Descriptive test names (e.g., `shouldSelectLatestVersionWhenAvailable`)
- Never commit failing tests (every commit must be production-ready, no exceptions)

### 8. Test Execution Tiers
- **Before every commit**: Run unit tests (mandatory, no exceptions)
- **Before pushing**: Run unit tests + integration tests
- **CI pipeline**: Runs all tests (unit + integration + E2E) as hard gate before merge

### 9. Refactoring Prerequisites
**Never refactor without tests. No exceptions.**
- Before refactoring, verify unit test coverage is at least 80% for the code being changed (unit tests only)
- If coverage is below 80%, STOP and write unit tests FIRST (separate commits)
- All tests MUST pass before starting any refactoring
- After each refactoring step, run ALL tests and commit immediately
- See `docs/AI_AGENT_WORKFLOW.md` and `docs/PRE_COMMIT_CHECKLIST.md` for full details

## External File Loading

CRITICAL: When you encounter a file reference below, use your Read tool to load it on a need-to-know basis. They contain detailed instructions relevant to SPECIFIC tasks.

Do NOT preemptively load all references. Use lazy loading based on actual need.

## Detailed Standards References

### All projects

For the micro-commit workflow and AI agent instructions: @docs/AI_AGENT_WORKFLOW.md
For language-agnostic coding practices, SOLID examples, testing, and TDD: @docs/CODING_PRACTICES.md
For the standards index (table of contents): @docs/CODING_STANDARDS.md
For the pre-commit quality checklist: @docs/PRE_COMMIT_CHECKLIST.md
For design patterns guidance: @docs/DESIGN_PATTERNS.md
For Architecture Decision Records (ADR) guidance: @docs/ADR_STANDARDS.md
For security standards (auth, secrets, OWASP, API security): @docs/SECURITY_STANDARDS.md
For logging standards (structured logging, log levels, correlation IDs, PII): @docs/LOGGING_STANDARDS.md

### TypeScript / Next.js projects

For TypeScript/JavaScript conventions (when working with TypeScript or JavaScript): @docs/TYPESCRIPT_STANDARDS.md
For Next.js App Router conventions: @docs/NEXTJS_STANDARDS.md
For DevOps standards (CI/CD, deployment, release workflows): @docs/DEVOPS_STANDARDS.md

### Other languages (skip for TypeScript/Next.js)

For Go-specific conventions (when working with Go): @docs/GO_STANDARDS.md
For Java-specific conventions (when working with Java): @docs/JAVA_STANDARDS.md
For Kotlin-specific conventions (when working with Kotlin): @docs/KOTLIN_STANDARDS.md
For Python-specific conventions (when working with Python): @docs/PYTHON_STANDARDS.md
For SOLID principles with multi-language examples: @docs/SOLID_PRINCIPLES.md
For static analysis standards (PMD, detekt, Checkstyle, CPD): @docs/STATIC_ANALYSIS_STANDARDS.md
For Checkstyle style enforcement (Java only): @docs/CHECKSTYLE_STANDARDS.md
For architecture testing with ArchUnit (Java/Kotlin): @docs/ARCHUNIT_STANDARDS.md
For SpotBugs bytecode bug detection (Java only): @docs/SPOTBUGS_STANDARDS.md
For conversion/porting plan template (gated phases, behavioral baseline, quality gates): @docs/CONVERSION_PLAN_TEMPLATE.md

## Before Making ANY Code Changes

1. Pull latest changes: `git pull`
2. Break work into micro-commits and execute one task at a time, committing after each logical change
3. Run unit tests before every commit
4. Run unit tests + integration tests before pushing
5. Follow the pre-commit checklist

## Selecting Work

When asked what to work on next, consult the active issue tracker. If the repo is on GitHub and `gh` is available, use: `gh issue list --label "P1: should fix" --state open`. Prioritize P1 over P2. Reference issues in commits (e.g., `closes #2`). See `docs/AI_AGENT_WORKFLOW.md` for full details.

## Closing Issues

**CRITICAL**: After completing work on an issue, ALWAYS close it in the active tracker.

Complete workflow: Implement → Test → Commit → Push → **Close Issue/Ticket**. See `docs/AI_AGENT_WORKFLOW.md` for tracker-specific commands.

## Red Flags (Stop and Ask User)

If you encounter these situations, STOP and ask:

1. **Unclear scope**: Change requires modifying 10+ files
2. **Breaking change**: Change will break a public API
3. **Test failures with unclear expected behavior**: Tests are failing after your change and expected behavior cannot be inferred from tests/docs/acceptance criteria
4. **Conflicting patterns**: Existing code doesn't follow SOLID
5. **Missing tests**: Code being changed has < 80% unit test coverage
