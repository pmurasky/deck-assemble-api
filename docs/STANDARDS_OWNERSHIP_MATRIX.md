# Standards Ownership Matrix

## Purpose

This document defines the **single canonical source** for each core rule family and
the allowed downstream mirrors. Use it to prevent drift across docs, skills, agents,
and tool-specific wrappers.

## Ownership Rules

1. Every rule family has exactly one canonical owner.
2. Mirrors may summarize, but must not redefine canonical thresholds or workflow steps.
3. If canonical and mirror disagree, canonical wins.
4. Drift fixes must update canonical first, then mirrors in the same PR.

## Canonical Ownership

| Rule Family | Canonical Source | Allowed Mirrors |
|---|---|---|
| TDD micro-commit workflow | `docs/AI_AGENT_WORKFLOW.md` | `skills/micro-commit-workflow/SKILL.md`, tool wrapper rules |
| Pre-commit quality gate | `docs/PRE_COMMIT_CHECKLIST.md` | `skills/pre-commit-checklist/SKILL.md`, `.opencode/commands/pre-commit.md`, `.opencode/agents/pre-commit-check.md` |
| Completion evidence / verification | `docs/VERIFICATION_STANDARDS.md` | `AGENTS.md`, `.opencode/agents/build.md`, workflow/checklist docs, `verification-before-completion` skill (environment-provided via [superpowers](https://github.com/obra/superpowers)) |
| Cross-language testing standards | `docs/TESTING_STANDARDS.md` | `skills/testing-standards/SKILL.md`, tool wrapper rules |
| SOLID deep-dive reference | `docs/SOLID_PRINCIPLES.md` | `skills/solid-principles/SKILL.md`, checklist docs |
| Language-agnostic coding practices | `docs/CODING_PRACTICES.md` | `skills/coding-practices/SKILL.md`, tool wrapper rules |
| Static analysis policy | `docs/STATIC_ANALYSIS_STANDARDS.md` | `skills/static-analysis/SKILL.md`, `skills/java-static-analysis/SKILL.md`, `.claude/rules/java-static-analysis.md`, `.cursor/rules/java-static-analysis.md`, `.github/instructions/java-static-analysis.instructions.md` |
| Security policy | `docs/SECURITY_STANDARDS.md` | `skills/security-standards/SKILL.md`, tool wrapper rules |
| Logging policy | `docs/LOGGING_STANDARDS.md` | `skills/logging-standards/SKILL.md`, tool wrapper rules |
| Skill authoring quality policy | `docs/SKILL_AUTHORING_STANDARDS.md` | `CONTRIBUTING.md`, `writing-skills` skill (environment-provided via [superpowers](https://github.com/obra/superpowers)) |
| Install contract | `config/install-manifest.json` + `.intellij/INSTALL.md § Install Policy` | `README.md`, `.github/INSTALL.md`, `.github/copilot-instructions.md`, `AGENTS.md` |

## Drift-Check Checklist (run before merge)

- [ ] Updated canonical source for each changed rule family.
- [ ] Updated all mirrors for that family in same PR.
- [ ] Verified no conflicting numbers/thresholds between canonical and mirrors.
- [ ] Verified README counts for OpenCode assets (agents/commands/skills).
- [ ] Verified compatibility matrix still matches actual behavior.
- [ ] Ran `python scripts/check_docs_adapter_parity.py` and confirmed zero errors.

## Automated Parity Contract

The machine-readable parity manifest at `config/parity-manifest.json` encodes:

- Expected adapter rule files per tool (Claude, Cursor, Copilot)
- Required `docs/` references in each root file (AGENTS.md, CLAUDE.md, copilot-instructions.md)
- Orphan doc exceptions (internal docs intentionally unreferenced)

The CI workflow `.github/workflows/docs-adapter-parity.yml` runs
`scripts/check_docs_adapter_parity.py` on every push/PR that touches docs,
adapters, or root files. Errors block merge; orphan warnings are advisory.

The machine-readable install contract at `config/install-manifest.json` encodes
the base payload, language packs, managed files, and validation rules locked
by `.planning/phases/01-define-install-contract/01-install-contract.md`. The CI
workflow `.github/workflows/docs-adapter-parity.yml` runs
`scripts/check_install_contract.py` on every push/PR that touches install
surfaces, the manifest, the validator, or its tests. Errors block merge —
there is no warn-only mode for the install contract.

## Suggested Commands for Fast Parity Checks

```bash
# Count OpenCode assets
ls .opencode/agents/*.md | wc -l
ls .opencode/commands/*.md | wc -l
ls skills/*/SKILL.md | wc -l

# Spot old hard-coded counts in docs
grep -R "22 skills\|8 commands\|3 agents" README.md CONTRIBUTING.md docs
```
