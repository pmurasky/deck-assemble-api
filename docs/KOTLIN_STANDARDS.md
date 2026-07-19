# Kotlin Coding Standards

## Overview
This document outlines Kotlin-specific coding standards that supplement the language-agnostic standards in `CODING_PRACTICES.md` and `CODING_STANDARDS.md`.

We support **Kotlin 2.3.0** and leverage modern Kotlin features where appropriate. Choose the version that matches your project.

## Official Style Guide
We follow the [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) 
with the following additions and clarifications.

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
- Each package represents a cohesive functional area
- Minimize cross-package dependencies
- Keep related functionality together

### Naming Conventions

Following Kotlin conventions:
- **Classes/Interfaces/Objects**: PascalCase (`OrderService`, `PaymentProcessor`)
- **Functions/Properties**: camelCase (`calculateTotal`, `orderCount`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Packages**: lowercase, no underscores (`com.example.project.order`)
- **Test Classes**: Same as source + `Test` suffix (`OrderServiceTest`)

### Immutability

**Prefer immutable data structures:**
- Use `val` over `var` whenever possible
- Use `data class` for immutable data transfer objects
- Use immutable collections: `listOf()`, `mapOf()`, `setOf()`
- When mutation needed, use mutable versions explicitly: `mutableListOf()`, etc.

**Example:**
```kotlin
// ✅ Good: Immutable
data class ScoreResult(
    val className: String,
    val score: Int,
    val penalties: List<Penalty>
)

// ❌ Bad: Mutable by default
data class ScoreResult(
    var className: String,
    var score: Int,
    val penalties: MutableList<Penalty>
)
```

### Null Safety

**Avoid `!!` operator:**
- Use safe calls `?.` for nullable access
- Use Elvis operator `?:` for default values
- Use `requireNotNull()` or `checkNotNull()` with descriptive messages
- Prefer non-nullable types in APIs

**Examples:**
```kotlin
// ✅ Good
val name = config.projectName ?: "unknown"
val dir = requireNotNull(projectDir) { "Project directory must be specified" }

// ❌ Bad
val name = config.projectName!!
```

### Function Design

Apply the same standards from CODING_PRACTICES.md:
- **Maximum 20 lines per function** (excluding blanks/braces)
- Prefer expression bodies for simple functions
- Use named arguments for functions with multiple parameters
- Default parameter values where appropriate

**Examples:**
```kotlin
// ✅ Good: Expression body for simple function
fun calculateScore(penalties: Int): Int = 
    (100 - penalties).coerceIn(0, 100)

// ✅ Good: Named arguments for clarity
fun generateReport(
    scores: List<Score>,
    outputDir: Path,
    format: ReportFormat = ReportFormat.MARKDOWN
)
```

### Ktor Route Function Exception

Ktor route extension functions used for route wiring/registration that receive injected services as parameters are exempt from the 5-parameter limit, because Ktor's routing DSL does not provide a DI container in the route scope. Instead:
- Group related services into a "context" or "dependencies" object if >7 parameters
- For ≤7 parameters, direct parameter passing is acceptable without `@Suppress`
- Document the DI limitation in a comment at the route registration site
- Route handlers and business logic functions must still follow the normal 5-parameter limit and use a context/dependencies object when more inputs are required

**Example:**
```kotlin
// ✅ Acceptable: Ktor route with 6 service parameters (no @Suppress needed)
fun Route.segmentRoutes(
    segmentService: SegmentService,
    validationService: ValidationService,
    authService: AuthService,
    auditLogger: AuditLogger,
    metricsCollector: MetricsCollector,
    errorHandler: ErrorHandler
) {
    // Route implementations...
}

// ✅ Better for >7 parameters: group into dependencies object
class SegmentRouteDeps(
    val segmentService: SegmentService,
    val validationService: ValidationService,
    val authService: AuthService,
    // ... more services
)

fun Route.segmentRoutes(deps: SegmentRouteDeps) {
    // Route implementations using deps.segmentService, etc.
}
```

### Extension Functions

**Use appropriately:**
- Enhance readability when operation is conceptually "on" the receiver
- Don't overuse - maintain discoverability
- Keep in appropriate util packages or companion objects
- Document well if not obvious

**Example:**
```kotlin
// ✅ Good: Natural extension
fun Path.ensureDirectory(): Path {
    if (!exists()) createDirectories()
    return this
}

// ❌ Bad: Unclear extension
fun String.process(): Result // What does this do?
```

### Data Classes and Sealed Classes

**Use data classes for:**
- DTOs (Data Transfer Objects)
- Configuration models
- Immutable value objects
- Automatically gets `equals()`, `hashCode()`, `toString()`, `copy()`

**Use sealed classes for:**
- Restricted type hierarchies
- Result types (Success/Failure patterns)
- Exhaustive `when` expressions

**Example:**
```kotlin
sealed class ParseResult {
    data class Success(val findings: List<Finding>) : ParseResult()
    data class Failure(val error: String) : ParseResult()
}
```

### Coroutines

**Use for async operations (if needed):**
- Parallel I/O operations
- Concurrent API calls
- Use structured concurrency
- Handle cancellation properly
- Prefer `suspend` functions over returning `Deferred`

### Structured Concurrency Best Practices

Always use `coroutineScope` or `supervisorScope` — never launch detached coroutines from `GlobalScope` in production code.

```kotlin
// ✅ coroutineScope: all children must complete; first failure cancels siblings
suspend fun fetchAll(ids: List<String>): List<Data> = coroutineScope {
    ids.map { id -> async { fetch(id) } }.awaitAll()
}

// ✅ supervisorScope: children fail independently — use for dashboards, batch jobs
suspend fun loadDashboard(): DashboardData = supervisorScope {
    val orders = async { loadOrders() }
    val metrics = async { loadMetrics() }
    DashboardData(
        orders = runCatching { orders.await() }.getOrNull(),
        metrics = runCatching { metrics.await() }.getOrNull(),
    )
}

// ✅ CoroutineExceptionHandler for top-level error handling
val handler = CoroutineExceptionHandler { _, throwable ->
    logger.error("Unhandled coroutine exception", throwable)
}
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
```

**`kotlinx.coroutines` version:** Use `1.9.x` or latest stable. Minimum `1.8.0` required for K2 compatibility.

### Flow Best Practices

```kotlin
// ✅ Use StateFlow for UI state (hot, always has a value)
private val _state = MutableStateFlow(UiState.Loading)
val state: StateFlow<UiState> = _state.asStateFlow()

// ✅ Use SharedFlow for events (hot, no initial value)
private val _events = MutableSharedFlow<UiEvent>()
val events: SharedFlow<UiEvent> = _events.asSharedFlow()

// ✅ Cold flow for data pipelines
fun orderUpdates(orderId: String): Flow<Order> = flow {
    while (true) {
        emit(repository.findById(orderId))
        delay(5.seconds)
    }
}.flowOn(Dispatchers.IO)

// ✅ collectLatest for UI — cancels previous when new value arrives
viewModelScope.launch {
    searchQuery.collectLatest { query ->
        updateResults(search(query))
    }
}
```

### Collections and Sequences

**Prefer idiomatic collection operations:**
- Use `map`, `filter`, `fold`, etc. over manual loops
- Use sequences for large collections or chained operations
- Leverage `associateBy`, `groupBy`, `partition` for clarity

**Example:**
```kotlin
// ✅ Good: Idiomatic
val scoresByClass = findings
    .groupBy { it.className }
    .mapValues { (_, findings) -> calculateScore(findings) }

// ❌ Bad: Imperative
val scoresByClass = mutableMapOf<String, Int>()
for (finding in findings) {
    // manual grouping logic...
}
```

### Exception Handling

**Kotlin patterns:**
- Use `runCatching` for try-catch blocks that return Result
- Throw specific exception types
- Document thrown exceptions in KDoc
- Use `use` for auto-closeable resources (replaces try-with-resources)

**Example:**
```kotlin
fun readConfig(path: Path): Result<Config> = runCatching {
    path.toFile().inputStream().use { stream ->
        objectMapper.readValue(stream, Config::class.java)
    }
}
```

### Documentation (KDoc)

**Requirements:**
- All public classes, functions, and properties must have KDoc
- Use KDoc format (similar to Javadoc):
  ```kotlin
  /**
   * Calculates the quality score for a class based on findings.
   *
   * @param findings List of code quality findings
   * @param weights Scoring weights for each category
   * @return Score between 0 and 100
   * @throws IllegalArgumentException if weights are invalid
   */
  fun calculateScore(findings: List<Finding>, weights: Weights): Int
  ```
- Include examples for complex APIs
- Document non-obvious behavior

### Testing in Kotlin

**Framework:** JUnit 5
- Use `@Test` annotation
- Leverage Kotlin features in tests (named arguments, backticks for test names)
- Consider MockK for mocking (more Kotlin-friendly than Mockito)
- Data classes simplify test assertions
- Use **Kotest Assertions** for fluent, idiomatic Kotlin assertions

### Power Assert Plugin (Kotlin 2.0+)

Enable the Power Assert compiler plugin for enhanced assertion messages — it shows the intermediate values in a failing expression without extra code.

```kotlin
// In build.gradle.kts
plugins {
    kotlin("plugin.power-assert") version "2.3.0"
}

powerAssert {
    functions = listOf("kotlin.assert", "kotlin.test.assertTrue", "kotlin.test.assertEquals")
}
```

```kotlin
// With Power Assert enabled, a failing assert like:
assert(user.age > 18 && user.active)
// Produces:
// assert(user.age > 18 && user.active)
//        |    |   |       |    |
//        User 17  false   User false
```

**Example:**
```kotlin
@Test
fun `should calculate correct score with multiple penalties`() {
    // Given
    val findings = listOf(
        Finding("Foo.java", 10, Severity.HIGH),
        Finding("Foo.java", 20, Severity.LOW)
    )
    
    // When
    val score = scorer.calculateScore(findings)
    
    // Then
    score shouldBe 82
}
```

### Kotest Assertions

Kotest Assertions is the preferred assertion library for Kotlin. It provides an idiomatic infix style, 350+ matchers, and native null-safety awareness with no Java interop friction. It works standalone with the JUnit 5 runner — the full Kotest framework is not required.

**Dependency** (test scope):
```kotlin
testImplementation("io.kotest:kotest-assertions-core:5.9.1")
```

**Infix style:**
```kotlin
name shouldBe "Frodo"
list shouldHaveSize 3
result shouldNotBe null
age shouldBeGreaterThan 0
```

**Chaining:**
```kotlin
"substring".shouldContain("str").shouldBeLowerCase()
```

**Collection matchers:**
```kotlin
list.shouldContainExactly(a, b, c)
list.shouldContainInOrder(a, b)
list.shouldHaveSize(3)
map.shouldContainKey("id")
```

**Exception assertions:**
```kotlin
shouldThrow<IllegalArgumentException> {
    myFunction(invalidInput)
}
```

**Soft assertions** — collect all failures before reporting:
```kotlin
assertSoftly {
    name shouldBe "Alice"
    age shouldBe 30
    email shouldContain "@"
}
```

**Failure context with `withClue`:**
```kotlin
withClue("user should be active after registration") {
    user.active shouldBe true
}
```

> **AssertJ note:** AssertJ is acceptable for Kotlin teams migrating from Java. Kotest Assertions is preferred for new Kotlin projects.

### Architecture Testing with ArchUnit

ArchUnit enforces architectural constraints as standard JUnit 5 tests: layer boundaries, package cycles, dependency direction, and naming conventions. ArchUnit has first-class Kotlin support and analyzes compiled bytecode, so data classes, companion objects, and extension functions are all visible.

- **Full documentation**: [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md)
- **Configuration**: `config/archunit/archunit.properties`
- **Dependency**: `com.tngtech.archunit:archunit-junit5:1.4.1` (test scope)
- **Note**: Kotlin `internal` visibility compiles to `public` + `@JvmSynthetic` -- ArchUnit sees these as public. Be aware when writing visibility-based rules.

Use detekt for Kotlin code style (naming, complexity, formatting) and ArchUnit for architecture enforcement. They serve complementary purposes.

### Gradle Kotlin DSL

**Use `build.gradle.kts`:**
- Type-safe build configuration
- Leverage IDE support
- Use explicit types where ambiguity exists
- Organize dependencies logically

**Example:**
```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    application
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
```

## Dependency Version Selection (MANDATORY)

**When adding or updating any dependency, always select the latest version that has no known vulnerabilities.**

1. **Identify the latest published version** on Maven Central.
2. **Scan for vulnerabilities** using the OWASP Dependency-Check Gradle plugin.
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document why in the commit message.
4. **Never pin an older version out of habit** — always start from latest and work backwards only if forced by a CVE.

```bash
# Check for known vulnerabilities in your dependency tree
./gradlew dependencyCheckAnalyze
```

**Commit message example when stepping back from latest:**
```
chore(deps): pin jackson-module-kotlin to 2.15.4 instead of 2.16.0

2.16.0 has CVE-2024-XXXXX (high severity, no fix yet).
2.15.4 is the latest clean version. Revisit when a patched 2.16.x is released.
```

See [CODING_PRACTICES.md](./CODING_PRACTICES.md#version-selection-rule-mandatory) for the full cross-ecosystem rule and audit tool table.

## Code Quality Tools

For the overarching static analysis philosophy, zero-tolerance policy, suppression strategy, incremental adoption, and cross-language tool matrix, see [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md). This section covers Kotlin-specific configuration and integration.

**Static Analysis:**
- **detekt**: Primary Kotlin static analysis tool (comprehensive rule set)
- **PMD**: Supplementary analysis (limited Kotlin support: 2 rules)
- **ktlint**: Formatting and linting
- **ArchUnit**: Architecture testing as unit tests -- layer boundaries, package cycles, dependency direction (see [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md))

**Same rules apply:**
- 20-line method maximum
- 0-2 private method guideline
- 80%+ unit test coverage (unit tests only -- integration/E2E tests do not count toward coverage)
- No duplicated code

### detekt vs PMD: When to Use Each

| Concern | Tool | Why |
|---------|------|-----|
| Complexity (method length, class size, cyclomatic) | **detekt** | Full Kotlin AST support with configurable thresholds |
| Naming conventions | **detekt** | Understands Kotlin naming patterns (backtick tests, companion objects) |
| Style and idioms | **detekt** | Kotlin-specific rules (scope functions, expression bodies, etc.) |
| Performance | **detekt** | Detects spread operator misuse, `ForEachOnRange`, sequences |
| Potential bugs | **detekt** | Nullable type safety, unreachable code, mutable state issues |
| Exception handling | **detekt** | Swallowed exceptions, too-generic catch/throw |
| Coroutine correctness | **detekt** | `GlobalScope` usage, `sleep` vs `delay`, dispatcher injection |
| KDoc documentation | **detekt** | Undocumented public APIs, outdated docs |
| Function name length | **PMD** | `FunctionNameTooShort` (min 3 chars) |
| equals/hashCode contract | **PMD** | `OverrideBothEqualsAndHashcode` (catches manual overrides) |

**Recommendation**: Run both tools. detekt handles the vast majority of analysis; PMD catches two additional edge cases that detekt does not duplicate.

### detekt Configuration

This repo provides a curated detekt configuration at `config/detekt/detekt.yml`. Thresholds are aligned with our engineering standards.

**Key thresholds enforced:**

| Metric | Threshold | Standard Reference |
|--------|-----------|-------------------|
| Cyclomatic complexity | 10 per method | CODING_PRACTICES.md |
| Cognitive complexity | 15 per method | CODING_PRACTICES.md |
| Method length | 20 lines | KOTLIN_STANDARDS.md |
| Class size | 300 lines | CODING_PRACTICES.md |
| Parameter count | 5 max (5 triggers) | CODING_PRACTICES.md |
| Nesting depth | 3 levels | CODING_PRACTICES.md |
| Max return statements | 3 | CODING_PRACTICES.md |
| String literal duplication | 3 max | DRY principle |
| Max line length | 120 chars | Kotlin conventions |

**Rule sets enabled:**
- **Complexity** -- method length, class size, cyclomatic/cognitive complexity, parameter count, nesting depth
- **Coroutines** -- `GlobalScope` usage, dispatcher injection, `sleep` vs `delay`
- **Empty Blocks** -- empty catch/if/when/for/while/try blocks
- **Exceptions** -- too-generic catch/throw, swallowed exceptions, stack trace preservation
- **Naming** -- class/function/variable naming patterns, package naming, boolean prefixes
- **Performance** -- spread operator, `ForEachOnRange`, array primitives, sequences
- **Potential Bugs** -- null safety, unreachable code, mutable collections, platform types
- **Style** -- magic numbers, wildcard imports, unused code, expression bodies, idiomatic Kotlin
- **Comments** -- undocumented public APIs, outdated KDoc, deprecated block tags

### PMD 7 Kotlin Configuration

This repo provides a Kotlin PMD 7 ruleset at `config/pmd/kotlin-ruleset.xml`. PMD 7 has limited Kotlin support (2 rules across 2 categories):

- **Best Practices**: `FunctionNameTooShort` -- function names must be at least 3 characters
- **Error Prone**: `OverrideBothEqualsAndHashcode` -- must override both or neither

### Gradle Integration

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    pmd
}

// detekt configuration
detekt {
    config.setFrom("config/detekt/detekt.yml")
    buildUponDefaultConfig = true  // Use our config on top of defaults
    allRules = false               // Only activate rules marked active: true
    parallel = true                // Faster analysis on multi-core machines
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
    }
}

