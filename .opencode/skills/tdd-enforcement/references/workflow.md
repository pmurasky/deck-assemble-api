# TDD-Enforcement Canonical Contract

## Purpose
Enforce strict test-first TDD sequencing with hard gates for RED → GREEN → REFACTOR cycles. Ensures proper test-driven development with observable evidence.

## Trigger Conditions
- User requests TDD workflow enforcement
- Before implementing new functionality with TDD
- Phrases: "TDD enforcement", "test-first", "RED GREEN REFACTOR", "enforce TDD cycle"

## Hard Gates
**Non-negotiable blocking conditions:**

**Mandatory TDD Sequencing (no reordering):**
1. **RED**: Write exactly one failing test
2. **VERIFY RED**: Run the test and capture proof it failed for the expected reason
3. **GREEN**: Write the minimum production code to pass that one test  
4. **VERIFY GREEN**: Run unit tests and confirm pass
5. **REFACTOR**: Improve structure without changing behavior
6. **VERIFY REFACTOR**: Run unit tests again and confirm pass

**Blocking Rules:**
- Never allow production code changes before a failing test is written and observed
- If no failing test evidence is shown → output BLOCKED and stop
- If more than one new failing test appears in a cycle → output BLOCKED and split work
- If implementation includes behavior not required by the current failing test → output BLOCKED and remove extra logic
- If RED failure reason is unrelated (wrong file, compile error, environment issue) → output BLOCKED and fix the test first

## Workflow Steps

### Must-Watch-It-Fail Verification
**For every RED phase:**
1. **Identify test command**: Use exact test command for RED (single test preferred)
2. **Capture failure evidence**: Record the failing assertion/error line
3. **Explain failure significance**: Why the failure proves missing behavior
4. **Proceed to GREEN**: Only after steps 1-3 are complete with observable evidence

### One-Test-At-A-Time Protocol
**Repeat this micro-cycle:**
1. Add one failing test
2. Make it pass with minimal code
3. Refactor safely  
4. Repeat with next behavior

### Rationalization Defense
**Reject these anti-TDD patterns:**
- "I skipped running it" → **Rejected**: Must show failure evidence
- "I implemented first" → **Rejected**: Test-first is mandatory  
- "I added two tests together" → **Rejected**: One failing test per cycle
- "Test failed due to setup" → **Rejected**: Must verify intended behavior gap
- "I fixed unrelated code" → **Rejected**: Keep scope to current cycle

## Status Vocabulary
**Required output format:**
1. **Status**: `READY FOR GREEN`, `READY FOR REFACTOR`, `READY FOR NEXT RED`, or `BLOCKED`
2. **Evidence**: 
   - RED command and key failure line
   - GREEN command and pass summary
   - REFACTOR verification command and pass summary
3. **Current cycle scope**: Single behavior being implemented
4. **Next smallest step**: Specific next action required

**Status indicators:**
- `READY FOR GREEN`: Failing test verified, ready for minimal implementation
- `READY FOR REFACTOR`: Test passes, ready for structure improvements
- `READY FOR NEXT RED`: Cycle complete, ready for next failing test
- `BLOCKED`: TDD violation detected, corrective action required

## Fail/Fix/Rerun Loop
**When TDD violations detected:**
1. **Stop immediately**: Do not proceed until violation is corrected
2. **Identify violation**: Reference rationalization defense patterns
3. **Correct approach**: Follow proper TDD sequence
4. **Resume workflow**: Return to appropriate TDD phase
5. **Verify compliance**: Ensure evidence requirements met

**Common corrections:**
- Missing RED evidence → Run test and capture failure
- Multiple failing tests → Remove extra tests, focus on one
- Premature implementation → Revert to minimal code for current test
- Unrelated changes → Remove scope creep, focus on current cycle

## Token Budget Intent
**Category**: On-demand workflow
**Estimated usage**: 900-1200 tokens per invocation
**Frequency**: During feature development (typically 10-30 cycles per feature)
**Optimization**: Keep adapter content minimal, reference canonical docs for detail

## Required References
- `docs/AI_AGENT_WORKFLOW.md` - Micro-commit workflow and TDD cycle details
- `docs/PRE_COMMIT_CHECKLIST.md` - TDD compliance checklist
- `docs/CODING_PRACTICES.md` - Testing standards and TDD principles

## Tool Adapter Requirements
**All adapters MUST preserve:**
- Hard gate blocking semantics for TDD violations
- Mandatory sequencing enforcement (RED → GREEN → REFACTOR)
- Status vocabulary and evidence requirements
- Rationalization defense patterns
- References to canonical documentation

**Adapters SHOULD:**
- Keep content minimal and reference this canonical contract
- Include tool-specific metadata and invocation patterns
- Maintain backward compatibility with existing consumer expectations
- Do not recommend commit readiness (separate workflow)
