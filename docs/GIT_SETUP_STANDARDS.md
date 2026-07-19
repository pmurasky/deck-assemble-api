# Git Setup Standards

**PURPOSE**: Standards for initializing and configuring git repositories in new projects. Ensures every project has version control from the first file, with proper ignore rules, branch naming, and push policy.

---

## 1. Repository Detection

Before initializing a new repository, check whether the current directory is already part of a git repository:

```bash
git rev-parse --is-inside-work-tree 2>/dev/null
```

**Decision tree:**

| Result | Meaning | Action |
|--------|---------|--------|
| `true` | Already inside a git repo (possibly a parent directory) | Do NOT run `git init`. Check which repo you're in with `git rev-parse --show-toplevel`. Verify this is intentional (e.g., monorepo) or ask the user. |
| Error/no output | Not inside any git repo | Proceed with `git init`. |

**Why this matters**: Running `git init` inside an existing repo creates a nested repo, which causes confusing behavior with submodules and ignored files. Always check first.

---

## 2. Repository Initialization

```bash
git init --initial-branch=main
```

**Rules:**
- Default branch MUST be `main` (not `master`).
- The `--initial-branch=main` flag sets this at init time, avoiding a rename step.
- If `git init` has already been run but the default branch is not `main`, rename it: `git branch -m master main`.

---

## 3. `.gitignore` Generation

### 3.1 Using gitignore.io API (Preferred)

Generate a `.gitignore` tailored to the project's tech stack using the gitignore.io API:

```bash
curl -fsSL "https://www.toptal.com/developers/gitignore/api/${TAGS}" > .gitignore
```

Where `${TAGS}` is a comma-separated list of technologies. Common combinations:

| Tech Stack | Tags |
|-----------|------|
| Kotlin + Gradle + IntelliJ | `kotlin,gradle,intellij` |
| Java + Gradle + IntelliJ | `java,gradle,intellij` |
| Java + Maven + IntelliJ | `java,maven,intellij` |
| Python + PyCharm | `python,pycharm,venv` |
| TypeScript + Node + VS Code | `node,typescript,visualstudiocode` |
| Go + VS Code | `go,visualstudiocode` |
| React + Node + VS Code | `node,react,visualstudiocode` |

### 3.2 Append Project-Specific Entries

After generating the base `.gitignore`, append entries specific to the project:

```bash
# Append project-specific ignores
echo "" >> .gitignore
echo "### Project-specific ###" >> .gitignore
echo ".sisyphus/" >> .gitignore
echo ".env" >> .gitignore
echo ".env.local" >> .gitignore
```

Common project-specific entries to consider:
- `.sisyphus/` — AI agent working directory
- `.env`, `.env.local` — environment files with secrets
- `*.log` — log files
- Docker volumes or local database data directories
- IDE run configurations with secrets

### 3.3 Fallback (API Unavailable)

If `curl` fails or the API is unreachable, create a `.gitignore` matching the reference project's patterns. At minimum, include:

```gitignore
# Build outputs
build/
dist/
out/
bin/
target/

# Gradle
.gradle/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

# IDE
.idea/
*.iws
*.iml
*.ipr
.vscode/
.classpath
.project
.settings/

# Environment
.env
.env.local

# AI agent
.sisyphus/

# OS
.DS_Store
Thumbs.db
```

---

## 4. Initial Commit

After creating the `.gitignore`, make the initial commit on `main`:

```bash
git add .gitignore
git commit -m "chore: initialize repository with .gitignore"
```

**Rules:**
- The `.gitignore` MUST be the first file committed.
- Commit it alone — do not bundle with other scaffold files.
- This establishes a clean `main` branch as the project's baseline.

---

## 5. Feature Branch

Before making any code changes, create a feature branch from `main`:

```bash
git checkout -b feat/<purpose>
```

**Branch naming conventions:**

| Prefix | Use When |
|--------|----------|
| `feat/` | Adding new features or functionality |
| `fix/` | Fixing bugs |
| `chore/` | Build, config, tooling changes |
| `refactor/` | Code restructuring without behavior change |
| `docs/` | Documentation-only changes |

**Examples:**
- `feat/conversion` — for framework conversion projects
- `feat/initial-setup` — for new project scaffolding
- `feat/add-auth` — for adding authentication

**Rules:**
- Never commit directly to `main` (treat it as protected even without a remote).
- Feature branch names should be descriptive and lowercase with hyphens.

---

## 6. Remote Detection and Push Policy

### 6.1 Check for Remote

```bash
git remote -v
```

| Result | Action |
|--------|--------|
| Remote(s) listed | For the current branch, push after every commit: `git push -u origin <branch>` (first push), then `git push` (subsequent), following the push policy below. |
| No remote | Commits stay local. This is fine for new projects without a remote yet. |

### 6.2 Push Policy

