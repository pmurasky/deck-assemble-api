# One-Command Init Workflow

This document specifies the unified initialization workflow for installing engineering standards into a downstream project with a single command.

## Overview

The current file-based install requires multiple steps:

```bash
git clone https://github.com/pmurasky/engineering-standards.git
python3 engineering-standards/scripts/install_standards.py --target your-project
```

The one-command init workflow collapses this into a single, self-bootstrapping command that handles cloning, profile selection, installation, and verification.

## Command Specification

### Entry Point

```bash
# Primary: curl-to-bash (always fetches latest)
curl -fsSL https://raw.githubusercontent.com/pmurasky/engineering-standards/main/scripts/init.sh | bash -s -- [OPTIONS] [TARGET]

# Alternative: wget
curl -fsSL https://raw.githubusercontent.com/pmurasky/engineering-standards/main/scripts/init.sh | bash -s -- [OPTIONS] [TARGET]

# With explicit arguments
curl -fsSL ... | bash -s -- --profile claude --profile copilot ./my-project
```

### Arguments

| Argument | Description | Default |
|----------|-------------|---------|
| `TARGET` | Project directory to install into | `.` (current directory) |

### Flags

| Flag | Description | Default |
|------|-------------|---------|
| `--profile PROFILE` | Tool profile to install (repeatable) | `core`, `opencode` |
| `--force` | Overwrite conflicting existing files | `false` |
| `--dry-run` | Preview changes without writing files | `false` |
| `--version VERSION` | Pin to specific git tag/commit | `main` |
| `--source-dir PATH` | Use local clone instead of fetching | (none) |
| `--help` | Show usage and exit | - |

### Exit Codes

| Code | Meaning |
|------|---------|
| `0` | Success (or dry-run completed) |
| `1` | General error (conflicts, invalid args, network failure) |
| `2` | Target already has engineering-standards installed (use update) |
| `3` | Unsupported platform (missing required tools) |

## Workflow Steps

```
┌─────────────────┐
│  Parse args     │
└────────┬────────┘
         ▼
┌─────────────────┐
│  Detect tools   │  ← Check for git, python3, curl/wget
└────────┬────────┘
         ▼
┌─────────────────┐
│  Check existing │  ← Look for .engineering-standards/manifest.json
│  installation   │
└────────┬────────┘
         │ already installed?
         ├──────── yes ────► Exit 2 (suggest update)
         │
         ▼ no
┌─────────────────┐
│  Resolve source │  ← --source-dir? clone from GitHub?
└────────┬────────┘
         ▼
┌─────────────────┐
│  Select profiles│  ← Default or --profile flags
└────────┬────────┘
         ▼
┌─────────────────┐
│  Run install    │  ← Delegate to install_standards.py
│  script         │
└────────┬────────┘
         ▼
┌─────────────────┐
│  Verify result  │  ← Check manifest written, files present
└────────┬────────┘
         ▼
┌─────────────────┐
│  Print summary  │  ← Files installed, profiles active, next steps
└─────────────────┘
```

## File Manifest

The init workflow installs files according to the selected profiles. See [`standards-package.json`](../../distribution/standards-package.json) for the canonical manifest.

### Profile: `core`

| Path | Description |
|------|-------------|
| `docs/` | All standards documents (40+ files) |

### Profile: `opencode`

| Path | Description |
|------|-------------|
| `AGENTS.md` | OpenCode / Copilot rules |
| `opencode.json` | OpenCode configuration |
| `.opencode/.gitignore` | Ignore local node_modules |
| `.opencode/agents/` | Subagent definitions |
| `.opencode/commands/` | Slash commands |
| `.opencode/package.json` | Local package manifest |
| `.opencode/package-lock.json` | npm lockfile |
| `.opencode/skills/` | On-demand skills |

### Profile: `claude`

