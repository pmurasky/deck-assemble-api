# Security Standards

## Overview

This document provides comprehensive, language-agnostic security standards for all projects. It expands on the brief security section in [CODING_PRACTICES.md](./CODING_PRACTICES.md) with actionable rules, checklists, and guidance aligned with OWASP and industry best practices.

**When to load this document:** Any change involving authentication, authorization, user input, API endpoints, secrets, HTTP configuration, data storage, or dependency management.

## OWASP Top 10 Reference

The [OWASP Top 10](https://owasp.org/www-project-top-ten/) is the baseline for web application security. Every developer should be familiar with these categories:

| # | Category | Key Mitigation |
|---|----------|----------------|
| A01 | Broken Access Control | Deny by default, enforce server-side |
| A02 | Cryptographic Failures | Encrypt sensitive data at rest and in transit |
| A03 | Injection | Parameterized queries, input validation |
| A04 | Insecure Design | Threat modeling, secure design patterns |
| A05 | Security Misconfiguration | Hardened defaults, minimal attack surface |
| A06 | Vulnerable Components | Dependency scanning, prompt patching |
| A07 | Authentication Failures | MFA, strong password policies, rate limiting |
| A08 | Data Integrity Failures | Verify signatures, use trusted sources |
| A09 | Logging & Monitoring Failures | Log security events, alert on anomalies |
| A10 | SSRF | Validate/sanitize URLs, allowlist destinations |

**Additional resources:**
- [OWASP Cheat Sheet Series](https://cheatsheetseries.owasp.org/) -- quick reference for specific topics
- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) -- verification standard for thorough security assessments

## Input Validation and Sanitization

Input validation is the single most impactful security practice. Treat all external input as untrusted.

### Rules

- **Validate on the server side.** Client-side validation is a UX convenience, not a security control.
- **Use allowlists over denylists.** Define what IS allowed rather than trying to block what is not.
- **Validate type, length, format, and range** for every input field.
- **Reject invalid input** -- do not attempt to "fix" or sanitize it into a valid form (sanitize output, validate input).
- **Use parameterized queries** for all database operations. Never concatenate user input into SQL.
- **Encode output** appropriate to context (HTML, URL, JavaScript, CSS, SQL).

### Checklist

- [ ] All user-supplied input is validated server-side
- [ ] Database queries use parameterized statements or ORM query builders
- [ ] Output is encoded for the appropriate context (HTML entity encoding, URL encoding, etc.)
- [ ] File uploads validate type, size, and content (not just file extension)
- [ ] Redirects and forwards validate destination against an allowlist

### Examples

```
-- BAD: SQL injection vulnerability
query = "SELECT * FROM users WHERE id = " + userId

-- GOOD: Parameterized query
query = "SELECT * FROM users WHERE id = ?"
params = [userId]
```

```
// BAD: Reflected XSS vulnerability
response.write("<p>Hello, " + userName + "</p>")

// GOOD: Output encoding
response.write("<p>Hello, " + htmlEncode(userName) + "</p>")
```

## Authentication Patterns

### Rules

- **Use established libraries and frameworks** for authentication. Do not implement custom auth from scratch.
- **Enforce strong password policies**: minimum 12 characters, no maximum limit, allow all Unicode characters.
- **Support multi-factor authentication (MFA)** for all user-facing applications.
- **Use bcrypt, scrypt, or Argon2id** for password hashing. Never use MD5, SHA-1, or SHA-256 alone for passwords.
- **Implement account lockout or rate limiting** after repeated failed login attempts.
- **Invalidate sessions** on logout, password change, and privilege escalation.

### Token and Session Management

- **Use short-lived access tokens** (15-60 minutes) with refresh token rotation.
- **Store tokens securely**: `HttpOnly`, `Secure`, `SameSite` cookies for web; secure storage for mobile/native.
- **Never store tokens in localStorage** -- it is accessible to XSS attacks.
- **Include token expiration** in all token types.
- **Implement token revocation** for logout and compromised credentials.

### Checklist

- [ ] Passwords are hashed with bcrypt/scrypt/Argon2id (with appropriate work factor)
- [ ] Authentication uses an established library, not custom implementation
- [ ] MFA is supported (and enforced for admin/privileged accounts)
- [ ] Failed login attempts are rate-limited or trigger account lockout
- [ ] Sessions are invalidated on logout and password change
- [ ] Tokens have expiration and support revocation
- [ ] No credentials are transmitted over unencrypted channels

## Authorization Patterns

### Rules

- **Deny by default.** Users have no access until explicitly granted.
- **Enforce authorization server-side** on every request. Never rely on client-side checks or hidden UI elements.
- **Use Role-Based (RBAC) or Attribute-Based (ABAC) access control** consistently.
- **Apply the Principle of Least Privilege**: grant the minimum permissions required for each role or service.
- **Validate object-level access** -- ensure users can only access resources they own or are authorized to view (IDOR prevention).

### Checklist

- [ ] Every endpoint enforces authorization checks server-side
- [ ] Default access is "deny" -- permissions are explicitly granted
- [ ] Object-level authorization prevents IDOR (Insecure Direct Object Reference)
- [ ] Admin and privileged operations require elevated authentication (re-auth or MFA step-up)
- [ ] Service-to-service communication uses scoped credentials (not shared admin keys)

## Secrets Management

### Rules

- **Never hardcode secrets** in source code, configuration files, or environment-specific files checked into version control.
- **Use environment variables or a secrets manager** (e.g., HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, GCP Secret Manager).
- **Rotate credentials regularly** -- at minimum quarterly, immediately on suspected compromise.
- **Use separate credentials** for each environment (dev, staging, production).
- **Scope secrets narrowly** -- each service should only have access to the secrets it needs.

### What Counts as a Secret

- API keys and tokens
- Database connection strings with credentials
- Encryption keys and certificates
- OAuth client secrets
- Webhook signing keys
- SSH private keys
- Any value that grants access to a system or data

### Checklist

- [ ] No secrets in source code (scan with tools like `gitleaks`, `truffleHog`, or `detect-secrets`)
- [ ] `.env` files and credential files are in `.gitignore`
- [ ] Secrets are loaded from environment variables or a secrets manager at runtime
- [ ] Each environment uses distinct credentials
- [ ] Secret rotation is automated or scheduled
- [ ] CI/CD pipelines use masked/encrypted secrets, not plaintext

### Pre-Commit Hook Example

```bash
# Use gitleaks as a pre-commit hook to prevent secret leaks
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.18.0
    hooks:
      - id: gitleaks
```

## Supply Chain Security

### Rules

- **Pin dependency versions** in lock files (`package-lock.json`, `go.sum`, `Cargo.lock`, `poetry.lock`, etc.).
- **Audit dependencies regularly** for known vulnerabilities using automated tools.
- **Use trusted registries** -- prefer official package registries and verify package provenance when available.
- **Review new dependencies** before adding them: check maintenance activity, license, download count, and known issues.
- **Generate and maintain SBOMs** (Software Bill of Materials) for production artifacts.

### Version Selection Rule (MANDATORY)

**When adding or updating any dependency, always select the latest version that has no known vulnerabilities.**

Follow this process every time:

1. **Identify the latest published version** of the library (Maven Central, npm, PyPI, pkg.go.dev, etc.).
2. **Scan that version for known vulnerabilities** using the audit tool for your ecosystem (see table below).
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document the reason in the commit message.
4. **Never pin an older version out of habit or convenience** — always start from the latest and work backwards only if a vulnerability forces it.

**Commit message example when stepping back from latest:**
```
chore(deps): pin requests to 2.31.0 instead of 2.32.0

2.32.0 has CVE-2024-XXXXX (high severity, no fix yet).
2.31.0 is the latest clean version. Revisit when a patched version is released.
```

### Recommended Tools

| Ecosystem | Audit Command | Scanning Tool |
|-----------|---------------|---------------|
| npm/Node | `npm audit` | Dependabot, Snyk |
| Python | `pip-audit` | Safety, Snyk |
| Go | `govulncheck ./...` | Dependabot, Snyk |
| Java/Kotlin | `./gradlew dependencyCheckAnalyze` (OWASP plugin) | Dependabot, Snyk |
| Rust | `cargo audit` | Dependabot |
| General | -- | Renovate, Dependabot |

### Checklist

- [ ] Lock files are committed and reviewed in PRs
- [ ] Dependency scanning runs in CI (fails on critical/high vulnerabilities)
- [ ] New dependencies are reviewed for trustworthiness before adoption
- [ ] **Version selection follows the latest-without-vulnerabilities rule** (see Version Selection Rule above)
- [ ] Dependency updates are applied regularly (automated PRs via Dependabot/Renovate)
- [ ] SBOM is generated for release artifacts

## Encryption

### Data in Transit

- **Enforce TLS 1.2 or higher** for all network communication. Disable TLS 1.0 and 1.1.
- **Use HSTS** (HTTP Strict Transport Security) to prevent protocol downgrade attacks.
- **Redirect all HTTP traffic to HTTPS** -- no mixed content.
- **Validate TLS certificates** in all clients. Never disable certificate verification (even in tests, use test CAs instead).

### Data at Rest

- **Encrypt sensitive data at rest** using AES-256 or equivalent.
- **Use envelope encryption** where possible (encrypt data with a data key, encrypt the data key with a master key).
- **Manage encryption keys separately from encrypted data** -- use a KMS (Key Management Service).
- **Hash passwords** with bcrypt/scrypt/Argon2id (see Authentication section).

### Hashing

- **Use SHA-256 or SHA-3** for integrity checks and non-password hashing.
- **Never use MD5 or SHA-1** for security purposes -- both are cryptographically broken.
- **Use HMAC** for message authentication (not plain hashing).

### Checklist

- [ ] All network communication uses TLS 1.2+
- [ ] HSTS is enabled with appropriate `max-age`
- [ ] Sensitive data at rest is encrypted (PII, financial data, health records)
- [ ] Encryption keys are managed through a KMS, not stored alongside data
- [ ] No use of deprecated algorithms (MD5, SHA-1, DES, RC4)
- [ ] Certificate validation is never disabled in production code

## API Security

### Rate Limiting

- **Implement rate limiting** on all public-facing endpoints.
- **Use tiered limits**: stricter limits on authentication endpoints, looser on read-heavy endpoints.
- **Return `429 Too Many Requests`** with a `Retry-After` header when limits are exceeded.
- **Rate limit by identity** (API key, user ID) not just by IP address (IP-based limiting alone is ineffective behind proxies).

### CORS (Cross-Origin Resource Sharing)

- **Never use `Access-Control-Allow-Origin: *`** on authenticated endpoints.
- **Allowlist specific origins** -- do not reflect the `Origin` header back blindly.
- **Limit allowed methods and headers** to what is actually required.
- **Set `Access-Control-Max-Age`** to cache preflight responses and reduce latency.

### General API Rules

- **Validate request size** -- set maximum payload limits to prevent abuse.
- **Use API keys or OAuth tokens** for authentication, not basic auth in headers.
- **Version your APIs** -- never break existing clients silently.
- **Validate content types** -- reject requests with unexpected `Content-Type` headers.
- **Disable unnecessary HTTP methods** (e.g., TRACE, OPTIONS if not needed).

### Checklist

- [ ] Rate limiting is enforced on all public endpoints
- [ ] CORS is configured with explicit origin allowlists (no wildcards on authenticated endpoints)
- [ ] Request payloads have size limits
- [ ] API authentication uses tokens, not embedded credentials in URLs
- [ ] Unnecessary HTTP methods are disabled
- [ ] API responses do not leak internal implementation details

## Security Headers

Configure these headers on all HTTP responses. They are defense-in-depth measures that mitigate entire classes of attacks.

### Required Headers

| Header | Recommended Value | Purpose |
|--------|-------------------|---------|
| `Content-Security-Policy` | Strict policy (see below) | Prevents XSS and data injection |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` | Forces HTTPS |
| `X-Content-Type-Options` | `nosniff` | Prevents MIME-type sniffing |
| `X-Frame-Options` | `DENY` or `SAMEORIGIN` | Prevents clickjacking |
| `Referrer-Policy` | `strict-origin-when-cross-origin` | Controls referrer information leakage |
| `Permissions-Policy` | Disable unused features | Limits browser feature access |

### Content Security Policy (CSP)

Start with a strict policy and relax only as needed:

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'
```

- **Never use `unsafe-inline` or `unsafe-eval`** unless absolutely necessary (and document the justification).
- **Use nonces or hashes** for inline scripts if they cannot be eliminated.
- **Deploy in report-only mode first** (`Content-Security-Policy-Report-Only`) to identify violations before enforcing.

### Headers to Remove

- Remove `Server` header (leaks server software/version)
- Remove `X-Powered-By` header (leaks framework information)

### Checklist

- [ ] All required security headers are set on HTTP responses
- [ ] CSP is configured and does not use `unsafe-inline` or `unsafe-eval` without justification
- [ ] `Server` and `X-Powered-By` headers are removed or masked
- [ ] HSTS is enabled with `includeSubDomains`

## Logging and Sensitive Data

### What to Log

- Authentication events (login, logout, failed attempts)
- Authorization failures (access denied)
- Input validation failures
- Application errors and exceptions
- Administrative actions (user creation, role changes, config changes)
- Security-relevant events (password resets, MFA enrollment)

### What to NEVER Log

- **Passwords** (plaintext or hashed)
- **Session tokens or API keys**
- **Credit card numbers or financial account details**
- **Social Security Numbers or government IDs**
- **Health records (PHI)**
- **Full request/response bodies** containing PII
- **Encryption keys or secrets**

### PII Handling

- **Mask or redact PII** in logs (e.g., show last 4 digits of phone, first initial of name).
- **Define a PII classification** for your domain and enforce it in code reviews.
- **Set retention policies** -- do not retain logs with PII longer than necessary.
- **Ensure log storage is access-controlled** -- logs containing security events should have restricted access.

### Checklist

- [ ] Security-relevant events are logged (auth, authz failures, admin actions)
- [ ] No secrets, tokens, or passwords appear in logs
- [ ] PII is masked or redacted in log output
- [ ] Log retention policies are defined and enforced
- [ ] Log access is restricted to authorized personnel

## SAST and DAST Tooling

Static Application Security Testing (SAST) and Dynamic Application Security Testing (DAST) should be integrated into the development lifecycle.

### SAST (Static Analysis)

Run SAST tools in CI to catch vulnerabilities before code reaches production.

| Tool | Languages | Focus |
|------|-----------|-------|
| Semgrep | Multi-language | Custom rules, OWASP patterns |
| CodeQL | Multi-language | Deep semantic analysis |
| Bandit | Python | Python-specific security |
| gosec | Go | Go-specific security |
| SpotBugs + Find Security Bugs | Java | Bytecode-level security (see [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md)) |
| ESLint security plugins | JavaScript/TypeScript | DOM XSS, injection patterns |
| Brakeman | Ruby | Rails-specific security |

### DAST (Dynamic Analysis)

Run DAST tools against running applications in staging or pre-production environments.

| Tool | Purpose |
|------|---------|
| OWASP ZAP | Open-source web app scanner |
| Burp Suite | Commercial web app scanner |
| Nuclei | Template-based vulnerability scanner |

### Integration Strategy

1. **Pre-commit**: Secret scanning (gitleaks)
2. **CI pipeline**: SAST tools (Semgrep, CodeQL, language-specific tools)
3. **Staging deploy**: DAST tools (ZAP, Nuclei)
4. **Regular cadence**: Dependency audits (weekly), penetration testing (quarterly or annually)

### Checklist

- [ ] SAST runs in CI on every PR
- [ ] Secret scanning runs as a pre-commit hook or in CI
- [ ] DAST runs against staging environments regularly
- [ ] Critical and high findings block the build
- [ ] Medium findings are tracked and resolved within a sprint

## Error Handling for Security

### Rules

- **Never expose internal details in error responses** -- no stack traces, SQL errors, file paths, or server versions in responses to clients.
- **Use generic error messages for clients** -- e.g., "Authentication failed" rather than "User not found" vs. "Invalid password" (which leaks whether the user exists).
- **Log detailed errors server-side** with correlation IDs for debugging.
- **Return consistent error response formats** to prevent information leakage through error structure differences.

### Examples

BAD — leaks internal details:

```json
{
  "error": "SQLException: table 'users' column 'email' - duplicate entry 'user@example.com'",
  "stack": "at com.example.UserDao.insert(UserDao.java:42)..."
}
```

GOOD — generic client-facing error with correlation ID (detailed error logged server-side with `requestId` for correlation):

```json
{
  "error": "Unable to process request",
  "code": "CONFLICT",
  "requestId": "req-abc123"
}
```

### Checklist

- [ ] Error responses do not contain stack traces, SQL errors, or file paths
- [ ] Authentication errors do not reveal whether a user exists
- [ ] All errors include a correlation ID for server-side debugging
- [ ] Error response format is consistent across all endpoints

## Security Code Review Checklist

Use this checklist in addition to the general code review checklist in [CODING_PRACTICES.md](./CODING_PRACTICES.md).

### For Every PR

- [ ] No hardcoded secrets, API keys, or credentials
- [ ] All user input is validated server-side
- [ ] Database queries use parameterized statements
- [ ] Output is encoded for the appropriate context
- [ ] Authentication and authorization are enforced on new endpoints
- [ ] Error responses do not leak internal details
- [ ] No PII or secrets in log statements
- [ ] New dependencies have been reviewed for security

### For Security-Sensitive Changes

- [ ] Threat model has been reviewed or updated
- [ ] Security headers are configured correctly
- [ ] Encryption uses current standards (no deprecated algorithms)
- [ ] Rate limiting is in place for new public endpoints
- [ ] Session/token management follows standards in this document
- [ ] CORS configuration uses explicit allowlists

## Quick Reference: Severity Classification

Use this to prioritize security findings:

| Severity | Description | Response Time | Example |
|----------|-------------|---------------|---------|
| **Critical** | Active exploitation possible, data breach risk | Fix immediately, deploy hotfix | SQL injection, hardcoded production credentials |
| **High** | Exploitable with moderate effort | Fix within current sprint | Broken access control, missing auth on endpoint |
| **Medium** | Exploitable with specific conditions | Fix within next sprint | Missing rate limiting, overly permissive CORS |
| **Low** | Defense-in-depth improvement | Schedule in backlog | Missing security header, verbose error message |

---

**Last Updated**: February 19, 2026
**Version**: 1.0 (Initial release -- closes #7)
