# Conversion Plan Template

**PURPOSE**: Reusable plan template for converting/porting a project from one framework to another. Enforces "behavior first, then quality" via explicitly gated phases.

**CRITICAL**: Each phase has a gate. You MUST pass the gate before advancing to the next phase. Do not skip phases or reorder them.

---

## How to Use This Template

1. Copy this template into your workspace as `<PROJECT>-PLAN.md`
2. Fill in the project-specific details in the **Project Context** section
3. Execute phases in order, checking off items as you go
4. At each phase gate, verify ALL criteria before proceeding
5. Generate reports at the end

---

## Project Context (Fill In)

| Field | Value |
|-------|-------|
| Source project | `<source-project-name>` |
| Source framework | `<e.g., Ktor 1.x, Spring Boot 2.x>` |
| Target project | `<target-project-name>` |
| Target framework | `<e.g., Ktor 4.x, Spring Boot 3.x>` |
| Language | `<e.g., Kotlin, Java>` |
| Date started | `<YYYY-MM-DD>` |
| Reference project | `<known-good project in target framework, e.g., company-creation-docket>` |

---

## Phase 1: Pre-Work

### 1.1 Read Engineering Standards

- [ ] Read `docs/CODING_PRACTICES.md` — general practices, SOLID, TDD
- [ ] Read `docs/AI_AGENT_WORKFLOW.md` — micro-commit workflow
- [ ] Read `docs/PRE_COMMIT_CHECKLIST.md` — pre-commit quality checklist
- [ ] Read language-specific standards (e.g., `docs/KOTLIN_STANDARDS.md`)
- [ ] Acknowledge: "I will follow the micro-commit workflow and all code quality gates"

### 1.2 Study Reference Project

Before porting any code, study the reference project to understand target framework patterns:

- [ ] **Project structure**: Package layout, module organization, build configuration
- [ ] **Framework patterns**: How routes, middleware, DI, and error handling are structured
- [ ] **Test patterns**: Test directory layout, test utilities, assertion style, acceptance test approach
- [ ] **Configuration**: How environment variables, application config, and profiles are managed
- [ ] **Domain modeling**: Data class patterns, validation approach, serialization
- [ ] Document key patterns in `<PROJECT>-REFERENCE-PATTERNS.md` for reference during porting

### 1.3 Capture Behavioral Baseline from Source Project

Before writing any code, document the source project's behavior:

- [ ] **Error response format**: Capture the exact JSON shape for error responses (field names, nesting, status codes)
- [ ] **Content-Type headers**: Document what each endpoint returns (e.g., `application/json`, `text/plain`)
- [ ] **Route inventory**: List all routes with HTTP method, path, and purpose
- [ ] **Authentication/authorization**: Document RBAC roles, permission model, and access rules
- [ ] **Health/status endpoints**: Document format, Content-Type, and response shape
- [ ] **Acceptance tests**: Identify existing acceptance/integration tests that verify behavior

#### Acceptance Test Categories (Mandatory)

During behavioral baseline capture, identify and document all behavioral categories. Each category MUST have at least one acceptance test in Phase 4. Minimum categories:

| Category | Examples |
|----------|----------|
| Happy path CRUD | Create, read, update, delete for each entity |
| Input validation | Missing fields, invalid formats, boundary values |
| Error responses | 4xx format, 5xx format, error body structure |
| Authentication/Authorization | Unauthenticated, unauthorized, role-based access |
| Content negotiation | Accept headers, Content-Type handling |
| Pagination/Filtering | Query parameters, defaults, edge cases |
| Concurrency/Idempotency | Duplicate requests, race conditions (if applicable) |

**Minimum acceptance test count**: At least 1 test per endpoint × behavioral category combination that exists in the source. Typical floor: 40–60 tests for a standard microservice.
Record the baseline in a `<PROJECT>-BEHAVIORAL-BASELINE.md` file.

#### Behavioral Baseline → Test Traceability

Every behavior documented in the baseline MUST be traceable to at least one acceptance test. Capture the baseline as a checklist, and in Phase 4, check off each item as its acceptance test passes. Any uncovered baseline behavior is a blocking gap.

### 1.4 Set Up Target Project

- [ ] Create target project repository/directory
- [ ] Initialize git repository (`git init --initial-branch=main`) — see `docs/GIT_SETUP_STANDARDS.md`
- [ ] Generate `.gitignore` (via gitignore.io API or manual fallback) and commit on `main`
- [ ] Create feature branch (e.g., `feat/conversion`) before making code changes
- [ ] Initialize build system (Gradle, Maven, npm, etc.)
- [ ] Add dependencies for target framework
- [ ] Copy agent files and engineering standards into target project
- [ ] Verify build compiles with empty project structure
- [ ] Set up static analysis (Detekt, PMD, ESLint, etc.) from the start

