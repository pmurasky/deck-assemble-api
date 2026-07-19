# ArchUnit Architecture Testing Standards

## Overview

ArchUnit is a library for checking the architecture of Java and Kotlin code. It verifies dependency direction, layer boundaries, package cycles, naming conventions, and module isolation -- all as regular unit tests.

### Why ArchUnit

Static analysis tools (PMD, Checkstyle, detekt) enforce **code-level** quality: method length, complexity, style. ArchUnit enforces **structural/architectural** quality that these tools cannot detect:

| Concern | PMD / Checkstyle / detekt | ArchUnit |
|---------|--------------------------|----------|
| Layer violations (controller -> service -> persistence) | Cannot detect | Core strength |
| Package cycle detection | Cannot detect | Detects and reports full cycle paths |
| Dependency direction enforcement | Cannot detect | Enforces which packages may depend on which |
| Architecture style (layered, onion, hexagonal) | Cannot enforce | Built-in support |
| Naming-location contracts ("Controllers live in controller package") | Limited pattern matching | Full cross-class enforcement |
| Module boundary / API enforcement | Cannot enforce | Enforces allowed dependencies between modules |
| Inheritance / annotation contracts across classes | Basic checks only | Full cross-class relationship checks |

**Use both**: PMD/detekt for code quality, ArchUnit for architecture quality. They are fully complementary.

### Relationship to Engineering Standards

ArchUnit enforces architectural constraints that support our SOLID principles:

| Principle | ArchUnit Enforcement |
|-----------|---------------------|
| **SRP** | Layer rules prevent classes from spanning responsibilities |
| **OCP** | Dependency direction rules enforce extension points |
| **LSP** | Inheritance checks verify substitutability contracts |
| **ISP** | Package dependency rules enforce focused interfaces |
| **DIP** | Layer rules ensure high-level modules don't depend on low-level details |

See [CODING_PRACTICES.md](./CODING_PRACTICES.md) for the full set of coding standards and [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) for SOLID principle deep-dives.

### Zero-Tolerance Policy

**ArchUnit test failures MUST fail the build.** Architecture tests are a hard gate, not advisory.