| Path | Description |
|------|-------------|
| `CLAUDE.md` | Claude Code rules |
| `.claude/agents/` | Subagent definitions |
| `.claude/hooks/` | Session bootstrap hooks |
| `.claude/rules/` | Path-scoped rules |
| `.claude/settings.json` | Hook configuration |
| `.claude/skills/` | On-demand skills |

### Profile: `cursor`

| Path | Description |
|------|-------------|
| `AGENTS.md` | Fallback rules (also used by Copilot) |
| `.cursor/rules/` | Cursor-specific rules |

### Profile: `copilot`

| Path | Description |
|------|-------------|
| `AGENTS.md` | Fallback rules |
| `.github/copilot-instructions.md` | Copilot instructions |
| `.github/instructions/` | Path-scoped instructions |

## Conflict Rules

When the target directory already contains files that would be overwritten:

### Default Behavior (no `--force`)

1. Compare SHA-256 checksum of existing file with source file
2. If identical: mark as `keep`, no action
3. If different: mark as `conflict`, halt installation
4. Print conflict list with paths
5. Exit code `1` with message: "Re-run with --force to overwrite conflicting files"

### With `--force`

1. Compare checksums as above
2. If identical: mark as `keep`
3. If different: mark as `overwrite`, proceed with copy
4. Log overwritten files in summary

### Special Cases

| Scenario | Rule |
|----------|------|
| Existing `.engineering-standards/manifest.json` | Treat as already installed → exit 2 |
| Existing directory where file expected | Remove directory, create file (with `--force`) |
| Existing file where directory expected | Back up file to `{path}.backup`, create directory |
| Symlinks | Follow symlink, compare target content |
| Git-tracked files | No special handling; standard conflict rules apply |

## Dry-Run Behavior

When `--dry-run` is specified:

1. Execute all detection and classification steps
2. Compute planned changes (create / overwrite / keep / remove)
3. Print summary table:

   ```
   Profiles: core, opencode
   Planned changes:
     create:    47
     overwrite:  2
     keep:       0
     conflicts:  1
   
   Conflicting files:
     - AGENTS.md
   
   Dry run only; no files were written.
   ```

4. Exit code `0` (success) even if conflicts exist
5. Do not write manifest, do not modify any files

## Self-Bootstrapping Script

The `init.sh` script is designed to be:

- **POSIX-compliant**: Works with `sh`, `bash`, `zsh`
- **Minimal dependencies**: Requires only `git`, `python3`, and `curl` (or `wget`)
- **Self-contained**: No external dependencies beyond the target repo
- **Idempotent**: Safe to run multiple times (will detect existing install)
- **Transparent**: Prints every step, shows full commands when `--verbose`

### Script Structure

```bash
#!/bin/sh
set -e

# 1. Parse arguments
# 2. Detect required tools (git, python3)
# 3. Check for existing installation
# 4. Create temporary clone (or use --source-dir)
# 5. Run install_standards.py with forwarded arguments
# 6. Clean up temporary clone
# 7. Print success summary
```

### Temporary Clone Management

- Clone to `$(mktemp -d)/engineering-standards`
- Checkout `--version` if specified (default: `main`)
- Remove clone after installation completes
- If installation fails, leave clone for debugging (print path)

## Consumer Scenarios

### Scenario 1: New Project Setup

**User**: Starting a new Python project, wants OpenCode standards.

```bash
mkdir my-project && cd my-project
git init
curl -fsSL https://raw.githubusercontent.com/pmurasky/engineering-standards/main/scripts/init.sh | bash
```

**Result**:
- `docs/` installed with all standards
- `AGENTS.md`, `opencode.json`, `.opencode/` installed
- `.engineering-standards/manifest.json` created
- User commits the installed files

### Scenario 2: Existing Project with Conflicts

**User**: Project already has `AGENTS.md` with project-specific rules.

```bash
curl -fsSL ... | bash -s -- --profile claude
# Conflicts detected:
#   - AGENTS.md
# Re-run with --force to overwrite.
```

**Resolution options**:

