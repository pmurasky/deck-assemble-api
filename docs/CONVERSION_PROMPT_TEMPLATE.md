# Conversion Prompt Template

**PURPOSE**: Reusable prompt for starting a framework conversion via OpenCode CLI. Produces a prompt file and a one-line CLI invocation. Designed to prevent bulk-porting, wrong agent selection, and premature completion — failure modes observed in prior conversion attempts.

**VERSION**: 2.7.0

---

## Quick Start

1. Copy the **Prompt File** section below into a file (e.g., `conversion-prompt.md`)
2. Replace all `$VARIABLES` with your values
3. Run the **CLI Invocation** command
4. Delete `conversion-prompt.md` when the conversion is complete

---

## Variables (fill in before use)

| `$WORKDIR` | Your workspace directory | `/Users/eheikki3/workspace` |
| `$PROJECT` | Short project identifier (used in filenames, UPPERCASE) | `DM-SEGMENT-KTOR7` |
| `$SOURCE` | Source project directory name | `dm-segment` |
| `$TARGET` | Target project directory name (must not exist for a fresh start; may already exist when resuming — see Resuming a Conversion) | `dm-segment-ktor7` |
| `$EXAMPLE` | Reference project in target framework | `company-creation-docket` |
| `$SOURCE_FRAMEWORK` | Source framework name and version | `Spring Boot 3.5.3 / Java 21` |
| `$TARGET_FRAMEWORK` | Target framework name and version | `Ktor 3.x / Kotlin 2.2` |
| `$GITIGNORE_TAGS` | Comma-separated tags for gitignore.io API | `kotlin,gradle,intellij` |
| `$TARGET_LANGUAGE_SKILL` | Language skill name for target framework | `kotlin-standards` |
| `$ACCEPTANCE_BASELINE` | *(optional)* A prior successful conversion of the same source project, used for cross-project acceptance test verification | `dm-segment-ktor` |

---

## CLI Invocation

```bash
opencode run \
  -m github-copilot/claude-opus-4.6 \
  --agent build \
  --dir $WORKDIR \
  --command ulw-loop \
  "Execute the conversion described in the attached file. Follow every instruction exactly." \
  -f $WORKDIR/conversion-prompt.md
```

**Flags explained:**

| Flag | Purpose | Why it matters |
|------|---------|----------------|
| `-m <MODEL_ID>` | Select the model (e.g., `github-copilot/claude-opus-4.6`) | Ensures strongest available model |
| `--agent build` | Select the build agent (Sisyphus) | **CRITICAL**: Without this, OpenCode may select Hephaestus (Deep Agent), which stalls on multi-phase work |
| `--dir $WORKDIR` | Set working directory | Ensures file operations resolve correctly |
| `--command ulw-loop` | Start the ultrawork continuation loop | Keeps the agent working across phases without stalling. **Fresh start ONLY** — resume sessions must use `--command ralph-loop` instead (see Resume CLI Invocation) |
| `-f $WORKDIR/conversion-prompt.md` | Attach the prompt file | Allows a detailed prompt without shell quoting issues |

### Resume CLI Invocation

When resuming a conversion that was interrupted (target directory already exists, commits already made), use this invocation. The inline message gives the agent its immediate recovery instructions; the prompt file provides rules reference (FORBIDDEN actions, completion criteria, quality gates).

**IMPORTANT**: Resume uses `--command ralph-loop`, NOT `--command ulw-loop`. The `ulw-loop` command injects system-level instructions that tell the agent to "launch multiple background agents IN PARALLEL" and "DELEGATE — DO NOT WORK YOURSELF." These instructions override the resume prompt's "use direct tools only" directive, causing the agent to fire background agents and then terminate before results arrive. The `ralph-loop` provides autonomous continuation without the aggressive delegation behavior.

```bash
opencode run \
  -m github-copilot/claude-opus-4.6 \
  --agent build \
  --dir $WORKDIR \
  --command ralph-loop \
  "Resume the $SOURCE → $TARGET conversion. Open $WORKDIR/$PROJECT-BOULDER.md and immediately reset the boulder's 'Session commit count' to 0, ignoring any previous value — you are starting a fresh session with a full commit budget. Then read the boulder to recover your position. The target project is at $WORKDIR/$TARGET on branch feat/conversion. Follow the NEXT ACTION in the boulder. If there are uncommitted changes, commit them first. Quality rules: zero !!, zero structural @Suppress, TDD micro-commits, each commit ≤ 300 lines of insertions, classes ≤300 lines, methods ≤15-20 lines. Update the boulder after every commit. Do NOT launch background agents (explore, librarian, oracle) — use direct tools only (Read, Grep, Glob, Bash)." \
  -f $WORKDIR/conversion-prompt.md
```

