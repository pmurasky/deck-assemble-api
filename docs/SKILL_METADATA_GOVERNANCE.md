# Skills 2.0 Metadata Governance

## Overview

This document defines lightweight metadata governance for skills and adapters in the Skills 2.0 system. The goal is to enable discoverability, versioning, and dependency tracking without introducing heavy runtime abstractions.

## Metadata Fields

### Required Fields (All Skills/Adapters)

All skills and adapters MUST include these fields in their frontmatter:

```yaml
---
name: skill-name
description: Clear description of what this skill does and when to use it.
version: 1.0.0
category: quality-gate
---
```

**Field Specifications:**

| Field | Required | Format | Description |
|-------|----------|--------|-------------|
| `name` | Yes | kebab-case | Unique identifier for the skill/adapter |
| `description` | Yes | Plain text | Clear description with trigger phrases |
| `version` | Yes | SemVer | Current version of the skill/adapter |
| `category` | Yes | Enum | Classification of the skill's purpose |

### Optional Fields

```yaml
---
name: skill-name
description: Clear description...
version: 1.0.0
category: quality-gate
dependencies:
  - pre-commit
  - test-coverage
budget:
  tokens: 800
  frequency: per-commit
---
```

**Field Specifications:**

| Field | Required | Format | Description |
|-------|----------|--------|-------------|
| `dependencies` | No | List of names | Other skills this skill depends on |
| `budget` | No | Object | Token budget guidance |
| `budget.tokens` | No | Integer | Estimated tokens per invocation |
| `budget.frequency` | No | String | How often the skill is invoked |

## Category Values

Skills and adapters MUST use one of these category values:

| Category | Description | Examples |
|----------|-------------|----------|
| `quality-gate` | Enforces code quality thresholds | pre-commit, code-quality, static-analysis-gate |
| `workflow` | Guides multi-step processes | micro-commit, tdd-enforcement |
| `review` | Performs code or commit review | commit-review, spec-compliance |
| `testing` | Testing-related utilities | test-coverage, refactoring-gate |
| `utility` | General-purpose utilities | (future) |

## Versioning Rules

- Use [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`
- **MAJOR**: Breaking changes to hard gates, status vocabulary, or workflow steps
- **MINOR**: New features, additional references, non-breaking enhancements
- **PATCH**: Bug fixes, documentation updates, clarifications

**Version Update Triggers:**
- Adding/removing hard gates → MAJOR bump
- Changing status vocabulary → MAJOR bump
- Adding new workflow steps → MINOR bump
- Updating references → MINOR bump
- Fixing typos or clarifying instructions → PATCH bump

## Dependencies

- List only direct dependencies (skills this one explicitly requires)
- Use the `name` field value (kebab-case) for dependency references
- Circular dependencies are not allowed
- Dependencies MUST exist in the same tool surface (Claude skills depend on Claude skills, OpenCode commands depend on OpenCode commands)

## Budget Hints

Budget hints are advisory and help with token planning:

```yaml
budget:
  tokens: 800        # Estimated tokens per invocation
  frequency: per-commit  # per-commit, per-session, on-demand
```

**Frequency Values:**
- `per-commit`: Invoked during every commit (e.g., pre-commit)
- `per-session`: Invoked once per development session (e.g., project setup)
- `on-demand`: Invoked only when explicitly requested (e.g., code-quality review)

## Validation

All metadata MUST be validated by enforcement tests:

1. **Required fields present**: name, description, version, category
2. **Version format**: Valid SemVer (x.y.z)
3. **Category valid**: Must be from approved enum values
4. **Dependencies valid**: All listed dependencies must exist
5. **Budget format**: If present, must have tokens (integer) and frequency (string)

## Migration from Skills 1.0

To add metadata to an existing skill:

1. Add `version` field (start at `1.0.0` if this is the first versioned release)
2. Add `category` field based on the skill's purpose
3. Optionally add `dependencies` and `budget` fields
4. Update enforcement tests to validate new metadata
5. Commit with `feat(skill): add metadata governance to [skill-name]`

## Examples

### Minimal Required Metadata

```yaml
---
name: pre-commit
description: Run pre-commit readiness checks. Use when user asks "ready to commit" or "pre-commit check".
version: 1.0.0
category: quality-gate
---
```

### Full Metadata with Optional Fields

```yaml
---
name: tdd-enforcement
description: Enforce strict test-first sequencing with hard gates. Use when user wants TDD enforced.
version: 2.1.0
category: workflow
dependencies:
  - pre-commit
  - test-coverage
budget:
  tokens: 1200
  frequency: per-commit
---
```

## Governance Rules

1. **All new skills MUST include full required metadata** before merging
2. **Version bumps MUST be documented** in the commit message
3. **Category changes are breaking changes** and require MAJOR version bump
4. **Dependencies MUST be validated** in CI before merge
5. **Budget hints are advisory** but should be kept reasonably accurate

## Enforcement

Validation is enforced by `tests/enforcement_integration/`:

- `validate_skill_metadata(skill_path)` - Checks required fields and formats
- `validate_metadata_dependencies(skill_path, all_skills)` - Verifies dependencies exist
- `validate_metadata_budget(skill_path)` - Validates budget format if present

Run validation:
```bash
python3 -m unittest discover -s tests/enforcement_integration -p "test_*.py"
```
