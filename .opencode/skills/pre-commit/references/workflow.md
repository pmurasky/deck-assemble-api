# Pre-Commit Canonical Contract

## Purpose
Validate that staged changes meet all required quality gates before committing. Ensures every commit is production-ready and follows engineering standards.

## Trigger Conditions
- User requests pre-commit check or validation
- Before any commit operation
- When staged changes exist and readiness validation is needed
- Phrases: "ready to commit", "pre-commit check", "can I commit", "validate changes"

## Hard Gates
**Non-negotiable blocking conditions:**

1. **Unit tests MUST pass** (when project test command exists)
2. **Build MUST succeed** (when project build command exists)  
3. **Lint MUST pass** (when project lint command exists)
4. **Static analysis MUST pass** (when PMD/detekt/Checkstyle configured)

**Blocking rules:**
- If any required gate fails → output `NOT READY` with blockers listed first
- If command unavailable → report `NOT CONFIGURED` explicitly
- Never recommend commit readiness when required gates fail
- Never create commits during validation

## Workflow Steps
1. **Review staging area**: Check `git diff --cached --stat`
2. **Validate quality gates**: Run available test/build/lint/static analysis commands
3. **Check coverage**: Verify minimum 80% unit test coverage for changed code
4. **Verify TDD compliance**: Ensure proper RED → GREEN → COMMIT cycle followed
5. **Output assessment**: Use required status vocabulary and ordering

## Status Vocabulary
**Required output format (preserve exact ordering):**
1. **Status**: `READY` or `NOT READY`
2. **Blocking items** (if any): List failures first, most critical to least critical
3. **Evidence**: Commands run and their summarized outcomes
4. **Next actions**: Specific recommendations for addressing blockers

**Status indicators:**
- `READY`: All quality gates pass, safe to commit
- `NOT READY`: One or more quality gates failed
- `NOT CONFIGURED`: Required tools/commands not available

## Fail/Fix/Rerun Loop
**When validation fails:**
1. **Stop immediately**: Do not proceed to commit
2. **Fix blockers**: Address each blocking item in order of priority
3. **Rerun validation**: Execute pre-commit check again after fixes
4. **Repeat**: Continue fail/fix/rerun until all gates pass

**Common fix actions:**
- Test failures → fix code or update tests
- Build failures → resolve compilation/syntax errors
- Lint failures → fix code style issues
- Static analysis failures → address PMD/detekt/Checkstyle violations

## Token Budget Intent
**Category**: On-demand workflow
**Estimated usage**: 800-1200 tokens per invocation
**Frequency**: Per commit (typically 5-20 times per development session)
**Optimization**: Keep adapter content minimal, reference canonical docs for detail

## Required References
- `docs/PRE_COMMIT_CHECKLIST.md` - Comprehensive checklist with TDD workflow
- `docs/AI_AGENT_WORKFLOW.md` - Micro-commit philosophy and TDD cycle
- `docs/STATIC_ANALYSIS_STANDARDS.md` - PMD/detekt/Checkstyle configuration
- `docs/CODING_PRACTICES.md` - Code quality standards and testing requirements

## Tool Adapter Requirements
**All adapters MUST preserve:**
- Hard gate blocking semantics
- Status vocabulary and output ordering
- References to canonical documentation
- Tool-specific metadata (e.g., `disable-model-invocation` for Claude)

**Adapters SHOULD:**
- Keep content minimal and reference this canonical contract
- Include tool-specific invocation patterns (e.g., agent routing for OpenCode)
- Maintain backward compatibility with existing consumer expectations