**Why attach the prompt on resume?** The prompt file contains the full rules (FORBIDDEN actions, completion criteria, quality gates). On a fresh start the agent reads the prompt and then follows Step 1 to open only the scoped standards docs referenced there. On resume, it has the boulder for position recovery, but still needs the prompt for rules reference. The agent's **Resuming a Conversion** section tells it to skip re-reading standards docs — it only uses the prompt for rules, not exploration.

**Fallback**: If `ralph-loop` also causes the agent to terminate prematurely, remove the `--command` option entirely and re-run the resume command manually after each agent turn.

---

## Prompt File

Save everything between the `---` markers below as `conversion-prompt.md`, then replace all `$VARIABLES`:

---

````markdown
# Framework Conversion: $SOURCE → $TARGET

## Your Mission

Convert `$SOURCE` ($SOURCE_FRAMEWORK) to `$TARGET` ($TARGET_FRAMEWORK) following a strict phased plan with quality gates. You are creating NEW code inspired by the source — you are NOT copying/translating files wholesale.

**AUTONOMY**: Work through ALL phases continuously without stopping to ask for permission. Do NOT ask "Want me to continue?" or "Should I proceed?" — the answer is always YES. Keep working until you hit the Completion Criteria or encounter a blocking error you cannot resolve. The only reason to stop and ask the user is if something is genuinely broken and you cannot fix it after 3 attempts.

## Paths

| `$WORKDIR` | Working directory root |
| `$WORKDIR/ai-code-sentinel/` | Engineering standards (read these FIRST) |
| `$WORKDIR/ai-code-sentinel/docs/CONVERSION_PLAN_TEMPLATE.md` | Conversion plan template (copy and fill in) |
| `$WORKDIR/$SOURCE` | Source project (READ ONLY — never modify) |
| `$WORKDIR/$TARGET` | Target project (must not exist for a fresh start; may already exist when resuming — see Resuming a Conversion) |
| `$WORKDIR/$EXAMPLE` | Reference project in target framework (study patterns, do not modify) |
| `$WORKDIR/$PROJECT-PLAN.md` | Your filled-in conversion plan (create from template) |
| `$WORKDIR/$PROJECT-BOULDER.md` | Progress tracker (create and update after every commit) |
| `$WORKDIR/$ACCEPTANCE_BASELINE` | *(optional)* Prior successful conversion — run its acceptance tests against `$TARGET` for cross-project verification (READ ONLY) |

## Required Skills

Load these skills using the `skill` tool before starting Step 0. They encode the operational knowledge that governs this conversion. Do NOT skip this step — the rules in these skills are non-negotiable.

| Skill | Purpose | When It Matters |
|-------|---------|-----------------|
| `micro-commit-workflow` | TDD micro-commit cycle, commit size discipline (300-line limit), tests-with-code gate | Every commit |
| `progress-tracker` | Boulder file structure, update protocol, NEXT ACTION specificity, resume protocol | After every commit; on resume |
| `multi-session-execution` | Mechanical commit caps, session budgets, context exhaustion protocol | Session boundaries |
| `context-budget` | Mid-session context health monitoring, clean exit criteria, handoff template | Mid-session; session end |
| `coding-practices` | YAGNI, SRP, SOLID summary, method/class limits, autonomous execution | All code changes |
| `tdd-strategies` | RED-GREEN-REFACTOR strategies, tests-with-code enforcement | All feature implementation |
| `$TARGET_LANGUAGE_SKILL` | Language-specific conventions (e.g., `kotlin-standards`, `java-standards`, `go-standards`) | All code in target language |
| `pre-commit-checklist` | SOLID violation checks, code metrics, testing requirements | Before every commit |

> **Note**: Replace `$TARGET_LANGUAGE_SKILL` with the appropriate language skill for your target framework (e.g., `kotlin-standards` for Ktor conversions, `go-standards` for Go conversions).

## Step 0: Scaffold Artifacts (FIRST 9 ACTIONS — No Exceptions)

These nine actions MUST be your very first actions. Do NOT read any project source files, explore any code structure, or analyze anything before completing all nine. Reading the plan template to copy it is part of scaffolding — do that in action 7, but use placeholder content for any details you do not yet know.