### Phase 1 Gate

**ALL must be true before proceeding:**
- [ ] Engineering standards read and acknowledged
- [ ] Behavioral baseline documented
- [ ] Reference project studied and key patterns documented
- [ ] Git repo initialized with at least 1 commit on `main` (`.gitignore`)
- [ ] Working on a feature branch (not `main`)
- [ ] Target project compiles with empty structure
- [ ] Static analysis configured and passing on empty project

---

## Phase 1.5: Characterization Tests

**Goal**: Before writing any conversion code, capture the source project's actual behavior in executable tests. These tests become the acceptance criteria for the conversion — if they pass against the converted code, the behavior is preserved.

### 1.5.1 Evaluate Source Project Tests

- [ ] Check if the source project has existing acceptance, integration, or characterization tests
- [ ] If existing tests exist, assess their coverage of behavioral contracts (routes, error handling, auth, edge cases)
- [ ] Identify behavioral gaps — behaviors documented in the baseline (Phase 1.3) that have no test coverage
- [ ] Decision: Do you need to write characterization tests against the source project first? (See criteria below)

### 1.5.2 Write Characterization Tests in Source Language (If Necessary)

**Evaluate whether this step is needed**: If the source project's existing tests adequately cover all behavioral contracts identified in Phase 1.3 (routes, error responses, auth, validation, edge cases), skip to 1.5.3.

- [ ] Write characterization tests against the RUNNING source project in its native language/framework
- [ ] These tests validate your understanding of the source behavior before porting
- [ ] Place these tests in a dedicated directory (e.g., `<SOURCE>/src/test/characterization/`) or document them separately
- [ ] Run them against the source project to verify they pass — these are your behavioral contracts

**When this step IS necessary**:
- Source project has < 40 acceptance/integration tests
- Complex business logic with no test coverage
- Error handling or auth behavior that is undocumented or ambiguous
- Edge cases that are unclear from reading code alone

**When this step can be SKIPPED**:
- Source project already has comprehensive acceptance tests (≥ 40 tests covering all behavioral categories from Phase 1.3)
- All routes, error formats, auth flows, and edge cases are already tested

### 1.5.3 Create Characterization Tests in Target Project

- [ ] Set up acceptance test infrastructure in the target project (e.g., `<TARGET>/src/acceptance/kotlin/`)
- [ ] Port or translate existing source project acceptance tests to the target framework's test style
- [ ] Fill gaps: for every behavioral contract documented in Phase 1.3 that lacks a test, write one
- [ ] Minimum coverage: at least 1 test per endpoint × behavioral category (happy path, validation, auth, error format)
- [ ] These tests will FAIL initially (target project has no implementation yet) — that is correct and expected
- [ ] **Target**: ≥ 40 acceptance tests for a standard microservice (justify if fewer)

### Phase 1.5 Gate

**ALL must be true before proceeding:**
- [ ] Source project tests evaluated and gaps identified
- [ ] If needed: characterization tests written and passing against source project
- [ ] Characterization/acceptance tests created in target project (expected to fail until Phase 2 implementation)
- [ ] Behavioral coverage documented: every baseline behavior from Phase 1.3 has a corresponding test
- [ ] Boulder updated with characterization test count and coverage summary
---

## Code Creation Principles (MANDATORY)

These principles govern HOW code is created during conversion. They apply to ALL phases. They exist because writing all code first and fixing quality violations retroactively is significantly slower than planning upfront and building incrementally.

### 1. Plan Before Code
- **Analyze source classes first**: Count lines, methods, parameters, responsibilities
- **Predict violations**: Extrapolate which classes will exceed size thresholds, which methods will have too many parameters, which interfaces will have too many functions
- **Design the class hierarchy upfront**: Decide on decomposition BEFORE writing any code
- **Map domain objects**: Identify all domain objects, estimate their sizes, split proactively if they seem too large

### 2. Strict TDD — Red → Green → Refactor
- **Write the test FIRST** (RED phase) — tests won't compile, that's expected
- **Write ONLY enough production code to compile and pass the test** (GREEN phase) — no more
- **Refactor** to improve quality (REFACTOR phase) — delete unused code, extract methods, rename
- **Commit after GREEN** and **commit after REFACTOR** — every commit is production-ready
- During RED phase, there WILL be non-compiling code — that's correct behavior

### 3. Do the Simplest Reasonable Thing
- Only write the code necessary to create a test or make a test pass
- Do NOT over-engineer or write more code than necessary
- Do NOT write production code that isn't exercised by a test
- Iterate: simple first, elegant later
- The Refactor phase exists for a reason — use it
- Delete unnecessary, unused code during the Refactor phase

### 4. Incremental Development
- Small, purposeful changes
- Each commit should be describable in one sentence
- Build up complexity gradually, not all at once
- If a class needs 10 methods, write them one at a time with tests for each

