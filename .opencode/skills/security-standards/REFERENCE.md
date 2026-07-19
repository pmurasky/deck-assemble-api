# Security Standards — Reference Details

Companion reference for `security-standards/SKILL.md`. Contains detailed tables
and tooling breakdowns extracted for progressive disclosure.

## Table of Contents

- [Logging: What to Log and What to Never Log](#logging-what-to-log-and-what-to-never-log)
- [SAST and DAST Tooling](#sast-and-dast-tooling)
- [Severity Classification](#severity-classification)

---

## Logging: What to Log and What to Never Log

**What to log:**
- Authentication events (login, logout, failed attempts)
- Authorization failures (access denied)
- Input validation failures
- Application errors and exceptions
- Administrative actions (user creation, role changes, config changes)
- Security-relevant events (password resets, MFA enrollment)

**What to NEVER log:**
- Passwords (plaintext or hashed)
- Session tokens or API keys
- Credit card numbers or financial account details
- Social Security Numbers or government IDs
- Health records (PHI)
- Full request/response bodies containing PII
- Encryption keys or secrets

**PII Handling:**
- Mask or redact PII in logs (e.g., show last 4 digits of phone, first initial of name).
- Define a PII classification for your domain and enforce it in code reviews.
- Set retention policies — do not retain logs with PII longer than necessary.
- Ensure log storage is access-controlled.

---

## SAST and DAST Tooling

**SAST (Static Analysis) — run in CI on every PR:**

| Tool | Languages | Focus |
|------|-----------|-------|
| Semgrep | Multi-language | Custom rules, OWASP patterns |
| CodeQL | Multi-language | Deep semantic analysis |
| Bandit | Python | Python-specific security |
| gosec | Go | Go-specific security |
| SpotBugs + Find Security Bugs | Java | Bytecode-level security |
| ESLint security plugins | JavaScript/TypeScript | DOM XSS, injection patterns |

**DAST (Dynamic Analysis) — run against staging:**

| Tool | Purpose |
|------|---------|
| OWASP ZAP | Open-source web app scanner |
| Burp Suite | Commercial web app scanner |
| Nuclei | Template-based vulnerability scanner |

**Integration Strategy:**
1. **Pre-commit**: Secret scanning (gitleaks)
2. **CI pipeline**: SAST tools (Semgrep, CodeQL, language-specific)
3. **Staging deploy**: DAST tools (ZAP, Nuclei)
4. **Regular cadence**: Dependency audits (weekly), penetration testing (quarterly or annually)

---

## Severity Classification

| Severity | Description | Response Time | Example |
|----------|-------------|---------------|---------|
| **Critical** | Active exploitation possible, data breach risk | Fix immediately, deploy hotfix | SQL injection, hardcoded production credentials |
| **High** | Exploitable with moderate effort | Fix within current sprint | Broken access control, missing auth on endpoint |
| **Medium** | Exploitable with specific conditions | Fix within next sprint | Missing rate limiting, overly permissive CORS |
| **Low** | Defense-in-depth improvement | Schedule in backlog | Missing security header, verbose error message |