// PMD configuration (supplementary)
pmd {
    toolVersion = "7.21.0"
    ruleSetFiles = files("config/pmd/kotlin-ruleset.xml")
    ruleSets = listOf()  // Clear defaults -- use only our custom ruleset
    isIgnoreFailures = false
    isConsoleOutput = true
}

tasks.withType<Pmd> {
    // PMD needs the Kotlin module for Kotlin file analysis
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

Run analysis:
```bash
./gradlew detekt           # Run detekt on all sources
./gradlew detektMain       # Run detekt on main sources only
./gradlew detektTest       # Run detekt on test sources only
./gradlew pmdMain          # Run PMD on main sources
./gradlew check            # Runs both as part of the check lifecycle
```

### Suppression Strategies

#### detekt Suppression

When a rule produces a false positive, suppress at the narrowest scope:

```kotlin
// Suppress a specific rule on a function
@Suppress("TooGenericExceptionCaught")  // Justified: top-level error handler
fun handleRequest(request: Request): Response {
    return try {
        processRequest(request)
    } catch (e: Exception) {
        Response.error(e.message ?: "Unknown error")
    }
}

// Suppress on a class
@Suppress("TooManyFunctions")  // Justified: test class with many test cases
class OrderServiceTest { ... }
```

**Guidelines:**
- Always include a justification comment explaining WHY the suppression is needed
- Suppress at the narrowest scope possible (function > class > file)
- Never suppress rules globally in `detekt.yml` to hide violations -- fix the code instead
- Use `@Suppress("RuleName")` (Kotlin standard annotation)
- Track suppressions in code reviews
- Consider `//noinspection` for IDE-level suppressions only

#### PMD Suppression

```kotlin
// Suppress with annotation
@SuppressWarnings("PMD.FunctionNameTooShort")  // Justified: domain-specific abbreviation
fun tx(): Transaction { ... }

// Suppress with NOPMD comment (use sparingly)
fun eq(other: Any?): Boolean = this == other  // NOPMD - intentional short name for DSL
```

#### Baseline Files

For existing projects adopting these standards, use detekt's baseline feature to track pre-existing issues without blocking CI:

```bash
# Generate baseline of current violations
./gradlew detektBaseline

# detekt will then only report NEW violations
```

Configure in `build.gradle.kts`:
```kotlin
detekt {
    baseline = file("config/detekt/baseline.xml")
}
```

**Important**: Baseline files are a migration tool, not a permanent suppression mechanism. Plan to eliminate baseline violations over time.

## Common Kotlin Idioms

**Scope Functions:**
- `apply`: Configure objects
- `let`: Transform or null-safe operations
- `run`: Execute block and return result
- `also`: Side effects
- Use appropriately - don't overuse

**Other Idioms:**
- Destructuring for data classes: `val (name, score) = result`
- `when` expressions instead of complex if-else chains
- String templates: `"Score: $score"` or `"Score: ${obj.score}"`
- Range expressions: `if (score in 0..100)`
- Elvis with return/throw: `val dir = projectDir ?: return`

## SOLID Principles Notes

Use the guide in `docs/SOLID_PRINCIPLES.md` and apply these Kotlin-specific practices:
- **SRP**: Use data classes for focused value objects; extract responsibilities into separate classes with constructor injection.
- **OCP**: Use sealed interfaces with `when` for known type hierarchies (compiler-enforced exhaustiveness); use interfaces + implementations for open extension.
- **LSP**: Sealed classes/interfaces help control substitution; avoid throwing `UnsupportedOperationException` in overrides -- redesign the hierarchy instead.
- **ISP**: Kotlin supports multiple interface implementation natively; keep interfaces small (1-3 methods) and compose them.
- **DIP**: Use constructor parameters with interface types; leverage default parameter values for production defaults while enabling test injection.

## Design Patterns Notes

Use the catalog in `docs/DESIGN_PATTERNS.md` and apply these Kotlin-specific practices:
- **Strategy/State**: Prefer sealed interfaces with `when` for exhaustive handling.
- **Builder**: Prefer named arguments and default parameters before introducing a builder.
- **Singleton**: Use `object` declarations; avoid global mutable state.
- **Decorator/Proxy**: Use delegation (`by`) to compose behavior cleanly.
- **Factory Method**: Use companion object factories for clarity and validation.

## K2 Compiler (Kotlin 2.0+)

Kotlin 2.0 ships the **K2 compiler** as the default. For developers, the practical changes are:

### What changed
- **Compilation speed**: 2× faster compilation on average for large projects
- **Improved smart casts**: K2 performs more accurate data-flow analysis — previously ambiguous casts that required explicit checks now resolve automatically
- **Stricter inference**: Some edge-case code that compiled on K1 now produces warnings or errors; treat these as correctness improvements, not regressions

### Smart cast improvements

```kotlin
// K1: required explicit check even after null-check
class Container(val value: String?)

fun process(c: Container) {
    if (c.value != null) {
        val length = c.value.length  // K1 sometimes needed c.value!!; K2 handles it
    }
}

// K2: smart cast works through property access in more cases
data class Config(val host: String?, val port: Int?)

fun connect(cfg: Config) {
    requireNotNull(cfg.host) { "host required" }
    requireNotNull(cfg.port) { "port required" }
    openConnection(cfg.host, cfg.port)  // K2: both smart-cast to non-null here
}
```

### Migration checklist
- [ ] Set `kotlin.jvm.target.validation.mode=warning` in `gradle.properties` to catch JVM target mismatches
- [ ] Upgrade `kotlinx.coroutines` to 1.8.0+ before enabling K2 (earlier versions have K2 incompatibilities)
- [ ] Run `./gradlew build` with K2 and address new warnings — most are correctness improvements
- [ ] `languageVersion = "2.0"` in your Kotlin compiler options (or use `kotlin("jvm") version "2.0.0"+`)

## Kotlin 2.3.0 Features

Leverage modern Kotlin features:
- Context receivers (if appropriate)
- Context parameters (Kotlin 2.x experimental) — successor to context receivers; pass contextual dependencies implicitly without adding them to every function signature. Status: experimental in 2.x, use with caution in production
- Improved type inference
- Inline value classes for type safety
- Follow Kotlin 2.0+ idioms

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Language-agnostic coding standards
- [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) -- SOLID principles with multi-language examples
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Design patterns guidance
- [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md) -- Static analysis philosophy and cross-language tool matrix
- [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md) -- Architecture testing with ArchUnit (Java/Kotlin)

---

**Last Updated**: 2026-04-30  
**Version**: 1.3  
**Kotlin Version**: 2.3.0
