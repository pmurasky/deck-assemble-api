# Testing Standards

## Overview

This is the authoritative reference for testing standards across all languages and projects. It covers the full testing lifecycle — from test pyramid strategy through mutation testing, flaky test policy, and test execution tiers.

For language-specific testing tooling (pytest, JUnit 5, Vitest, Go testing package), see the relevant language standards doc. For general coding practices and TDD workflow, see [CODING_PRACTICES.md](./CODING_PRACTICES.md) and [AI_AGENT_WORKFLOW.md](./AI_AGENT_WORKFLOW.md).

---

## Table of Contents

1. [Test Pyramid](#1-test-pyramid)
2. [Coverage Requirements](#2-coverage-requirements)
3. [Test Doubles Taxonomy](#3-test-doubles-taxonomy)
4. [Test Naming and Structure](#4-test-naming-and-structure)
5. [Parameterized and Table-Driven Tests](#5-parameterized-and-table-driven-tests)
6. [Test Data Management](#6-test-data-management)
7. [Serialization Round-Trip Tests](#7-serialization-round-trip-tests)
8. [Mutation Testing](#8-mutation-testing)
9. [Property-Based Testing](#9-property-based-testing)
10. [Contract Testing](#10-contract-testing)
11. [Snapshot Testing](#11-snapshot-testing)
12. [Flaky Test Policy](#12-flaky-test-policy)
13. [Test Execution Tiers](#13-test-execution-tiers)

---

## 1. Test Pyramid

The test pyramid defines the ideal ratio of test types. More unit tests, fewer E2E tests — not because E2E tests are bad, but because they are slower, more expensive to maintain, and harder to diagnose.

```
        /\
       /E2E\          ← few, slow, expensive, high confidence
      /------\
     /  Integ  \      ← moderate, catch cross-component issues
    /------------\
   /  Unit Tests  \   ← many, fast, isolated, deterministic
  /----------------\
```

### Recommended Ratios

| Layer | Proportion | Characteristics |
|-------|-----------|-----------------|
| Unit | ~70% | < 1 second each, no I/O, fully isolated |
| Integration | ~20% | May use real DB/file system, test component wiring |
| E2E / Acceptance | ~10% | Full stack, real dependencies, slowest |

### When to Prefer Each Layer

**Write a unit test when:**
- Testing a single class or function in isolation
- Testing business logic, algorithms, or domain rules
- Testing error paths and edge cases
- You can mock all external dependencies cleanly

**Write an integration test when:**
- Testing that two components work together (e.g., service + repository)
- Verifying database queries return correct results
- Testing HTTP client/server interactions with a test server
- Verifying serialization/deserialization of complex real payloads (e.g., full API responses against a test server)
- **Note**: Simple round-trip serialization tests (serialize → deserialize → equals original) are **unit tests**, not integration tests. See [Serialization Round-Trip Tests](#7-serialization-round-trip-tests) below

**Write an E2E test when:**
- Verifying a complete user-facing workflow
- Testing system behavior against a deployed environment
- Validating acceptance criteria for a feature
- Covering critical paths that span multiple services

### Anti-Pattern: The Ice Cream Cone

The **ice cream cone** (inverted pyramid) is the failure mode — too many E2E tests, too few unit tests. Symptoms:

- Test suite takes > 30 minutes to run
- A single code change breaks 50+ tests
- Tests fail for unrelated infrastructure reasons
- Developers skip running tests locally because they're too slow

---

## 2. Coverage Requirements

**Coverage is calculated from unit tests only.** Integration and E2E tests are valuable but do NOT count toward coverage thresholds. This ensures core logic is verified in fast, isolated, deterministic tests.

| Tier | Threshold | Applies To |
|------|-----------|------------|
| Overall line coverage | 80% minimum | All production code |
| Branch coverage | 75% minimum | All production code |
| Critical paths | 100% | Business logic, auth/authz, financial calculations, data pipelines |

### What Counts as a Critical Path

- Authentication and authorization flows
- Payment or financial calculations
- Data transformation and validation pipelines
- Core domain rules and business logic
- Security-sensitive operations

### Coverage Is a Floor, Not a Goal

Hitting 80% does not mean your tests are good. 80% coverage with tests that only verify happy paths and never assert anything meaningful is worse than useful — it creates false confidence. Coverage thresholds exist to catch **gaps**, not to certify quality.

### Lesson from Conversion Experiments

Unit test volume alone does not guarantee behavioral correctness. A conversion with 605 unit tests and 261/261 mutants killed (100% mutation score) still missed behavioral categories that were caught by a broader acceptance test suite. Always pair high unit test coverage with comprehensive acceptance tests covering all documented behavioral categories.
---

## 3. Test Doubles Taxonomy

The word "mock" is used informally to mean many different things. Precision matters — using the wrong type of test double leads to brittle tests or tests that give false confidence.

### Definitions

| Type | Definition | Use When |
|------|-----------|----------|
| **Dummy** | Passed in but never used | Satisfying a required parameter you don't care about |
| **Stub** | Returns pre-configured responses, no verification | Controlling indirect inputs (e.g., what a repo returns) |
| **Fake** | Working implementation with shortcuts (e.g., in-memory DB) | Integration-style tests without real infrastructure |
| **Spy** | Real object that also records calls | Verifying side effects while preserving real behavior |
| **Mock** | Pre-configured with expectations, verified at end | Verifying that specific interactions happened |

### Java / Kotlin Examples (Mockito)

```java
// Stub — controls what the dependency returns
UserRepository stub = Mockito.mock(UserRepository.class);
when(stub.findById(42L)).thenReturn(Optional.of(testUser));

// Mock — verifies the interaction happened
EmailService mock = Mockito.mock(EmailService.class);
service.registerUser(userData);
verify(mock).sendWelcomeEmail(testUser);

// Spy — real object, but you can verify/override calls
UserService spy = Mockito.spy(new UserService(repo));
doReturn(cachedUser).when(spy).findUser(42L);
```

```kotlin
// Prefer fakes over mocks for repositories
class InMemoryUserRepository : UserRepository {
    private val store = mutableMapOf<Long, User>()
    override fun save(user: User) { store[user.id] = user }
    override fun findById(id: Long) = store[id]
}
```

### Python Examples (unittest.mock)

```python
from unittest.mock import MagicMock, patch, call

# Stub — controls return value
repo = MagicMock()
repo.find_by_id.return_value = test_user

# Mock — verify interaction
email_service = MagicMock()
service.register_user(user_data)
email_service.send_welcome_email.assert_called_once_with(test_user)

# Fake — in-memory implementation
class InMemoryUserRepository:
    def __init__(self):
        self._store = {}

    def save(self, user):
        self._store[user.id] = user

    def find_by_id(self, user_id):
        return self._store.get(user_id)
```

### Guidelines

- **Prefer fakes over mocks** for repositories and data stores — fakes are more realistic and less brittle
- **Mock at the boundary** — mock HTTP clients, external APIs, message queues; not internal collaborators
- **Never mock value objects** — if it has no behavior, don't mock it
- **Verify interactions sparingly** — over-verification ties tests to implementation details
- **One mock per test** — if you need many mocks, the class under test likely has too many dependencies (SRP violation)

---

## 4. Test Naming and Structure

### Naming Convention

Use descriptive names that express the **scenario and expected outcome**, not the method name.

**Format:** `should<ExpectedOutcome>When<Condition>`

```
✅ shouldReturnEmptyListWhenNoUsersExist
✅ shouldThrowValidationExceptionWhenEmailIsInvalid
✅ shouldDeductPenaltyPointsForEachCriticalViolation
✅ shouldSelectLatestNonVulnerableVersionWhenMultipleExist

❌ test1
❌ testVersionSelection
❌ userServiceTest
❌ itWorks
```

### Given-When-Then (GWT) and Arrange-Act-Assert (AAA)

GWT and AAA are two names for the same four-phase test structure:

| Phase | GWT label | AAA label | Purpose |
|-------|-----------|-----------|---------|
| 1 | **Given** | **Arrange** | Set up inputs, collaborators, and preconditions |
| 2 | **When** | **Act** | Execute the single action under test |
| 3 | **Then** | **Assert** | Verify the observable outcome |
| 4 | *(teardown)* | *(teardown)* | Release resources (often handled by `@AfterEach`) |

GWT is the BDD name (common in Cucumber/Gherkin and behavior-driven conversations). AAA is the unit-test name (common in xUnit literature). They are interchangeable — pick one label style per codebase and be consistent.

```java
@Test
void shouldFilterOutVulnerableVersions() {
    // Given
    var versions = List.of(
        new Version("2.15.4", false),
        new Version("2.16.0", true)   // vulnerable
    );

    // When
    var result = selector.selectSafe(versions);

    // Then
    assertThat(result).containsExactly(new Version("2.15.4", false));
}
```

### How Many Assertions Per Test

**Rule: one logical behavior per test, not one physical `assert` line.**

Multiple `assertThat()` calls are correct and expected when they all verify the same observable outcome. Splitting them into separate tests adds noise without adding safety.

```java
// ✅ Two assert lines — one logical behavior (a fund transfer mutates both sides)
@Test
void shouldTransferFundsAndUpdateBothBalances() {
    // Given
    var sender    = new Account("A", Money.of(100));
    var recipient = new Account("B", Money.of(0));

    // When
    transferService.transfer(sender, recipient, Money.of(40));

    // Then
    assertThat(sender.balance()).isEqualTo(Money.of(60));
    assertThat(recipient.balance()).isEqualTo(Money.of(40));
}

// ❌ Two behaviors bundled — split into two tests
@Test
void shouldShowPurchaseConfirmationAndLowBalanceWarning() {
    // When
    var result = checkout.complete(order);

    // Then — these are separate behaviors; the test name needs "and" as a warning sign
    assertThat(result.messages()).contains("Purchase confirmed");
    assertThat(result.warnings()).contains("Low balance");
}
```

**Warning sign:** if you need the word "and" in the test name (e.g., `shouldDoXAndY`), the test is verifying two distinct behaviors — split it.

#### Assertion Roulette Anti-Pattern

**Assertion Roulette** occurs when a test has many unrelated assertions with no descriptive context, so when one fails you cannot tell which behavior broke.

```java
// ❌ Assertion Roulette — which assertion failed, and why?
assertThat(order.status()).isEqualTo(CONFIRMED);
assertThat(order.total()).isEqualTo(Money.of(99.99));
assertThat(emailService.sentCount()).isEqualTo(1);
assertThat(inventory.reserved("SKU-42")).isTrue();
```

Mitigate with:
- **Descriptive labels** — use `.as("description")` (AssertJ) on each assertion so failure messages identify the broken behavior
- **Soft assertions** for multi-property checks (see next section)
- **Test splitting** when assertions verify truly independent behaviors

```java
// ✅ Labeled assertions — failure message names the broken expectation
assertThat(order.status()).as("order status after confirmation").isEqualTo(CONFIRMED);
assertThat(order.total()).as("order total with tax").isEqualTo(Money.of(99.99));
```

### Soft Assertions vs. Fail-Fast

**Fail-fast (default):** the test stops at the first failing assertion.

**Soft assertions:** all assertions are collected and reported together, even if earlier ones fail.

#### When to use fail-fast (default)

Use fail-fast when later assertions depend on an earlier result being valid:

```java
// If result is null the second assertThat would throw NullPointerException — stop immediately
assertThat(result).isNotNull();
assertThat(result.id()).isEqualTo(expectedId);
```

#### When to use soft assertions

Use soft assertions when verifying multiple **independent** properties of the same result object, so a single test run reveals all failures at once:

```java
// ✅ Soft assertions — all field failures reported in one run
@Test
void shouldPopulateAllOrderFields() {
    // Given / When
    var order = factory.create(request);

    // Then
    assertSoftly(softly -> {                              // AssertJ
        softly.assertThat(order.id()).isNotNull();
        softly.assertThat(order.status()).isEqualTo(PENDING);
        softly.assertThat(order.total()).isEqualTo(Money.of(49.99));
        softly.assertThat(order.createdAt()).isNotNull();
    });
}
```

| Scenario | Use |
|----------|-----|
| Later assertions depend on earlier results (null checks, prerequisite state) | **Fail-fast** |
| Verifying all fields of a response/domain object | **Soft assertions** |
| Verifying an exception is thrown | **Fail-fast** (`assertThatThrownBy`) |
| Validating multiple independent business rules in one fixture setup | **Soft assertions** |

> **Language support:** AssertJ `assertSoftly` / `SoftAssertions` (Java), Kotest `assertSoftly` (Kotlin). See the language standards docs for any additional, language-specific soft assertion tooling.

### Fluent Assertion Libraries by Language

| Language | Library | Entry Point | Soft Assertions |
|----------|---------|-------------|-----------------|
| Java | [AssertJ](https://assertj.github.io/doc/) | `assertThat(actual)` | `assertSoftly(softly -> { … })` |
| Kotlin | [Kotest Assertions](https://kotest.io/docs/assertions/assertions.html) | `actual shouldBe expected` | `assertSoftly { … }` |
| Python | [assertpy](https://github.com/assertpy/assertpy) | `assert_that(actual)` | `with soft_assertions():` |
| Go | [testify/assert + require](https://pkg.go.dev/github.com/stretchr/testify) | `assert.Equal(t, expected, actual)` | `assert.*` continues; `require.*` stops |
| TypeScript/JS | [Vitest `expect`](https://vitest.dev/api/expect.html) | `expect(actual).toEqual(expected)` | `expect.soft(actual)` |

For full API examples and setup instructions, see the language-specific standards docs:
- Java → `JAVA_STANDARDS.md` — AssertJ section
- Kotlin → `KOTLIN_STANDARDS.md` — Kotest Assertions section
- Python → `PYTHON_STANDARDS.md` — assertpy section
- Go → `GO_STANDARDS.md` — testify section
- TypeScript/JS → `TYPESCRIPT_STANDARDS.md` — Vitest assertions section

### Rules

- **One logical behavior per test** — multiple assertions are fine if they all verify the same behavior
- **No logic in tests** — no `if`, `for`, `try/catch` in test bodies; extract helpers instead
- **No shared mutable state** — each test sets up its own data; no reliance on test execution order
- **Test public API only** — never test private methods directly; if you want to, extract the logic into a testable class
- **Clean up resources** — close connections, delete temp files in teardown

---

## 5. Parameterized and Table-Driven Tests

Use parameterized tests when the same behavior needs to be verified across multiple inputs. This eliminates copy-paste test duplication while keeping each case explicit and readable.

### Java — JUnit 5 `@ParameterizedTest`

```java
@ParameterizedTest(name = "email={0} should be valid={1}")
@CsvSource({
    "user@example.com,  true",
    "user@domain.co.uk, true",
    "not-an-email,      false",
    "missing@tld,       false",
    ",                  false"
})
void shouldValidateEmailFormat(String email, boolean expected) {
    assertThat(validator.isValid(email)).isEqualTo(expected);
}

// For complex objects, use @MethodSource
@ParameterizedTest
@MethodSource("violationPenaltyProvider")
void shouldCalculatePenaltyForViolation(Violation violation, double expectedPenalty) {
    assertThat(scorer.penaltyFor(violation)).isEqualTo(expectedPenalty);
}

static Stream<Arguments> violationPenaltyProvider() {
    return Stream.of(
        Arguments.of(new CheckstyleViolation("error"), 5.0),
        Arguments.of(new PmdViolation(1),               3.0),
        Arguments.of(new SonarViolation("CRITICAL"),    10.0)
    );
}
```

### Python — pytest `@pytest.mark.parametrize`

```python
import pytest

@pytest.mark.parametrize("email,expected", [
    ("user@example.com",  True),
    ("user@domain.co.uk", True),
    ("not-an-email",      False),
    ("missing@tld",       False),
    ("",                  False),
])
def test_should_validate_email_format(email, expected):
    assert validator.is_valid(email) == expected


# For complex cases, use ids for readable output
@pytest.mark.parametrize("violation,expected_penalty", [
    pytest.param(CheckstyleViolation("error"), 5.0,  id="checkstyle-error"),
    pytest.param(PmdViolation(priority=1),     3.0,  id="pmd-critical"),
    pytest.param(SonarViolation("CRITICAL"),   10.0, id="sonar-critical"),
])
def test_should_calculate_penalty(violation, expected_penalty):
    assert scorer.penalty_for(violation) == expected_penalty
```

### Go — Table-Driven Tests

```go
func TestShouldValidateEmailFormat(t *testing.T) {
    tests := []struct {
        name     string
        email    string
        expected bool
    }{
        {"valid simple email",    "user@example.com",  true},
        {"valid subdomain email", "user@domain.co.uk", true},
        {"no at sign",            "not-an-email",      false},
        {"missing tld",           "missing@tld",       false},
        {"empty string",          "",                  false},
    }

    for _, tt := range tests {
        t.Run(tt.name, func(t *testing.T) {
            result := ValidateEmail(tt.email)
            if result != tt.expected {
                t.Errorf("ValidateEmail(%q) = %v, want %v", tt.email, result, tt.expected)
            }
        })
    }
}
```

### TypeScript — Vitest / Jest `test.each`

```typescript
test.each([
    ["user@example.com",  true],
    ["user@domain.co.uk", true],
    ["not-an-email",      false],
    ["missing@tld",       false],
    ["",                  false],
])("should validate email %s as %s", (email, expected) => {
    expect(validator.isValid(email)).toBe(expected);
});

// With object rows for readability
test.each([
    { email: "user@example.com",  expected: true,  label: "valid email" },
    { email: "not-an-email",      expected: false, label: "no at sign" },
])("$label: $email", ({ email, expected }) => {
    expect(validator.isValid(email)).toBe(expected);
});
```

### Rules

- **Always name your cases** — use `name`, `ids`, or descriptive labels so failures are immediately readable
- **Keep cases data-only** — no logic inside the data provider; compute expected values ahead of time
- **Limit case count** — if you have > 20 cases, consider grouping by equivalence class
- **Don't parameterize across unrelated behaviors** — one parameterized test covers one behavior with many inputs, not many different behaviors

---

## 6. Test Data Management

### Strategies

| Strategy | Use When |
|----------|----------|
| **Inline literals** | Simple, self-contained values; short tests |
| **Object mothers / builders** | Complex domain objects needed in many tests |
| **Fixture files** | Large payloads (JSON, XML, CSV) or realistic real-world samples |
| **Database seeding** | Integration tests that require persistent state |

### Object Mother Pattern

An object mother is a factory class that creates ready-to-use test objects with sensible defaults, overridable per test.

```java
// Java — Object Mother
public class UserMother {
    public static User aValidUser() {
        return User.builder()
            .id(1L)
            .email("user@example.com")
            .name("Test User")
            .role(Role.STANDARD)
            .build();
    }

    public static User anAdminUser() {
        return aValidUser().toBuilder().role(Role.ADMIN).build();
    }

    public static User aUserWithEmail(String email) {
        return aValidUser().toBuilder().email(email).build();
    }
}

// In tests:
var user = UserMother.aValidUser();
var admin = UserMother.anAdminUser();
```

```python
# Python — dataclass factory with defaults
from dataclasses import dataclass, field, replace

@dataclass
class UserMother:
    @staticmethod
    def a_valid_user() -> User:
        return User(id=1, email="user@example.com", name="Test User", role=Role.STANDARD)

    @staticmethod
    def an_admin_user() -> User:
        return replace(UserMother.a_valid_user(), role=Role.ADMIN)

    @staticmethod
    def a_user_with_email(email: str) -> User:
        return replace(UserMother.a_valid_user(), email=email)
```

### Fixture Files

Store in `test/resources/fixtures/` (or `tests/fixtures/` for Python/Go).

```
tests/
  fixtures/
    checkstyle-report-with-errors.xml
    pmd-report-empty.xml
    valid-user.json
    invalid-payload-missing-fields.json
```

Rules:
- Use realistic but minimal examples — only include fields relevant to the test
- Name files after the scenario, not the format: `user-missing-email.json` not `test2.json`
- Document non-obvious fixture data with a `README.md` in the fixtures directory

### Database Seeding (Integration Tests)

- Use a dedicated test database or an in-memory alternative (H2, SQLite, testcontainers)
- Seed data in `@BeforeEach` / `setup()` / `t.Cleanup()` — never rely on leftover state
- Clean up in `@AfterEach` / teardown — or use transactions that roll back after each test
- Never share seeded data between tests (leads to test order dependency)

---

## 7. Serialization Round-Trip Tests

### Why Round-Trip Testing Matters

Any class annotated for serialization (e.g., `@Serializable`, `@JsonSerializable`, `@dataclass` with JSON encoders, Go structs with JSON tags) can silently break if:

- A field is renamed without updating the serialization annotation
- A custom serializer has a bug (e.g., UUID, Instant, or enum serializers)
- A new required field is added without a default value
- A field type changes in a way the deserializer cannot handle

Round-trip tests catch these failures at the unit test level — fast, isolated, and deterministic.

### What Is a Round-Trip Test?

A round-trip test verifies that an object survives serialization and deserialization without data loss:

1. **Serialize**: Convert an in-memory object to its serialized form (JSON, XML, protobuf, etc.)
2. **Deserialize**: Convert the serialized form back to an in-memory object
3. **Assert equality**: The deserialized object must equal the original

### When to Write Round-Trip Tests

**Write a round-trip test for every serializable data class** — regardless of what the class represents (domain object, request, response, message, event, DTO, or anything else). If it is annotated for serialization, it needs a test.

**Minimum requirement**: One happy-path test per direction (serialize, deserialize) per class.

**Higher-risk classes that deserve extra attention:**
- Classes using custom serializers (UUID, Instant, date/time, enum, polymorphic)
- Classes with nullable fields that have non-null defaults
- Classes with collections (lists, sets, maps) that may serialize as empty vs. absent
- Classes involved in external API contracts (requests/responses)

### Test Structure

Each round-trip test verifies two things:
1. **Serialize → JSON → Deserialize → equals original** (the full round-trip)
2. **Deserialize from known JSON → object has expected values** (ensures external compatibility)

### Examples

#### Kotlin (kotlinx.serialization)

```kotlin
class UserTest : ShouldSpec({
    context("serialization") {
        should("survive round-trip serialization") {
            // Given
            val original = User(
                id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
                email = "user@example.com",
                name = "Test User",
                role = Role.STANDARD
            )

            // When
            val json = Json.encodeToString(original)
            val deserialized = Json.decodeFromString<User>(json)

            // Then
            deserialized shouldBe original
        }

        should("deserialize from known JSON") {
            // Given
            val json = """
                {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "email": "user@example.com",
                    "name": "Test User",
                    "role": "STANDARD"
                }
            """.trimIndent()

            // When
            val result = Json.decodeFromString<User>(json)

            // Then
            result.id shouldBe UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
            result.email shouldBe "user@example.com"
            result.name shouldBe "Test User"
            result.role shouldBe Role.STANDARD
        }
    }
})
```

#### Java (Jackson)

```java
@Test
void shouldSurviveRoundTripSerialization() throws Exception {
    // Given
    var original = new User(1L, "user@example.com", "Test User", Role.STANDARD);

    // When
    String json = objectMapper.writeValueAsString(original);
    User deserialized = objectMapper.readValue(json, User.class);

    // Then
    assertThat(deserialized).isEqualTo(original);
}

@Test
void shouldDeserializeFromKnownJson() throws Exception {
    // Given
    String json = """
        {"id": 1, "email": "user@example.com", "name": "Test User", "role": "STANDARD"}
        """;

    // When
    User result = objectMapper.readValue(json, User.class);

    // Then
    assertThat(result.email()).isEqualTo("user@example.com");
    assertThat(result.role()).isEqualTo(Role.STANDARD);
}
```

#### Python (dataclasses + json / pydantic)

```python
def test_should_survive_round_trip_serialization():
    # Given
    original = User(id=1, email="user@example.com", name="Test User", role=Role.STANDARD)

    # When
    json_str = original.model_dump_json()  # pydantic
    deserialized = User.model_validate_json(json_str)

    # Then
    assert deserialized == original


def test_should_deserialize_from_known_json():
    # Given
    json_str = '{"id": 1, "email": "user@example.com", "name": "Test User", "role": "STANDARD"}'

    # When
    result = User.model_validate_json(json_str)

    # Then
    assert result.email == "user@example.com"
    assert result.role == Role.STANDARD
```

#### Go (encoding/json)

```go
func TestUser_ShouldSurviveRoundTripSerialization(t *testing.T) {
    // Given
    original := User{ID: 1, Email: "user@example.com", Name: "Test User", Role: RoleStandard}

    // When
    data, err := json.Marshal(original)
    require.NoError(t, err)

    var deserialized User
    err = json.Unmarshal(data, &deserialized)
    require.NoError(t, err)

    // Then
    assert.Equal(t, original, deserialized)
}

func TestUser_ShouldDeserializeFromKnownJSON(t *testing.T) {
    // Given
    jsonStr := `{"id": 1, "email": "user@example.com", "name": "Test User", "role": "STANDARD"}`

    // When
    var result User
    err := json.Unmarshal([]byte(jsonStr), &result)
    require.NoError(t, err)

    // Then
    assert.Equal(t, "user@example.com", result.Email)
    assert.Equal(t, RoleStandard, result.Role)
}
```

### Guidelines

- **One test class per serializable data class** — or group closely related classes (e.g., a request and its nested types) into one test class
- **Use realistic field values** — not empty strings or zeroed-out IDs; use values that exercise custom serializers
- **Test with the same serializer configuration used in production** — if your app configures `Json { ignoreUnknownKeys = true }`, use that same configuration in tests
- **Test nullable fields explicitly** — verify that null values serialize to JSON `null` and deserialize back correctly
- **Test default values** — verify that omitting optional fields in JSON produces the correct defaults
- **These are unit tests** — no I/O, no external services; they verify data integrity of the serialization contract

### Relationship to Other Test Types

- **Round-trip tests (this section)**: Unit tests. Verify that a single class serializes/deserializes correctly in isolation. Fast, deterministic, no I/O.
- **Property-based testing (Section 9)**: Can generate random valid instances and verify the round-trip property holds for all of them. Complementary, not a replacement.
- **Integration tests**: Verify serialization against a real API server or message broker. Slower but catches schema mismatches between services.

---

## 8. Mutation Testing

### What Is Mutation Testing?

A test coverage percentage tells you which lines were *executed* during tests. Mutation testing tells you whether your tests actually *detect bugs*. A **mutant** is a small automated change to production code (e.g., `>` changed to `>=`, `+` changed to `-`). If your tests don't catch the mutant, they're not asserting what they should be.

**Mutation score** = percentage of mutants killed by your tests. Target: **100%** for business logic (zero surviving mutants).

### When to Use It

- Critical business logic: scoring engines, pricing rules, authorization checks, data transformations
- After reaching 80% line coverage — mutation testing reveals whether that coverage is *meaningful*. Target 100% kill rate on business logic.
- Before declaring a module "done" — a green test suite with a low mutation score means your tests are weak

### Tool: pitest (Java / Kotlin)

**Gradle setup:**

```kotlin
// build.gradle.kts
plugins {
    id("info.solidsoft.pitest") version "1.15.0"
}

pitest {
    // Target all application code; rely on excludedClasses to filter out non-business-logic
    targetClasses.set(listOf("com.example.*"))
    excludedClasses.set(listOf(
        "com.example.domain.*Dto",
        "com.example.domain.*Config",
        // Exclude infrastructure — see "Infrastructure Exclusions" below
        "com.example.infrastructure.*",
        "com.example.plugins.*",
        "com.example.config.*"
    ))
    mutators.set(listOf("STRONGER"))        // DEFAULTS, STRONGER, or ALL
    threads.set(4)
    outputFormats.set(listOf("HTML", "XML"))
    mutationThreshold.set(100)             // fail build if any mutant survives
    coverageThreshold.set(80)
    timeoutConstant.set(5000)
}
```

**Run:**
```bash
./gradlew pitest
# Report at: build/reports/pitest/index.html
```

**Interpreting results:**
- 🟢 **Killed** — your tests caught the mutation (good)
- 🔴 **Survived** — your tests didn't catch the mutation (write a better assertion)
- 🟡 **No coverage** — no test even executed this mutant (write a test)
- ⚪ **Timed out** — mutant caused an infinite loop (usually killed, counts as caught)

**Common surviving mutants and fixes:**

| Surviving Mutant | Fix |
|-----------------|-----|
| `>` → `>=` survives | Add boundary value test at the exact threshold |
| Return value changed survives | Assert the actual return value, not just that no exception was thrown |
| Null check removed survives | Add test with null input |
| Condition negated survives | Add test for the false branch |

### Infrastructure Exclusions

**Exclude infrastructure code from mutation testing targets.** Infrastructure classes are integration-wiring concerns where mutations are typically void-call removals in DSL lambdas. Unit-testing them requires heavy framework test-host mocking and produces fragile, low-value tests.

**What to exclude:**
- Route definitions / controller wiring (e.g., Ktor `routing {}`, Spring `@RestController` boilerplate)
- Application bootstrap / plugin configuration (e.g., `Application.module()`, `install(ContentNegotiation)`)
- HTTP client factory classes (e.g., `BearerAuthHttpClientFactory`)
- Database connection factories (e.g., `DatabaseFactory`, `DataSource` configuration)
- Dependency injection wiring (e.g., `ServiceFactory`, Koin/Dagger modules)
- Monitoring / observability plugin setup (e.g., `install(MicrometerMetrics)`)

**What NOT to exclude:**
- Business logic, domain services, repositories with query logic
- Data transformation / parsing / validation
- Any class that makes decisions based on input

**Example `excludedClasses` for a Ktor project:**
```kotlin
excludedClasses.set(listOf(
    "com.example.plugins.*",          // Ktor plugin configuration
    "com.example.routes.*Routes*",     // Route wiring (not route handlers with logic)
    "com.example.config.*",            // Application configuration
    "com.example.infrastructure.*",    // HTTP client factories, DB factories
    "com.example.ApplicationKt"        // Application.kt entry point
))
```

**Why 100% on business logic?** Infrastructure exclusions mean pitest only targets code that makes decisions. Every surviving mutant in business logic represents a real behavioral gap in your tests — a bug that would go undetected. Zero survivors means your test suite is a reliable safety net.

### Tool: mutmut (Python)

**Install:**
```bash
pip install mutmut
```

**Setup (`pyproject.toml`):**
```toml
[tool.mutmut]
paths_to_mutate = "src/"
tests_dir = "tests/"
runner = "python -m pytest -x"
```

**Run:**
```bash
mutmut run
mutmut results          # show surviving mutants
mutmut show <id>        # show the specific mutation that survived
mutmut html             # generate HTML report
```

**Interpreting results:**
```
Survived mutants  (tests didn't catch them) → add assertions
Killed mutants    (tests caught them)       → good
```

### Tool: Stryker (TypeScript / JavaScript)

**Install:**
```bash
npm install --save-dev @stryker-mutator/core @stryker-mutator/jest-runner
```

**Config (`stryker.config.mjs`):**
```javascript
export default {
  packageManager: "npm",
  reporters: ["html", "clear-text", "progress"],
  testRunner: "jest",
  coverageAnalysis: "perTest",
  mutate: ["src/**/*.ts", "!src/**/*.test.ts", "!src/**/*.spec.ts"],
  thresholds: {
    high: 80,
    low: 60,
    break: 50    // fail CI if score drops below 50%
  }
};
```

**Run:**
```bash
npx stryker run
# Report at: reports/mutation/html/index.html
```

### CI Integration

Run mutation testing in CI on pull requests that touch critical business logic. It is expensive — don't run it on every commit; run it as a separate CI job.

```yaml
# Example GitHub Actions step (run on PR only)
- name: Mutation Testing
  if: github.event_name == 'pull_request'
  run: ./gradlew pitest
```

---

## 9. Property-Based Testing

### What Is Property-Based Testing?

Instead of writing specific input/output pairs, you define **properties** that must hold true for *any* valid input, and the framework generates hundreds of random inputs to try to falsify them.

**Example property**: "Sorting a list and then sorting it again produces the same result as sorting it once."

This is particularly powerful for:
- Encoding, serialization/deserialization round-trips
- Mathematical invariants (commutativity, associativity, idempotence)
- Parsers and validators (any valid input must parse without throwing)
- State machines (any sequence of valid operations leaves the system in a valid state)

### Tool: Hypothesis (Python)

```python
from hypothesis import given, settings, assume
from hypothesis import strategies as st

# Property: any valid email that passes validation can be stored and retrieved
@given(st.emails())
def test_valid_email_survives_round_trip(email):
    user = User(email=email)
    stored = repository.save(user)
    retrieved = repository.find_by_id(stored.id)
    assert retrieved.email == email


# Property: score is always between 0 and 100
@given(
    st.lists(st.integers(min_value=1, max_value=5), max_size=20)
)
def test_score_is_always_in_valid_range(violation_priorities):
    violations = [PmdViolation(p) for p in violation_priorities]
    score = scorer.calculate(violations)
    assert 0.0 <= score <= 100.0


# Use assume() to filter invalid inputs
@given(st.text())
def test_parser_never_throws_on_non_empty_input(text):
    assume(len(text) > 0)
    # Should parse or return an error, never throw an unhandled exception
    result = parser.parse(text)
    assert result is not None
```

**Settings for CI:**
```python
from hypothesis import settings, HealthCheck

@settings(max_examples=500, suppress_health_check=[HealthCheck.too_slow])
@given(st.emails())
def test_email_validation_property(email):
    ...
```

### Tool: jqwik (Java)

```java
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

class ScorePropertyTest {

    @Property
    void scoreShouldAlwaysBeBetweenZeroAndHundred(
        @ForAll @IntRange(min = 1, max = 5) List<Integer> priorities
    ) {
        var violations = priorities.stream()
            .map(PmdViolation::new)
            .toList();

        double score = scorer.calculate(violations);

        assertThat(score).isBetween(0.0, 100.0);
    }

    @Property
    void serializationRoundTripPreservesData(
        @ForAll("validUsers") User user
    ) {
        String json = serializer.toJson(user);
        User deserialized = serializer.fromJson(json, User.class);
        assertThat(deserialized).isEqualTo(user);
    }

    @Provide
    Arbitrary<User> validUsers() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(1),
            Arbitraries.emails()
        ).as(User::new);
    }
}
```

**Gradle dependency:**
```kotlin
testImplementation("net.jqwik:jqwik:1.8.4")
```

### When Property-Based Testing Adds Value

✅ Use it for:
- Round-trip invariants (serialize → deserialize → equals original)
- Boundary conditions you can express as rules (score always 0–100)
- Stateless pure functions with mathematical properties
- Parsers and validators (valid input never throws)

❌ Don't use it for:
- Simple CRUD operations (example-based tests are clearer)
- Tests requiring specific expected values
- Tests that are inherently stateful or order-dependent

---

## 10. Contract Testing

> **Status: Planned** — Full contract testing guidance (Pact / consumer-driven contracts for microservices) is planned for a future update. Track progress in the project issue tracker.

### Summary (Until Full Guidance Is Available)

Contract testing verifies that a consumer (e.g., a frontend or downstream service) and a provider (e.g., an API) agree on the shape of their interface — **without deploying both simultaneously**.

**When to consider it:**
- You have multiple services consuming a shared API
- Your E2E tests are brittle because they require all services to be deployed together
- A provider change broke a consumer and wasn't caught until integration

**Tool to evaluate:** [Pact](https://docs.pact.io/) — supports Java, Python, Go, JavaScript, and more.

---

## 11. Snapshot Testing

### What Is It?

Snapshot testing captures the output of a function (typically a rendered UI component or a serialized data structure) and saves it to a file. Future test runs compare against that saved snapshot and fail if the output changes.

### When It Is Appropriate

✅ Use snapshot testing for:
- UI component rendering output (React, Vue) where visual structure matters
- Complex serialized outputs (large JSON/XML) that change rarely and deliberately
- CLI output formatting that must remain stable

### When It Becomes a Maintenance Burden

❌ Avoid snapshot testing when:
- The output changes frequently (you'll spend more time updating snapshots than writing tests)
- The snapshot is so large that reviewers rubber-stamp updates without reading them
- You're tempted to snapshot-test business logic (use explicit assertions instead)
- Snapshots are committed alongside auto-generated code that regenerates differently per machine

### Rules

- **Review snapshot diffs carefully in PRs** — a changed snapshot is a changed behavior; don't blindly accept it
- **Keep snapshots small** — if a snapshot is > 50 lines, consider asserting specific fields instead
- **Name snapshots after their scenario** — not after the component or method
- **Never update snapshots without understanding why they changed**

---

## 12. Flaky Test Policy

### What Is a Flaky Test?

A **flaky test** is a test that passes sometimes and fails other times without any code change. The result is non-deterministic.

**Common causes:**

| Cause | Example |
|-------|---------|
| Timing dependency | `Thread.sleep(500)` hoping async work finishes |
| Shared / global state | One test leaves dirty state that breaks the next |
| Test order dependency | Test B only passes if test A ran first |
| External dependency | Hitting a real network endpoint or clock in a unit test |
| Race condition | Concurrent code with non-deterministic scheduling |
| Random data without seed | Random input that occasionally hits an unhandled edge case |

### Why Flaky Tests Are Dangerous

When tests are known to flake, developers start ignoring failures: *"Oh, that one just flakes sometimes."* This is how real bugs get missed. A flaky test suite erodes trust in the entire test suite.

### Policy

#### Step 1: Tag It Immediately

When a test is identified as flaky, tag it the same day. Do not leave it untagged.

```java
// Java / Kotlin — JUnit 5
@Tag("flaky")
@Disabled("Flaky: timing issue with async email sender — see #42")
@Test
void shouldSendWelcomeEmailAfterRegistration() { ... }
```

```python
# Python — pytest
@pytest.mark.skip(reason="Flaky: race condition in async handler — see #42")
def test_should_send_welcome_email_after_registration():
    ...
```

```go
// Go
func TestShouldSendWelcomeEmailAfterRegistration(t *testing.T) {
    t.Skip("Flaky: timing issue with async sender — see #42")
    ...
}
```

#### Step 2: Open a Tracking Issue Immediately

Every flaky test must have a GitHub issue opened on the same day it is tagged. The issue must include:
- Which test is flaky
- Observed failure rate (e.g., "fails ~1 in 10 runs")
- Suspected cause
- Link to the tag/skip commit

#### Step 3: Quarantine (Exclude from CI Block)

Move the tagged test to a separate CI job (`flaky-tests`) that runs but does **not** block the pipeline. This prevents the flake from blocking legitimate work while still running the test to gather data.

#### Step 4: Fix Within 2 Sprints

Flaky tests must be fixed within **2 sprints** of being tagged. If not fixed, escalate.

**Fix strategies by cause:**

| Cause | Fix |
|-------|-----|
| `Thread.sleep` / timing | Use `awaitility`, `asyncio.wait_for`, or proper async test utilities |
| Shared state | Reset state in `@BeforeEach` / `setup()`; make tests independent |
| Test order dependency | Identify which test pollutes state; fix that test's cleanup |
| Real network/clock | Mock the dependency; inject a test clock |
| Race condition | Use synchronization primitives or redesign for determinism |
| Random data | Seed the random number generator; log the seed on failure |

#### Step 5: Re-enable After Fix

After fixing, remove the tag/skip, move back to the main test suite, verify it passes 10+ consecutive runs in CI before closing the issue.

### Zero Tolerance Policy

- **Never add a new flaky test.** If your PR introduces a flaky test, it will be rejected.
- **Never accept a flaky test as "normal."** Flakiness is a defect, not a quirk.
- **Never update a snapshot / expected value to make a flaky test pass without understanding why it changed.**

---

## 13. Test Execution Tiers

This is the canonical reference for when each test type runs. The same table appears as a summary in `CODING_PRACTICES.md` — this is the authoritative version.

| When | What Runs | Mandatory? | Why |
|------|-----------|------------|-----|
| Before every commit | Unit tests | ✅ Yes — no exceptions | Fast, isolated, deterministic. No reason to skip. |
| Before pushing (pre-push hook) | Unit tests + integration tests | ✅ Yes | Catch cross-component issues before they reach the team. |
| CI pipeline (every push/PR) | Unit + integration + E2E | ✅ Yes — hard gate | Full validation before merge. Failures block the PR. |
| Mutation testing job | Mutation tests | On PRs touching critical logic | Expensive — run as a separate CI job, not on every commit. |
| Performance / load tests | Performance suite | On release branches / schedule | Environment-sensitive; run on dedicated infrastructure. |

### Rules

- **Unit tests are mandatory before every commit.** If they fail, do not commit. Fix the code or the test first.
- **Integration tests must pass before pushing.** Use a pre-push hook or run manually. If they fail, fix locally before pushing.
- **CI is the hard gate.** Even if a developer skips local hooks, CI catches failures before merge. CI failures block the PR — no exceptions for "urgent" fixes.
- **Never skip tests to push faster.** If integration tests are too slow to run pre-push, optimize the test suite — don't skip it.
- **Mutation tests run on PRs touching critical paths.** Not on every commit — they are compute-intensive.

### Example Pre-Commit Hook

```bash
#!/bin/bash
# .git/hooks/pre-commit
echo "Running pre-commit unit tests..."

# Replace with your project's unit test command:
# ./gradlew test       (Java / Kotlin)
# npm test             (Node / TypeScript)
# pytest tests/unit/   (Python)
# go test ./...        (Go)

echo "✅ Unit tests passed — proceeding with commit"
```

### Example Pre-Push Hook

```bash
#!/bin/bash
# .git/hooks/pre-push
echo "Running pre-push checks..."

# 1. Unit tests (should already pass from pre-commit)
# ./gradlew test || exit 1

# 2. Integration tests
# ./gradlew integrationTest || exit 1
# pytest tests/integration/ || exit 1
# go test -tags=integration ./... || exit 1

echo "✅ All pre-push checks passed"
```

---

## Quick Reference

### Before Every Commit
- [ ] All unit tests pass
- [ ] Test names are descriptive (`should<Expected>When<Condition>`)
- [ ] Tests follow Given-When-Then structure
- [ ] No logic inside test bodies
- [ ] No shared mutable state between tests
- [ ] No `Thread.sleep` / `time.sleep` in tests

### Before Every Push
- [ ] All unit tests pass (already done from pre-commit)
- [ ] All integration tests pass

### For New Code
- [ ] 80% minimum unit test coverage for all production code
- [ ] 100% coverage for critical paths
- [ ] Happy path, edge cases, and error paths covered
- [ ] Used correct test double type (fake/stub/mock/spy)
- [ ] Complex test objects use object mother or builder
- [ ] Serializable data classes have round-trip tests (serialize → deserialize → equals)

### Red Flags
- 🚨 `Thread.sleep` / `time.sleep` in a test → timing-dependent, replace with async utilities
- 🚨 Test name is `test1` or `testMethod` → rename
- 🚨 Test has an `if` statement → extract helper, split into separate tests
- 🚨 Test mocks the class under test → testing the wrong thing
- 🚨 Test only asserts no exception was thrown → assert the actual return value too
- 🚨 Test passes 100% of the time but mutation score is < 50% → assertions are weak

---

**Last Updated**: February 2026
**Version**: 3.0 (Removed Behavior-First Testing Policy section; renumbered all sections)
