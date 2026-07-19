---
name: github-pr-review-consumer
description: >
  Use when you have received a PR review and need to act on the feedback —
  fetches live GitHub PR review comments, organizes them by thread, identifies
  action items, and produces a structured change plan. Automatically fetches
  PR diff and comments via gh CLI.
triggers:
  - "addressing pull request review feedback systematically"
  - "triaging many github review comments by thread"
  - "understanding outstanding review comments after time away"
  - "checking all comments before requesting review again"
not_for:
  - "local code review before opening a pull request"
  - "creating a pull request or doing git housekeeping"
  - "blindly applying comments without thread context"
context: fork
agent: Explore
allowed-tools: Bash(gh *)
---

# GitHub PR Review Consumer

Systematic workflow for consuming GitHub pull request review feedback and translating it into actionable changes.

## Use when

- When you receive PR review feedback and need to systematically address it
- When a PR has multiple reviewers with different types of feedback
- When you need to triage a large number of review comments
- When returning to a PR after time away and need to understand outstanding feedback
- Before re-requesting review to ensure all comments have been addressed

## Not for

- Local code review on changes not yet in a GitHub pull request
- Creating a PR, rebasing, or git housekeeping unrelated to review comments
- Blindly applying every comment without reading the full thread and diff context

## Live PR Context

- PR description: !`gh pr view $ARGUMENTS`
- Changed files: !`gh pr diff $ARGUMENTS --name-only`
- PR diff: !`gh pr diff $ARGUMENTS`
- PR comments: !`gh pr view $ARGUMENTS --comments`

## Your Task

Organize the review feedback above into a structured change plan using the workflow below.

---

## Workflow

### Step 1: Organize Comments by Thread

Group comments using `in_reply_to_id`: root comments (null) start threads; replies point to them. Group threads by `path` for file-by-file processing.

### Step 2: Classify Comments

| Type | Action Required |
|------|-----------------|
| **Must Fix** — CHANGES_REQUESTED + comment | Yes — fix before re-review |
| **Suggestion** — "consider", "could", "might want to" | Yes — implement or respond |
| **Question** — "?" without code suggestion | Yes — respond |
| **Nitpick** — "nit:", "minor:" | Optional |
| **Praise / Informational / Resolved** | No action |

### Step 3: Generate Change Plan

```markdown
## PR Review Change Plan — PR #<NUMBER>

### Must Fix (Blocking)
1. **[file:line]** — Description | Reviewer: @user | Plan: what you will do

### Suggestions (Non-Blocking)
1. **[file:line]** — Description | Plan: Accept/decline with rationale

### Questions (Need Response)
1. **[file:line]** — Question | Response: your answer

### Nitpicks (Optional)
1. **[file:line]** — Minor issue | Plan: fix/decline reason
```

### Step 4: Execute Changes

For each action item: make the change, commit with `fix(scope): address review feedback — description`, push. Optionally reply to the thread: `gh api repos/{owner}/{repo}/pulls/<PR>/comments --method POST -f body="Addressed in <SHA>" -F in_reply_to=<ID>`.

---

## Tips

- Read the full diff before processing comments — context matters.
- Prioritize "Changes Requested" reviews over "Comment" reviews.
- Look for patterns — multiple reviewers flagging the same issue = high priority.
- For large PRs (50+ comments): use `gh api --paginate` and process file-by-file.
- Re-request review after addressing all feedback: `gh pr ready <PR_NUMBER>`.

## Prerequisites

- `gh` CLI installed and authenticated (`gh auth status`). If not: `gh auth login`.

---

## Anti-Patterns

| ❌ Never | ✅ Always |
|---|---|
| Act without reading the full thread | Read full thread before acting |
| Blindly agree with all suggestions | Evaluate technically; push back if incorrect |
| Fix only the mentioned line | Consider whether the issue exists elsewhere |
| Treat `nit:` as mandatory | Focus on functional and design feedback first |
