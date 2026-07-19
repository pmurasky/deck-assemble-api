# Installation Modes

This document describes all supported ways to consume engineering standards, organized by [support tier](../adr/ADR-0002-distribution-modes.md).

## Support Tiers

| Tier | Description |
|------|-------------|
| **Recommended** | First-class support, automatic updates, CI validation |
| **Advanced** | Supported, manual updates, fully documented |
| **Legacy** | Documented for reference, not actively maintained |

---

## Recommended Tier

### OpenCode Plugin

The default and recommended installation mode for OpenCode users.

**Installation:**

```bash
# Add to your project's opencode.json
{
  "plugin": ["@pmurasky/engineering-standards"]
}

# Then install
opencode plugin install
# or shorthand
opencode plug @pmurasky/engineering-standards
```

**Update mechanism:**
- Automatic update checks on OpenCode startup
- Manual update via `updateStandards` tool
- Changelog displayed after update

**Tooling requirements:**
- OpenCode >= 0.1.0
- npm (for plugin installation)

**Tradeoffs:**
- ✅ Lowest friction (single command)
- ✅ Richest feature set (tools, hooks, auto-discovery)
- ✅ Automatic updates
- ❌ OpenCode-only
- ❌ Requires npm infrastructure

**Consumer type:** All OpenCode users

---

## Advanced Tier

### File-Based Install

Managed Python installer with profile selection and manifest tracking. Best for non-OpenCode tools or when version pinning is required.

**Installation:**

```bash
git clone https://github.com/pmurasky/engineering-standards.git
python3 engineering-standards/scripts/install_standards.py --target your-project
```

**Profiles:**
- `core` - Shared docs (`docs/`)
- `opencode` - `AGENTS.md`, `opencode.json`, `.opencode/`
- `claude` - `CLAUDE.md`, `.claude/`
- `cursor` - `AGENTS.md`, `.cursor/rules/`
- `copilot` - `AGENTS.md`, `.github/copilot-instructions.md`, `.github/instructions/`

Profiles are additive: `--profile claude --profile copilot` installs both.

**Update mechanism:**

```bash
python3 engineering-standards/scripts/update_standards.py --target your-project
```

- Preserves installed profiles by default
- Detects local modifications (refuses to overwrite unless `--force`)
- Removes stale managed files
- Supports `--dry-run` for preview

**Version pinning:**

```bash
git -C engineering-standards fetch --tags
git -C engineering-standards checkout v1.0.0
python3 engineering-standards/scripts/update_standards.py --target your-project
```

**Tooling requirements:**
- Python 3.8+
- Git (for cloning)

**Tradeoffs:**
- ✅ Works with all supported tools
- ✅ Version pinning supported
- ✅ Detects local changes
- ✅ Dry-run preview
- ❌ Manual update required
- ❌ Requires Python

**Consumer type:** Claude Code, Cursor, Copilot users; teams requiring pinned versions

### Git Submodule

Git-native dependency management for teams already using submodules.

**Installation:**

```bash
cd your-project
git submodule add https://github.com/pmurasky/engineering-standards.git engineering-standards
```

**Integration options:**

1. **Symlink approach** (keeps configs in sync):
   ```bash
   ln -s engineering-standards/docs docs/engineering-standards
   ln -s engineering-standards/.cursor/rules .cursor/rules/engineering-standards
   ```

2. **Copy approach** (snapshot at specific commit):
   ```bash
   cp -r engineering-standards/docs your-project-docs/
   cp -r engineering-standards/.cursor/rules .cursor/rules/
   ```

**Update mechanism:**

```bash
git submodule update --remote engineering-standards
git add engineering-standards
git commit -m "chore: update engineering standards submodule"
```

**Tooling requirements:**
- Git with submodule support
- All contributors must initialize submodules

**Tradeoffs:**
- ✅ Exact commit-level pinning
- ✅ No external package managers
- ✅ Native git workflow
- ❌ All contributors must manage submodules
- ❌ Symlinks may not work on all platforms
- ❌ More complex setup

**Consumer type:** Teams using submodules for other dependencies; environments with restricted package managers

---

## Legacy Tier

### Manual Copy

Direct file copying without manifest tracking or update support.

**Installation:**

Copy files for your tool:

| Tool | Files to Copy |
|------|--------------|
| OpenCode | `AGENTS.md`, `opencode.json`, `.opencode/` |
| Claude Code | `CLAUDE.md`, `.claude/` |
| Cursor | `.cursor/rules/` (also reads `AGENTS.md`) |
| GitHub Copilot | `.github/copilot-instructions.md`, `.github/instructions/` (also reads `AGENTS.md`) |

**Update mechanism:**
None. Manual re-copy required.

**Tooling requirements:**
None.

**Tradeoffs:**
- ✅ No dependencies
- ✅ Works anywhere
- ❌ No update tracking
- ❌ No conflict detection
- ❌ Easy to drift out of sync

**Consumer type:** Quick evaluation; environments with no other options

---

## Mode Selection Guide

| If you... | Use |
|-----------|-----|
| Use OpenCode | [OpenCode Plugin](#opencode-plugin) (Recommended) |
| Use Claude Code, Cursor, or Copilot | [File-Based Install](#file-based-install) |
| Need exact commit pinning | [Git Submodule](#git-submodule) or file-based with git checkout |
| Already use submodules | [Git Submodule](#git-submodule) |
| Cannot run Python or npm | [Manual Copy](#manual-copy) |
| Want automatic updates | [OpenCode Plugin](#opencode-plugin) |
| Want update preview/dry-run | [File-Based Install](#file-based-install) |

---

## Future Modes (M010)

The following modes are under research for future milestones:

- **npm Package**: Distribution via npm for Node.js projects
- **VS Code Extension**: Integrated extension for Cursor/VS Code users
- **Homebrew Formula**: macOS/Linux package manager support

See M010 planning documents for details.
