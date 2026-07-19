# Go Coding Standards

## Overview
This document outlines Go-specific coding standards that supplement the language-agnostic standards in `CODING_PRACTICES.md` and `CODING_STANDARDS.md`.

We support **Go 1.22+** (stay within N-1 of latest stable; Go 1.23+ preferred for new projects) and leverage modern Go features where appropriate. Choose the version that matches your project.

## Official Style Guide
We follow [Effective Go](https://go.dev/doc/effective_go) and [Go Code Review Comments](https://go.dev/wiki/CodeReviewComments) with the following project-specific additions and clarifications.

## Package Organization (Domain-Driven Design)

Follow domain-driven project structure for services:

```
project/
├── cmd/
│   └── service/
│       └── main.go           # Entry point (keep thin)
├── internal/
│   ├── app/                  # Application wiring, composition root
│   ├── config/               # Configuration load + validation
│   ├── transport/            # HTTP/gRPC handlers + middleware
│   ├── domain/               # Business logic (pure where possible)
│   ├── data/                 # Persistence + external clients
│   └── observability/        # Logging/metrics/tracing setup
├── pkg/                      # Optional: shared libraries for reuse
├── api/                      # OpenAPI/Proto definitions
├── scripts/                  # Build/deployment scripts
└── test/                     # Integration/E2E tests, fixtures
```

**Key Principles:**
- Keep `main.go` thin -- composition only
- Domain layer must not depend on transport or storage packages
- Use `internal/` for non-exported application code
- Package by domain (what it does), not by layer (what it is)
- Each package represents a cohesive functional area

## Naming Conventions

Following Go conventions:
- **Packages**: short, lowercase, no underscores (`auth`, `store`, `httpapi`)
- **Exported identifiers**: PascalCase (`OrderService`, `CalculateTotal`)
- **Unexported identifiers**: camelCase (`orderService`, `calculateTotal`)
- **Receivers**: short and consistent (1-2 chars: `s *Service`, `c *Client`)
- **Constants**: MixedCaps or SCREAMING_SNAKE_CASE for exported (`MaxRetries` or `MAX_RETRIES`)
- **Test files**: `*_test.go` suffix

**Naming anti-patterns to avoid:**
- `*Manager`, `*Handler`, `*Helper`, `*Utility` -- usually SRP violations
- Long package names with underscores
- Stuttering names (avoid `http.HTTPServer`, prefer `http.Server`)

## Go Modules and Dependencies

**MUST:**
- Use Go modules (`go.mod` / `go.sum`)
- No GOPATH workflows
- Pin dependencies with `go.mod`
- Run `go mod tidy` before committing

**Dependency control:**
- Minimize external dependencies
- Prefer standard library where reasonable
- Document reason for new dependencies in commit message
- Review licenses before adding dependencies

**Version Selection Rule (MANDATORY):** When adding or updating any dependency, always select the latest version that has no known vulnerabilities.

1. **Identify the latest published version** on pkg.go.dev.
2. **Scan for vulnerabilities** using `govulncheck ./...` after updating `go.mod`.
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document why in the commit message.
4. **Never pin an older version out of habit** — always start from latest and work backwards only if forced by a CVE.

```bash
# Add the latest version of a module
go get example.com/pkg@latest

# Scan for known vulnerabilities
govulncheck ./...
```

**Commit message example when stepping back from latest:**
```
chore(deps): pin golang.org/x/net to v0.23.0 instead of v0.24.0

v0.24.0 has CVE-2024-XXXXX (high severity, no fix yet).
v0.23.0 is the latest clean version. Revisit when a patched version is released.
```

See [CODING_PRACTICES.md](./CODING_PRACTICES.md#version-selection-rule-mandatory) for the full cross-ecosystem rule and audit tool table.

### Framework vs. Standard Library Decision Guide

When deciding whether to use a framework/library vs. standard library, consider:

**Prefer Standard Library When:**
- ✅ The requirement is simple and well-defined
- ✅ Standard library solution is straightforward (< 100 lines)
- ✅ You only need basic functionality
- ✅ The external tool/binary will always be available (e.g., git on developer machines)
- ✅ Following YAGNI - you don't need advanced features yet

**Consider a Framework/Library When:**
- ⚠️ You need advanced features beyond basic operations
- ⚠️ String parsing becomes complex and error-prone
- ⚠️ Type safety and API contracts are important
- ⚠️ The external tool may not be available in deployment environments
- ⚠️ You're implementing 3+ complex operations from the same domain
- ⚠️ A well-maintained library provides significant value

**Decision Process:**
1. Start with standard library (YAGNI principle)
2. Document the decision in code comments or ADR
3. Note potential frameworks for future consideration
4. Refactor to framework when you "feel the pain" of standard library limitations

**Example: Git Operations**
```go
// Current implementation uses exec.Command("git", ...) for simplicity
// Future consideration: go-git (github.com/go-git/go-git) if we need:
// - Complex git operations (blame, advanced diff parsing, etc.)
// - Environments without git binary
// - Type-safe git object manipulation
// Decision: Standard library sufficient for MVP (only need `git status`)
```

## Formatting and Style

**MUST:**
- All `.go` files must be `gofmt`-clean
- Use `gofmt` or `goimports` to format code automatically

**SHOULD:**
- Use `goimports` to manage imports automatically
- Group imports: stdlib, external, internal
- One import group per category, separated by blank lines

**Example:**
```go
import (
    "context"
    "fmt"
    "time"

    "github.com/google/uuid"
    "go.uber.org/zap"

    "example.com/project/internal/domain"
)
```

## Idiomatic Go

### Interfaces

**Define interfaces where they are used (consumer side):**
```go
// Good: Interface defined by consumer
package storage

type Repository interface {
    Save(ctx context.Context, order Order) error
    Find(ctx context.Context, id string) (Order, error)
}

// Good: Concrete implementation in separate package
package postgres

type OrderRepository struct { /* ... */ }

func (r *OrderRepository) Save(ctx context.Context, order storage.Order) error {
    // implementation
}
```

**Keep interfaces small:**
- Prefer single-method interfaces when possible
- Many small interfaces > one large interface
- The smaller the interface, the more powerful the abstraction

**Prefer: accept interfaces, return concrete structs:**
```go
// Good
func ProcessOrder(repo Repository, order Order) error {
    // ...
}

// Good: Return concrete type
func NewOrderService(db *sql.DB) *OrderService {
    return &OrderService{db: db}
}
```

### Modern Stdlib Additions (Go 1.21+)

#### min / max builtins (Go 1.21)

Use the built-in `min` and `max` instead of `math.Min`/`math.Max` or manual comparisons:

```go
// Before Go 1.21
score := int(math.Min(float64(raw), 100))

// Go 1.21+
score := min(raw, 100)
remaining := max(0, total - used)
```

#### slices and maps packages (Go 1.21)

Prefer stdlib packages over manual loops for common slice/map operations:

```go
import (
    "slices"
    "maps"
)

// Sort a slice
slices.Sort(scores)

// Check if slice contains a value
if slices.Contains(allowed, input) { ... }

// Clone a map
copy := maps.Clone(original)

// Delete keys matching a predicate
maps.DeleteFunc(m, func(k, v string) bool { return v == "" })
```

#### range over integers (Go 1.22)

Range over an integer to loop N times — no more manual `for i := 0; i < n; i++`:

```go
// Before Go 1.22
for i := 0; i < 10; i++ {
    process(i)
}

// Go 1.22+
for i := range 10 {
    process(i)
}
```

#### range over functions / iterators (Go 1.23)

Functions with the signature `func(yield func(V) bool)` can be ranged over:

```go
// Define an iterator
func FilteredItems(items []Item, pred func(Item) bool) iter.Seq[Item] {
    return func(yield func(Item) bool) {
        for _, item := range items {
            if pred(item) && !yield(item) {
                return
            }
        }
    }
}

// Range over it
for item := range FilteredItems(all, isActive) {
    process(item)
}
```

Use the `iter` package types (`iter.Seq[V]`, `iter.Seq2[K, V]`) for iterator signatures.

### Error Handling

**Handle errors explicitly:**
```go
// Good: Explicit error handling
result, err := doSomething()
if err != nil {
    return fmt.Errorf("doing something: %w", err)
}
```

**MUST:**
- Never ignore errors (no naked `_` on error returns without justification)
- Wrap errors at boundaries using `fmt.Errorf("context: %w", err)`
- Use `errors.Is` / `errors.As` for error checking (no string matching)
- Avoid `panic` for expected error conditions (use for programmer errors only)

**Log errors once at the edge:**
```go
// Good: Log at handler level, wrap at intermediate levels
func (h *Handler) CreateOrder(w http.ResponseWriter, r *http.Request) {
    err := h.service.CreateOrder(r.Context(), order)
    if err != nil {
        log.Error("failed to create order", zap.Error(err))
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
}

func (s *Service) CreateOrder(ctx context.Context, order Order) error {
    // Wrap errors with context, but don't log here
    if err := s.repo.Save(ctx, order); err != nil {
        return fmt.Errorf("saving order: %w", err)
    }
    return nil
}
```

### Concurrency and Context

**MUST:**
- Every request path must accept `context.Context` as the first parameter
- Outbound calls must use context-aware APIs and honor cancellation
- Never start goroutines without a stop condition
- Prefer bounded queues / worker pools for load control

**Context best practices:**
```go
// Good: Context as first parameter
func ProcessOrder(ctx context.Context, order Order) error {
    // Pass context to downstream calls
    if err := validate(ctx, order); err != nil {
        return err
    }
    return save(ctx, order)
}

// Good: Respect context cancellation
func fetchData(ctx context.Context) (Data, error) {
    select {
    case <-ctx.Done():
        return Data{}, ctx.Err()
    case result := <-dataChan:
        return result, nil
    }
}
```

**Goroutine management:**
```go
// Bad: Unbounded goroutine leak
func process(items []Item) {
    for _, item := range items {
        go handleItem(item) // Leak: no way to stop
    }
}

// Good: Use worker pool with bounded concurrency
func process(ctx context.Context, items []Item) error {
    sem := make(chan struct{}, 10) // Max 10 concurrent
    g, ctx := errgroup.WithContext(ctx)
    
    for _, item := range items {
        item := item
        g.Go(func() error {
            sem <- struct{}{}
            defer func() { <-sem }()
            return handleItem(ctx, item)
        })
    }
    return g.Wait()
}
```

### Function Design

Apply the same standards from CODING_PRACTICES.md:
- **Maximum 25 lines per function** (excluding blank lines and braces)
- If a function exceeds the limit, extract helper functions
- Use descriptive names that explain what the function does
- Maximum 5 parameters per function (use structs for more)

**Parameter objects for complex inputs:**
```go
// Bad: Too many parameters
func CreateOrder(userID, productID, quantity, discountCode string, priority int, 
                 shippingAddress, billingAddress Address) error {
    // ...
}

// Good: Use parameter object
type CreateOrderRequest struct {
    UserID          string
    ProductID       string
    Quantity        int
    DiscountCode    string
    Priority        int
    ShippingAddress Address
    BillingAddress  Address
}

func CreateOrder(ctx context.Context, req CreateOrderRequest) error {
    // ...
}
```

## Security Baseline

**MUST:**
- Validate inputs at all boundaries (HTTP/gRPC, message queues, CLI)
- SQL queries must use parameterized queries (no string concatenation)
- Use `crypto/rand` for security-sensitive randomness (never `math/rand`)
- Password hashing: use `bcrypt` or `argon2`
- TLS: do not skip certificate verification in production
- Never commit secrets (tokens, private keys, passwords)
- Never print secrets to logs

**Input validation example:**
```go
// Good: Validate at boundary
func (h *Handler) CreateOrder(w http.ResponseWriter, r *http.Request) {
    var req CreateOrderRequest
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }
    
    if err := req.Validate(); err != nil {
        http.Error(w, err.Error(), http.StatusBadRequest)
        return
    }
    
    // Process validated request
}

func (r CreateOrderRequest) Validate() error {
    if r.UserID == "" {
        return errors.New("user_id is required")
    }
    if r.Quantity < 1 {
        return errors.New("quantity must be positive")
    }
    return nil
}
```

**SQL parameterization:**
```go
// Bad: SQL injection risk
query := fmt.Sprintf("SELECT * FROM users WHERE id = '%s'", userID)
rows, err := db.Query(query)

// Good: Parameterized query
rows, err := db.Query("SELECT * FROM users WHERE id = ?", userID)
```

## Method Length Rules

Following CODING_PRACTICES.md:
- **Maximum 25 lines per function/method** (excluding blank lines and braces)
- Exception: Test functions may be longer if needed for clarity
- Exception: Table-driven test data structures don't count toward line limit

**Counting lines:**
```go
// This function has 7 lines (excluding blank lines and braces)
func calculateScore(penalties int) int {
    score := 100 - penalties
    
    if score < 0 {
        return 0
    }
    
    return score
}
```

## Production Service Requirements

**For services deployed to production, MUST implement:**
- Graceful shutdown (handle SIGTERM/SIGINT)
- Timeouts on all inbound and outbound requests
- Structured logging with context
- Metrics (request count, latency, errors)
- Tracing hooks (OpenTelemetry preferred)
- Health endpoints (liveness/readiness)

**Graceful shutdown example:**
```go
func main() {
    srv := &http.Server{Addr: ":8080"}
    
    go func() {
        if err := srv.ListenAndServe(); err != http.ErrServerClosed {
            log.Fatal(err)
        }
    }()
    
    // Wait for interrupt signal
    quit := make(chan os.Signal, 1)
    signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
    <-quit
    
    // Graceful shutdown with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
    defer cancel()
    
    if err := srv.Shutdown(ctx); err != nil {
        log.Fatal("Server forced to shutdown:", err)
    }
}
```

**Health endpoints:**
```go
// Liveness: Is the service running?
func handleLiveness(w http.ResponseWriter, r *http.Request) {
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("OK"))
}

// Readiness: Can the service handle requests?
func handleReadiness(w http.ResponseWriter, r *http.Request) {
    // Check dependencies (database, external APIs, etc.)
    if err := checkDependencies(); err != nil {
        w.WriteHeader(http.StatusServiceUnavailable)
        return
    }
    w.WriteHeader(http.StatusOK)
    w.Write([]byte("Ready"))
}
```

## Observability (Required for Services)

Production services MUST implement comprehensive observability:

### Logging

**MUST:**
- Use structured logging (JSON) in production
- Include context: timestamp, level, service name, version, request/trace ID, message
- Do not log sensitive data (passwords, tokens, PII)
- Use appropriate log levels (see CODING_PRACTICES.md)

**Recommended libraries (in order of preference):**
- `log/slog` — stdlib structured logging (Go 1.21+). **Use this for all new projects.** No external dependency, JSON output, context-aware.
- `go.uber.org/zap` — when you need the absolute highest throughput (benchmarked bottleneck)
- `github.com/rs/zerolog` — alternative zero-allocation logger

### log/slog (Stdlib — Preferred)

```go
import "log/slog"

func main() {
    // JSON handler for production
    logger := slog.New(slog.NewJSONHandler(os.Stdout, &slog.HandlerOptions{
        Level: slog.LevelInfo,
    }))
    slog.SetDefault(logger)

    // Structured log with attributes
    slog.Info("service started",
        slog.String("version", version),
        slog.Int("port", port),
    )

    slog.Error("request failed",
        slog.String("method", r.Method),
        slog.String("path", r.URL.Path),
        slog.Any("error", err),
    )
}

// Pass logger via context for request-scoped logging
func ProcessOrder(ctx context.Context, order Order) error {
    logger := slog.With(
        slog.String("order_id", order.ID),
        slog.String("user_id", order.UserID),
    )
    logger.Info("processing order")
    // ...
}
```

**slog guidelines:**
- Use `slog.NewJSONHandler` in production, `slog.NewTextHandler` in development
- Use `slog.With(...)` to create child loggers with persistent attributes (e.g., request ID)
- Pass logger through `context.Context` using a wrapper; avoid package-level mutable loggers
- Log levels: `Debug`, `Info`, `Warn`, `Error` — map to the same semantics as other languages

**Example with zap (when throughput requires it):**
```go
import "go.uber.org/zap"

func main() {
    logger, _ := zap.NewProduction()
    defer logger.Sync()
    
    logger.Info("service started",
        zap.String("version", version),
        zap.String("environment", env),
        zap.Int("port", port),
    )
}

func (h *Handler) CreateOrder(w http.ResponseWriter, r *http.Request) {
    logger := h.logger.With(
        zap.String("request_id", requestID),
        zap.String("user_id", userID),
    )
    
    logger.Info("creating order",
        zap.String("order_id", orderID),
    )
    
    if err := h.service.CreateOrder(ctx, order); err != nil {
        logger.Error("failed to create order",
            zap.Error(err),
        )
        return
    }
    
    logger.Info("order created successfully")
}
```

### Metrics

**Minimum metrics required:**
- Request count by route and status code
- Request latency histogram
- In-flight requests gauge
- Dependency call latency and errors
- Go runtime metrics (goroutines, heap, GC)

**Recommended libraries:**
- Prometheus client (`github.com/prometheus/client_golang`)
- OpenTelemetry metrics

**Example:**
```go
import (
    "github.com/prometheus/client_golang/prometheus"
    "github.com/prometheus/client_golang/prometheus/promauto"
)

var (
    requestsTotal = promauto.NewCounterVec(
        prometheus.CounterOpts{
            Name: "http_requests_total",
            Help: "Total number of HTTP requests",
        },
        []string{"method", "path", "status"},
    )
    
    requestDuration = promauto.NewHistogramVec(
        prometheus.HistogramOpts{
            Name:    "http_request_duration_seconds",
            Help:    "HTTP request latency",
            Buckets: prometheus.DefBuckets,
        },
        []string{"method", "path"},
    )
)

func metricsMiddleware(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        start := time.Now()
        
        wrapped := &responseWriter{ResponseWriter: w, statusCode: http.StatusOK}
        next.ServeHTTP(wrapped, r)
        
        duration := time.Since(start).Seconds()
        requestDuration.WithLabelValues(r.Method, r.URL.Path).Observe(duration)
        requestsTotal.WithLabelValues(r.Method, r.URL.Path, 
            strconv.Itoa(wrapped.statusCode)).Inc()
    })
}
```

### Tracing

**OpenTelemetry is the preferred tracing standard:**

```go
import (
    "go.opentelemetry.io/otel"
    "go.opentelemetry.io/otel/trace"
)

var tracer = otel.Tracer("order-service")

func (s *Service) CreateOrder(ctx context.Context, order Order) error {
    ctx, span := tracer.Start(ctx, "CreateOrder")
    defer span.End()
    
    span.SetAttributes(
        attribute.String("order.id", order.ID),
        attribute.String("user.id", order.UserID),
    )
    
    // Pass context to downstream calls for trace propagation
    if err := s.repo.Save(ctx, order); err != nil {
        span.RecordError(err)
        return err
    }
    
    return nil
}
```

**MUST:**
- Propagate trace context through `context.Context`
- Instrument inbound requests and outbound dependencies
- Record errors and important attributes

## HTTP/API Practices

For HTTP services, follow these practices:

### Timeouts

**MUST enforce timeouts on both servers and clients:**

```go
// Server with timeouts
srv := &http.Server{
    Addr:         ":8080",
    ReadTimeout:  10 * time.Second,
    WriteTimeout: 30 * time.Second,
    IdleTimeout:  120 * time.Second,
}

// Client with timeout
client := &http.Client{
    Timeout: 30 * time.Second,
    Transport: &http.Transport{
        DialContext: (&net.Dialer{
            Timeout: 5 * time.Second,
        }).DialContext,
        TLSHandshakeTimeout:   10 * time.Second,
        ResponseHeaderTimeout: 10 * time.Second,
    },
}
```

### Request Body Limits

**Limit request body sizes to prevent memory exhaustion:**

```go
func limitBodySize(next http.Handler) http.Handler {
    return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1 MB limit
        next.ServeHTTP(w, r)
    })
}
```

### Error Responses

**Return consistent error schema:**

```go
type ErrorResponse struct {
    Error   string `json:"error"`
    Message string `json:"message"`
    Code    string `json:"code,omitempty"`
}

func writeError(w http.ResponseWriter, status int, message string) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    json.NewEncoder(w).Encode(ErrorResponse{
        Error:   http.StatusText(status),
        Message: message,
    })
}
```

### Middleware Ordering

**Recommended middleware order (outermost to innermost):**
1. Request ID / tracing
2. Logging
3. Recovery (panic handling)
4. Metrics
5. Authentication / Authorization
6. Rate limiting
7. Request validation
8. Business handlers

```go
func setupMiddleware(handler http.Handler) http.Handler {
    handler = recoveryMiddleware(handler)
    handler = metricsMiddleware(handler)
    handler = loggingMiddleware(handler)
    handler = requestIDMiddleware(handler)
    return handler
}
```

## Testing Standards

Following CODING_PRACTICES.md, apply these Go-specific testing practices:

### Test Coverage Requirements

- **Minimum 80% unit test coverage** overall (unit tests only)
- **100% coverage for critical paths** (business logic, security, financial)
- **Branch coverage**: Minimum 75% (unit tests only)
- Integration/E2E tests do NOT count toward coverage thresholds

**Run coverage:**
```bash
go test ./... -cover
go test ./... -coverprofile=coverage.out
go tool cover -html=coverage.out
```

### Test File Organization

**Test files:**
- Place test files in the same package as the code under test
- Name test files with `_test.go` suffix
- Use package name + `_test` for black-box tests (tests external API only)

```go
// whitebox_test.go - tests internal implementation
package order

import "testing"

func TestCreateOrder(t *testing.T) {
    // Can access unexported functions and fields
}
```

```go
// blackbox_test.go - tests public API only
package order_test

import (
    "testing"
    "example.com/project/internal/order"
)

func TestOrderService(t *testing.T) {
    // Only access exported API
}
```

### Table-Driven Tests

**Prefer table-driven tests for multi-case logic:**

```go
func TestCalculateScore(t *testing.T) {
    tests := []struct {
        name     string
        input    int
        expected int
    }{
        {"zero penalties", 0, 100},
        {"some penalties", 20, 80},
        {"max penalties", 100, 0},
        {"over max", 150, 0},
    }
    
    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            got := calculateScore(tt.input)
            if got != tt.expected {
                t.Errorf("calculateScore(%d) = %d, want %d", 
                    tt.input, got, tt.expected)
            }
        })
    }
}
```

### Test Helpers

**Use `t.Helper()` for test helper functions:**

```go
func assertNoError(t *testing.T, err error) {
    t.Helper()
    if err != nil {
        t.Fatalf("unexpected error: %v", err)
    }
}

func TestOrderCreation(t *testing.T) {
    order, err := CreateOrder(validRequest)
    assertNoError(t, err) // Will report correct line number in test
}
```

### Fuzz Testing

**Add fuzz tests for:**
- Parsers and decoders
- Input validation and sanitization
- Security-sensitive transformations
- Data encoding/decoding

```go
func FuzzParseOrder(f *testing.F) {
    // Seed corpus
    f.Add([]byte(`{"id":"123","total":100}`))
    f.Add([]byte(`{"id":"","total":-1}`))
    
    f.Fuzz(func(t *testing.T, data []byte) {
        order, err := ParseOrder(data)
        if err != nil {
            return // Invalid input is okay
        }
        
        // Properties that must hold for valid orders
        if order.Total < 0 {
            t.Errorf("negative total not allowed: %d", order.Total)
        }
        if order.ID == "" {
            t.Errorf("empty ID not allowed")
        }
    })
}
```

**Run fuzz tests:**
```bash
go test -fuzz=FuzzParseOrder -fuzztime=30s
```

### Test Isolation

**Tests must be independent:**
- No shared mutable state between tests
- Each test should be able to run alone or with others
- Use `t.Parallel()` for parallel-safe tests
- Clean up resources in `t.Cleanup()`

```go
func TestOrderService(t *testing.T) {
    t.Parallel() // Run in parallel with other tests
    
    db := setupTestDB(t)
    t.Cleanup(func() {
        db.Close()
    })
    
    // Test logic
}
```

### Mocking and Test Doubles

**Prefer interfaces for testability:**

```go
// Production code defines interface
type OrderRepository interface {
    Save(ctx context.Context, order Order) error
    Find(ctx context.Context, id string) (Order, error)
}

// Test code provides mock
type mockOrderRepository struct {
    orders map[string]Order
}

func (m *mockOrderRepository) Save(ctx context.Context, order Order) error {
    m.orders[order.ID] = order
    return nil
}

func TestOrderService(t *testing.T) {
    repo := &mockOrderRepository{orders: make(map[string]Order)}
    service := NewOrderService(repo)
    
    // Test service behavior
}
```

**Mocking libraries (optional):**
- `github.com/stretchr/testify/mock`
- `github.com/golang/mock/gomock`

### testify — Key Patterns

testify is the standard assertion library for Go (25.9k GitHub stars, used by HashiCorp, Docker, and most major Go projects). It has two sub-packages with different failure behavior — understanding the distinction is the most important thing to know.

**`assert` vs `require` — the critical distinction:**
```go
// assert — continues after failure (like soft assertions)
// Use when failures are independent
assert.Equal(t, expected, actual)
assert.NoError(t, err)

// require — stops the test immediately on failure
// Use when subsequent code depends on this result
user, err := repo.FindByID(ctx, 42)
require.NoError(t, err)         // stop here if err != nil — user will be nil
require.NotNil(t, user)         // stop here if user is nil — next line will panic
assert.Equal(t, "Alice", user.Name)
```

**Key assertion methods:**
```go
assert.Equal(t, expected, actual)
assert.NotEqual(t, unexpected, actual)
assert.Nil(t, value)
assert.NotNil(t, value)
assert.True(t, condition)
assert.False(t, condition)
assert.ErrorIs(t, err, target)
assert.ElementsMatch(t, expected, actual)  // order-independent slice equality
assert.JSONEq(t, expectedJSON, actualJSON)
assert.Contains(t, collection, element)
```

**Object style** — avoid passing `t` to every call:
```go
func TestUserService(t *testing.T) {
    assert := assert.New(t)
    require := require.New(t)

    user, err := service.Create(ctx, input)
    require.NoError(err)
    assert.Equal("Alice", user.Name)
    assert.Equal("alice@example.com", user.Email)
}
```

**Custom failure messages:**
```go
assert.Equal(t, expected, actual, "invoice total should reflect applied discount")
```

> **Ginkgo/Gomega:** Ginkgo/Gomega is preferred for BDD-style e2e suites and Kubernetes operator development. The Kubernetes project uses testify for unit tests and Ginkgo/Gomega for e2e. For most Go projects, testify is the right default.

### Determinism

**MUST:**
- Tests must be deterministic (no flakiness)
- Avoid `time.Sleep()` for timing -- use channels or context
- Mock time-dependent behavior
- Seed randomness for reproducibility

```go
// Bad: Flaky test
func TestAsync(t *testing.T) {
    go processAsync()
    time.Sleep(100 * time.Millisecond) // May not be enough
    // assert result
}

// Good: Use synchronization
func TestAsync(t *testing.T) {
    done := make(chan struct{})
    go func() {
        processAsync()
        close(done)
    }()
    
    select {
    case <-done:
        // assert result
    case <-time.After(5 * time.Second):
        t.Fatal("timeout waiting for async operation")
    }
}
```

### Test Naming

**Use descriptive test names:**

```go
// Good: Describes what is being tested
func TestOrderService_CreateOrder_ReturnsErrorWhenUserNotFound(t *testing.T) {}
func TestCalculateScore_ReturnsZeroWhenPenaltiesExceedMaximum(t *testing.T) {}

// Bad: Generic names
func TestOrder1(t *testing.T) {}
func TestCreate(t *testing.T) {}
```

### Integration Tests

**Place integration tests in `test/` directory or use build tags:**

```go
//go:build integration
// +build integration

package test

func TestDatabaseIntegration(t *testing.T) {
    // Integration test that requires real database
}
```

**Run integration tests separately:**
```bash
go test ./... -tags=integration
```

## Build and Tooling

### Required Quality Gates (CI/CD)

**All PRs MUST pass these gates before merging:**

1. **Formatting**: `gofmt -l .` (no output = pass)
2. **Unit tests**: `go test ./...` (all pass)
3. **Race detector**: `go test -race ./...` (default on; may be tiered by repo size)
4. **Linting**: `golangci-lint run` (no errors)
5. **Vulnerability scanning**: `govulncheck ./...` (no unpatched vulnerabilities)
6. **Build**: Build artifacts succeed

### Formatting

**MUST:**
```bash
# Check formatting
gofmt -l .

# Format all files
gofmt -w .

# Or use goimports (includes import management)
goimports -w .
```

**In CI:**
```bash
# Fail if any files need formatting
test -z "$(gofmt -l .)"
```

### Linting

**Use `golangci-lint` with recommended linters:**

```yaml
# .golangci.yml
linters:
  enable:
    - errcheck        # Check error returns
    - gosimple        # Simplify code
    - govet           # Go vet checks
    - ineffassign     # Detect ineffectual assignments
    - staticcheck     # Advanced static analysis
    - unused          # Unused code
    - gocyclo         # Cyclomatic complexity
    - gofmt           # Format check
    - misspell        # Spell check
    - goconst         # Repeated strings that could be constants
    - revive          # Replacement for golint

linters-settings:
  gocyclo:
    min-complexity: 15
  govet:
    check-shadowing: true

run:
  timeout: 5m
  tests: true
```

**Run linting:**
```bash
golangci-lint run
golangci-lint run --fix  # Auto-fix issues
```

### Vulnerability Scanning

**Use `govulncheck` to scan for known vulnerabilities:**

```bash
# Install govulncheck
go install golang.org/x/vuln/cmd/govulncheck@latest

# Scan for vulnerabilities
govulncheck ./...
```

**MUST:**
- Run `govulncheck` in CI before merging
- Address or document exceptions for any vulnerabilities
- Gate releases on clean vulnerability scans

### Race Detection

**Run tests with race detector:**

```bash
# Run all tests with race detector
go test -race ./...

# Run specific package
go test -race ./internal/order
```

**MUST:**
- Run race detector in CI (default on)
- Fix all race conditions before merging
- May tier by repo size for very large codebases

### Build Verification

**Ensure build artifacts succeed:**

```bash
# Build all commands
go build ./cmd/...

# Build specific binary
go build -o bin/service ./cmd/service

# Build with version info
go build -ldflags "-X main.version=$(git describe --tags)" -o bin/service ./cmd/service
```

### Module Management

**Keep dependencies tidy:**

```bash
# Add missing dependencies and remove unused ones
go mod tidy

# Verify dependencies
go mod verify

# Vendor dependencies (optional)
go mod vendor
```

**Before committing:**
```bash
go mod tidy
git add go.mod go.sum
```

### Pre-Commit Checklist

**Before every commit, verify:**
```bash
# Format code
gofmt -w .

# Tidy dependencies
go mod tidy

# Run tests
go test ./...

# Run linter
golangci-lint run

# Build
go build ./...
```

**Automated pre-commit hook:**
```bash
#!/bin/sh
# .git/hooks/pre-commit

set -e

echo "Running pre-commit checks..."

# Format check
if [ -n "$(gofmt -l .)" ]; then
    echo "Code not formatted. Run: gofmt -w ."
    exit 1
fi

# Tests
go test ./...

# Linter
golangci-lint run

echo "✅ Pre-commit checks passed"
```

## AI-Specific Anti-Patterns

When working with AI coding agents, avoid these common mistakes:

**DO NOT:**
- Create giant PRs with mixed concerns -- use micro-commits
- Add scope creep ("while we're here" refactors) -- stay focused
- Create new abstractions without need -- YAGNI principle
- Add dependencies for trivial helpers -- use stdlib first
- Log raw requests, PII, or secrets -- sanitize sensitive data
- Create unbounded goroutines or channels -- use worker pools
- Match errors with strings -- use `errors.Is` / `errors.As`
- Skip error handling -- always check errors
- Ignore test failures -- all tests must pass before committing

**DO:**
- Make small, focused commits (one logical change per commit)
- Follow the TDD micro-commit workflow (RED → GREEN → COMMIT → REFACTOR → COMMIT)
- Keep functions under 25 lines
- Use table-driven tests
- Add fuzz tests for input validation
- Run all quality gates before committing
- Document decisions in commit messages

## Definition of Done

A change is considered **done** when:

- [ ] Code compiles cleanly (`go build ./...`)
- [ ] All tests pass (`go test ./...`)
- [ ] Race detector passes (`go test -race ./...`)
- [ ] Code is formatted (`gofmt -l .` returns nothing)
- [ ] Linter passes (`golangci-lint run`)
- [ ] Vulnerability scan passes (`govulncheck ./...`)
- [ ] Test coverage meets requirements (80% unit tests minimum)
- [ ] Observability maintained (logging, metrics, tracing)
- [ ] Documentation updated (README, godoc comments)
- [ ] Commit message follows conventional commits format
- [ ] Human review completed (if applicable)

## References

For deeper understanding and updates, consult:
- [Effective Go](https://go.dev/doc/effective_go) (official)
- [Go Code Review Comments](https://go.dev/wiki/CodeReviewComments) (official)
- [Uber Go Style Guide](https://github.com/uber-go/guide/blob/master/style.md)
- [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices-quick-reference-guide/)
- [OpenTelemetry Go Documentation](https://opentelemetry.io/docs/languages/go/)
- [Go Proverbs](https://go-proverbs.github.io/)

---

**Last Updated**: 2026-04-30  
**Version**: 1.1