- **Local development**: Failures fail the build immediately
- **CI/CD pipelines**: Failures block the PR from merging
- **Legacy codebases**: Use [Freezing Arch Rules](#freezing-arch-rules-for-legacy-codebases) to adopt incrementally -- never disable tests

---

## Installation

### Version

Use ArchUnit **1.4.1+** (latest stable as of this writing).

### Maven (JUnit 5)

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.4.1</version>
    <scope>test</scope>
</dependency>
```

### Gradle (JUnit 5)

```kotlin
dependencies {
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
}
```

### Maven Plugin (Alternative)

The [arch-unit-maven-plugin](https://github.com/societe-generale/arch-unit-maven-plugin) by Societe Generale runs ArchUnit rules from Maven configuration without writing test classes. Useful for enforcing rules across multiple projects from a shared config.

ArchUnit tests are regular unit tests -- no special build plugin is required. They run in the standard `test` phase.

---

## Configuration

### `archunit.properties`

Place at `src/test/resources/archunit.properties`. This repo provides a sample configuration at `config/archunit/archunit.properties`.

```properties
# ===== Resolution Behavior =====
# Avoid resolving transitive dependencies from classpath for faster tests
resolveMissingDependenciesFromClassPath=false

# ===== Cycle Detection =====
# Max cycles to detect (default 100). Reduce for faster CI.
cycles.maxNumberToDetect=50
# Max dependencies per cycle edge in report (default 20)
cycles.maxNumberOfDependenciesPerEdge=10

# ===== Freezing Arch Rules =====
# Path for violation store (commit to VCS)
freeze.store.default.path=archunit_store
# CI: prevent accidental store creation
freeze.store.default.allowStoreCreation=false
# Allow store updates when violations are fixed
freeze.store.default.allowStoreUpdate=true

# ===== Empty Rule Safety =====
# Fail if "should" evaluates against zero classes (prevents dead rules)
archRule.failOnEmptyShould=true
```

Override any property via system property: `-Darchunit.propertyName=value`.

### Key Configuration Decisions

| Property | Default | Recommendation | Rationale |
|----------|---------|----------------|-----------|
| `resolveMissingDependenciesFromClassPath` | `true` | `false` | Faster tests; only import your own code |
| `cycles.maxNumberToDetect` | `100` | `50` | Sufficient for actionable reports; reduces CPU |
| `archRule.failOnEmptyShould` | `true` | `true` (keep) | Prevents dead rules that silently pass |
| `freeze.store.default.allowStoreCreation` | `true` | `false` in CI | Prevents accidental baseline resets |

---

## Recommended Rules by Tier

### Tier 1: Critical (Adopt Immediately)

These rules provide the highest value and should be adopted in every project.

#### Layered Architecture

Enforces that layers only access allowed dependencies. This is the single most valuable ArchUnit check.

**Java:**
```java
@ArchTest
static final ArchRule layered_architecture_is_respected =
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Persistence").definedBy("..persistence..")
        .layer("Domain").definedBy("..domain..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Persistence")
        .because("layer boundaries enforce separation of concerns (SRP) and dependency inversion (DIP)");
```

**Kotlin:**
```kotlin
@ArchTest
val layered_architecture_is_respected: ArchRule =
    layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Persistence").definedBy("..persistence..")
        .layer("Domain").definedBy("..domain..")
        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
        .whereLayer("Domain").mayOnlyBeAccessedByLayers("Service", "Persistence")
        .because("layer boundaries enforce separation of concerns (SRP) and dependency inversion (DIP)")
```

#### Package Cycle Detection

Circular package dependencies are the single biggest cause of accidental complexity. This rule prevents them.

**Java:**
```java
@ArchTest
static final ArchRule no_package_cycles =
    slices().matching("com.myapp.(*)..").should().beFreeOfCycles()
        .because("package cycles create tight coupling and make refactoring impossible");
```

**Kotlin:**
```kotlin
@ArchTest
val no_package_cycles: ArchRule =
    slices().matching("com.myapp.(*)..").should().beFreeOfCycles()
        .because("package cycles create tight coupling and make refactoring impossible")
```

#### General Coding Rules

ArchUnit ships with pre-built rules for common violations:

**Java:**
```java
@ArchTest
static final ArchRule no_standard_streams =
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("use SLF4J/Logback instead of System.out/err");

@ArchTest
static final ArchRule no_generic_exceptions =
    GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
        .because("throw specific exceptions for meaningful error handling");

@ArchTest
static final ArchRule no_field_injection =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
        .because("use constructor injection for testability and explicit dependencies (DIP)");

@ArchTest
static final ArchRule no_java_util_logging =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
        .because("use SLF4J as the logging facade for consistency");
```

**Kotlin:**
```kotlin
@ArchTest
val no_standard_streams: ArchRule =
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("use SLF4J/Logback instead of System.out/err")

@ArchTest
val no_generic_exceptions: ArchRule =
    GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
        .because("throw specific exceptions for meaningful error handling")

@ArchTest
val no_field_injection: ArchRule =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
        .because("use constructor injection for testability and explicit dependencies (DIP)")

@ArchTest
val no_java_util_logging: ArchRule =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
        .because("use SLF4J as the logging facade for consistency")
```

### Tier 2: High Value (Adopt After Tier 1)

#### Dependency Direction Rules

Enforce that lower layers never depend on higher layers:

```java
@ArchTest
static final ArchRule services_should_not_depend_on_controllers =
    noClasses().that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..controller..")
        .because("services must not know about the presentation layer (DIP)");

@ArchTest
static final ArchRule persistence_should_not_depend_on_services =
    noClasses().that().resideInAPackage("..persistence..")
        .should().dependOnClassesThat().resideInAPackage("..service..")
        .because("persistence must not know about business logic (DIP)");
```

**Important**: Use `dependOnClassesThat` (not `accessClassesThat`) for stricter enforcement. `dependOn` catches field types, method parameter types, extends/implements, and annotations -- not just runtime method calls.

#### Naming Convention Enforcement

Enforce that class names match their package location:

```java
@ArchTest
static final ArchRule controllers_should_be_suffixed =
    classes().that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith("Controller")
        .because("naming consistency makes the codebase navigable");

@ArchTest
static final ArchRule services_should_be_suffixed =
    classes().that().resideInAPackage("..service..")
        .and().areNotInterfaces()
        .should().haveSimpleNameEndingWith("Service").orShould().haveSimpleNameEndingWith("ServiceImpl")
        .because("naming consistency makes the codebase navigable");

@ArchTest
static final ArchRule repositories_should_be_suffixed =
    classes().that().resideInAPackage("..repository..")
        .should().haveSimpleNameEndingWith("Repository")
        .because("naming consistency makes the codebase navigable");
```

#### Annotation Enforcement

Enforce framework annotations are used correctly:

```java
@ArchTest
static final ArchRule spring_controllers_should_be_annotated =
    classes().that().haveSimpleNameEndingWith("Controller")
        .should().beAnnotatedWith("org.springframework.web.bind.annotation.RestController")
        .orShould().beAnnotatedWith("org.springframework.stereotype.Controller")
        .because("Spring requires controller annotations for request mapping");

@ArchTest
static final ArchRule logger_fields_should_be_private_static_final =
    fields().that().haveRawType(org.slf4j.Logger.class)
        .should().bePrivate()
        .andShould().beStatic()
        .andShould().beFinal()
        .because("logger fields must follow the standard private static final pattern");
```

### Tier 3: Advanced (For Large / DDD Projects)

#### Onion Architecture (Hexagonal / Ports & Adapters)

For domain-driven design projects:

```java
@ArchTest
static final ArchRule onion_architecture_is_respected =
    onionArchitecture()
        .domainModels("com.myapp.domain.model..")
        .domainServices("com.myapp.domain.service..")
        .applicationServices("com.myapp.application..")
        .adapter("cli", "com.myapp.adapter.cli..")
        .adapter("persistence", "com.myapp.adapter.persistence..")
        .adapter("rest", "com.myapp.adapter.rest..")
        .because("domain must not depend on infrastructure (DIP)");
```

This enforces:
- Domain has no outward dependencies
- Adapters don't depend on each other
- Application can use domain but not vice versa
- Only adapters depend on external frameworks

#### Module Rules

For large monoliths or modulith architectures:

```java
@ArchTest
static final ArchRule modules_should_be_free_of_cycles =
    modules().definedByPackages("com.myapp.(*)..").should().beFreeOfCycles()
        .because("module cycles prevent independent deployment and testing");
```

For annotation-based module definitions (e.g., with a custom `@AppModule` annotation):

```java
@ArchTest
static final ArchRule modules_respect_declared_dependencies =
    modules()
        .definedByAnnotation(AppModule.class)
        .should().respectTheirAllowedDependenciesDeclaredIn("allowedDependencies",
            consideringOnlyDependenciesInAnyPackage("..myapp.."))
        .andShould().onlyDependOnEachOtherThroughPackagesDeclaredIn("exposedPackages")
        .because("module boundaries must be enforced to maintain modularity");
```

#### PlantUML Diagram Enforcement

Derive architecture rules directly from architecture diagrams:

```java
@ArchTest
static final ArchRule architecture_matches_diagram =
    classes().should(adhereToPlantUmlDiagram(
        getClass().getResource("/architecture.puml"),
        consideringAllDependencies()))
        .because("code must match the documented architecture");
```

This enables "living documentation" -- the architecture diagram is both documentation and an executable test.

#### Architecture Metrics

ArchUnit can compute software architecture metrics (Lakos, Martin):

```java
JavaClasses classes = new ClassFileImporter().importPackages("com.myapp");
MetricsComponents<JavaClass> components = MetricsComponents.fromPackages(classes.getPackages());

LakosMetrics lakos = ArchitectureMetrics.lakosMetrics(components);
System.out.println("Cumulative Component Dependency: " + lakos.getCumulativeComponentDependency());

Set<ComponentDependencyMetrics> martinMetrics = ArchitectureMetrics.componentDependencyMetrics(components);
martinMetrics.forEach(m -> {
    System.out.printf("Package %s: Instability=%.2f, Abstractness=%.2f, Distance=%.2f%n",
        m.getName(), m.getInstability(), m.getAbstractness(),
        m.getNormalizedDistanceFromMainSequence());
});
```

Use these metrics for trend analysis -- track instability and abstractness over time in dashboards.

---

## Test Organization

### Recommended Structure

Organize ArchUnit tests by concern in dedicated test classes:

```
src/test/java/com/myapp/architecture/
    ArchitectureTest.java              -- master test composing all rule groups
    CodingRulesTest.java               -- GeneralCodingRules, logging, exceptions
    LayerDependencyRulesTest.java      -- layer access and dependency direction rules
    NamingConventionTest.java          -- naming suffix/prefix and location rules
    CycleFreeTest.java                 -- slice and package cycle checks
    ModuleRulesTest.java               -- module boundary rules (if applicable)
```

### Master Test with Rule Composition

Use `ArchTests` to compose rule groups into a single test class:

**Java:**
```java
@AnalyzeClasses(packagesOf = MyApplication.class,
    importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
public class ArchitectureTest {

    @ArchTest
    static final ArchTests codingRules = ArchTests.in(CodingRulesTest.class);

    @ArchTest
    static final ArchTests layerRules = ArchTests.in(LayerDependencyRulesTest.class);

    @ArchTest
    static final ArchTests namingRules = ArchTests.in(NamingConventionTest.class);

    @ArchTest
    static final ArchTests cycleRules = ArchTests.in(CycleFreeTest.class);
}
```

**Kotlin:**
```kotlin
@AnalyzeClasses(packagesOf = [MyApplication::class],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class])
class ArchitectureTest {

    @ArchTest
    val codingRules = ArchTests.`in`(CodingRulesTest::class.java)

    @ArchTest
    val layerRules = ArchTests.`in`(LayerDependencyRulesTest::class.java)

    @ArchTest
    val namingRules = ArchTests.`in`(NamingConventionTest::class.java)

    @ArchTest
    val cycleRules = ArchTests.`in`(CycleFreeTest::class.java)
}
```

### Best Practices

1. **Use `@AnalyzeClasses`** with JUnit 5 for automatic class caching between tests
2. **Use `packagesOf`** instead of string-based package names for refactoring safety
3. **Add `.because()`** to every rule to document intent -- this appears in failure messages
4. **Filter with `ImportOption`** to exclude test classes and JARs from analysis
5. **Use `consideringAllDependencies()`** in layered/onion architecture rules for strict enforcement
6. **One concern per test class** -- separate layer rules, naming rules, cycle rules, etc.

---

## Kotlin-Specific Considerations

ArchUnit has first-class Kotlin support. Key differences from Java usage:

### Syntax

```kotlin
@AnalyzeClasses(packagesOf = [MyApplication::class])
class MyArchitectureTest {

    // Rules as val fields
    @ArchTest
    val services_should_not_depend_on_controllers: ArchRule =
        noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")

    // Rules as methods (receive JavaClasses)
    @ArchTest
    fun services_should_not_access_controllers(importedClasses: JavaClasses) {
        val rule = noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
        rule.check(importedClasses)
    }
}
```

### Key Notes

| Topic | Detail |
|-------|--------|
| **JUnit 5** | No `@RunWith` needed -- just use `@AnalyzeClasses` |
| **Rule fields** | Use `val` (immutable), not `var` |
| **Package references** | Use array syntax: `packagesOf = [MyClass::class]` |
| **Bytecode analysis** | ArchUnit analyzes compiled bytecode -- data classes, companion objects, extension functions are all visible |
| **`internal` visibility** | Compiles to `public` + `@JvmSynthetic` -- ArchUnit sees these as public. Be aware when writing visibility-based rules |
| **Combine with detekt** | Use detekt for Kotlin code style (naming, complexity) and ArchUnit for architecture enforcement |

---

## Freezing Arch Rules for Legacy Codebases

For existing projects with many architecture violations, use `FreezingArchRule` to adopt ArchUnit incrementally without blocking CI.

### How Freezing Works

1. **First run**: Records all current violations to a `ViolationStore` (text files)
2. **Subsequent runs**: Only **new** violations cause failures; existing violations are tracked
3. **Violations fixed**: Automatically removed from the store
4. **Ratchet pattern**: Violation count can only decrease over time

### Usage

```java
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@ArchTest
static final ArchRule layered_architecture =
    FreezingArchRule.freeze(
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Persistence").definedBy("..persistence..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
    );
```

### Configuration

```properties
# Store path -- COMMIT to VCS so the baseline is shared
freeze.store.default.path=archunit_store

# CI: prevent accidental store creation (only read existing)
freeze.store.default.allowStoreCreation=false

# Local dev: allow store creation for initial setup
# Override: -Darchunit.freeze.store.default.allowStoreCreation=true

# Allow updating store when violations are fixed
freeze.store.default.allowStoreUpdate=true

# Emergency: refreeze all current violations (use sparingly)
freeze.refreeze=false
```

### Alternative: Ignore Patterns

For permanently ignoring specific classes (e.g., generated code), place `archunit_ignore_patterns.txt` at the classpath root:

```
# Ignore generated code
.*\.generated\..*

# Ignore legacy service with known violations
.*some\.pkg\.LegacyService.*
```

### When to Use Which

| Approach | Use Case |
|----------|----------|
| `FreezingArchRule` | Adopting ArchUnit in legacy codebases; violations ratchet down over time |
| `archunit_ignore_patterns.txt` | Permanently ignoring known exceptions (generated code, third-party adapters) |
| Rule suppression (none needed) | ArchUnit has no per-class suppression; use freezing or ignore patterns instead |

---

## `accessClassesThat` vs `dependOnClassesThat`

This is a critical distinction for writing correct rules:

| Method | What It Catches | When to Use |
|--------|----------------|-------------|
| `accessClassesThat` | Runtime accesses only (field access, method calls, constructor calls) | Rarely -- too narrow for most architecture rules |
| `dependOnClassesThat` | All dependencies: field types, method parameter/return types, extends/implements, annotations, type parameters | **Default choice** for architecture rules |

**Rule of thumb**: Always use `dependOnClassesThat` unless you have a specific reason to check only runtime accesses. Using `accessClassesThat` can create false confidence -- code may compile-time depend on a forbidden layer without triggering runtime access.

---

## Incremental Adoption Strategy

### Phase 1: Foundation (Week 1)

Start with high-value, low-noise rules:

1. **General coding rules**: `NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS`, `NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS`, `NO_CLASSES_SHOULD_USE_FIELD_INJECTION`
2. **Cycle detection**: `slices().matching("com.myapp.(*)..").should().beFreeOfCycles()`
3. **Freeze if needed**: Wrap all rules in `FreezingArchRule.freeze()` for legacy codebases

**Goal**: ArchUnit running in CI. No new violations introduced.

### Phase 2: Layer Enforcement (Week 2-3)

Add architecture-specific rules:

1. **Layered architecture** (or onion architecture for DDD projects)
2. **Dependency direction rules**: Services don't depend on controllers, persistence doesn't depend on services
3. **Freeze new rules if needed** for legacy violations

**Goal**: Architecture boundaries enforced. No new layer violations.

### Phase 3: Conventions & Modules (Week 4+)

Add refinement rules:

1. **Naming conventions**: Controller/Service/Repository suffixes
2. **Annotation enforcement**: Framework annotations applied correctly
3. **Module rules** (for large projects)
4. **Begin unfreezing**: Fix frozen violations sprint by sprint

**Goal**: Full architecture test coverage. Frozen violation count trending to zero.

---

## CI/CD Integration

ArchUnit tests are regular unit tests. No special CI configuration is needed beyond running tests.

### Key CI Considerations

1. **ArchUnit tests run in the `test` phase** -- they execute alongside unit tests
2. **Commit `archunit_store/`** to VCS if using frozen rules
3. **Override properties in CI**: `-Darchunit.freeze.store.default.allowStoreCreation=false`
4. **Performance**: First run imports all classes (can take seconds for large codebases). `@AnalyzeClasses` caches between tests.
5. **Memory**: Large codebases may need increased test heap: `-Xmx512m` or higher

### Example CI Configuration

**GitHub Actions:**
```yaml
- name: Run tests (includes ArchUnit)
  run: ./gradlew test
  env:
    JAVA_OPTS: "-Darchunit.freeze.store.default.allowStoreCreation=false"
```

**Maven:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <archunit.freeze.store.default.allowStoreCreation>false</archunit.freeze.store.default.allowStoreCreation>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Using `accessClassesThat` when `dependOnClassesThat` is needed | Always prefer `dependOnClassesThat` for architecture rules |
| Not using `.because()` on rules | Every rule must document its intent for meaningful failure messages |
| String-based package names that break on refactoring | Use `packagesOf = MyClass.class` where possible |
| Dead rules that silently pass on zero classes | Keep `archRule.failOnEmptyShould=true` (default) |
| Importing test classes in production rules | Use `ImportOption.DoNotIncludeTests.class` |
| Freezing rules without committing the store | Always commit `archunit_store/` to VCS |
| Running ArchUnit without JUnit 5 caching | Use `@AnalyzeClasses` for automatic class caching |
| Overly broad package patterns | Use specific patterns like `com.myapp.order.service..` over `..service..` when disambiguation is needed |

---

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Code quality standards that ArchUnit architectural rules support
- [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) -- SOLID principles that ArchUnit layer/dependency rules enforce
- [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md) -- PMD, Checkstyle, detekt standards (complementary to ArchUnit)
- [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) -- Java-specific conventions including ArchUnit integration
- [KOTLIN_STANDARDS.md](./KOTLIN_STANDARDS.md) -- Kotlin-specific conventions including ArchUnit integration
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Design patterns that inform architectural structure
- CI/CD pipeline integration guide (see issue #30)

---

**Last Updated**: February 19, 2026
**Version**: 1.0
