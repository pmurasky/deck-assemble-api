---
name: gitignore-bootstrap
description: >
  Use when setting up a new project or when an existing .gitignore needs a
  comprehensive overhaul — generates .gitignore files via the gitignore.io API
  with project-specific customizations. Covers API usage, template selection,
  common gotchas, merge strategies, and validation to prevent accidentally
  ignoring tracked files.
triggers:
  - "creating a gitignore for a new repository"
  - "overhauling an outdated or minimal gitignore"
  - "updating ignore rules after adding new tech stack"
  - "setting ignore patterns before initial commit"
not_for:
  - "tiny one line ignore tweaks"
  - "removing secrets already committed to history"
  - "dockerignore or npmignore workflow changes"
---

# gitignore.io Bootstrap

Systematic workflow for generating `.gitignore` files via the [gitignore.io](https://www.toptal.com/developers/gitignore) API with project-specific customizations.

## Table of Contents

- [Use when](#use-when)
- [Not for](#not-for)
- [Workflow](#workflow)
- [API Fallback](#api-fallback)
- [Merging with an Existing .gitignore](#merging-with-an-existing-gitignore)
- [Anti-Patterns](#anti-patterns)

## Use when

- When initializing a new project repository
- When a project's `.gitignore` is minimal or outdated
- When switching tech stacks (e.g., adding a frontend to a backend project)
- Before creating the initial commit in a new repository

## Not for

- Tiny one-line ignore tweaks when the existing `.gitignore` is already healthy
- Cleaning secrets out of git history after they were already committed
- `.dockerignore`, `.npmignore`, or other non-git ignore-file workflows

---

## Workflow

**Step 1: Fetch from API** — pick templates matching your stack:

```bash
curl -sL "https://www.toptal.com/developers/gitignore/api/kotlin,java,gradle,intellij,visualstudiocode" -o .gitignore
```

Common stacks: Java/Spring → `java,gradle,maven,intellij,visualstudiocode` | Python → `python,pycharm,visualstudiocode` | Node/React → `node,react,visualstudiocode` | Go → `go,visualstudiocode`

Template name gotchas: use `visualstudiocode` (not `vscode`), `intellij` (not `idea`).

**Step 2: Add project-specific entries** — append to `.gitignore`:

```gitignore
### Project-Specific ###
.env
.env.local
.env.*.local
.sisyphus/
docker-compose.override.yml
```

**Step 3: Add exceptions for tracked files** — check first:

```bash
git ls-files -i --exclude-from=.gitignore
```

If tracked files appear, add `!` rules (e.g., `!.vscode/mcp.json`, `!.idea/runConfigurations/`).

**Step 4: Validate** before committing:

```bash
git ls-files -i --exclude-from=.gitignore   # Must be empty (or only intended removals)
git check-ignore .gitignore                  # Must show nothing
git status --ignored --short                 # Review what's ignored
```

**Step 5: Commit alone** — `.gitignore` changes should be their own commit.

---

## API Fallback

If gitignore.io is unavailable, create a minimal `.gitignore` by hand covering: `build/`, `dist/`, `target/`, `.idea/`, `.vscode/*`, `.DS_Store`, `.env`, `node_modules/`, `__pycache__/`, `*.log`.

---

## Merging with an Existing .gitignore

1. Fetch API output to `.gitignore.new`
2. `diff .gitignore .gitignore.new` — compare
3. Replace (if existing is minimal) or append new patterns
4. Remove `.gitignore.new`
5. Re-validate with `git ls-files -i`

---

## Anti-Patterns

| Anti-Pattern | Do This Instead |
|-------------|-----------------|
| Write from memory | Use the gitignore.io API |
| Use `vscode` as template name | Use `visualstudiocode` |
| Overwrite without checking tracked files | Run `git ls-files -i` first |
| Add `!` exceptions without comments | Comment each exception rule |
| Commit `.gitignore` with code changes | Commit `.gitignore` alone |