1. **Check for existing `$WORKDIR/$TARGET`.** If it does **not** exist, continue to action 2. If it **does** exist, treat this as a *potential* resume: before proceeding, verify that (a) `$WORKDIR/$PROJECT-PLAN.md` and `$WORKDIR/$PROJECT-BOULDER.md` exist, (b) `$WORKDIR/$TARGET` is a git repo in the expected state (e.g., `main` and `feat/conversion` branches), and (c) the repo clearly belongs to this conversion. If any check fails, **STOP and ask the user** whether to delete/rename the directory or update variables — do **not** reuse it implicitly. If all checks pass, skip to the **Resuming a Conversion** section below.
2. **Create the target directory**: `mkdir $WORKDIR/$TARGET`
3. **Check for existing git repo**: Run `git -C $WORKDIR/$TARGET rev-parse --is-inside-work-tree 2>/dev/null`. If already inside a repo, STOP and ask the user (the target directory may be inside a monorepo or other git project).
4. **Initialize git**: `cd $WORKDIR/$TARGET && git init --initial-branch=main`
5. **Generate .gitignore**: `curl -fsSL "https://www.toptal.com/developers/gitignore/api/$GITIGNORE_TAGS" > $WORKDIR/$TARGET/.gitignore` — then append project-specific entries (`.sisyphus/`, `.env`, `.env.local`). If `curl` fails, create a manual `.gitignore` per `docs/GIT_SETUP_STANDARDS.md` section 3.3.
6. **Initial commit on main**: `cd $WORKDIR/$TARGET && git add .gitignore && git commit -m "chore: initialize repository with .gitignore"`
7. **Copy the plan template**: Read `$WORKDIR/ai-code-sentinel/docs/CONVERSION_PLAN_TEMPLATE.md` and write it to `$WORKDIR/$PROJECT-PLAN.md`, filling in any `<placeholder>` fields whose values you already know and leaving unknown ones as TODO/placeholder entries to be filled after Step 1 exploration. **If the plan file already exists and appears pre-populated by an orchestrator** (e.g., phases are filled in, endpoints are listed, acceptance test categories are documented), verify the contents against the template structure instead of overwriting — check that all required sections exist and that the phase gates are present. Fix any gaps but preserve the orchestrator's work.
8. **Create the progress tracker**: Create `$WORKDIR/$PROJECT-BOULDER.md` with the phase status table (all phases 🔲 Not started)
9. **Create feature branch**: `cd $WORKDIR/$TARGET && git checkout -b feat/conversion`

**Note**: The plan (`$WORKDIR/$PROJECT-PLAN.md`) and boulder (`$WORKDIR/$PROJECT-BOULDER.md`) live in `$WORKDIR`, outside the target project's git repo. They are workspace-level artifacts and are intentionally NOT source-controlled.

**Checkpoint**: After Step 0, verify:
- `$WORKDIR/$TARGET` exists with a git repo (`git rev-parse --is-inside-work-tree` returns `true`)
- `git log --oneline` shows exactly 1 commit (initial .gitignore on main)
- `git branch --show-current` shows `feat/conversion`
- `$WORKDIR/$PROJECT-PLAN.md` and `$WORKDIR/$PROJECT-BOULDER.md` exist on disk

## Resuming a Conversion (When Target Already Exists)

If `$WORKDIR/$TARGET` already exists and passed the Step 0 action 1 checks, this is a **resume session**. Skip the remainder of Steps 0 and 1 (scaffolding + bounded exploration) and follow this recovery protocol:

1. **Verify resume prerequisites (STOP if any fail)**:
   - Confirm `$WORKDIR/$PROJECT-BOULDER.md` and `$WORKDIR/$PROJECT-PLAN.md` both exist on disk.
   - Run `git -C $WORKDIR/$TARGET rev-parse --is-inside-work-tree` and ensure it prints `true`.
   - Run `git -C $WORKDIR/$TARGET branch --show-current` and ensure it prints `feat/conversion`.
   - If any of these checks fail, **STOP IMMEDIATELY** and ask the user for guidance. Do **not** guess the state, re-initialize git, or create missing plan/boulder files on your own.
2. **Reset session commit count**: Open `$WORKDIR/$PROJECT-BOULDER.md`. **Before reading any other field, immediately overwrite "Session commit count" with `0`** — whatever value it holds reflects a previous session and must not be used to gate any action. You are starting a fresh session with a full commit budget.
3. **Read the boulder**: Read your exact position, metrics, and NEXT ACTION from `$WORKDIR/$PROJECT-BOULDER.md`.
4. **Check git state**: Run `git -C $WORKDIR/$TARGET log --oneline -10` and `git -C $WORKDIR/$TARGET status` to verify commits and detect uncommitted work.
5. **Commit uncommitted work first**: If `git status` shows modified or untracked files, commit them with appropriate messages before doing anything else.
6. **Verify the build is green**: Run the project's build/test command (e.g., `./gradlew build` or equivalent). If it fails, make only the minimal fixes required to restore green, commit them, and rerun until the build passes. **STOP and ask the user for guidance** if you cannot resolve the failures. Do NOT continue to the next step on a red build.
7. **Read the plan**: `$WORKDIR/$PROJECT-PLAN.md` — find the first unchecked item to confirm alignment with the boulder's NEXT ACTION.
8. **Continue from NEXT ACTION**: Execute the boulder's NEXT ACTION. Do NOT re-read engineering standards, re-explore the source project, or re-study the reference project unless you need specific information that is not already in the plan.

