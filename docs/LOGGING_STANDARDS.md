# Logging Standards

## Overview

This document defines standards for application logging: structured logging formats, log level usage, correlation IDs for distributed tracing, and PII handling. These standards are language-agnostic -- see language-specific standards for framework recommendations and implementation examples.

When to load this document: When implementing logging in any application, reviewing logging practices, or setting up log infrastructure.

**Related documents:**
- [SECURITY_STANDARDS.md](./SECURITY_STANDARDS.md) -- Security-specific logging (what to log/never log for security events)
- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Brief logging summary within general practices

## Structured Logging

### Why Structured Logging

Unstructured log messages (free-text strings) are difficult to parse, search, and alert on. Structured logging emits log entries as key-value pairs or JSON objects, enabling:

- Machine-parseable log entries for aggregation tools
- Consistent field names across services
- Efficient filtering and querying
- Automated alerting on specific field values

### Required Fields

Every log entry MUST include these fields:

| Field | Description | Example |
|-------|-------------|---------|
| `timestamp` | ISO 8601 UTC timestamp | `2026-02-19T14:30:00.123Z` |
| `level` | Log level (see Log Levels section) | `INFO` |
| `message` | Human-readable description | `"Order processed successfully"` |
| `service` | Service or application name | `"order-service"` |
| `correlation_id` | Request/trace identifier (see Correlation IDs) | `"abc-123-def-456"` |

### Recommended Fields

Include these fields when available:

| Field | Description | Example |
|-------|-------------|---------|
| `logger` | Logger name (typically class/module) | `"com.example.OrderService"` |
| `thread` | Thread or goroutine identifier | `"main-worker-3"` |
| `user_id` | Authenticated user identifier (never PII) | `"user-7829"` |
| `request_id` | HTTP request identifier | `"req-abc-123"` |
| `duration_ms` | Operation duration in milliseconds | `142` |
| `error_code` | Application-specific error code | `"ORDER_LIMIT_EXCEEDED"` |
| `stack_trace` | Exception stack trace (ERROR level only) | `"..."` |

### Format Rules

- **Use JSON format for production.** JSON is universally supported by log aggregation tools.
- **Use human-readable format for local development.** Colored, single-line output aids debugging.
- **Never build log messages with string concatenation.** Use parameterized logging to avoid unnecessary string allocation when the log level is disabled.
- **Keep messages concise and descriptive.** The message field should summarize what happened; context goes in structured fields.

```
// Bad: unstructured, string concatenation
log("Processing order " + orderId + " for user " + userId + " took " + duration + "ms")

// Good: structured with parameterized fields
log.info("Order processed",
    "order_id", orderId,
    "user_id", userId,
    "duration_ms", duration)
```

```
// Bad: context buried in message string
log.error("Failed to connect to database at host=db-primary port=5432 after 3 retries")

// Good: context as structured fields
log.error("Database connection failed",
    "host", "db-primary",
    "port", 5432,
    "retries", 3)
```

### Structured Logging Checklist

- [ ] All log entries include the five required fields (timestamp, level, message, service, correlation_id)
- [ ] Production logging uses JSON format
- [ ] Log messages use parameterized logging, not string concatenation
- [ ] Context is passed as structured fields, not embedded in the message string

## Log Levels

### Level Definitions

Use these levels consistently across all services. Each level has a specific purpose -- do not conflate them.

| Level | Purpose | When to Use | Example |
|-------|---------|-------------|---------|
| **ERROR** | Application failure requiring attention | Unhandled exceptions, failed external calls that cannot be retried, data corruption, SLA violations | `"Payment processing failed"` |
| **WARN** | Unexpected condition, but application continues | Deprecated API usage, retry succeeded after failure, approaching resource limits, fallback logic triggered | `"Cache miss, falling back to database"` |
| **INFO** | Significant business or lifecycle events | Request received/completed, service started/stopped, configuration loaded, batch job completed | `"Order created"` |
| **DEBUG** | Diagnostic information for troubleshooting | Method entry/exit, intermediate computation results, cache hit/miss details, query parameters | `"Evaluating discount rules"` |
| **TRACE** | Highly detailed diagnostic information | Wire-level protocol data, full request/response payloads (non-PII), loop iterations | `"HTTP response body received"` |

