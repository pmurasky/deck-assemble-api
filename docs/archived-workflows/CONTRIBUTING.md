# Canonical Contract Contributor Guide (Legacy Archive)

This guide exists so contributors can discover and validate historical canonical contracts in `docs/archived-workflows/`.

> Active behavior remains in skill-local `SKILL.md` + references. These contracts are maintained for parity checks and historical traceability.

## When to Create Contracts

- Prefer creating or updating live behavior in `.claude/skills/` and `.opencode/commands/`.
- Add or update a contract in `docs/archived-workflows/` only when preserving legacy parity requirements or documenting migration-era behavior.
- If a new contract is added, add it to `docs/archived-workflows/contracts.registry.json` in the same change.

## Adapter Update Process

1. Update the relevant contract markdown in `docs/archived-workflows/*.md`.
2. Update each mapped adapter listed in `contracts.registry.json`.
3. Run enforcement tests so contract structure and registry coverage stay green.
4. Use `scripts/report_contract_adapter_impact.py` to inspect potentially impacted adapters on PRs.

## Versioning Strategy

- Contract IDs are stable (`pre-commit`, `micro-commit`, `tdd-enforcement`).
- Prefer additive documentation changes for archival contracts.
- For incompatible historical reinterpretations, create a new contract ID rather than mutating old intent.

## Discovery Mechanism

- Registry: `docs/archived-workflows/contracts.registry.json`
- Schema: `schemas/canonical-workflow-contract.schema.json`
- Contract files: `docs/archived-workflows/*.md` (excluding README/template)

Contributors should treat the registry as the discoverability index for legacy canonical contracts.