**What NOT to do on resume:**
- Do NOT re-read `AGENTS.md`, `CODING_PRACTICES.md`, the language-specific standards doc (e.g., `KOTLIN_STANDARDS.md`), or `AI_AGENT_WORKFLOW.md` — their rules are already encoded in your agent config and the plan file.
- Do NOT re-explore the source or reference projects for information that is already documented in the plan.
- Do NOT re-scaffold, re-initialize git, or re-create the plan/boulder.
- Do NOT start from Phase 1 — the boulder tells you exactly where you are.
- Do NOT launch background agents (explore, librarian, oracle, etc.) — they cause the session to terminate before results arrive. Use direct tools only (Read, Grep, Glob, Bash). If you need information from the source or reference project, read the specific file directly.

**Token budget on resume**: By skipping Steps 0–1 and standards re-reads, you save ~50–80K tokens (~25–40% of context window) for actual code work.

## Step 1: Bounded Exploration (Budget: 15 tool calls TOTAL)

Read ONLY what you need to fill in the plan. Do NOT read every file in every project. You have a budget of 15 tool calls for ALL of Step 1 combined.

**Token-aware reading**: Your build agent config already encodes the engineering standards (SOLID, TDD, micro-commits, quality gates). Do NOT read large standards documents end-to-end just to "acknowledge" them. For Step 1.1, read `AGENTS.md` (~7KB) for the standards index, then read ONLY the language-specific standards file (e.g., `KOTLIN_STANDARDS.md` ~20KB). Skip `CODING_PRACTICES.md` (~35KB) and `AI_AGENT_WORKFLOW.md` (~23KB) — their rules are already in your agent config.

| Sub-step | Budget | What to read |
|----------|--------|--------------|
| 1.1 Engineering standards | 2 calls | `$WORKDIR/ai-code-sentinel/AGENTS.md` (index) + 1 language-specific standards file. Skip general practices docs — already in agent config. |
| 1.2 Reference project patterns | 4 calls | Study `$WORKDIR/$EXAMPLE` for project structure, routing, DI, testing, and configuration patterns. Document key patterns. |
| 1.3 Source project analysis | 5 calls | Inventory routes, models, services, clients. Document behavioral baseline. Identify god classes for decomposition. |
| 1.4 Fill in the plan | 4 calls | Update `$WORKDIR/$PROJECT-PLAN.md` with all findings. Update boulder with Phase 1 (Step 1) status. |

**After Step 1 / Phase 1**: Your plan file must be fully populated. Your boulder must show Phase 1 ✅ complete. If you haven't finished filling in the plan within 15 calls, STOP exploring and fill in the plan with what you have — you can refine it later during porting.

## Step 2: Characterization Tests (Capture Behavior BEFORE Porting)

**PURPOSE**: Before writing any conversion code, capture the source project's actual behavior in executable tests. These tests become the acceptance criteria for the conversion — if they pass against the converted code, the behavior is preserved.

### 2.1 Evaluate Source Project Tests

- [ ] Check if the source project has existing acceptance, integration, or characterization tests
- [ ] If existing tests exist, assess their coverage of behavioral contracts (routes, error handling, auth, edge cases)
- [ ] Identify behavioral gaps — behaviors that exist in source code but have no test coverage

### 2.2 Write Characterization Tests in Source Project Language (If Necessary)

**Evaluate whether this step is needed**: If the source project's existing tests adequately cover all behavioral contracts (routes, error responses, auth, validation, edge cases), skip to 2.3. Otherwise:

- [ ] Write characterization tests against the RUNNING source project in its native language/framework
- [ ] These tests validate your understanding of the source behavior before porting
- [ ] Place these tests in a dedicated directory (e.g., `$WORKDIR/$SOURCE/src/test/characterization/`) or document them separately
- [ ] Run them against the source project to verify they pass — these are your behavioral contracts

**When this step IS necessary**:
- Source project has < 40 acceptance/integration tests
- Complex business logic with no test coverage
- Error handling or auth behavior that is undocumented or ambiguous
- Edge cases that are unclear from reading code alone

**When this step can be SKIPPED**:
- Source project already has comprehensive acceptance tests (≥ 40 tests covering all behavioral categories)
- All routes, error formats, auth flows, and edge cases are already tested

### 2.3 Create Characterization Tests in Target Project

- [ ] Port or create acceptance tests in the target project (`$WORKDIR/$TARGET/src/acceptance/kotlin/` or equivalent)
- [ ] Start with any existing source project acceptance tests — translate them to the target framework
- [ ] Fill gaps: for every behavioral contract identified in Phase 1.3 that lacks a test, write one
- [ ] Minimum coverage: at least 1 test per endpoint × behavioral category (happy path, validation, auth, error format)
- [ ] These tests will FAIL initially (target project has no implementation yet) — that is correct and expected
- [ ] **Target**: ≥ 40 acceptance tests for a standard microservice (justify if fewer)

**Checkpoint**: Update the boulder with characterization test count and behavioral coverage summary.

## How to Execute the Conversion

Follow the phases in `$WORKDIR/$PROJECT-PLAN.md` **in order**, starting from Phase 1 and completing Phase 1.5 characterization tests before entering Phase 2 (Port Behavior). Each phase has a gate — you MUST pass the gate before advancing.

