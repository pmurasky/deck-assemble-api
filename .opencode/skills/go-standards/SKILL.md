---
name: go-standards
description: >
  Use when writing, reviewing, or setting up tooling for Go code — Go-specific conventions
  for Go 1.22+: idiomatic Go style, interfaces, error handling, concurrency, context,
  goroutine management, modules, formatting (gofmt/goimports), structured logging (log/slog),
  range-over-func iterators, slices/maps stdlib packages, min/max builtins, testing
  (table-driven, fuzz tests), observability, HTTP best practices, security baseline, and CI quality gates.
triggers:
  - "writing new go code with idiomatic standards"
  - "reviewing a go pull request for idioms"
  - "setting up go module tooling and ci"
  - "debugging go error handling or concurrency issues"
  - "configuring golangci lint or govulncheck"
  - "using log slog or stdlib structured logging in go"
not_for:
  - "non go repositories or other language work"
  - "react frontend or browser related standards"
  - "generic design guidance better handled elsewhere"
---

# Go Standards (Go 1.22+)

## Table of Contents

- [Go Version Policy](#go-version-policy)
- [Project Structure](#project-structure)
- [Naming Conventions](#naming-conventions)
- [Interfaces](#interfaces)
- [Error Handling](#error-handling)
- [Concurrency & Context](#concurrency--context)
- [Security Baseline](#security-baseline)
- [HTTP Best Practices](#http-best-practices)
- [Testing (Table-Driven)](#testing-table-driven)

---

## Use when

- Writing any new Go code
- Reviewing a Go pull request for style and idioms
- Setting up a new Go module or CI pipeline
- Debugging error handling or concurrency issues
- Configuring linting (`golangci-lint`, `govulncheck`)

## Not for

- Non-Go languages or repositories with no Go module/toolchain in scope
- Frontend UI, browser, or React-specific standards work
- Generic cross-language testing or design guidance when a specialized skill fits better

---

## Go Version Policy

Go 1.22+ minimum; Go 1.23+ for range-over-func iterators. Pin in `go.mod`. Run `go mod tidy` before every commit.

## Project Structure

- `cmd/myservice/main.go` — thin entry point
- `internal/` — app, domain, transport/http, data, observability (use aggressively)
- `pkg/` — exported packages (use sparingly)

## Naming Conventions

- Packages: short lowercase, no underscores
- Exported: PascalCase; unexported: camelCase
- Error vars: `Err` prefix (`ErrOrderNotFound`)
- Interfaces: noun or `-er` suffix (`Reader`, `OrderRepository`)

## Interfaces

- Define on the **consumer** side, not the producer side
- Prefer single-method interfaces; compose for larger contracts
- Accept interfaces, return concrete structs

## Stdlib Additions (Go 1.21+)

- `log/slog` for structured logging — no external dep needed
- `min`/`max` builtins, `slices`/`maps` packages
- Range over integers (1.22): `for i := range 10`
- Range over functions / `iter.Seq` (1.23)

## Error Handling

- Always check errors — no naked `_`
- Wrap: `fmt.Errorf("context: %w", err)`; use `errors.Is`/`errors.As`
- Log once at the boundary; wrap (don't log) at intermediate levels
- No `panic` for expected error conditions

## Concurrency & Context

- `context.Context` as first param on all I/O functions
- Honor cancellation: check `ctx.Err()` before expensive work
- Bound goroutines with `errgroup` + semaphore — no unbounded pools

## Security Baseline

- Parameterized SQL only — never string concatenation
- `crypto/rand` for tokens — never `math/rand`
- Never disable TLS verification; never log secrets

## HTTP Best Practices

- Always set `ReadTimeout`, `WriteTimeout`, `IdleTimeout`
- Body size limits: `http.MaxBytesReader`
- Graceful shutdown: handle SIGTERM/SIGINT
- Expose `/health/live` and `/health/ready`

## Testing (Table-Driven)

- `t.Parallel()` for stateless unit tests; `t.Cleanup()` for teardown
- Never `time.Sleep()` — use channels or context timeout
- Fuzz tests for all parsers and input processors
- Inject `Clock` and `rand.Source` via constructor

**Go Test Review Checklist** — watch for these anti-patterns:
- **Happy-path bias**: tests only cover the success path; missing error/edge cases
- **Coincidence**: test passes for wrong reasons (wrong assertion, correct output by accident)
- **Premature mocking**: mocking simple value types instead of testing real behavior

→ [Full test examples and CI commands](./REFERENCE.md)

## CI/CD Quality Gates

`gofmt`, `go test ./...`, `go test -race ./...`, `golangci-lint run`, `govulncheck ./...`, `go build ./...` — all must pass before merge.

## Definition of Done

- [ ] `gofmt -l .` outputs nothing
- [ ] `go mod tidy` run
- [ ] `go build ./...` and `go test ./...` pass
- [ ] `go test -race ./...` passes (no races)
- [ ] `golangci-lint run` and `govulncheck ./...` pass
- [ ] All errors checked; `context.Context` first param on I/O functions
- [ ] All goroutines bounded; server/client timeouts set
- [ ] `crypto/rand` for tokens; no secrets in logs
- [ ] Unit test coverage ≥ 80%