### Level Selection Rules

- **ERROR means someone needs to act.** If no action is needed, it is not an error -- use WARN or INFO.
- **WARN means something unexpected happened but the system recovered.** If the system did not recover, use ERROR.
- **INFO is for business-relevant events.** A good rule of thumb: if an operator or support engineer would want to see it during normal operation, it belongs at INFO.
- **DEBUG is off in production by default.** Do not rely on DEBUG logs being available. They should aid local development and on-demand troubleshooting.
- **TRACE is rarely enabled.** Use TRACE for data that would be excessive even at DEBUG level.

### Level Anti-Patterns

- **Do not log expected conditions as ERROR or WARN.** A 404 response for a missing resource is not an error -- it is normal application behavior. Log at INFO or DEBUG.
- **Do not log at INFO inside tight loops.** This floods logs and degrades performance. Use DEBUG or TRACE.
- **Do not use ERROR for validation failures.** User input validation failures are expected -- log at INFO or WARN.
- **Do not suppress all logging in libraries.** Libraries should log at DEBUG/TRACE; let the consuming application control the level.

```
// Bad: expected condition logged as error
if (!user.found) {
    log.error("User not found", "user_id", userId)  // This is normal, not an error
}

// Good: expected condition at appropriate level
if (!user.found) {
    log.debug("User not found", "user_id", userId)
}
```

```
// Bad: logging inside a tight loop at INFO
for (item in items) {
    log.info("Processing item", "item_id", item.id)  // Thousands of entries
}

// Good: summary at INFO, detail at DEBUG
log.info("Processing batch", "item_count", items.size)
for (item in items) {
    log.debug("Processing item", "item_id", item.id)
}
```

### Production Log Level Configuration

- **Default production level: INFO.** Services MUST be deployable with INFO and produce useful operational output.
- **Enable DEBUG/TRACE dynamically.** Support runtime log level changes without redeployment (via configuration, feature flags, or admin endpoints).
- **Never deploy with DEBUG/TRACE enabled permanently.** This causes excessive log volume, increased storage costs, and potential PII exposure.

### Log Level Checklist

- [ ] ERROR is reserved for conditions requiring human intervention
- [ ] WARN is used for recovered/recoverable unexpected conditions
- [ ] INFO provides meaningful operational visibility without excessive volume
- [ ] DEBUG/TRACE are disabled in production by default
- [ ] Log levels can be changed at runtime without redeployment

## Correlation IDs

### Purpose

Correlation IDs (also called trace IDs) link log entries across multiple services, threads, or operations for a single logical request. Without correlation IDs, debugging distributed systems requires manual timestamp correlation -- which is error-prone and slow.

### Requirements

- **Every inbound request MUST receive a correlation ID.** Generate one at the system boundary (API gateway, message consumer, scheduled job) if the caller does not provide one.
- **Propagate the correlation ID to all downstream calls.** Pass it via HTTP headers, message metadata, or thread-local/context storage.
- **Include the correlation ID in every log entry.** It MUST appear as a structured field, not embedded in the message string.

### Header Convention

Use a consistent header name for HTTP propagation:

| Header | Usage |
|--------|-------|
| `X-Correlation-ID` | Primary correlation ID header (widely supported) |
| `X-Request-ID` | Alternative; acceptable if used consistently across all services |
| `traceparent` | W3C Trace Context standard; use when integrating with OpenTelemetry or similar |

If your system uses a distributed tracing tool (OpenTelemetry, Jaeger, Zipkin), use the trace ID provided by that tool as your correlation ID. Do not generate a separate one.

### Implementation Pattern

```
// At system boundary (e.g., HTTP middleware)
correlationId = request.header("X-Correlation-ID")
if (correlationId is empty) {
    correlationId = generateUUID()
}
context.set("correlation_id", correlationId)

// In application code -- correlation ID is automatically included
log.info("Order received", "order_id", orderId)
// Output: {"correlation_id": "abc-123", "message": "Order received", "order_id": "12345", ...}

// When calling downstream services
httpClient.setHeader("X-Correlation-ID", context.get("correlation_id"))
```

### Correlation ID Rules

