# ADR-0001: Superpowers Upstream Sync Model

## Status

Proposed

## Date

2026-03-14

## Context

Epic #31 requires stronger workflow enforcement while preserving this repository's role as a local standards product across Claude, OpenCode, Cursor, and Copilot. We want to adopt useful Superpowers patterns and keep receiving upstream improvements without coupling local consumers to upstream path changes or losing local policy control.

Current architecture already has stable local seams:

- Canonical contracts: `docs/workflows/*.md`
- Thin adapters: `.claude/skills/*/SKILL.md`, `.opencode/commands/*.md`
- Enforcement tests: `tests/enforcement_integration/*`
- Token governance: `scripts/report-token-usage.py`

The missing piece is an explicit, testable upstream sync model with provenance and divergence governance.

## Decision

Adopt a **pull-only upstream mirror with local canonical composition**.

1. Mirror selected upstream Superpowers files into `upstream/superpowers/` as read-only snapshots.
2. Track source provenance and mapping in `upstream/superpowers.lock.json`.
3. Validate lockfile structure with `schemas/upstream-superpowers-lock.schema.json`.
4. Keep local canonical contracts in `docs/workflows/` as the stable API for all adapters.
5. Require each workflow mapping to declare divergence mode: `adopt`, `extend`, `override`, or `local-only`.

This keeps upstream ingestion explicit while preserving local ownership of policy and consumer-facing paths.

## Alternatives Considered

### Alternative 1: Copy-and-freeze
- **Pros**: Lowest setup overhead, full local editing freedom
- **Cons**: No reliable provenance, high drift risk, expensive manual updates
- **Why rejected**: Does not support repeatable upstream refresh with auditable change tracking

### Alternative 2: Direct submodule/subtree as primary architecture
- **Pros**: Strong upstream provenance, straightforward source updates
- **Cons**: Couples local customization to vendor layout, noisy sync diffs, higher contributor friction
- **Why rejected**: Poor fit for local policy evolution and stable local adapter contracts

### Alternative 3: Directly reference upstream paths from adapters
- **Pros**: Minimal local duplication
- **Cons**: Fragile runtime coupling, no compatibility guarantees, token and availability risk
- **Why rejected**: Breaks local stability guarantees and weakens deterministic enforcement

## Consequences

### Positive
- Preserves stable local paths while enabling controlled upstream refresh
- Makes drift visible via explicit divergence metadata
- Supports CI gating on provenance and parity, not only phrase checks

### Negative
- Adds governance overhead (lock maintenance, sync reviews)
- Requires disciplined separation between mirrored snapshots and local canonicals

### Neutral
- Existing adapter surfaces remain unchanged
- Existing parity and token tests continue to apply, with additional sync checks layered in

## References

- Epic #31: `https://github.com/pmurasky/engineering-standards/issues/31`
- Epic #41: `https://github.com/pmurasky/engineering-standards/issues/41`
- `docs/workflows/README.md`
- `tests/enforcement_integration/test_enforcement_gates.py`
- `scripts/report-token-usage.py`
- `https://github.com/obra/superpowers`
