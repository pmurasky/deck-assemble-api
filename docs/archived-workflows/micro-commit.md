# Micro-Commit Canonical Contract

## Purpose
Guide creation of production-ready commits that contain exactly one logical change. Enforces atomic commits, proper message format, and quality gates for maintainable git history.

## Trigger Conditions
- User requests help with committing changes
- Before any commit operation
- When analyzing current changes for commit readiness
- Phrases: "commit my changes", "micro-commit", "help me commit", "ready to commit", "create commit"

## Hard Gates
**Non-negotiable blocking conditions:**

### Single Logical Change Rule
- **One logical change per commit** (never bundle multiple changes)
- Each commit must represent exactly one of: refactor step, feature implementation, test update, or documentation update
- If multiple logical changes detected → output `SPLIT REQUIRED` with specific breakdown

### Production-Ready Requirements
1. **All unit tests MUST pass** (when project test command exists)
2. **Build MUST succeed** (when project build command exists)
3. **No lint errors** (when project lint command exists)
4. **Conventional Commits format** required for all commit messages

**Blocking rules:**
- If any quality gate fails → output `NOT READY` and list blockers
- If multiple logical changes detected → output `SPLIT REQUIRED` with commit breakdown
- If commit message doesn't follow Conventional Commits → output `MESSAGE INVALID`
- Never create commits during analysis phase
- Never bypass failing tests or build

## Workflow Steps

### 1. Change Analysis
- **Review staged changes**: `git diff --cached --stat`
- **Review unstaged changes**: `git diff --stat`
- **Identify logical boundaries**: Determine if changes represent one or multiple logical changes
- **Assess scope**: Ensure changes are atomic and coherent

### 2. Change Classification
**Determine change type and split if needed:**
- **Single logical change**: Proceed to commit preparation
- **Multiple logical changes**: Output specific breakdown for separate commits
- **Mixed scope**: Recommend staging strategy for proper separation

### 3. Commit Ordering (when multiple commits needed)
**Required precedence order:**
1. **Refactor commits** (structure improvements without behavior change)
2. **Feature commits** (new functionality with tests)
3. **Test commits** (test-only additions/updates)
4. **Documentation commits** (docs-only changes)

### 4. Message Crafting
- **Use Conventional Commits**: `<type>(<scope>): <description>`
- **Types**: feat, fix, refactor, test, docs, perf, chore
- **Scope**: Optional but recommended (component/module name)
- **Description**: Clear, imperative mood, explain WHAT changed
- **Body**: Optional, explain WHY (not what)

### 5. Quality Gate Verification
- **Run unit tests**: Execute project test command
- **Run build**: Execute project build command
- **Run lint**: Execute project lint command
- **Verify all pass**: Ensure no failures before committing

## Status Vocabulary
**Required output format:**
1. **Status**: `READY`, `NOT READY`, `SPLIT REQUIRED`, or `MESSAGE INVALID`
2. **Change summary**: What logical changes were detected
3. **Commit plan**: Specific commits to create (if split needed)
4. **Quality gates**: Results of test/build/lint execution
5. **Next actions**: Specific steps to proceed

**Status indicators:**
- `READY`: Single logical change, all quality gates pass, proper message format
- `NOT READY`: Quality gates failed, must fix before committing
- `SPLIT REQUIRED`: Multiple logical changes detected, must separate into individual commits
- `MESSAGE INVALID`: Commit message doesn't follow Conventional Commits format

## Fail/Fix/Rerun Loop

### When Quality Gates Fail
1. **Stop immediately**: Do not proceed to commit
2. **Fix blockers**: Address test failures, build errors, or lint issues
3. **Rerun verification**: Execute quality gates again after fixes
4. **Repeat**: Continue until all gates pass

### When Split Required
1. **Stage selectively**: Use `git add -p` or file-by-file staging
2. **Create first commit**: Commit one logical change
3. **Stage next change**: Prepare next logical change for commit
4. **Repeat**: Continue until all changes are committed separately

### Common Fix Actions
- **Test failures**: Fix code or update tests to reflect intended behavior
- **Build failures**: Resolve compilation/syntax errors
- **Lint failures**: Fix code style and formatting issues
- **Multiple changes**: Use selective staging to separate logical changes
- **Message format**: Rewrite to follow Conventional Commits specification

## Token Budget Intent
**Category**: On-demand workflow
**Estimated usage**: 600-1000 tokens per invocation
**Frequency**: Per commit (typically 5-20 times per development session)
**Optimization**: Keep adapter content minimal, reference canonical docs for detail

## Required References
- `docs/AI_AGENT_WORKFLOW.md` - Comprehensive micro-commit philosophy and examples
- `docs/PRE_COMMIT_CHECKLIST.md` - Quality gates checklist
- `docs/CODING_PRACTICES.md` - Production-ready commit requirements
- `docs/CODING_STANDARDS.md` - Conventional Commits format specification

## Tool Adapter Requirements
**All adapters MUST preserve:**
- Single logical change enforcement
- Production-ready quality gates
- Conventional Commits message format
- Status vocabulary and output ordering
- References to canonical documentation

**Adapters SHOULD:**
- Keep content minimal and reference this canonical contract
- Include tool-specific staging and commit commands
- Provide specific commit message drafts
- Include quality gate execution commands for the project
- Maintain backward compatibility with existing consumer expectations

**Adapters MUST NOT:**
- Automatically create commits without explicit user confirmation
- Skip quality gate verification
- Allow multiple logical changes in single commit
- Bypass production-ready requirements