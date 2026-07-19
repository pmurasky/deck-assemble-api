---
name: logging-standards
description: >
  Use when implementing logging, reviewing logging practices, or setting up log
  infrastructure — structured logging standards: required fields (timestamp, level,
  message, service, correlation_id), JSON format for production, parameterized logging
  (no string concatenation), log level definitions and selection rules
  (ERROR/WARN/INFO/DEBUG/TRACE), level anti-patterns, correlation IDs for distributed
  tracing, PII handling and masking strategies, and performance considerations.
triggers:
  - "adding logging to a new service or module"
  - "reviewing log statements in a pull request"
  - "setting up structured logging framework configuration"
  - "auditing logs for pii exposure or correlation ids"
  - "choosing the correct log level for event"
not_for:
  - "full authentication or secrets design outside logging"
  - "metrics or tracing architecture without logging issue"
  - "general code style review without log concerns"
---

# Logging Standards

Load this skill when implementing logging, reviewing logging practices, or setting up
log infrastructure in any application.

Canonical owner: LOGGING_STANDARDS (see STANDARDS_OWNERSHIP_MATRIX).

---

## Table of Contents

- [Use when](#use-when)
- [Not for](#not-for)
- [Structured Logging](#structured-logging)
- [Log Levels](#log-levels)
- [Correlation IDs](#correlation-ids)
- [PII Handling in Logs](#pii-handling-in-logs)
- [Performance Considerations](#performance-considerations)

## Use when

- Adding logging to a new service or module
- Reviewing a PR that introduces or modifies log statements
- Setting up a logging framework (log4j, SLF4J, structlog, zap, slog)
- Auditing existing logs for PII exposure or missing correlation IDs
- Choosing the correct log level for a given event

## Not for

- Full authentication, authorization, or secrets-management design outside logging concerns
- Metrics or tracing architecture when log-event design is not the problem
- General code style reviews that do not involve log structure, levels, or sensitive data handling

---

## Structured Logging

Every log entry **MUST** include these five required fields: `timestamp` (ISO 8601 UTC), `level`, `message`, `service`, `correlation_id`.

**Key rules:**
- Use **JSON format** for production; human-readable for local dev
- **Never concatenate strings** for log messages — use parameterized logging to avoid unnecessary allocation when the level is disabled
- Keep messages concise; put context in structured fields, not embedded in the message string
- Include recommended fields when available: `logger`, `thread`, `user_id`, `request_id`, `duration_ms`, `error_code`, `stack_trace` (ERROR only)

## Log Levels

| Level | Purpose | When to use |
|-------|---------|-------------|
| **ERROR** | Requires human intervention | Unhandled exceptions, failed calls that cannot be retried |
| **WARN** | Unexpected but system recovered | Retry succeeded, approaching limits, fallback triggered |
| **INFO** | Business/lifecycle events | Request received/completed, service started/stopped |
| **DEBUG** | Diagnostic (off in prod by default) | Method entry/exit, cache hit/miss details |
| **TRACE** | Wire-level detail | Full payloads, loop iterations |

**Anti-patterns:** Do not log expected 404s as ERROR. Do not log at INFO inside tight loops. Do not use ERROR for user input validation failures.

Default production level: **INFO**. Support runtime level changes without redeployment. Never deploy with DEBUG/TRACE enabled permanently.

## Correlation IDs

- **Every inbound request MUST receive a correlation ID** — generate one (UUID v4) at the system boundary if the caller does not provide one
- **Propagate to all downstream calls** via HTTP headers (`X-Correlation-ID`, `X-Request-ID`, or W3C `traceparent`)
- **Include in every log entry** as a structured field, not embedded in the message string
- **Include in API error responses** so callers can reference it in support requests

## PII Handling in Logs

**Never log raw PII** — no full names, email addresses, phone numbers, SSNs, passwords, tokens, API keys, session IDs, financial data, health data, GPS coordinates.

Log `user_id: "u-7829"` instead of `email: "john@example.com"`. When partial context is required for troubleshooting, use consistent masking (e.g., `***@example.com`, `***-***-1234`).

Apply masking at the logging layer using a deny-list of field names. Audit logs periodically. Define and enforce retention policies.

## Performance Considerations

- Guard expensive log computations — check `isDebugEnabled()` before serializing objects
- Do not log large payloads at INFO; full request/response bodies belong at DEBUG/TRACE (and only when PII-free)
- Use asynchronous logging in high-throughput services to avoid blocking application threads

For complete reference material, see [REFERENCE.md](REFERENCE.md) in this skill directory.