- **Use UUIDs (v4) or equivalent** for generated correlation IDs. They must be unique across the system.
- **Do not reuse correlation IDs across unrelated requests.** Each logical request gets its own ID.
- **Include correlation IDs in error responses.** Return the correlation ID in API error responses so callers can reference it in support requests.
- **Log the correlation ID at the start and end of request processing.** This makes it easy to find the full lifecycle of a request.

### Correlation ID Checklist

- [ ] Every inbound request receives a correlation ID (generated if not provided)
- [ ] Correlation IDs are propagated to all downstream service calls
- [ ] Every log entry includes the correlation ID as a structured field
- [ ] API error responses include the correlation ID
- [ ] A consistent header name is used across all services

## PII Handling in Logs

### Principles

Logs are a liability when they contain Personally Identifiable Information (PII). PII in logs creates compliance risk (GDPR, CCPA, HIPAA), increases breach impact, and complicates data retention.

- **Never log raw PII.** If PII must appear in logs, it MUST be masked or redacted.
- **Prefer identifiers over personal data.** Log `user_id: "u-7829"` instead of `email: "john@example.com"`.
- **Treat PII rules as non-negotiable.** No exception for "just debugging" or "temporary logging."

### PII Classification

Define what constitutes PII for your domain. At minimum, the following are always PII:

| Category | Examples | Log Treatment |
|----------|----------|---------------|
| **Direct identifiers** | Full name, email, phone, SSN, government ID | NEVER log |
| **Financial data** | Credit card numbers, bank accounts, income | NEVER log |
| **Health data** | Medical records, diagnoses, prescriptions | NEVER log |
| **Authentication secrets** | Passwords, tokens, API keys, session IDs | NEVER log |
| **Location data** | Home address, GPS coordinates | NEVER log |
| **Indirect identifiers** | IP address, device fingerprint, user agent | Mask or log with justification |

For a detailed list of what to log and what to never log from a security perspective, see [SECURITY_STANDARDS.md](./SECURITY_STANDARDS.md#logging-and-sensitive-data).

### Masking Strategies

When context requires partial PII for troubleshooting, use consistent masking:

```
// Email: show domain only
"user_email": "***@example.com"

// Phone: show last 4 digits
"phone": "***-***-1234"

// IP address: mask last octet
"client_ip": "192.168.1.***"

// Credit card: show last 4 digits
"card": "****-****-****-5678"

// Name: first initial only
"name": "J***"
```

### Implementation Rules

- **Apply masking at the logging layer, not the application layer.** Use a log sanitizer or custom formatter that intercepts sensitive fields before they are written.
- **Maintain a deny-list of field names** that must always be masked (e.g., `email`, `password`, `ssn`, `credit_card`, `token`).
- **Audit logs periodically** for PII leaks. Automated scanning of log output should be part of CI or operational checks.
- **Set log retention policies.** Logs that could contain indirect identifiers should have defined retention periods. Delete logs when the retention period expires.

### PII Handling Checklist

- [ ] Raw PII is never written to logs
- [ ] User identifiers (not personal data) are used for correlation
- [ ] A PII deny-list of field names is maintained and enforced
- [ ] Masking is applied at the logging layer, not in business logic
- [ ] Log retention policies are defined and enforced
- [ ] Periodic audits check for PII leaks in log output

## Performance Considerations

- **Guard expensive log computations.** If building a log message requires computation (serializing objects, formatting data), check the log level first or use lazy evaluation.
- **Do not log large payloads at INFO.** Full request/response bodies belong at DEBUG or TRACE, and only when they do not contain PII.
- **Use asynchronous logging in high-throughput services.** Synchronous logging can block application threads and degrade latency.
- **Monitor log volume.** Excessive logging increases storage costs and can mask important entries. Set alerts on unexpected log volume spikes.

```
// Bad: expensive operation runs even when DEBUG is disabled
log.debug("Request details", "body", serialize(request))

// Good: guarded evaluation
if (log.isDebugEnabled()) {
    log.debug("Request details", "body", serialize(request))
}
```

---

**Last Updated**: February 19, 2026
**Version**: 1.0 (Initial logging standards -- structured logging, log levels, correlation IDs, PII handling)
