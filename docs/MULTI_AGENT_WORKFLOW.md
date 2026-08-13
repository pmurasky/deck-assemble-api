# Multi-Agent Cross-Repo Workflow

This repo is one lane (backend) in a multi-agent workflow coordinated through
the sibling `../specs/` repo. This only applies when you've been given a
kickoff instruction referencing a `specs/work/<id>` task — for all other work,
this file doesn't apply.

If you've been told you're the BACKEND agent for a `work/<id>` task: read
`../specs/AGENTS.md` in full before doing anything else. It defines:

- how to read your lane's plan and the frozen API contract
- how to report status back (`../specs/work/<id>/status.json`)
- how to escalate instead of improvising when something's unclear

Your existing engineering standards in this repo's `AGENTS.md` still apply in
full — this protocol only adds cross-repo coordination, it doesn't replace TDD,
micro-commits, or anything else there.
