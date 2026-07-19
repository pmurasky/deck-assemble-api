---
name: security-standards
description: >
  Use when working on authentication, authorization, user input, API endpoints, secrets,
  HTTP configuration, data storage, or dependency management — comprehensive security
  standards: OWASP Top 10 reference and mitigation mapping, secrets management (no hardcoded
  secrets, env vars, vault), input validation and sanitization rules, authentication patterns
  (password hashing, token management, MFA), authorization (RBAC/ABAC, deny-by-default, IDOR
  prevention), API security (rate limiting, CORS, payload limits), security headers (CSP,
  HSTS, X-Frame-Options), encryption (TLS 1.2+, AES-256, no MD5/SHA-1), supply chain
  security, SAST/DAST tooling, secure error handling, and a security code review checklist.
triggers:
  - "implementing authentication or session management securely"
  - "reviewing authorization rules and access control"
  - "validating user input or protecting api endpoints"
  - "managing secrets encryption and security headers"
  - "running security review on dependencies or pull request"
not_for:
  - "routine style questions without security impact"
  - "formal penetration testing or compliance audit replacement"
  - "pure logging format choices without security angle"
---

# Security Standards

## Table of Contents

- [Use when](#use-when)
- [OWASP Top 10 Reference](#owasp-top-10-reference)
- [Input Validation and Sanitization](#input-validation-and-sanitization)
- [Authentication Patterns](#authentication-patterns)
- [Authorization Patterns](#authorization-patterns)
- [Secrets Management](#secrets-management)
- [Encryption](#encryption)
- [API Security](#api-security)
- [Security Headers](#security-headers)
- [SAST and DAST Tooling](#sast-and-dast-tooling)
- [Security Code Review Checklist](#security-code-review-checklist)

Canonical owner: SECURITY_STANDARDS (see STANDARDS_OWNERSHIP_MATRIX).

## Use when

- Implementing or reviewing authentication, authorization, or session/token management
- Handling user input at system boundaries or adding API endpoints
- Managing secrets, configuring security headers, or setting up encryption
- Running SAST/DAST in CI or reviewing a PR for security anti-patterns

## Not for

- Style questions with no security impact
- Replacing a formal penetration test or compliance audit
- Logging-format decisions with no security or PII angle

---

## OWASP Top 10 Reference

| # | Category | Key Mitigation |
|---|----------|----------------|
| A01 | Broken Access Control | Deny by default, enforce server-side |
| A02 | Cryptographic Failures | Encrypt sensitive data at rest and in transit |
| A03 | Injection | Parameterized queries, input validation |
| A05 | Security Misconfiguration | Hardened defaults, minimal attack surface |
| A06 | Vulnerable Components | Dependency scanning, prompt patching |
| A07 | Authentication Failures | MFA, strong passwords, rate limiting |
| A09 | Logging & Monitoring Failures | Log security events, alert on anomalies |
| A10 | SSRF | Validate URLs, allowlist destinations |

---

## Input Validation

- Server-side only; allowlists over denylists; validate type, length, format, range
- Parameterized queries — never concatenate user input into SQL
- Encode output for context (HTML, URL, JS, CSS)

## Authentication

- Use established libraries — never custom auth from scratch
- Passwords: bcrypt/scrypt/Argon2id; never MD5 or plain SHA-1
- Short-lived tokens (15–60 min) + refresh rotation; never store in `localStorage`
- Invalidate sessions on logout, password change, privilege escalation

## Authorization

- Deny by default; enforce server-side on every request
- RBAC or ABAC; Least Privilege; validate object-level access (IDOR prevention)

## Secrets Management

**HARD BLOCK: Never hardcode secrets in source code or VCS.**

- Env vars or secrets manager; rotate regularly; separate per environment
- Scan with `gitleaks` or `detect-secrets` in pre-commit hooks

## Encryption

- TLS 1.2+ for transit; HSTS enabled; AES-256 for data at rest; keys via KMS
- SHA-256/SHA-3 for integrity; HMAC for message auth
- Never MD5, SHA-1, DES, RC4; never disable cert verification

## API Security

- Rate limiting on all public endpoints (`429` + `Retry-After`)
- CORS: explicit allowlists — never `*` on authenticated endpoints
- Payload size limits; disable unused HTTP methods

## Security Headers

| Header | Value |
|--------|-------|
| `Content-Security-Policy` | Strict; no `unsafe-inline`/`unsafe-eval` |
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` or `SAMEORIGIN` |

Remove `Server` and `X-Powered-By` headers.

## Logging

Log: auth events, authz failures, input validation errors, admin actions.
Never log: passwords, tokens, API keys, PII, encryption keys.

## SAST and DAST

SAST (Semgrep, CodeQL, gosec, SpotBugs) in CI every PR. DAST (ZAP, Burp) against staging.

→ [Full tooling and configuration](./REFERENCE.md#sast-and-dast-tooling)

## Secure Error Handling

- Never expose stack traces, SQL errors, or file paths in responses
- Generic client errors with correlation IDs; log details server-side

## Security Code Review Checklist

- [ ] No hardcoded secrets; DB queries parameterized
- [ ] Auth and authz enforced on new endpoints
- [ ] Error responses don't leak internals; no PII in logs
- [ ] Security headers set; encryption uses current standards
- [ ] Rate limiting on public endpoints; CORS uses allowlists
