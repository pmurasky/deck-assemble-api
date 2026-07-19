# Skill Authoring Standards

> **Canonical source** for skill authoring quality policy.
> Mirrors: `CONTRIBUTING.md` (thin summary), `writing-skills` environment skill (TDD workflow).
> Ownership: see [STANDARDS_OWNERSHIP_MATRIX.md](./STANDARDS_OWNERSHIP_MATRIX.md).

---

## 1. Metadata Contract

Every skill MUST include YAML frontmatter. Required fields:

```yaml
---
name: skill-name              # kebab-case, matches directory name, ≤64 chars
description: >
  Use when [specific situation]. Covers [key concepts]. Third-person,
  trigger-focused, ≤1024 chars. Start with "Use when" or noun phrase
  describing what the skill does — not a workflow summary.
---
```

**Naming rules:**
- kebab-case only — no underscores, no spaces
- ≤64 characters
- No reserved words (`default`, `base`, `core`, `util`)
- Matches the directory name exactly

**Description rules:**
- ≤1024 characters
- Third-person voice ("Covers X, enforces Y" — not "Use this to...")
- MUST include concrete trigger keywords: error messages, tool names, symptoms, task types
- MUST NOT summarize the workflow or list headings — describe what firing the skill provides

---

## 2. Trigger Optimization

Every skill MUST contain a "Use when / Not for" section **within the first 30 lines** of the file body (after frontmatter).

**Required format:**

```markdown
## Use when
- [Concrete situation A — include symptom, error, or task type]
- [Concrete situation B]
- [Concrete situation C]

## Not for
- [Situation X] — use [other-skill] instead
- [Situation Y] — not covered here
```

**Rules:**
- Include 2–4 positive triggers (should-fire cases)
- Include 1–2 negative triggers (must-not-fire cases) — prevents misfires
- Use concrete language: "When you see `ImportError`", "Before committing", "When refactoring a class with no tests"
- Avoid vague triggers: "When coding", "For best practices", "When needed"

---

## 3. Progressive Disclosure

**SKILL.md line limits:**

| Lines | Severity | Action |
|---|---|---|
| ≤400 | ✅ OK | No action needed |
| 401–500 | ⚠️ Warning | Consider extracting reference material |
| >500 | ❌ Error | MUST extract content to `references/` |

If a skill is approaching the limit:

1. Identify reference-heavy sections (long tables, exhaustive rule lists, tool configs)
2. Extract them to `references/<topic>.md` in the same skill directory
3. In SKILL.md, replace the extracted content with a one-line link: `See references/topic.md` (plain path, not a markdown link — reference files live in the skill directory)

**Reference file rules:**
- One level deep only — `references/*.md`, no nested subdirectories (enforced by linter)
- Reference files >100 lines MUST include a table of contents at the top
- Prefer domain-specific splitting: `references/finance.md`, `references/error-types.md`
- Reference files are exempt from the line limit but MUST be justified (not just padding)

**Table of contents rules:**

| Lines | Severity | Action |
|---|---|---|
| ≤250 | No TOC needed | — |
| 251–350 | ⚠️ Warning | Add a table of contents |
| >350 | ❌ Error | MUST include a table of contents |

A TOC is detected as: a heading containing "Table of Contents" or "TOC", or a markdown list with ≥3 internal anchor links.

**Do not split** sections that define core behavior — only reference material (examples, tool configs, exhaustive tables).

---

## 4. Structural Requirements

Every skill MUST contain all of the following:

| Section | Requirement |
|---|---|
| Trigger section | "Use when / Not for" within first 30 lines |
| Core content | Specific, actionable rules — not topic labels |
| Concrete example | At least one code block or step-by-step workflow |
| Anti-patterns | "Common Mistakes" or "Never do" section |

Every skill SHOULD contain:

| Section | Guidance |
|---|---|
| Verification step | How to confirm the skill was applied correctly |
| Checklist | For multi-step workflows — enables progress tracking |
| Failure recovery | What to do when the workflow breaks down |

---

## 5. Token Budget

| Artifact | Limit | Severity | Check |
|---|---|---|---|
| SKILL.md body | >600 words | ⚠️ Warning | `wc -w SKILL.md` |
| SKILL.md lines | >500 lines | ❌ Error | `wc -l SKILL.md` |
| Single reference file | No word limit, but justify its existence | — | — |
| Total skill footprint | ≤2000 words (SKILL.md + all references) | Guideline | `wc -w SKILL.md references/*.md` |

The 600-word threshold is a **soft warning**, not a hard gate. Skills exceeding 600 words should be reviewed for extraction opportunities but are not blocked from merging. The 500-line hard cap is enforced by the linter.

