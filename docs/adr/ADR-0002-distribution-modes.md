# ADR-0002: Distribution Modes for Engineering Standards

## Status

Proposed

## Date

2026-05-11

## Context

Engineering standards must reach consumers across multiple AI coding tools (OpenCode, Claude Code, Cursor, GitHub Copilot) and installation preferences. The project currently supports three installation paths with varying maturity, but lacks explicit documentation of tradeoffs, support tiers, and a recommended default. This ADR establishes a clear distribution strategy with defined support tiers.

Current state:
- OpenCode plugin (`@pmurasky/engineering-standards`) exists with auto-update hooks
- Python-based file installer (`scripts/install_standards.py`) supports profile selection and manifest tracking
- Manual copy and git submodule paths are documented but not actively maintained
- No explicit support tier classification
- Future milestone (M010) explores additional packaging formats (npm, VS Code extension)

## Decision

Adopt a **tiered distribution model** with one recommended mode, multiple advanced modes, and documented legacy paths.

### Support Tiers

| Tier | Modes | Commitment |
|------|-------|------------|
| **Recommended** | OpenCode Plugin | First-class support, auto-updates, CI validation |
| **Advanced** | File-Based Install, Git Submodule | Supported, manual updates, documented |
| **Legacy** | Manual Copy | Documented, not maintained, no update support |

### Recommended Mode: OpenCode Plugin

The OpenCode plugin is the default and recommended distribution mode for all OpenCode users.

**Rationale:**
- Provides the richest feature set (tools, hooks, auto-discovery)
- Enables automatic update checks on OpenCode startup
- Has the lowest friction (single command install)
- Validated by CI on every release

### Advanced Modes

**File-Based Install:** Managed Python installer with profile selection, conflict detection, dry-run support, and manifest tracking. Suitable for:
- Non-OpenCode tools (Claude Code, Cursor, Copilot)
- Projects requiring pinned versions
- Environments where plugin installation is restricted

**Git Submodule:** Submodule workflow for teams preferring git-native dependency management. Suitable for:
- Teams already using submodules for other dependencies
- Projects requiring exact commit-level pinning
- Environments where external package managers are unavailable

### Legacy Modes

**Manual Copy:** Direct file copying without manifest tracking or update support. Documented for reference but not maintained.

## Alternatives Considered

### Alternative 1: Plugin-Only Distribution
- **Pros**: Simpler maintenance, single code path, strongest feature set
- **Cons**: Excludes non-OpenCode users, requires npm/opencode infrastructure
- **Why rejected**: Would abandon Claude Code, Cursor, and Copilot users who represent significant adoption

### Alternative 2: Equal Support for All Modes
- **Pros**: Maximum flexibility, no user left behind
- **Cons**: Unsustainable maintenance burden, dilutes quality, slows iteration
- **Why rejected**: Team size and velocity cannot support first-class maintenance of 5+ installation paths

### Alternative 3: npm Package for All Tools
- **Pros**: Familiar package manager, version pinning, wide ecosystem
- **Cons**: Not natively integrated with any target tool, requires post-install script
- **Why rejected**: Adds dependency without solving integration; file-based install is simpler for non-OpenCode tools

## Consequences

### Positive
- Clear guidance for new users ("use the plugin")
- Maintainers can focus quality investment on the plugin
- Advanced users retain flexibility with documented alternatives
- Legacy paths remain discoverable without maintenance commitment

### Negative
- Non-OpenCode users have a less polished experience
- File-based installer may lag behind plugin features
- Risk of fragmentation if advanced modes diverge

### Neutral
- Existing installations continue to work
- M010 (future packaging research) can extend the model without changing tiers
- Support tiers can be reclassified based on adoption metrics

## References

- `docs/distribution/installation-modes.md`
- `README.md` (Installation section)
- `distribution/standards-package.json`
- `scripts/install_standards.py`
- `scripts/update_standards.py`
- M010: OpenCode Plugin Packaging (future milestone)