### MANDATORY Rules

1. **TDD micro-commit cycle**: STOP → RED → GREEN → COMMIT → REFACTOR → COMMIT. Every commit is production-ready.
2. **One feature at a time**: Port ONE endpoint/service method at a time. Write its test first, then implement, then commit.
3. **Study the reference project BEFORE writing code**: `$WORKDIR/$EXAMPLE` shows how the target framework is used in production. Match its patterns for project structure, routing, DI, testing, and configuration.
4. **Quality gates are enforced during porting, not after**: Every class ≤ 300 lines. Every method 15–20 lines of code (excluding blank lines and braces; see language-specific standards). No `!!`. No structural `@Suppress`. Fix violations AS YOU WRITE, not in a later phase.
5. **Update the boulder after EVERY commit**: After every `git commit`, immediately update `$WORKDIR/$PROJECT-BOULDER.md`: (1) record the commit hash and what it contained in the History table, (2) update the NEXT ACTION to be extremely specific, (3) update metrics. This is your recovery checkpoint — if the session is interrupted, the boulder tells you exactly where to resume. The NEXT ACTION must be **extremely specific**: not "continue Phase 2.2" but "implement SegmentCrudService.create() with unit test, then wire to POST /segment route."
6. **Git discipline**: Commit after every GREEN and every REFACTOR step. Push after every commit if a remote exists (`git remote -v`). Verify your commit history periodically with `git log --oneline` — if you have written code but `git log` shows no new commits, STOP and commit immediately.
7. **Per-commit line limit (300 lines)**: No single commit may exceed 300 lines of insertions. Before committing, run `git diff --cached --stat` and check the insertions count. If it exceeds 300 lines, split the change into multiple commits before proceeding. Scaffolding commits created during Step 0 project/build setup for the target project (the plan's "set up target project / build scaffolding" phase) are exempt from this limit; all feature implementation commits in Phase 2+ must comply.
8. **Tests-with-code (no production-only commits)**: Every commit in Phase 2+ that adds or modifies production code MUST also add or update unit tests for that code in the same commit. Production-only commits (code without tests) are FORBIDDEN. This enforces TDD — you cannot defer tests to a later commit or batch.
9. **Bottom-up porting order**: Port business logic in this order: domain models → repository → one service + its unit tests → wire service to route(s) → verify acceptance test(s) pass → next service. Do NOT create route stubs for all endpoints at once — each route should be wired only when its backing service is implemented and tested.

### Multi-Session Awareness

This conversion will likely require **multiple sessions**. Your context window is finite (~200K tokens), and a typical conversion of a microservice with 15+ endpoints requires 800K–1.2M tokens of work. Plan accordingly.

#### Session Budget

Each session has a **hard commit cap** to ensure clean stopping before context exhaustion. These caps are NON-NEGOTIABLE — when you hit the cap, you MUST stop.

| Phase | Commit Cap Per Session | Expected Sessions |
|-------|------------------------|-------------------|
| Steps 0–1 + Phase 1.5 (scaffold, explore, characterization tests) | 4 total commits per session (shared across these steps) | 1 (single combined session) |
| Phase 2 (port behavior) | **5** | 4–6 |
| Phase 3–5 (quality, verification, reports) | 5 | 1–2 |

**Hard stop rule**: When you reach the commit cap for your current phase, execute the Context Exhaustion Protocol below and end the session. Do NOT start another feature. **Exception**: if the build is red when you hit the cap, one additional minimal build-fix commit is permitted — then you MUST stop immediately. This exception only relaxes the per-session commit cap: all other mandatory Phase 2+ commit rules (including tests-with-code coupling, the ≤300 insertions per non-test commit limit, and green-build requirements before refactors) still apply to the build-fix commit.

#### Boulder Update Frequency

- **Update the boulder AFTER EVERY COMMIT.** This is not optional. The commit sequence is: (1) `git add` + `git commit`, (2) update boulder History + NEXT ACTION + metrics. Only then continue to the next feature.
- **ALWAYS update the boulder before stopping** — whether you stop due to the commit cap, completion, context pressure, or a blocking error.
- **Count your commits.** After every commit, increment the boulder's "Session commit count" metric and check it against the Session Budget cap. If you've hit the cap, execute the Context Exhaustion Protocol immediately. **This counter tracks the CURRENT session only** — it must be reset to 0 at the start of every resume session (see Resuming a Conversion, step 2).

#### Context Exhaustion Protocol

**Trigger**: Execute this protocol when you hit the **session commit cap** (see Session Budget above). Do NOT rely on self-monitoring token usage — you cannot accurately estimate your remaining context. The commit cap is your mechanical limit.

1. **Do NOT start another feature.** An incomplete feature with uncommitted code is worse than stopping cleanly between commits.
2. **Verify all work is committed.** Run `git status` — if anything is modified or untracked, commit it now with an appropriate message.
3. **Update the boulder** with exact state: commit hash, what was just completed, current metrics, and a specific NEXT ACTION for the next session.
4. **Verify the build passes**: `./gradlew build` (or equivalent). If the build fails, make a minimal follow-up commit with only the fixes required to get back to green, rerun the build, and do not proceed until it passes. This build-fix commit is the one permitted exception to the session commit cap — stop immediately after it. Never stop a session on a red build.
5. **Stop.** The next session will resume from the boulder's NEXT ACTION.

Every session should end at a **passing commit boundary** — latest commit passes build/tests, all work committed, and the boulder updated.

#### Compaction Resilience

Your session may also be interrupted by context compaction (summarization of earlier context). To ensure recovery:

- **The boulder file IS your memory.** It has your exact position, metrics, and next action.
- **If you detect a compaction event** (loss of prior context), read the boulder file FIRST to recover your position, then continue from the NEXT ACTION.
- **The plan file IS your roadmap.** Check off items as you complete them. On recovery, find the first unchecked item.
- **Git history IS your safety net.** After recovery, run `git log --oneline` to verify your commits are intact and `git status` to check for uncommitted work.

### FORBIDDEN Actions (Anti-Patterns)

These are the most common failure modes. Violating ANY of these will produce a failed conversion.

| # | Forbidden Action | Why It Fails | What To Do Instead |
|---|-----------------|--------------|-------------------|
| 1 | **Bulk translation** — copying all source files and translating them to the target language/framework in one pass | Produces god classes, misses framework idioms, creates 1000+ line files, generates dozens of linter violations | Port ONE class/endpoint at a time using TDD. Write the test first, then the minimal implementation. |
| 2 | **Skipping the reference project study** — jumping straight to porting without studying `$EXAMPLE` | Target code won't follow framework conventions (wrong DI pattern, wrong routing style, wrong test structure) | Spend Step 1.2 studying `$EXAMPLE`. Document its patterns. Refer back to it throughout porting. |
| 3 | **Declaring "complete" when quality gates fail** — marking work done despite Detekt/PMD violations, low coverage, or failing tests | Leaves a broken deliverable that requires more work to fix than to redo | You are NOT done until Phase 4 gate passes: zero structural violations, ≥95% coverage, all characterization and acceptance tests pass. |
| 4 | **Writing production code without a failing test** — implementing before RED phase | Code won't be test-covered, you'll write more than needed, refactoring becomes risky | Always write the failing test FIRST. Only write enough production code to make it pass. |
| 5 | **Suppressing structural violations** — adding `@Suppress("LargeClass")` etc. instead of fixing the design | Masks fundamental design problems that compound as more code is added | Decompose the class/method. Use Strategy pattern, extract services, split responsibilities. |
| 6 | **Working outside `$WORKDIR/$TARGET`** — creating files in wrong directories or modifying the source project | Corrupts the source project or creates orphaned files | ALL new code goes in `$WORKDIR/$TARGET`. Source project is READ-ONLY. |
| 7 | **Monolithic commits** — bundling multiple features/endpoints into one commit, or exceeding 300 lines of insertions in a single commit | Impossible to bisect, review, or revert individual changes. Large commits also burn context faster. | One logical change per commit. Verify with `git diff --cached --stat` before committing — if insertions exceed 300, split the commit. |
| 8 | **Stopping to ask for permission** — asking "Want me to continue?" or "Should I proceed?" between phases | Breaks autonomous execution; the ulw-loop is designed for continuous work | Keep working through all phases. Only stop if genuinely blocked after 3 fix attempts. |
| 9 | **Analysis paralysis** — spending all context reading standards and exploring code without creating any artifacts | Session runs out of context or hits compaction before any real work is done | Create plan + boulder within Step 0 (first 9 actions). Explore within budget (Step 1, 15 calls). Then START BUILDING. |
| 10 | **Working without version control** — writing code without initializing git or making commits | No recovery from mistakes, no incremental history, session interruption loses everything, impossible to verify progress | Initialize git in Step 0 (actions 3-6). Commit after every GREEN/REFACTOR. Verify with `git log --oneline`. |
| 11 | **Deferring tests** — writing all production code first and adding tests afterward in a separate commit or batch | Violates TDD, produces untested code that may never get tests if context runs out, leaves abandoned work on termination | Every Phase 2+ commit that adds production code MUST include unit tests in the same commit. No exceptions. |
| 12 | **Using detekt/lint baseline as escape hatch** — creating a `detekt-baseline.xml` (or equivalent baseline file) to suppress existing violations instead of fixing them | Baselines hide design problems (god classes, long methods, high complexity) that compound as more code is added. A conversion is greenfield — there are no "legacy" violations to grandfather in. | Fix every violation as you go. If a rule triggers, decompose the class/method. Zero violations is the only acceptable state for a new project. |

## Completion Criteria

You are done ONLY when ALL of the following are true:

- [ ] Phase 4 gate in `$PROJECT-PLAN.md` passes (all checkboxes checked)
- [ ] `$PROJECT-BOULDER.md` shows all phases complete
- [ ] Zero structural `@Suppress` annotations
- [ ] Zero `!!` in production code
- [ ] Unit test coverage ≥ 95% (unit tests only; higher than the 80% repo minimum to ensure behavioral parity for conversions)
- [ ] All characterization/acceptance tests pass against the converted project
- [ ] *(if `$ACCEPTANCE_BASELINE` is set)* Acceptance tests from `$ACCEPTANCE_BASELINE` also pass against `$TARGET` — cross-project verification confirms behavioral parity with a prior successful conversion
- [ ] Detekt/static analysis: zero violations
- [ ] Build succeeds
- [ ] No commit exceeds 300 lines of insertions (verify: `git log --no-merges --format='%h' | while read h; do echo -n "$h "; git diff --stat "$h^" "$h" 2>/dev/null | tail -1; done` — for the initial commit, run `git show --stat <hash>` manually)
- [ ] Git history reflects incremental development (`git log --oneline` shows multiple small commits, not bulk changes)

If you cannot meet these criteria, document what is blocking you in the boulder file and ask the user — do NOT declare the conversion complete.
````

---

## Example (filled in)

With `$WORKDIR=/Users/eheikki3/workspace`, `$PROJECT=DM-SEGMENT-KTOR7`, `$SOURCE=dm-segment`, `$TARGET=dm-segment-ktor7`, `$EXAMPLE=company-creation-docket`, `$SOURCE_FRAMEWORK=Spring Boot 3.5.3 / Java 21`, `$TARGET_FRAMEWORK=Ktor 3.x / Kotlin 2.2`, `$TARGET_LANGUAGE_SKILL=kotlin-standards`, `$ACCEPTANCE_BASELINE=dm-segment-ktor`:

**CLI:**
```bash
opencode run \
  -m github-copilot/claude-opus-4.6 \
  --agent build \
  --dir /Users/eheikki3/workspace \
  --command ulw-loop \
  "Execute the conversion described in the attached file. Follow every instruction exactly." \
  -f /Users/eheikki3/workspace/conversion-prompt.md
```

**Prompt file** (`/Users/eheikki3/workspace/conversion-prompt.md`):
```markdown
# Framework Conversion: dm-segment → dm-segment-ktor7

## Your Mission

Convert `dm-segment` (Spring Boot 3.5.3 / Java 21) to `dm-segment-ktor7` (Ktor 3.x / Kotlin 2.2) following a strict phased plan with quality gates. You are creating NEW code inspired by the source — you are NOT copying/translating files wholesale.

## Paths

| Path | Purpose |
|------|---------|
| `/Users/eheikki3/workspace` | Working directory root |
| `/Users/eheikki3/workspace/ai-code-sentinel/` | Engineering standards (read these FIRST) |
| `/Users/eheikki3/workspace/ai-code-sentinel/docs/CONVERSION_PLAN_TEMPLATE.md` | Conversion plan template (copy and fill in) |
| `/Users/eheikki3/workspace/dm-segment` | Source project (READ ONLY — never modify) |
| `/Users/eheikki3/workspace/dm-segment-ktor7` | Target project (create this for a new conversion; reuse if resuming) |
| `/Users/eheikki3/workspace/company-creation-docket` | Reference project in target framework (study patterns, do not modify) |
| `/Users/eheikki3/workspace/DM-SEGMENT-KTOR7-PLAN.md` | Your filled-in conversion plan (create from template) |
| `/Users/eheikki3/workspace/DM-SEGMENT-KTOR7-BOULDER.md` | Progress tracker (create and update after every commit) |
| `/Users/eheikki3/workspace/dm-segment-ktor` | Prior successful conversion — run its acceptance tests against `dm-segment-ktor7` for cross-project verification (READ ONLY) |

(... rest of prompt follows the template above with variables replaced ...)
```

---

## Lessons from Prior Failures

This template was hardened based on analysis of failed conversion attempts:

| Failure | Root Cause | Template Fix |
|---------|-----------|--------------|
| Wrong agent selected (Hephaestus instead of Sisyphus) | `--agent` flag not specified | CLI invocation requires `--agent build` |
| Agent stalled after 3 messages | `--command ulw-loop` not used with correct agent | CLI template includes both `--agent build` and `--command ulw-loop` |
| 1,412-line god class produced | Bulk translation of source files | "FORBIDDEN Actions" table explicitly bans bulk translation |
| 72 Detekt violations at "completion" | Quality gates ignored during porting | Rules state gates enforced DURING porting, not after |
| Agent declared "complete" with failing gates | No explicit completion criteria in prompt | Completion checklist with "you are NOT done until..." |
| Files created outside target directory | Working directory not specified | `--dir` flag + Step 0 directory verification |
| Shell quoting issues with long prompts | Prompt passed as inline shell string | Prompt file with `-f` flag |
| `-f` flag consumed positional message as filename | `-f` (array type) placed before message argument | Message argument placed BEFORE `-f` in CLI invocation |
| Agent stopped to ask "Want me to continue?" | No autonomy directive in prompt | AUTONOMY section + FORBIDDEN Action #8 ban asking for permission |
| Agent spent entire context reading standards and exploring code without creating any artifacts | No exploration budget; plan/boulder creation deferred until after exploration | Step 0 forces scaffold creation within the FIRST 9 actions; Step 1 caps exploration at 15 tool calls; FORBIDDEN Action #9 |
| Session compaction caused agent to lose context and stall | No recovery mechanism; agent had no external checkpoint to resume from | Compaction Resilience directive: boulder includes NEXT ACTION; plan uses checkboxes; agent reads boulder on recovery |
| No behavioral safety net before porting | Agent ported code without characterization tests to verify behavioral parity | Step 2 (Characterization Tests) added: capture source behavior in executable tests BEFORE writing conversion code |
| Agent wrote 46 files with zero git commits | No git init step in scaffold; no commit enforcement during porting | Step 0 expanded to 9 actions (git init, .gitignore, initial commit, feature branch); MANDATORY Rule #6 (git discipline); FORBIDDEN #10 (working without version control) |
| Agent ran out of context window mid-conversion, leaving uncommitted work and stale boulder | No session-boundary awareness; template assumed single-session completion | Multi-Session Awareness section: session budgets, context exhaustion protocol, boulder update after EVERY commit |
| Resume session re-read all standards docs and re-explored source/reference projects, wasting ~50–80K tokens | No resume protocol; template only handled fresh starts | Resuming a Conversion section: skip Steps 0–1, read boulder for position, skip redundant standards reads |
| Agent read CODING_PRACTICES.md (35KB) and AI_AGENT_WORKFLOW.md (23KB) despite rules already being in agent config | No token-awareness guidance for Step 1 reads | Step 1.1 explicitly skips general practices docs; token-aware reading note added |
| Context exhaustion protocol never triggered despite running out of context (ktor10) | Soft rule ("trigger at ~90% budget") requires self-monitoring, which LLM agents cannot reliably do | Replaced soft context-monitoring with hard commit cap per session (5 in Phase 2). Agent counts commits, not tokens. |
| 790+ lines changed across 14 files in one monolithic commit (ktor10) | No per-commit size limit; agent bundled entire features together | MANDATORY Rule #7: 300-line insertion limit per commit. Agent must verify with `git diff --cached --stat` before committing. |
| All tests deferred to end — 905 lines of uncommitted test code left at death (ktor10) | Template allowed production-only commits; tests written as afterthought | MANDATORY Rule #8: Every Phase 2+ commit with production code MUST include unit tests in the same commit. FORBIDDEN Action #11. |
| Boulder went stale — last 2 commits never recorded, showed "Not started" for completed work (ktor10) | "Every 3–5 commits" boulder update frequency was too lax; agent skipped updates entirely | Boulder update required after EVERY commit, not every 3–5. Hard stop if boulder is stale. |

| Resume session stopped immediately — made zero commits and did no work (ktor11 Session 2) | Boulder's "Session commit count" from Session 1 was not reset; resume agent read "4" and thought it was at the cap | Resuming a Conversion step 2: ignore existing "Session commit count" and reset to 0 before reading any further fields. Boulder Update Frequency clarifies counter is per-session. |
| Resume session launched background agents then terminated before results arrived (ktor11 Sessions 2–3) | Agent fired 3 explore agents on resume, said "waiting for results," and the session exited before background tasks completed | "What NOT to do on resume" now bans background agents. NEXT ACTION in boulder must be prescriptive enough to start coding without exploration. |
| Resume agent ignored "no background agents" instruction despite it being in prompt, CLI, and boulder (ktor11 Sessions 2–4) | The `--command ulw-loop` injects system-level instructions ("launch multiple background agents IN PARALLEL", "DELEGATE — DO NOT WORK YOURSELF") that override user-level prompt instructions | Resume CLI uses `--command ralph-loop` instead of `ulw-loop`. ralph-loop provides continuation without aggressive delegation behavior. Fallback: remove `--command ralph-loop` entirely. |
| Agent created `detekt-baseline.xml` to bypass 72 violations instead of fixing them (ktor12) | No explicit prohibition on lint baselines; agent treated conversion as brownfield and grandfathered violations | FORBIDDEN Action #12 explicitly bans baseline files. Conversions are greenfield — zero violations is the only acceptable state. |

---

**Last Updated**: March 2026
**Version**: 2.7.0 (Add: Optional `$ACCEPTANCE_BASELINE` variable for cross-project acceptance test verification — run a prior successful conversion's tests against the new target. Add pre-populated plan guidance to Step 0 action 7 — verify orchestrator-provided plans instead of overwriting. Based on ktor12 analysis.)
