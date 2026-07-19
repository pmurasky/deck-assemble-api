# Claude Memory Strategy

## Purpose

Keep Claude context small, stable, and reusable by separating:
- Always-on instructions
- On-demand operational workflows
- Auto-captured observations

This document defines what belongs in each Claude memory surface.

## Memory Hierarchy

1. `CLAUDE.md` (root)
   - Use for short, high-priority project policy.
   - Keep concise and pointer-first.
   - Avoid long duplicated standards text.

2. `.claude/rules/*.md`
   - Use for modular constraints that should auto-apply.
   - Keep as checklist-style rules with canonical doc references.
   - Prefer path-scoped rules for language/framework specifics.

3. `.claude/skills/*/SKILL.md`
   - Use for task workflows and slash-command style procedures.
   - Keep `SKILL.md` focused and move details to supporting files when needed.
   - Mark side-effecting workflows with `disable-model-invocation: true`.

4. `.claude/agents/*.md`
   - Use for specialized execution contexts (build, review, pre-commit checks).
   - Keep prompts focused on role boundaries and expected outputs.

5. Auto memory
   - Use for learned observations and repo-specific discoveries.
   - Do not store normative policy here; store policy in docs/rules instead.

## Token Hygiene Rules

- Prefer one canonical source per topic.
- Replace repeated prose with short pointers.
- Keep always-loaded files short; move long examples to docs.
- Avoid copying large checklists into multiple rule files.
- Use skills/subagents for heavy workflows instead of inflating always-on prompts.

## Practical Authoring Guidelines

- `CLAUDE.md`
  - Keep under ~150 lines where possible.
  - Include only global behavior and references.

- `.claude/rules/`
  - One topic per file.
  - Prefer quick checks over detailed tutorials.

- `.claude/skills/`
  - Keep `SKILL.md` under ~500 lines.
  - Use `argument-hint` for predictable invocation.
  - Use `context: fork` only when isolation is needed.

- `.claude/agents/`
  - Restrict tool usage based on responsibility.
  - Use explicit output format expectations.

## Recommended Review Cadence

- Monthly: scan `.claude/rules/` and `CLAUDE.md` for duplication.
- Quarterly: prune stale skills and agent prompts.
- After major process changes: update canonical docs first, then pointers.

## References

- Anthropic Skills: https://docs.anthropic.com/en/docs/claude-code/skills
- Anthropic Subagents: https://docs.anthropic.com/en/docs/claude-code/sub-agents
- Anthropic Hooks: https://docs.anthropic.com/en/docs/claude-code/hooks
- Anthropic Memory: https://docs.anthropic.com/en/docs/claude-code/memory