- **If remote exists**: For non-protected branches, push after every commit. This provides backup and enables collaboration.
- **If no remote**: Do not attempt to push. Do not create a remote unless the user requests it.
- **Never force-push** unless explicitly requested by the user.
- **Initial `main` setup exception**: You MAY push the initial `main` branch once (e.g., the `.gitignore` and bootstrap commit) to establish the default branch on the remote.
- **After initial setup, never push directly to `main`** — use feature branches and pull requests, merging into `main` via reviewed PRs only.

---

## 7. Verification

After setup, verify the repository is correctly configured:

```bash
# Verify we're in a git repo
git rev-parse --is-inside-work-tree

# Verify current branch / 'main' branch exists
git branch --show-current      # Shows the current working branch (e.g., 'main' or your feature branch)
git branch --list main         # Should list 'main' if it exists as the default branch

# Verify .gitignore exists and is committed
git log --oneline  # Should show at least the initial commit

# Verify clean working tree (after initial commit)
git status
```

**Expected state after setup:**
- Git repo initialized with `main` as default branch
- `.gitignore` committed on `main`
- Working on a feature branch (e.g., `feat/conversion`)
- Clean working tree (no unstaged changes)
- 1 commit on `main` (the `.gitignore`)

---

## 8. Git Worktrees (Optional — Advanced Parallel Development)

> **Standard branching is the default.** Use worktrees only when you genuinely
> need two or more branches checked out simultaneously.

A **git worktree** is an additional working directory linked to the same
repository. Each worktree has its own checked-out branch, but they all share the
same `.git` database.

### When to Use Worktrees

| Scenario | Example |
|----------|---------|
| Hotfix while a feature is in progress | `feat/auth` is mid-flight; production needs `fix/login-crash` |
| Two independent features in parallel | Working on both without stashing |
| Reviewing a colleague's branch without losing your work | Run their branch in a separate directory |

Do NOT use worktrees to work around incomplete micro-commits — finish the commit instead.

### Creating a Worktree

```bash
# Add a worktree for an existing branch
git worktree add ../my-project-hotfix fix/login-crash

# Add a worktree AND create a new branch from main
git worktree add -b fix/login-crash ../my-project-hotfix main
```

**Naming convention**: use `<project-name>-<branch-slug>` as the sibling directory name.

### Switching Between Worktrees

Change directories — no stashing required:

```bash
cd ../my-project-hotfix   # work on hotfix
cd ../my-project           # back to feature work
```

### Listing Worktrees

```bash
git worktree list
# /Users/you/my-project          abc1234 [feat/auth]
# /Users/you/my-project-hotfix   def5678 [fix/login-crash]
```

### Cleanup After Merge

```bash
# 1. Remove the worktree directory
git worktree remove ../my-project-hotfix

# 2. Prune stale entries (if directory was deleted manually)
git worktree prune

# 3. Delete the branch
git branch -d fix/login-crash
git push origin --delete fix/login-crash
```

For the full worktree workflow — including rules, naming conventions, and a
cleanup checklist — load the `using-git-worktrees` skill (environment-provided
via [superpowers](https://github.com/obra/superpowers)).

---

## 9. Anti-Patterns

| Anti-Pattern | Why It's a Problem | What To Do Instead |
|-------------|-------------------|-------------------|
| Writing code without `git init` | No version control, no recovery from mistakes, no incremental history | Always initialize git before writing any code |
| Running `git init` inside an existing repo | Creates confusing nested repo or submodule situation | Check `git rev-parse --is-inside-work-tree` first |
| Committing to `main` directly | No clean baseline, harder to review, no PR workflow | Create a feature branch before making changes |
| Skipping `.gitignore` | Build artifacts, IDE files, and secrets get committed | Generate `.gitignore` as the very first file |
| Bulk-writing files without committing | All-or-nothing — session interruption loses everything | Commit after every logical change (see micro-commit-workflow skill) |
| Force-pushing without explicit request | Destroys remote history, breaks collaborators | Never force-push unless the user explicitly asks |
| Writing multiple features before first commit | Impossible to revert individual changes | One logical change per commit from the start |

---

## 10. Complete Setup Sequence (Quick Reference)

```bash
# 1. Check if already in a git repo — stop if so
git rev-parse --is-inside-work-tree 2>/dev/null \
  && { echo "Already in a git repo. Choose a different directory for a new repo. Stopping."; return 1 2>/dev/null || false; }

# 2. Initialize
git init --initial-branch=main

# 3. Generate .gitignore (replace TAGS with your stack)
curl -fsSL "https://www.toptal.com/developers/gitignore/api/kotlin,gradle,intellij" > .gitignore
echo -e "\n### Project-specific ###\n.sisyphus/\n.env\n.env.local" >> .gitignore

# 4. Initial commit on main
git add .gitignore
git commit -m "chore: initialize repository with .gitignore"

# 5. Create feature branch
git checkout -b feat/conversion

# 6. Check for remote
git remote -v  # Push after commits if remote exists

# 7. Verify
git log --oneline
git status
```

---

**Last Updated**: March 2026
**Version**: 1.1 (Added Git Worktrees section)
