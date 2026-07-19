# Go Standards — Extended Reference

This companion file contains extended code examples referenced in [SKILL.md](./SKILL.md).

## Table of Contents

- [Table-Driven Test — Full Example](#table-driven-test--full-example)
- [Fuzz Test Example](#fuzz-test-example)
- [CI/CD Quality Gates — Commands and golangci-lint Config](#cicd-quality-gates--commands-and-golangci-lint-config)
- [Go Test Review Checklist](#go-test-review-checklist)

---

## Table-Driven Test — Full Example

```go
func TestOrderService_CreateOrder(t *testing.T) {
    tests := []struct {
        name        string
        request     CreateOrderRequest
        setupMock   func(*MockOrderRepository)
        wantErr     bool
        wantErrType error
    }{
        {
            name:    "should create order when request is valid",
            request: CreateOrderRequest{CustomerName: "Alice", Email: "alice@example.com"},
            setupMock: func(m *MockOrderRepository) {
                m.On("Save", mock.Anything, mock.AnythingOfType("*Order")).Return(nil)
            },
        },
        {
            name:        "should return error when email is empty",
            request:     CreateOrderRequest{CustomerName: "Alice"},
            wantErr:     true,
            wantErrType: ErrInvalidRequest,
        },
    }

    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            t.Parallel()

            repo := &MockOrderRepository{}
            if tt.setupMock != nil {
                tt.setupMock(repo)
            }
            service := NewOrderService(repo, &MockMailer{})

            _, err := service.CreateOrder(context.Background(), tt.request)

            if tt.wantErr {
                require.Error(t, err)
                assert.ErrorIs(t, err, tt.wantErrType)
            } else {
                require.NoError(t, err)
            }
            repo.AssertExpectations(t)
        })
    }
}
```

**Test naming:** `TestServiceName_MethodName_DescriptionOfScenario`

**Key testing rules:**
- `t.Parallel()` for all unit tests that don't share state
- `t.Helper()` in test helper functions
- `t.Cleanup()` for resource teardown
- Never `time.Sleep()` in tests — use channels or context with timeout
- Mock via interfaces defined in production code

---

## Fuzz Test Example

```go
func FuzzParseOrderID(f *testing.F) {
    f.Add("order-123")
    f.Add("")
    f.Fuzz(func(t *testing.T, input string) {
        _, err := ParseOrderID(input)
        if err != nil && !errors.Is(err, ErrInvalidOrderID) {
            t.Errorf("unexpected error type: %v", err)
        }
    })
}
```

Fuzz tests for all parsers and input processors. **Integration tests:** build-tag separated:
```go
//go:build integration
```
Run with: `go test -tags=integration ./...`

---

## CI/CD Quality Gates — Commands and golangci-lint Config

All gates must pass before a PR can merge:

```bash
# 1. Formatting
gofmt -l . | grep . && exit 1 || true

# 2. Unit tests
go test ./...

# 3. Race detector
go test -race ./...

# 4. Linter
golangci-lint run

# 5. Vulnerability scan
govulncheck ./...

# 6. Build
go build ./...
```

**golangci-lint configuration (`.golangci.yml`):**
```yaml
linters:
  enable:
    - errcheck
    - gosimple
    - govet
    - staticcheck
    - gocyclo
    - misspell
    - revive
linters-settings:
  gocyclo:
    min-complexity: 15
```

---

## Go Test Review Checklist

Use this checklist when reviewing or writing Go tests. Each item maps to a concrete signal to look for in the code.

### Anti-Pattern Flags

| Flag | Signal | Fix |
|------|--------|-----|
| **Happy-path bias** | All table cases succeed; no error paths, no empty inputs, no boundary values | Add cases: empty string, zero value, max value, invalid input, dependency returning error |
| **Passing by coincidence** | Fake/stub return value equals the assertion value (e.g., fake returns `""` and test asserts `""`) | Change the fake to a value that differs from the expected; test must go RED before it can be trusted |
| **Premature mocking** | Mock is introduced before verifying the real dependency is hard to use (no DI, no interface) | Define the interface in the consumer package first; try an in-memory fake before reaching for a mock library |
| **Implementation-tied tests** | Test reaches into unexported fields, calls private functions via reflection, or asserts internal state rather than output | Test only through the public API; if internal state must be verified, expose it via a method that belongs on the type |
| **Opaque helper abstractions** | Shared test helper is so general that you must read its body to understand what the test is asserting | Name helpers after the scenario they create (`orderWithExpiredCard()`), not after their structure (`makeOrder()`) |
| **Assertion-free test** | `t.Run` block runs code but has no `assert.*`, `require.*`, or `t.Error*` call, and the test is not explicitly a race-detector-only test | Every test case should assert return values or side effects; the common exception is a test whose explicit purpose is `go test -race`, where the race detector is the assertion |
| **Single happy-path mock** | Mock is set up only for the success path; error cases use zero-value or nil without explicit setup | Add a case where the mock returns an error; verify the service propagates or wraps it correctly |

---

### Concurrency Test Patterns

**Never use `time.Sleep` in tests.** It creates timing-dependent failures. Use these patterns instead.

**Pattern 1 — channel synchronization:**
```go
func TestWorker_ProcessesItemsConcurrently(t *testing.T) {
    done := make(chan struct{})
    worker := NewWorker(func(item string) {
        // signal when work completes
        close(done)
    })

    worker.Submit("item")

    select {
    case <-done:
        // success
    case <-time.After(2 * time.Second):
        t.Fatal("worker did not process item within timeout")
    }
}
```

**Pattern 2 — context deadline instead of sleep:**
```go
func TestPoller_StopsOnContextCancel(t *testing.T) {
    ctx, cancel := context.WithTimeout(context.Background(), 100*time.Millisecond)
    defer cancel()

    poller := NewPoller(10 * time.Millisecond)
    err := poller.Run(ctx)

    if !errors.Is(err, context.DeadlineExceeded) {
        t.Errorf("expected DeadlineExceeded, got %v", err)
    }
}
```

**Pattern 3 — race detector as the assertion:**
```go
// Run with: go test -race ./...
// The race detector is the primary assertion; explicit postcondition checks are optional
func TestCounter_ConcurrentIncrements(t *testing.T) {
    t.Parallel()
    c := NewCounter()
    var wg sync.WaitGroup
    for i := 0; i < 100; i++ {
        wg.Add(1)
        go func() {
            defer wg.Done()
            c.Increment()
        }()
    }
    wg.Wait()
    if c.Value() != 100 {
        t.Errorf("expected 100, got %d", c.Value())
    }
}
```

**Always run `go test -race ./...` in CI.** A test suite that only passes without `-race` is not a complete test suite.

---

### Time and Randomness Injection

Tests that call `time.Now()` directly or `rand.Intn()` are non-deterministic. Inject both as interfaces.

**Clock injection:**
```go
// Production interface — defined in the consumer package
type Clock interface {
    Now() time.Time
}

// Real implementation used in production
type realClock struct{}
func (realClock) Now() time.Time { return time.Now() }

// Constructor accepts Clock
func NewScheduler(clock Clock) *Scheduler {
    return &Scheduler{clock: clock}
}

// In tests — fully deterministic
type fixedClock struct{ t time.Time }
func (c fixedClock) Now() time.Time { return c.t }

func TestScheduler_IsOverdue_WhenDeadlineInPast(t *testing.T) {
    deadline := time.Date(2024, 1, 1, 0, 0, 0, 0, time.UTC)
    clock := fixedClock{t: deadline.Add(time.Hour)} // 1 hour past deadline
    scheduler := NewScheduler(clock)

    if !scheduler.IsOverdue(deadline) {
        t.Error("expected task to be overdue")
    }
}
```

**Random source injection:**
```go
// Production code accepts a rand.Source
func NewIDGenerator(src rand.Source) *IDGenerator {
    return &IDGenerator{rng: rand.New(src)}
}

// In tests — seed with a fixed value for determinism
func TestIDGenerator_ProducesNonEmptyID(t *testing.T) {
    gen := NewIDGenerator(rand.NewSource(42)) // fixed seed
    id := gen.Generate()
    if id == "" {
        t.Error("expected non-empty ID")
    }
}
```

**Rule:** If the code under test calls `time.Now()` or any `rand.*` function directly (not via injection), flag it as non-deterministic and require injection before writing the test.

---

### Confidence Self-Check

Before marking a test as done, verify it is actually testing the right thing:

1. **Delete the assertion** — does the test still pass? If yes, the assertion is doing nothing.
2. **Flip the expected value** — does the test now fail? If not, the assertion is not tight enough.
3. **Delete the production code path** — does the test fail? If not, the test does not cover that path.
4. **Check the fake/stub return value** — is it different from what the assertion checks? If they are the same value, the test may be passing by coincidence.

These four checks take under a minute and catch the most common "false green" patterns in Go test suites.