### 5. Domain Objects First
- Analyze and define domain objects (data classes, value objects, enums) before services
- If a domain object seems too large, break it up immediately
- Domain objects are the foundation — get them right early

### 6. Commit Size Discipline
- **No single commit may exceed 300 lines of insertions.** Before committing, run `git diff --cached --stat` and check the insertions count.
- If a commit would exceed 300 lines, split it into multiple smaller commits, each with its own tests.
- Scaffolding commits (Step 0 project/build setup) are exempt; all Phase 2+ commits must comply.
- This limit exists because monolithic commits burn context faster, are impossible to bisect, and leave orphaned work when sessions end unexpectedly.

---

## Phase 2: Port Behavior (Behavior First)

**Goal**: Get the converted project to behave identically to the source project. Follow all code quality gates during porting — do NOT accumulate technical debt.

### 2.1 Port Core Infrastructure

- [ ] Configuration/environment handling
- [ ] Database connectivity and migrations
- [ ] Authentication/authorization middleware
- [ ] Error handling and response formatting (MUST match source baseline)
- [ ] Health/status endpoints (MUST match source Content-Type and format)
- [ ] Logging setup

### 2.2 Port Business Logic

For each domain area in the source project:

- [ ] Port domain models / data classes
- [ ] Port service layer (follow SRP — decompose if source has god classes)
- [ ] Port routes/controllers
- [ ] Write unit tests (meet repo standard: minimum 80% overall unit test coverage, 100% for critical paths)
- [ ] Run characterization/acceptance tests from Phase 1.5 against converted project; where feasible, also run original source acceptance tests as additional verification

**Quality rules during porting (NO EXCEPTIONS):**
- Every class <= 300 lines. If porting a class that would exceed 300 lines, decompose it NOW, not later.
- Every method <= language-specific limit (15-20 lines). Extract methods immediately.
- No `@Suppress` for structural violations (`LargeClass`, `TooManyFunctions`, `LongMethod`, `LongFunction`, `CyclomaticComplexity`, `CyclomaticComplexMethod`, `LongParameterList`). If the linter flags it, fix it. For conversion-specific allowances (`TooGenericExceptionCaught`, `UNCHECKED_CAST`), see the agent's **Conversion-Specific `@Suppress` Allowances** table — each requires a mandatory justification comment.
- No `!!` in production Kotlin code. Fix nullability at the source.
- Constructor injection for all dependencies. No hard-coded instantiation.
- Follow TDD micro-commit cycle: STOP -> RED -> GREEN -> COMMIT -> REFACTOR -> COMMIT
- **Every Phase 2+ commit that adds production code MUST include unit tests for that code in the same commit.** No production-only commits. No deferring tests to a later batch.

### 2.3 Port Remaining Features

- [ ] Port any remaining routes/endpoints
- [ ] Port scheduled tasks, background jobs, etc.
- [ ] Port any middleware or filters

### Phase 2 Gate

**ALL must be true before proceeding:**
- [ ] All source project routes exist in converted project
- [ ] Error response format matches source baseline exactly
- [ ] Content-Type headers match source baseline
- [ ] RBAC/authorization behavior matches source
- [ ] Characterization/acceptance tests from Phase 1.5 pass against converted project (target: 100%); where feasible, also run original source acceptance tests against the converted project as additional verification
- [ ] Unit test coverage >= 80% (Kover/JaCoCo, unit tests only)
- [ ] All unit tests pass
- [ ] Build succeeds
- [ ] Static analysis passes (Detekt/PMD/ESLint — zero violations)
- [ ] Git history shows incremental commits (`git log --oneline` — no bulk changes)
- [ ] No non-merge commit exceeds 300 lines of insertions (verify: `git log --no-merges --format='%h' | while read h; do ins=$(git diff --numstat "$h^!" 2>/dev/null | awk '$1=="-"{print 999999;exit}{a+=$1}END{print a+0}'); echo "$h $ins"; done` — each line prints `<hash> <insertions>`; ensure insertions ≤ 300; any commit containing a binary file will show `999999` (automatically over-limit) — split the commit or justify the binary addition; for the initial commit, run `git show --stat <hash>` manually)

---

## Phase 3: Quality Sweep

**Goal**: Verify and improve code quality beyond the minimums enforced during Phase 2.

### 3.1 Structural Quality Audit

- [ ] No class exceeds 300 lines
- [ ] No method exceeds language-specific limit (15-20 lines)
- [ ] No method has more than 5 parameters
- [ ] Zero `@Suppress` for structural violations (`LargeClass`, `TooManyFunctions`, `LongMethod`, `LongFunction`, `CyclomaticComplexity`, `CyclomaticComplexMethod`, `LongParameterList`)
- [ ] Any conversion-specific `@Suppress` (`TooGenericExceptionCaught`, `UNCHECKED_CAST`) has a mandatory justification comment and is documented in Phase 3 notes
- [ ] Zero `!!` in production code
- [ ] Maximum 0-2 private methods per class (SRP)
- [ ] Interfaces used for dependency injection (DIP)

