# Legacy Workflow Archive

This directory is a historical archive from the pre-migration hybrid architecture.

## Status

- `docs/archived-workflows/` is **not** the active source of truth for skill behavior.
- Current skill behavior is defined by each skill's own `SKILL.md` and local `references/` files.
- These archived workflow contracts are retained for historical context and learning.

## Why It Exists

Earlier revisions used canonical contracts in this folder plus tool-specific adapters. The repository now targets Agent Skills-compliant, self-contained skills, so canonical-contract-driven behavior is deprecated.

## Archived Files

- `pre-commit.md`
- `micro-commit.md`
- `tdd-enforcement.md`

## Contributor Guidance

- Do not add new active workflows here.
- If you need to update live behavior, update the corresponding skill under `.claude/skills/` or `.opencode/skills/`.
- If you annotate historical docs here, keep notes clearly marked as archival.
- For legacy canonical-contract maintenance, use:
  - `docs/archived-workflows/CONTRIBUTING.md` (process)
  - `docs/archived-workflows/contracts.registry.json` (discovery index)
  - `schemas/canonical-workflow-contract.schema.json` (validation schema)
