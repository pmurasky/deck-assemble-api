# Java Coding Standards

## Overview
This document outlines Java-specific coding standards that supplement the language-agnostic standards in `CODING_PRACTICES.md` and `CODING_STANDARDS.md`.

We support **Java 21** (LTS) and **Java 25** (LTS) and leverage modern Java features where appropriate. Choose the version that matches your project.

## Official Style Guide
We follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with the following project-specific additions and clarifications.

## Package Organization (Domain-Driven Design)

Follow domain-driven package structure:

```
com.example.project/
├── order/            # Order domain
├── payment/          # Payment processing
├── inventory/        # Inventory management
├── notification/     # Notification services
├── config/           # Configuration
└── common/           # Shared utilities (keep minimal)
```

**Key Principles:**
- Package by domain (what it does), not by layer (what it is)
- Avoid `util`, `helper`, `manager` packages -- they become dumping grounds
- Each package represents a cohesive functional area
- Minimize cross-package dependencies
- Use package-private visibility by default; only expose what's needed

## Naming Conventions

- **Classes/Interfaces**: PascalCase (`OrderService`, `PaymentProcessor`)
- **Methods/Fields**: camelCase (`calculateTotal`, `orderCount`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase, no underscores (`com.example.project.order`)
- **Test Classes**: Same as source + `Test` suffix (`OrderServiceTest`)
- **Type Parameters**: Single uppercase letter or descriptive (`T`, `E`, `K`, `V`)

**Naming anti-patterns to avoid:**
- `*Manager`, `*Handler`, `*Helper`, `*Utility` -- usually SRP violations
- `*Impl` suffix without a clear reason -- if there's only one implementation, drop the interface
- Abbreviations -- use `customer` not `cust`, `repository` not `repo`

## Modern Java Features (Java 21)

### Records (Use for DTOs and Value Objects)

```java
// Good: Immutable value object
public record ScoreResult(String className, int score, List<Penalty> penalties) {
    // Compact constructor for validation
    public ScoreResult {
        Objects.requireNonNull(className, "className must not be null");
        penalties = List.copyOf(penalties); // Defensive copy for immutability
    }
}

// Bad: Mutable class with boilerplate
public class ScoreResult {
    private String className;
    private int score;
    // ... getters, setters, equals, hashCode, toString
}
```

### Sealed Classes and Interfaces

```java
// Good: Restricted type hierarchy with exhaustive pattern matching
public sealed interface ParseResult permits ParseResult.Success, ParseResult.Failure {
    record Success(List<Finding> findings) implements ParseResult {}
    record Failure(String error) implements ParseResult {}
}
```

### Pattern Matching

```java
// Good: Pattern matching for instanceof (Java 16+)
if (result instanceof ParseResult.Success success) {
    process(success.findings());
}

// Good: Switch pattern matching (Java 21)
return switch (result) {
    case Success s -> processFindings(s.findings());
    case Failure f -> handleError(f.error());
};

// Bad: Manual casting
if (result instanceof ParseResult.Success) {
    ParseResult.Success success = (ParseResult.Success) result;
    process(success.findings());
}
```

### Text Blocks

```java
// Good: Multi-line strings
String query = """
    SELECT name, score
    FROM class_scores
    WHERE score > %d
    ORDER BY score DESC
    """.formatted(threshold);

// Bad: String concatenation
String query = "SELECT name, score\n"
    + "FROM class_scores\n"
    + "WHERE score > " + threshold;
```

### Optional

```java
// Good: Optional for return types that may be absent
public Optional<User> findByEmail(String email) {
    return repository.find(email);
}

// Good: Chaining
String name = findByEmail(email)
    .map(User::displayName)
    .orElse("Unknown");

// Bad: Optional as parameter or field
public void process(Optional<String> name) { ... }  // Don't do this

// Bad: Using get() without check
User user = findByEmail(email).get();  // Throws if empty
```

## Modern Java Features (Java 22-25)

### Unnamed Variables and Patterns (Java 22, JEP 456)

Use `_` for variables or patterns you don't need. Reduces noise and signals intent.

```java
// Good: Unnamed variable in try-with-resources
try (var _ = ScopedContext.acquire()) {
    process();
}

// Good: Unnamed variable in catch
try {
    parse(input);
} catch (NumberFormatException _) {
    return defaultValue;
}

// Good: Unnamed pattern in switch
return switch (result) {
    case Success s -> s.findings();
    case Failure _ -> List.of();  // We don't need the error details here
};

// Good: Unnamed variable in enhanced for loop (side effects only)
int count = 0;
for (var _ : items) {
    count++;
}

// Good: Unnamed lambda parameters
map.forEach((_, value) -> process(value));
```

### Markdown Documentation Comments (Java 23, JEP 467)

Use `///` for Javadoc with Markdown syntax instead of `/** */` with HTML.

```java
/// Calculates the quality score for a class based on code findings.
///
/// The score starts at 100 and decreases based on the severity
/// and count of findings. The minimum score is 0.
///
/// @param findings list of code quality findings, must not be null
/// @param weights scoring weights for each severity category
/// @return score between 0 and 100 inclusive
/// @throws IllegalArgumentException if weights contain negative values
public int calculateScore(List<Finding> findings, Weights weights) { ... }

/// Example with code blocks (no need for {@code} or <pre> tags):
///
/// ```java
/// var scorer = new ClassScorer();
/// int score = scorer.calculateScore(findings, Weights.defaults());
/// ```
public class ClassScorer { ... }
```

**Prefer `///` Markdown comments for new code.** They are cleaner than `/** */` with HTML tags. Both styles are valid; be consistent within a file.

### Stream Gatherers (Java 24, JEP 485)

Custom intermediate stream operations via `Stream.gather(Gatherer)`. Use when built-in operations (`map`, `filter`, `flatMap`) are insufficient.

```java
// Built-in gatherer: sliding window
List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowSliding(3))
    .toList();
// [[1, 2, 3], [2, 3, 4], [3, 4, 5]]

// Built-in gatherer: fixed-size groups
List<List<Order>> batches = orders.stream()
    .gather(Gatherers.windowFixed(10))
    .toList();

// Built-in gatherer: fold (stateful accumulation as stream)
Stream<BigDecimal> runningTotals = amounts.stream()
    .gather(Gatherers.fold(
        () -> BigDecimal.ZERO,
        BigDecimal::add
    ));

// Built-in gatherer: scan (like fold but emits every intermediate value)
Stream<BigDecimal> runningTotals = amounts.stream()
    .gather(Gatherers.scan(
        () -> BigDecimal.ZERO,
        BigDecimal::add
    ));

// Built-in gatherer: mapConcurrent (parallel mapping with concurrency limit)
List<Response> responses = urls.stream()
    .gather(Gatherers.mapConcurrent(10, url -> httpClient.send(url)))
    .toList();
```

**Guidelines:**
- Prefer built-in gatherers (`windowFixed`, `windowSliding`, `fold`, `scan`, `mapConcurrent`) over custom ones
- Use gatherers when the operation is genuinely a custom intermediate transform
- Don't use gatherers for things already handled by `map`, `filter`, `reduce`, `collect`

### Module Import Declarations (Java 25, JEP 511)

Import all public top-level types exported by a module with a single declaration.

```java
// Good: Import entire module (replaces many individual imports)
import module java.base;
import module java.sql;

// Replaces:
// import java.util.*;
// import java.util.stream.*;
// import java.io.*;
// import java.nio.file.*;
// import java.sql.*;
// ... etc.
```

**Guidelines:**
- Use `import module java.base;` to avoid repetitive `java.util.*`, `java.io.*`, etc.
- Useful for modules with many packages you commonly use
- Single-type imports take precedence over module imports (no ambiguity)
- Prefer specific imports over module imports for third-party dependencies to keep dependencies visible

### Flexible Constructor Bodies (Java 25, JEP 513)

Execute statements before `super()` or `this()` calls. Enables validation and computation of arguments before delegating.

```java
// Good: Validate before calling super (Java 25+)
public class PositiveAmount extends Amount {
    public PositiveAmount(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + value);
        }
        super(value);  // Now we know value is valid
    }
}

// Good: Compute derived values before delegating
public class FullName extends Name {
    public FullName(String first, String last) {
        var combined = first.strip() + " " + last.strip();
        super(combined);
    }
}

// Good: Shared validation before this() delegation
public class ConnectionConfig {
    public ConnectionConfig(String host, int port) {
        Objects.requireNonNull(host, "host must not be null");
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        this(host, port, Duration.ofSeconds(30));  // Delegate after validation
    }

    public ConnectionConfig(String host, int port, Duration timeout) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }
}

// Before Java 25, you had to use static helper methods:
// public PositiveAmount(BigDecimal value) {
//     super(validate(value));  // Awkward workaround
// }
// private static BigDecimal validate(BigDecimal v) { ... }
```

**Guidelines:**
- Statements before `super()`/`this()` cannot access `this` (instance is not yet initialized)
- Use for argument validation, transformation, and logging
- Replaces the old pattern of static helper methods for pre-super validation

### Compact Source Files and Instance Main Methods (Java 25, JEP 512)

Simplified entry points for small programs. Primarily useful for scripts, prototypes, and learning.

```java
// Good: Compact source file (Java 25) -- no class declaration needed
void main() {
    var items = List.of("apple", "banana", "cherry");
    items.forEach(IO::println);
}

// Equivalent to the traditional form:
// public class Main {
//     public static void main(String[] args) {
//         var items = List.of("apple", "banana", "cherry");
//         items.forEach(System.out::println);
//     }
// }
```

**Guidelines:**
- Use for scripts, prototypes, and small utilities
- For production application code, use the traditional `public static void main(String[] args)` form
- `IO.println()`, `IO.readln()` are available in compact source files for simple I/O

### String Templates -- WITHDRAWN

String templates (JEP 430/459) were previewed in Java 21-22 but **withdrawn** in Java 23. The feature is being redesigned. **Do not use `STR."..."` or `FMT."..."` syntax** -- it does not exist in Java 23+.

Use `String.formatted()` or `MessageFormat` instead:

```java
// Good: String.formatted()
String msg = "User %s scored %d points".formatted(name, score);

// Good: Text block with formatted()
String html = """
    <h1>%s</h1>
    <p>Score: %d</p>
    """.formatted(name, score);

// Good: StringBuilder for complex cases
var sb = new StringBuilder();
for (var item : items) {
    sb.append("- ").append(item.name()).append("\n");
}
```

## Immutability

**Prefer immutable data structures:**
- Use `record` types for value objects
- Use `final` fields
- Use `List.of()`, `Map.of()`, `Set.of()` for immutable collections
- Use `Collections.unmodifiableList()` for defensive copies
- Return `List.copyOf()` from methods to prevent mutation

```java
// Good: Immutable
public record OrderSummary(
    String orderId,
    List<LineItem> items,
    BigDecimal total
) {
    public OrderSummary {
        items = List.copyOf(items);
    }
}

// Bad: Mutable
public class OrderSummary {
    private List<LineItem> items = new ArrayList<>();
    public List<LineItem> getItems() { return items; }  // Leaks mutable reference
}
```

## Null Safety

**Avoid returning null:**
- Use `Optional<T>` for methods that may not return a value
- Use `Objects.requireNonNull()` for parameter validation
- Use `@Nullable` and `@NonNull` annotations from `jakarta.annotation` or `org.jspecify`
- Fail fast with descriptive messages

```java
// Good: Explicit null handling
public OrderService(OrderRepository repository, NotificationService notifications) {
    this.repository = Objects.requireNonNull(repository, "repository must not be null");
    this.notifications = Objects.requireNonNull(notifications, "notifications must not be null");
}

// Good: Optional return
public Optional<Order> findOrder(String orderId) {
    return Optional.ofNullable(repository.find(orderId));
}

// Bad: Returning null
public Order findOrder(String orderId) {
    return repository.find(orderId);  // Caller doesn't know this can be null
}
```

## Function/Method Design

Apply the same standards from CODING_PRACTICES.md:
- **Maximum 20 lines per method** (excluding blanks/braces)
- Single responsibility per method
- Maximum 5 parameters (use parameter objects or builders)
- Prefer returning values over mutating state

```java
// Good: Small, focused methods
public int calculateScore(List<Finding> findings) {
    int totalPenalty = findings.stream()
        .mapToInt(this::penaltyFor)
        .sum();
    return Math.max(0, 100 - totalPenalty);
}

// Good: Parameter object when > 3 params
public Report generateReport(ReportConfig config) { ... }

public record ReportConfig(
    List<Score> scores,
    Path outputDir,
    ReportFormat format,
    boolean includeDetails,
    Locale locale
) {}

// Bad: Too many parameters
public Report generateReport(
    List<Score> scores, Path outputDir, ReportFormat format,
    boolean includeDetails, Locale locale, String title
) { ... }
```

## Dependency Injection

**Use constructor injection exclusively:**

```java
// Good: Constructor injection (all dependencies visible, immutable)
public class OrderService {
    private final OrderRepository repository;
    private final PaymentProcessor processor;

    public OrderService(OrderRepository repository, PaymentProcessor processor) {
        this.repository = Objects.requireNonNull(repository);
        this.processor = Objects.requireNonNull(processor);
    }
}

// Bad: Field injection (hidden dependencies, hard to test)
public class OrderService {
    @Inject
    private OrderRepository repository;
}
```

## Interface Design

**Prefer focused interfaces (ISP):**

```java
// Good: Focused interfaces
public interface ScoreCalculator {
    int calculate(List<Finding> findings);
}

public interface ReportWriter {
    void write(Report report, Path output);
}

// Bad: Fat interface
public interface CodeAnalyzer {
    List<Finding> parse(Path source);
    int calculateScore(List<Finding> findings);
    void writeReport(Report report, Path output);
    void sendNotification(Report report);
    Config loadConfig(Path configPath);
}
```

## Exception Handling

**Best practices:**
- Define domain-specific exceptions extending `RuntimeException`
- Never catch `Exception` or `Throwable` unless re-throwing
- Use try-with-resources for `AutoCloseable` resources
- Log at the appropriate level and include context
- Don't use exceptions for control flow

```java
// Good: Domain exception with context
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }

    public String getOrderId() { return orderId; }
}

// Good: Try-with-resources
public Config readConfig(Path path) {
    try (var stream = Files.newInputStream(path)) {
        return objectMapper.readValue(stream, Config.class);
    } catch (IOException e) {
        throw new ConfigException("Failed to read config: " + path, e);
    }
}

// Bad: Swallowing exceptions
try {
    process(data);
} catch (Exception e) {
    // silently ignored
}
```

## Streams and Collections

**Prefer functional collection operations:**

```java
// Good: Stream operations
Map<String, Integer> scoresByClass = findings.stream()
    .collect(Collectors.groupingBy(
        Finding::className,
        Collectors.summingInt(Finding::penalty)
    ));

// Good: Collectors for complex operations
var partitioned = orders.stream()
    .collect(Collectors.partitioningBy(Order::isPaid));

// Bad: Imperative with mutation
Map<String, Integer> scoresByClass = new HashMap<>();
for (Finding finding : findings) {
    scoresByClass.merge(finding.className(), finding.penalty(), Integer::sum);
}
```

**Stream guidelines:**
- Use streams for transformation, filtering, aggregation
- Don't use streams for simple iterations with side effects
- Break complex stream pipelines into named intermediate steps
- Prefer `toList()` (Java 16+) over `collect(Collectors.toList())`

## Documentation (Javadoc)

**Requirements:**
- All public classes, methods, and fields must have Javadoc
- Use `@param`, `@return`, `@throws` tags
- First sentence is the summary (appears in index)
- Include examples for complex APIs

```java
/**
 * Calculates the quality score for a class based on code findings.
 *
 * <p>The score starts at 100 and decreases based on the severity
 * and count of findings. The minimum score is 0.
 *
 * @param findings list of code quality findings, must not be null
 * @param weights scoring weights for each severity category
 * @return score between 0 and 100 inclusive
 * @throws IllegalArgumentException if weights contain negative values
 */
public int calculateScore(List<Finding> findings, Weights weights) { ... }
```

## Testing in Java

**Framework:** JUnit 5

**Structure:**
- Use `@Test`, `@ParameterizedTest`, `@Nested` for organization
- Given-When-Then structure in every test
- Descriptive method names using `should...When...` pattern
- Use AssertJ for fluent assertions
- Use Mockito for mocking (constructor injection makes this easy)

```java
@Test
void shouldCalculateCorrectScoreWithMultiplePenalties() {
    // Given
    var findings = List.of(
        new Finding("Foo.java", 10, Severity.HIGH),
        new Finding("Foo.java", 20, Severity.LOW)
    );

    // When
    int score = scorer.calculateScore(findings);

    // Then
    assertThat(score).isEqualTo(82);
}

@Nested
class WhenNoFindingsExist {
    @Test
    void shouldReturnPerfectScore() {
        assertThat(scorer.calculateScore(List.of())).isEqualTo(100);
    }
}

@ParameterizedTest
@CsvSource({"HIGH,30", "MEDIUM,15", "LOW,5"})
void shouldApplyCorrectPenaltyPerSeverity(Severity severity, int expected) {
    var finding = new Finding("Foo.java", 1, severity);
    assertThat(scorer.penaltyFor(finding)).isEqualTo(expected);
}
```

### AssertJ Fluent Assertions

AssertJ provides a fluent, discoverable API that produces readable failure messages. It is the preferred assertion library for all Java tests.

**Dependency** (test scope):
```xml
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.3</version>
    <scope>test</scope>
</dependency>
```

**Fluent chaining on strings and objects:**
```java
assertThat(name).startsWith("Fro")
                .endsWith("do")
                .isEqualToIgnoringCase("frodo");
```

**Collection assertions:**
```java
// Exactly these elements, in this order
assertThat(results).containsExactly(a, b, c);

// Exactly these elements, any order
assertThat(results).containsExactlyInAnyOrder(a, b, c);

// Contains at least these (may have others)
assertThat(results).contains(a, b);

// Pull a field from each element and assert on it
assertThat(findings).extracting(Finding::severity)
                    .containsExactly(HIGH, LOW);

// Filter then assert
assertThat(findings).filteredOn(f -> f.severity() == HIGH)
                    .hasSize(1);
```

**Exception assertions:**
```java
assertThatThrownBy(() -> service.process(null))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("input must not be null");
```

**Soft assertions** — collect all failures before reporting:
```java
assertSoftly(softly -> {
    softly.assertThat(response.getStatus()).isEqualTo(200);
    softly.assertThat(response.getBody()).contains("success");
    softly.assertThat(response.getHeaders()).containsKey("Content-Type");
});
```

Use soft assertions when verifying multiple independent properties of the same result.
Use fail-fast (default) when later assertions depend on an earlier result.

**Custom error messages with `as()`:**
```java
// CORRECT — as() must come BEFORE the assertion
assertThat(user.getAge()).as("check %s's age", user.getName()).isGreaterThan(0);

// WRONG — as() after the assertion is silently ignored; no error, no label
assertThat(user.getAge()).isGreaterThan(0).as("check age");  // ❌
```

**Common pitfalls:**
```java
// WRONG — assertThat(boolean) asserts nothing meaningful (always passes)
assertThat(actual.equals(expected));            // ❌ compiles, passes, tests nothing

// CORRECT — compare through AssertJ's fluent API
assertThat(actual).isEqualTo(expected);         // ✅
```

### Architecture Testing with ArchUnit

ArchUnit enforces architectural constraints as standard JUnit 5 tests: layer boundaries, package cycles, dependency direction, and naming conventions. It catches violations that PMD/Checkstyle cannot detect.

- **Full documentation**: [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md)
- **Configuration**: `config/archunit/archunit.properties`
- **Dependency**: `com.tngtech.archunit:archunit-junit5:1.4.1` (test scope)

Include ArchUnit tests in the unit test suite. They run in the standard `test` phase alongside other JUnit 5 tests.

## Build Tool (Maven/Gradle)

**Maven (`pom.xml`):**
```xml
<!-- Java 21 (LTS) -->
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
</properties>

<!-- Java 25 (LTS) -->
<properties>
    <java.version>25</java.version>
    <maven.compiler.source>${java.version}</maven.compiler.source>
    <maven.compiler.target>${java.version}</maven.compiler.target>
</properties>
```

**Gradle (`build.gradle.kts`):**
```kotlin
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))  // or 25
    }
}
```

## Dependency Version Selection (MANDATORY)

**When adding or updating any dependency, always select the latest version that has no known vulnerabilities.**

1. **Identify the latest published version** on Maven Central.
2. **Scan for vulnerabilities** using the OWASP Dependency-Check plugin.
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document why in the commit message.
4. **Never pin an older version out of habit** — always start from latest and work backwards only if forced by a CVE.

```bash
# Check for known vulnerabilities in your dependency tree
./gradlew dependencyCheckAnalyze
# Maven equivalent
mvn org.owasp:dependency-check-maven:check
```

**Commit message example when stepping back from latest:**
```
chore(deps): pin jackson-databind to 2.15.4 instead of 2.16.0

2.16.0 has CVE-2024-XXXXX (high severity, no fix yet).
2.15.4 is the latest clean version. Revisit when a patched 2.16.x is released.
```

See [CODING_PRACTICES.md](./CODING_PRACTICES.md#version-selection-rule-mandatory) for the full cross-ecosystem rule and audit tool table.

## Code Quality Tools

For the overarching static analysis philosophy, zero-tolerance policy, suppression strategy, incremental adoption, and cross-language tool matrix, see [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md). This section covers Java-specific configuration and integration.

**Static Analysis:**
- **PMD**: Code quality, complexity, and best-practice enforcement (see configuration below)
- **Checkstyle**: Formatting and style enforcement (see configuration below)
- **SpotBugs**: Bytecode bug detection -- null dereferences, concurrency issues, security vulnerabilities (see [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md))
- **Error Prone**: Compile-time bug detection
- **SonarQube**: Comprehensive code quality analysis
- **ArchUnit**: Architecture testing as unit tests -- layer boundaries, package cycles, dependency direction (see [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md))

**Same rules apply:**
- 20-line method maximum
- 0-2 private methods per class (SRP guideline)
- 80%+ unit test coverage (JaCoCo, unit tests only -- integration/E2E tests do not count toward coverage)
- No duplicated code

### Checkstyle Configuration

This repo provides a curated Checkstyle 10.x configuration at `config/checkstyle/checkstyle.xml`. Based on the Google Java Style Guide with thresholds aligned to our engineering standards.

**Key areas enforced:**

| Area | What It Enforces |
|------|-----------------|
| Javadoc | Public classes, methods, and constructors must have Javadoc with `@param`, `@return`, `@throws` |
| Naming | PascalCase types, camelCase methods/fields, SCREAMING_SNAKE constants |
| Imports | No wildcard imports, no unused imports, alphabetical ordering |
| Formatting | K&R braces, 4-space indentation, 120-char line length |
| Size limits | 20-line methods, 5 parameters max, 500-line files |
| Coding | No magic numbers, no fall-through in switch, equals/hashCode contract |

**Javadoc enforcement**: PMD's `CommentRequired` rule is intentionally excluded in favor of Checkstyle's more configurable Javadoc checks (`MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod`). Test methods annotated with `@Test`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, and `@Nested` are exempt from Javadoc requirements.

### Checkstyle Maven Integration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>config/checkstyle/checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
        <includeTestSourceRoots>false</includeTestSourceRoots>
        <linkXRef>false</linkXRef>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>com.puppycrawl.tools</groupId>
            <artifactId>checkstyle</artifactId>
            <version>10.21.1</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Run Checkstyle:
```bash
mvn checkstyle:check      # Run Checkstyle analysis
mvn checkstyle:checkstyle # Generate Checkstyle report without failing
mvn verify                # Runs Checkstyle as part of the build lifecycle
```

### Checkstyle Gradle Integration

```kotlin
// build.gradle.kts
plugins {
    checkstyle
}

checkstyle {
    toolVersion = "10.21.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    isShowViolations = true
    maxWarnings = 0
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    // Exclude test sources from Checkstyle
    exclude("**/test/**")
}
```

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'checkstyle'
}

checkstyle {
    toolVersion = "10.21.1"
    configFile = file("config/checkstyle/checkstyle.xml")
    ignoreFailures = false
    showViolations = true
    maxWarnings = 0
}
```

Run Checkstyle:
```bash
./gradlew checkstyleMain  # Run Checkstyle on main sources
./gradlew check           # Runs Checkstyle as part of the check lifecycle
```

### Checkstyle Suppression

When a rule produces a false positive, suppress it at the narrowest scope possible:

```java
// Suppress a specific check on a single element
@SuppressWarnings("checkstyle:MagicNumber")  // Justified: HTTP status codes are well-known
public static final int HTTP_OK = 200;

// Suppress Javadoc requirement on a specific method
@SuppressWarnings("checkstyle:MissingJavadocMethod")  // Internal helper, self-documenting name
int calculatePenalty(Finding finding) { ... }
```

**Guidelines:**
- Always include a justification comment explaining WHY the suppression is needed
- Prefer `@SuppressWarnings("checkstyle:CheckName")` over suppression filters
- Never disable checks globally to hide violations -- fix the code instead
- Track suppressions in code reviews

### PMD 7 Configuration

This repo provides a curated Java PMD 7 ruleset at `config/pmd/java-ruleset.xml`. Rules are selected individually (not bulk category imports) with thresholds aligned to our engineering standards.

**Key thresholds enforced:**

| Metric | Threshold | Standard Reference |
|--------|-----------|-------------------|
| Cyclomatic complexity | 10 per method | CODING_PRACTICES.md |
| Cognitive complexity | 15 per method | CODING_PRACTICES.md |
| Method length (NCSS) | 20 lines | JAVA_STANDARDS.md |
| Class length (NCSS) | 300 lines | CODING_PRACTICES.md |
| Parameter count | 5 max | CODING_PRACTICES.md |
| Nesting depth | 3 levels | CODING_PRACTICES.md |
| Duplicate literals | 3 max | DRY principle |

**Rule categories enabled:**
- **Best Practices** -- unused code detection, loose coupling, stack trace preservation, test best practices
- **Code Style** -- braces, imports, naming conventions, unnecessary constructs, modern Java patterns
- **Design** -- complexity limits, class size, God class detection, SRP enforcement, exception design
- **Error Prone** -- null checks, comparison errors, resource management, duplicate literals
- **Multithreading** -- synchronization issues, double-checked locking, thread safety
- **Performance** -- string handling, collection inefficiencies
- **Security** -- hard-coded crypto keys, insecure IVs
- **Documentation** -- uncommented empty constructors/methods

**Intentionally excluded rules** (with rationale documented in the XML):
- `AtLeastOneConstructor` -- too noisy for records and DTOs
- `OnlyOneReturn` -- multiple returns improve readability
- `LocalVariableCouldBeFinal` / `MethodArgumentCouldBeFinal` -- too noisy
- `LawOfDemeter` -- too many false positives with fluent APIs and streams
- `CommentRequired` -- handled by Checkstyle Javadoc rules instead (see `config/checkstyle/checkstyle.xml`)
- `AvoidInstantiatingObjectsInLoops` -- modern JVMs handle this via escape analysis

### PMD Maven Integration

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.26.0</version>
    <configuration>
        <rulesets>
            <ruleset>config/pmd/java-ruleset.xml</ruleset>
        </rulesets>
        <failOnViolation>true</failOnViolation>
        <printFailingErrors>true</printFailingErrors>
        <analysisCache>true</analysisCache>
        <analysisCacheLocation>${project.build.directory}/pmd/pmd.cache</analysisCacheLocation>
        <linkXRef>false</linkXRef>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>net.sourceforge.pmd</groupId>
            <artifactId>pmd-java</artifactId>
            <version>7.9.0</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
                <goal>cpd-check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Run PMD:
```bash
mvn pmd:check          # Run PMD analysis
mvn pmd:cpd-check      # Run copy-paste detection (DRY)
mvn pmd:pmd            # Generate PMD report without failing
mvn verify             # Runs PMD as part of the build lifecycle
```

### PMD Gradle Integration

```kotlin
// build.gradle.kts
plugins {
    pmd
}

pmd {
    toolVersion = "7.9.0"
    ruleSetFiles = files("config/pmd/java-ruleset.xml")
    ruleSets = listOf() // Clear defaults -- use only our custom ruleset
    isIgnoreFailures = false
    isConsoleOutput = true
}

tasks.withType<Pmd> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

```groovy
// build.gradle (Groovy DSL)
plugins {
    id 'pmd'
}

pmd {
    toolVersion = "7.9.0"
    ruleSetFiles = files("config/pmd/java-ruleset.xml")
    ruleSets = [] // Clear defaults -- use only our custom ruleset
    ignoreFailures = false
    consoleOutput = true
}
```

Run PMD:
```bash
./gradlew pmdMain      # Run PMD on main sources
./gradlew pmdTest      # Run PMD on test sources
./gradlew check        # Runs PMD as part of the check lifecycle
```

### CPD (Copy-Paste Detection)

CPD runs alongside PMD to detect duplicated code blocks, enforcing the DRY principle.

**Maven** -- CPD is included via `cpd-check` goal (see Maven snippet above). Configure the minimum token count:

```xml
<configuration>
    <!-- ... existing PMD config ... -->
    <minimumTokens>100</minimumTokens> <!-- Minimum token length for duplicate detection -->
</configuration>
```

**Gradle** -- CPD is included automatically with the `pmd` plugin. Configure via:

```kotlin
// build.gradle.kts
tasks.withType<Cpd> {
    minimumTokenCount.set(100)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

### PMD Incremental Analysis

PMD supports incremental analysis to speed up repeated runs. The analysis cache stores results from previous runs and only re-analyzes changed files.

**Maven**: Set `analysisCache` to `true` (included in the Maven snippet above).

**Gradle**: Enabled by default. The cache is stored at `build/pmd/pmd.cache`.

**CI/CD**: Cache the `pmd.cache` file between pipeline runs to reduce analysis time. See the CI/CD integration guide (issue #30) for details.

### PMD Suppression

When a rule produces a false positive, suppress it at the narrowest scope possible:

```java
// Suppress a specific rule on a single method
@SuppressWarnings("PMD.GodClass")  // Justified: orchestration class, refactoring tracked in JIRA-1234
public class OrderWorkflow { ... }

// Suppress in code with NOPMD comment (use sparingly)
String output = System.getenv("HOME"); // NOPMD - required for environment detection
```

**Guidelines:**
- Always include a justification comment explaining WHY the suppression is needed
- Prefer `@SuppressWarnings("PMD.RuleName")` over `// NOPMD` comments
- Never suppress rules globally in the ruleset to hide violations -- fix the code instead
- Track suppressions in code reviews

### SpotBugs Configuration

This repo provides a sample SpotBugs exclusion filter at `config/spotbugs/spotbugs-exclude.xml`. SpotBugs analyzes compiled bytecode to find bugs that source-level tools cannot detect. See [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md) for comprehensive standards.

**Key configuration:**

| Setting | Value | Rationale |
|---------|-------|-----------|
| Effort | `Max` | Full interprocedural analysis |
| Threshold | `Medium` | Balanced signal-to-noise ratio |
| Max rank | `14` | Fail on Scariest + Scary + Troubling bugs |
| Find Security Bugs | Always enabled | 138 OWASP Top 10 detectors |

**What SpotBugs catches that PMD/Checkstyle cannot:**
- Null pointer dereferences via data-flow analysis across methods
- Concurrency bugs (race conditions, deadlocks, inconsistent synchronization)
- Security vulnerabilities (SQL injection, XSS, path traversal, XXE)
- Resource leaks tracked through bytecode lifecycles
- Infinite recursive loops via call graph analysis

**Suppression**: Use `@SuppressFBWarnings(value = "BUG_PATTERN", justification = "reason")`. Justification is mandatory. See [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md) for annotation rules.

## Common Anti-Patterns to Avoid

### God Classes
```java
// Bad: Class doing too many things
public class ApplicationManager {
    public void processOrder() { ... }
    public void sendEmail() { ... }
    public void generateReport() { ... }
    public void syncInventory() { ... }
}

// Good: Split by responsibility
public class OrderProcessor { ... }
public class EmailNotifier { ... }
public class ReportGenerator { ... }
public class InventorySynchronizer { ... }
```

### Service Locator (Anti-Pattern)
```java
// Bad: Hidden dependencies
public class OrderService {
    public void process(Order order) {
        var repo = ServiceLocator.get(OrderRepository.class);  // Hidden!
        repo.save(order);
    }
}

// Good: Explicit constructor injection
public class OrderService {
    private final OrderRepository repository;
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

## SOLID Principles Notes

Use the guide in `docs/SOLID_PRINCIPLES.md` and apply these Java-specific practices:
- **SRP**: Use records for focused data carriers; extract services by domain responsibility.
- **OCP**: Use sealed interfaces + pattern matching (`switch` expressions) for known, closed type hierarchies; use Strategy pattern (interface + implementations) for open extension.
- **LSP**: Use sealed interfaces to control the type hierarchy; avoid `UnsupportedOperationException` -- redesign the abstraction instead.
- **ISP**: Define small, focused interfaces (`ScoreCalculator`, `ReportWriter`); use Java's ability to implement multiple interfaces to compose capabilities.
- **DIP**: Constructor injection exclusively (no field injection with `@Inject`); validate with `Objects.requireNonNull()`.

## Design Patterns Notes

Use the catalog in `docs/DESIGN_PATTERNS.md` and apply these Java-specific practices:
- **Strategy**: Prefer sealed interfaces for strategy families with a small, known set of implementations.
- **Factory Method/Abstract Factory**: Use static factory methods or factory interfaces; avoid reflection-based factories.
- **Builder**: Prefer builders for complex construction with optional fields; keep builders immutable.
- **Prototype**: Use copy constructors or record copy patterns instead of mutable cloning.
- **Singleton**: Avoid global state; if necessary, use enum singletons for safety.
- **Decorator/Proxy**: Use composition over inheritance; keep wrappers small and focused.

### Anemic Domain Model
```java
// Bad: Data class with no behavior, logic lives elsewhere
public class Order {
    private BigDecimal total;
    private List<LineItem> items;
    // Only getters/setters
}

public class OrderService {
    public BigDecimal calculateTotal(Order order) { ... }
}

// Good: Domain object with behavior
public class Order {
    private final List<LineItem> items;

    public BigDecimal calculateTotal() {
        return items.stream()
            .map(LineItem::subtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## Java Version Feature Summary

| Feature | Minimum Java Version | JEP |
|---------|---------------------|-----|
| Records | 16 | 395 |
| Sealed Classes | 17 | 409 |
| Pattern Matching for `instanceof` | 16 | 394 |
| Switch Pattern Matching | 21 | 441 |
| Text Blocks | 15 | 378 |
| Virtual Threads | 21 | 444 |
| Sequenced Collections | 21 | 431 |
| Unnamed Variables & Patterns (`_`) | 22 | 456 |
| Markdown Javadoc (`///`) | 23 | 467 |
| Stream Gatherers | 24 | 485 |
| Scoped Values | 25 | 506 |
| Module Import Declarations | 25 | 511 |
| Flexible Constructor Bodies | 25 | 513 |
| Compact Source Files / Instance Main | 25 | 512 |

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Language-agnostic coding standards
- [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) -- SOLID principles with multi-language examples
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Design patterns guidance
- [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md) -- Static analysis philosophy and cross-language tool matrix
- [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md) -- Architecture testing with ArchUnit (Java/Kotlin)

---

**Last Updated**: April 30, 2026
**Version**: 2.1
**Java Versions**: 21 (LTS), 25 (LTS)
