---
description: Guide through the micro-commit workflow for current changes
agent: standards-build
---

I need to commit my current changes following the micro-commit workflow.

Here are my current changes:
!`git diff --stat`

And here are any staged changes:
!`git diff --cached --stat`

Please help me:

1. **Analyze** - Look at the changes and determine if they represent ONE logical change or multiple
2. **Split if needed** - If multiple logical changes are bundled, recommend how to split them into separate commits
3. **Order** - Suggest the correct commit order (refactor -> feat -> test -> docs)
4. **Message** - Draft a Conventional Commits message for each commit
5. **Verify** - Remind me to run tests before each commit

Follow the workflow in `docs/AI_AGENT_WORKFLOW.md`.

Each commit must be:
- One logical change
- Production-ready (tests pass, builds, no lint errors)
- Following Conventional Commits format: `<type>(<scope>): <description>`