**Check before every commit:** `wc -lw skills/<skill-name>/SKILL.md`

---

## 6. CSO Rule (Complete, Self-Contained, Opinionated)

Every skill MUST satisfy all three properties:

| Property | Meaning | Violation |
|---|---|---|
| **Complete** | All rules are inline — no "see `docs/FOO.md`" redirects | Skill tells agent to read another file |
| **Self-contained** | No `read docs/...` instructions inside the skill | Skill depends on external context to be actionable |
| **Opinionated** | Specific, actionable rules — not a list of topics | Bullets are nouns ("error handling") instead of imperatives ("never swallow exceptions") |

**Token cascade warning:** Skills that reference `docs/` files cause agents to load those files eagerly, consuming context budget before any work begins. Keep all rule content inline.

---

## 7. Consistent Terminology

Within a single skill, pick **one** term per concept and use it everywhere:

- Pick "method" or "function" — not both
- Pick "handler" or "controller" — not both
- If the choice is ambiguous, add a comment at the top: `<!-- terminology: "handler" used throughout -->`

---

## 8. Testing Requirement

Every new or edited skill MUST have at least one documented test scenario in `tests/skills/scenarios/<skill-name>/basic.yaml`.

**Required scenario format:**

```yaml
# tests/skills/scenarios/<skill-name>/basic.yaml
skill_name: <skill-name>
trigger: "<example user phrase that should invoke this skill>"
expected_behavior: "<what the skill should produce>"
not_for: "<example phrase that should NOT invoke this skill>"
```

**Example:**

```yaml
skill_name: pre-commit
trigger: "ready to commit?"
expected_behavior: "Run pre-commit readiness checks and report blockers using strict quality gates for tests, build, lint, and static analysis."
not_for: "Early design discussions with no staged changes"
```

**CI enforcement:**
The CI workflow (`.github/workflows/ci.yml`) validates all scenario test YAML files on every push and pull request. PRs with missing or invalid scenario tests will fail CI.

Run scenario validation locally:
```bash
python3 -c "import yaml; yaml.safe_load(open('tests/skills/scenarios/<skill-name>/basic.yaml'))"
```

---

## 9. PR Checklist

Before submitting a skill PR:

- [ ] SKILL.md is ≤500 lines (hard cap; 401-500 triggers warning)
- [ ] `wc -w SKILL.md` result reviewed (>600 words triggers warning, not a blocker)
- [ ] "Use when / Not for" section exists within first 30 lines
- [ ] No `read docs/...` instructions anywhere in the file (CSO rule)
- [ ] CSO test passes: agent can act from this file alone without loading other docs
- [ ] No exhaustive examples (≤1 per concept)
- [ ] No background/history prose
- [ ] Every bullet is an imperative ("do X" / "never Y"), not a topic label
- [ ] `description` frontmatter is trigger-focused, third-person, includes symptom/task keywords
- [ ] No forbidden frontmatter fields (`argument-hint`, `disable-model-invocation`, `user-invocable`)
- [ ] At least one test scenario exists at `tests/skills/scenarios/<skill-name>/basic.yaml`
- [ ] Scenario test follows the required format (skill_name, trigger, expected_behavior, not_for)
- [ ] References are one level deep only (`references/*.md`, no nested subdirectories)
- [ ] Files >250 lines have a table of contents (>350 lines = required)
- [ ] `npm test` passes (or `python3 -m pytest tests/enforcement_integration/ -v`)
- [ ] CI passes on the PR

---

## Quick Reference: Violation Signals

| Signal | Problem | Fix |
|---|---|---|
| "See docs/FOO.md for details" | CSO violation — redirect, not skill | Extract the relevant content inline |
| Bullets are nouns ("error handling") | Not opinionated | Rewrite as imperatives ("never swallow exceptions") |
| No "Use when" section | Missing trigger | Add within first 30 lines |
| SKILL.md > 500 lines | Over line limit (error) | Extract reference content to `references/` |
| SKILL.md 401–500 lines | Approaching line limit (warning) | Review for extraction opportunities |
| `wc -w` > 600 | Over word budget (warning) | Trim or extract to references |
| >250 lines, no TOC | Missing table of contents (warning) | Add TOC heading with anchor links |
| >350 lines, no TOC | Missing table of contents (error) | Add TOC heading with anchor links |
| `references/sub/file.md` | Nested reference depth (error) | Flatten to `references/file.md` |
| Forbidden frontmatter fields present | Agent Skills spec violation | Remove `argument-hint`, `disable-model-invocation`, `user-invocable` |