1. **Merge manually** (recommended):
   ```bash
   # Back up existing
   cp AGENTS.md AGENTS.md.backup
   # Re-run with force
   curl -fsSL ... | bash -s -- --profile claude --force
   # Merge backup content into new AGENTS.md
   ```

2. **Use different profile** (if conflict is tool-specific):
   ```bash
   # Skip opencode profile, use claude only
   curl -fsSL ... | bash -s -- --profile claude
   ```

3. **Force overwrite** (if existing file is outdated):
   ```bash
   curl -fsSL ... | bash -s -- --profile claude --force
   ```

### Scenario 3: CI / Automated Installation

**User**: Installing standards in CI pipeline for compliance checks.

```bash
# In CI workflow (GitHub Actions example)
- name: Install Engineering Standards
  run: |
    curl -fsSL https://raw.githubusercontent.com/pmurasky/engineering-standards/main/scripts/init.sh | \
      bash -s -- --profile core --dry-run
    curl -fsSL https://raw.githubusercontent.com/pmurasky/engineering-standards/main/scripts/init.sh | \
      bash -s -- --profile core --force
```

**Result**:
- Dry-run first for visibility in logs
- Force install for unattended execution
- Only `docs/` installed (no tool configs in CI)

### Scenario 4: Version Pinning

**User**: Team wants reproducible installs at a specific version.

```bash
# Pin to v1.2.0
curl -fsSL ... | bash -s -- --version v1.2.0 --profile opencode --profile claude

# Or use commit hash
curl -fsSL ... | bash -s -- --version abc1234 --profile opencode
```

**Result**:
- Clone checks out specified tag/commit
- Manifest records `source_version` and `source_revision`
- Future updates must explicitly change version

### Scenario 5: Offline / Air-Gapped

**User**: Corporate network with no internet access to GitHub.

```bash
# Pre-clone repo to shared network drive
git clone https://github.com/pmurasky/engineering-standards.git /shared/engineering-standards

# Users install from local source
curl -fsSL ... | bash -s -- --source-dir /shared/engineering-standards --profile opencode
```

**Result**:
- No network access required during init
- Uses local clone instead of fetching
- Same manifest tracking and update capability

## Verification Checklist

After running init, the following should be true:

- [ ] `.engineering-standards/manifest.json` exists and is valid JSON
- [ ] Manifest `profiles` matches requested profiles
- [ ] All files listed in manifest exist at specified paths
- [ ] Checksums in manifest match actual file checksums
- [ ] No unmanaged files from engineering-standards are present (unless manually copied)

## Comparison with Other Installation Modes

| Aspect | One-Command Init | Manual File-Based | Plugin Install |
|--------|------------------|-------------------|----------------|
| Commands to run | 1 | 2+ | 1 |
| Requires Python | Yes (auto-detected) | Yes | No |
| Requires npm | No | No | Yes |
| Manifest tracking | Yes | Yes | No |
| Dry-run support | Yes | Yes | No |
| Version pinning | Yes | Yes | No |
| Auto-updates | No | No | Yes |
| Best for | Quick start, CI | Advanced users | OpenCode users |

## Future Enhancements

- **Interactive profile selector**: When no `--profile` specified and TTY detected, show interactive menu
- **Post-install hook**: Run compliance check after installation
- **Update reminder**: Print message if newer version available
- **Windows support**: PowerShell equivalent script (`init.ps1`)

## See Also

- [`installation-modes.md`](./installation-modes.md) — All supported installation methods
- [`../adr/ADR-0002-distribution-modes.md`](../adr/ADR-0002-distribution-modes.md) — Distribution mode ADR
- [`../../scripts/install_standards.py`](../../scripts/install_standards.py) — Underlying install script
- [`../../scripts/update_standards.py`](../../scripts/update_standards.py) — Update script for existing installs
- [`../../distribution/standards-package.json`](../../distribution/standards-package.json) — Canonical file manifest