### 3.2 Documentation

- [ ] KDoc/Javadoc on all public classes and methods
- [ ] README updated with setup instructions
- [ ] Architecture decisions documented (ADR if significant)

### 3.3 Test Quality

- [ ] Unit test coverage >= 80% overall (unit tests only)
- [ ] 100% unit test coverage for critical paths
- [ ] All tests follow Given-When-Then structure
- [ ] Descriptive test names (e.g., `shouldReturnErrorWhenUserNotFound`)
- [ ] Mutation testing passes (if configured)

### Phase 3 Gate

**ALL must be true before proceeding:**
- [ ] All structural quality items pass
- [ ] All documentation items complete
- [ ] All test quality items pass
- [ ] Full Detekt/PMD/ESLint scan passes with zero violations
- [ ] All acceptance tests still pass (no regressions from quality improvements)
- [ ] Unit test coverage >= 90% overall (unit tests only)

---

## Phase 4: Verification

**Goal**: Final verification that BOTH behavioral fidelity AND code quality are achieved.

### 4.1 Behavioral Verification

- [ ] Run characterization/acceptance tests (from Phase 1.5) against converted project
- [ ] Document pass rate: `___/___` tests passing
- [ ] For any failures, document root cause and resolution
- [ ] Compare error responses side-by-side with source baseline
- [ ] Verify all Content-Type headers match

### 4.2 Quality Verification

- [ ] Run full static analysis suite — zero violations
- [ ] Run full unit test suite — all pass
- [ ] Run coverage report — unit test coverage >= 95% overall (unit tests only)
- [ ] Run mutation testing (if configured) — meets thresholds
- [ ] Count structural `@Suppress` annotations — must be zero (`LargeClass`, `TooManyFunctions`, `LongMethod`, `LongFunction`, `CyclomaticComplexity`, `CyclomaticComplexMethod`, `LongParameterList`)
- [ ] Count conversion-specific `@Suppress` annotations (`TooGenericExceptionCaught`, `UNCHECKED_CAST`) — each must have justification comment; document total: `___`
- [ ] Count `!!` in production code — must be zero
- [ ] Largest class line count: `___` (must be <= 300)
- [ ] Largest method line count: `___` (must be <= language limit)

### Phase 4 Gate

**ALL must be true to consider the conversion complete:**
- [ ] 100% acceptance test pass rate (or documented exceptions with user approval)
- [ ] Zero structural `@Suppress` annotations
- [ ] Zero `!!` in production code
- [ ] All quality metrics within thresholds
- [ ] Build passes
- [ ] Static analysis passes

- [ ] Unit test coverage >= 95% overall (unit tests only)
- [ ] Acceptance test gate: All behavioral categories identified in Phase 1.3 (Acceptance Test Categories / baseline capture) must have corresponding characterization/acceptance tests. If fewer than 40 tests exist, justify why in the status/progress tracking log.
---

## Phase 5: Reports

### 5.1 Generate Reports

- [ ] **Verification report**: Acceptance test results, pass rate, any failures with root cause
- [ ] **Comparison report**: Side-by-side metrics (source vs. converted) — LOC, test count, coverage, class count, largest class, etc.
- [ ] **Quality summary**: Static analysis results, coverage, mutation testing

### 5.2 Cleanup

- [ ] Stop any running services (app servers, database containers)
- [ ] Remove any temporary test data
- [ ] Update project status tracking or project management system (if used)

---

## Summary Metrics (Fill In When Complete)

| Metric | Source | Converted |
|--------|--------|-----------|
| Lines of code | | |
| Number of files | | |
| Unit tests | | |
| Characterization tests | | |
| Acceptance tests | | |
| Unit test coverage | | |
| Largest class (lines) | | |
| Largest method (lines) | | |
| Structural `@Suppress` count | | |
| Conversion-specific `@Suppress` count | | |
| `!!` count | | |
| Detekt/PMD violations | | |
| Acceptance test pass rate | | |
| Commits (`git log --oneline \| wc -l`) | N/A | |

---

## Lessons Learned (Fill In)

Document what went well, what could be improved, and any patterns discovered during the conversion.

1. ...
2. ...
3. ...

---

**Last Updated**: March 2026
**Version**: 3.5 (Added: Principle #6 — Commit Size Discipline [300-line insertion limit per commit], tests-with-code enforcement in Phase 2.2, 300-line verification in Phase 2 gate. Based on ktor10 post-mortem analysis.)
