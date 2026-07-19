# SpotBugs Static Analysis Standards

## Overview

SpotBugs is a static analysis tool that finds bugs in Java bytecode. It detects over 400 bug patterns including null pointer dereferences, infinite loops, concurrency issues, and security vulnerabilities. As a bytecode analyzer, it catches defects that source-level tools (PMD, Checkstyle) cannot.

### Why SpotBugs

Source-level static analysis tools (PMD, Checkstyle) enforce code quality and style. SpotBugs analyzes compiled bytecode, catching a different class of defects:

| Concern | PMD / Checkstyle | SpotBugs |
|---------|-----------------|----------|
| Null dereference detection | Limited pattern matching | Full data-flow analysis across methods |
| Concurrency bugs (race conditions, deadlocks) | Cannot detect | Deep multithreading analysis |
| Resource leak detection | Basic checks | Tracks resource lifecycles through bytecode |
| Security vulnerabilities | Limited | 138 detectors via Find Security Bugs plugin |
| Infinite recursive loops | Cannot detect | Detects through call graph analysis |
| Serialization issues | Basic naming checks | Full serialization contract verification |
| Integer overflow / bit manipulation bugs | Cannot detect | Bytecode-level arithmetic analysis |
| Incorrect API usage (e.g., `equals` on incompatible types) | Limited | Type-aware cross-method analysis |

**Use both**: PMD/Checkstyle for code quality and style, SpotBugs for bytecode-level bug detection. They are fully complementary.

### Relationship to Engineering Standards

SpotBugs supports our engineering standards by catching defects before they reach production:

| Standard | SpotBugs Enforcement |
|----------|---------------------|
| **Production-ready commits** | Catches null dereferences, resource leaks, and concurrency bugs that would cause runtime failures |
| **Security** | Find Security Bugs plugin detects SQL injection, XSS, path traversal, and other OWASP Top 10 vulnerabilities |
| **Code quality** | Identifies dead code, redundant operations, and incorrect API usage |
| **Testing** | Bytecode analysis catches bugs that unit tests may miss (race conditions, edge-case null paths) |

See [CODING_PRACTICES.md](./CODING_PRACTICES.md) for the full set of coding standards and [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) for SOLID principle deep-dives.

### Applicability

**SpotBugs is Java-only.** It analyzes JVM bytecode compiled from Java source. It does not support Kotlin, Groovy, Scala, or other JVM languages. For Kotlin static analysis, use detekt (see [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md)).

### Zero-Tolerance Policy

**SpotBugs violations at rank 1-9 (Scariest and Scary) MUST fail the build.** These represent high-confidence bugs that will cause runtime failures or security vulnerabilities.

- **Local development**: High-rank violations fail the build immediately
- **CI/CD pipelines**: Violations block the PR from merging
- **Legacy codebases**: Use [exclusion filters](#exclusion-filter-files) to adopt incrementally -- never disable the tool

---

## Installation

### Version

Use SpotBugs **4.9.7+** (latest stable as of this writing). Maven plugin version: **4.9.7.0**.

### Maven

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.9.7.0</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <excludeFilterFile>config/spotbugs/spotbugs-exclude.xml</excludeFilterFile>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.12.0</version>
            </plugin>
        </plugins>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Gradle

```kotlin
plugins {
    id("com.github.spotbugs") version "6.1.3"
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}

spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/spotbugs-exclude.xml"))
}
```

### Find Security Bugs Plugin

The Find Security Bugs plugin adds 138 security-focused detectors for OWASP Top 10 vulnerabilities. **Always include it.** It covers:

- SQL injection, command injection, LDAP injection
- Cross-site scripting (XSS), cross-site request forgery (CSRF)
- Path traversal, XML external entities (XXE)
- Insecure cryptography, weak hashing
- Hardcoded passwords and secrets
- Deserialization vulnerabilities
- HTTP response splitting

---

## Configuration

### Effort Levels

Effort controls the depth of analysis. Higher effort finds more bugs but takes longer.

| Level | Description | Use Case |
|-------|-------------|----------|
| `Min` | Minimal analysis, fastest | Not recommended |
| `Less` | Reduced analysis | Not recommended |
| `More` | Standard analysis (default) | Acceptable for very large codebases with CI time constraints |
| `Max` | Full interprocedural analysis | **Recommended** -- use for all projects |

**Standard**: Use `Max` effort. The additional analysis time is negligible for most projects and catches significantly more bugs.

### Threshold (Confidence) Levels

Threshold controls which confidence level of bugs to report:

| Level | Description | Recommendation |
|-------|-------------|----------------|
| `High` | Only highest-confidence bugs | Too restrictive -- misses real bugs |
| `Medium` | Medium and high confidence | **Recommended** -- good signal-to-noise ratio |
| `Low` | All bugs including low confidence | Use for security-critical code or periodic deep scans |
| `Exp` | Experimental detectors included | Not recommended for CI |

**Standard**: Use `Medium` threshold for CI builds. Use `Low` for periodic security audits.

### Bug Rank System

SpotBugs assigns a rank (1-20) to each bug, indicating severity:

| Rank Range | Category | Policy |
|------------|----------|--------|
| 1-4 | **Scariest** | Must fix immediately. Build MUST fail. |
| 5-9 | **Scary** | Must fix before merge. Build MUST fail. |
| 10-14 | **Troubling** | Should fix. Build should warn (fail in strict mode). |
| 15-20 | **Of Concern** | Review and fix if practical. Advisory only. |

Configure the rank threshold in the exclusion filter or build plugin to enforce policy. By default, with `threshold=Medium`, ranks 1-14 are typically reported.

### Bug Categories

SpotBugs organizes detectors into categories:

| Category | Code | Description | Priority |
|----------|------|-------------|----------|
| Correctness | `CORRECTNESS` | Probable bugs (null dereference, infinite loops, bad casts) | **Critical** -- always enable |
| Multithreaded Correctness | `MT_CORRECTNESS` | Concurrency bugs (races, deadlocks, inconsistent sync) | **Critical** -- always enable |
| Security | `SECURITY` | Security vulnerabilities (via Find Security Bugs) | **Critical** -- always enable |
| Bad Practice | `BAD_PRACTICE` | Deviations from recommended practice (equals/hashCode, exceptions) | High -- always enable |
| Performance | `PERFORMANCE` | Inefficient code (unnecessary object creation, boxing) | High -- enable by default |
| Dodgy Code | `STYLE` | Code that is confusing, anomalous, or error-prone | Medium -- enable selectively |
| Malicious Code Vulnerability | `MALICIOUS_CODE` | Code vulnerable to attacks (mutable statics, exposed internals) | High -- always enable |
| Internationalization | `I18N` | Internationalization issues (hardcoded locale) | Low -- enable if relevant |
| Experimental | `EXPERIMENTAL` | Experimental detectors, higher false-positive rate | Low -- disable in CI |

### Recommended Category Configuration

Enable all categories except `EXPERIMENTAL` in CI. Use the exclusion filter to disable specific noisy detectors within categories.

---

## Exclusion Filter Files

SpotBugs uses XML-based filter files to include or exclude bugs from analysis. This repo provides a sample exclusion filter at `config/spotbugs/spotbugs-exclude.xml`.

### Filter File Syntax

```xml
<?xml version="1.0" encoding="UTF-8"?>
<FindBugsFilter>
    <!-- Match by bug pattern -->
    <Match>
        <Bug pattern="DM_DEFAULT_ENCODING"/>
    </Match>

    <!-- Match by bug category -->
    <Match>
        <Bug category="EXPERIMENTAL"/>
    </Match>

    <!-- Match by class -->
    <Match>
        <Class name="com.myapp.legacy.OldService"/>
    </Match>

    <!-- Match by package -->
    <Match>
        <Package name="com.myapp.generated"/>
    </Match>

    <!-- Match by method -->
    <Match>
        <Class name="com.myapp.util.StringHelper"/>
        <Method name="unsafeConvert"/>
        <Bug pattern="DM_CONVERT_CASE"/>
    </Match>

    <!-- Compound match: specific bug in specific package -->
    <Match>
        <And>
            <Package name="~com\.myapp\.dto\..*"/>
            <Bug pattern="EI_EXPOSE_REP"/>
        </And>
    </Match>

    <!-- Match by rank (suppress low-severity bugs) -->
    <Match>
        <Rank value="16"/><!-- ranks 16-20 (of concern) -->
    </Match>

    <!-- Negate: suppress everything EXCEPT a specific category -->
    <Match>
        <Not>
            <Bug category="SECURITY"/>
        </Not>
    </Match>
</FindBugsFilter>
```

### Key Filter Elements

| Element | Purpose | Example |
|---------|---------|---------|
| `<Bug pattern="..."/>` | Match specific bug detector | `<Bug pattern="NP_NULL_ON_SOME_PATH"/>` |
| `<Bug category="..."/>` | Match entire category | `<Bug category="EXPERIMENTAL"/>` |
| `<Bug code="..."/>` | Match bug abbreviation code | `<Bug code="NP"/>` (all null pointer bugs) |
| `<Class name="..."/>` | Match specific class (FQCN) | `<Class name="com.myapp.Config"/>` |
| `<Package name="..."/>` | Match package (prefix-based) | `<Package name="com.myapp.generated"/>` |
| `<Package name="~regex"/>` | Match package by regex | `<Package name="~.*\.dto\..*"/>` |
| `<Method name="..."/>` | Match specific method | `<Method name="toString"/>` |
| `<Rank value="N"/>` | Match bugs at rank N or above | `<Rank value="16"/>` |
| `<Confidence value="N"/>` | Match by confidence level | `<Confidence value="3"/>` (low confidence) |
| `<And>`, `<Or>`, `<Not>` | Logical combinators | Combine matchers for precision |

### Filter File Best Practices

1. **Exclude generated code** -- always exclude packages containing generated sources (DTOs from Protobuf, OpenAPI, etc.)
2. **Exclude test code** -- SpotBugs analyzes test bytecode too; exclude `src/test` classes
3. **Never suppress security bugs globally** -- suppress only specific false positives with justification
4. **Use narrow matches** -- prefer `Class` + `Method` + `Bug` over broad `Package` matches
5. **Document exclusions** -- add XML comments explaining why each exclusion exists
6. **Review exclusions quarterly** -- re-evaluate whether suppressed bugs are still appropriate

---

## Suppression Annotations

### `@SuppressFBWarnings`

For suppressing specific false positives in source code:

```java
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(
    value = "EI_EXPOSE_REP",
    justification = "Intentional: Date is defensively copied in the setter"
)
public Date getCreatedAt() {
    return createdAt;
}
```

### Annotation Rules

| Rule | Rationale |
|------|-----------|
| **`justification` is mandatory** | Documents why the suppression is safe |
| **Suppress at the narrowest scope** | Apply to the method, not the class, when possible |
| **One suppression per annotation** | Use separate annotations for separate bugs |
| **Never suppress SECURITY category** without security team review | Security bugs require expert assessment |
| **Review suppressions during code review** | Suppressions must be justified and approved |

### Maven Dependency for Annotations

```xml
<dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>4.9.7</version>
    <optional>true</optional>
</dependency>
```

### Gradle Dependency for Annotations

```kotlin
compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.7")
```

### Nullability Annotations

SpotBugs respects JSR-305 and SpotBugs nullability annotations for enhanced null analysis:

```java
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class UserService {

    public User findById(@NonNull String id) {
        // SpotBugs will flag callers passing null
        return repository.findById(id);
    }

    @Nullable
    public User findByEmail(@NonNull String email) {
        // SpotBugs will flag callers that dereference the return value without null check
        return repository.findByEmail(email);
    }
}
```

**Standard**: Use `@NonNull` and `@Nullable` annotations on all public API boundaries. This enables SpotBugs to perform interprocedural null analysis.

---

## Common Bug Patterns

### Correctness (Must Fix)

| Bug Pattern | Description | Fix |
|-------------|-------------|-----|
| `NP_NULL_ON_SOME_PATH` | Possible null pointer dereference | Add null check or use `Optional` |
| `NP_NULL_ON_SOME_PATH_EXCEPTION` | Null dereference in exception path | Handle null in catch/finally blocks |
| `NP_NONNULL_PARAM_VIOLATION` | Null passed to `@NonNull` parameter | Pass non-null value or fix annotation |
| `RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE` | Redundant null check on non-null value | Remove unnecessary check |
| `EC_UNRELATED_TYPES` | `equals()` called on incompatible types | Fix the comparison type |
| `RV_RETURN_VALUE_IGNORED` | Return value of method ignored | Use or check the return value |
| `IL_INFINITE_RECURSIVE_LOOP` | Infinite recursion detected | Fix the recursion termination |
| `DMI_RANDOM_USED_ONLY_ONCE` | `Random` created and used once | Reuse `Random` instance |

### Multithreaded Correctness (Must Fix)

| Bug Pattern | Description | Fix |
|-------------|-------------|-----|
| `IS2_INCONSISTENT_SYNC` | Field accessed inconsistently with synchronization | Synchronize all accesses or use `volatile` |
| `DC_DOUBLECHECK` | Double-checked locking without `volatile` | Add `volatile` or use `Initialization-on-demand` pattern |
| `LI_LAZY_INIT_STATIC` | Lazy init of static field is not thread-safe | Use `Initialization-on-demand` or `synchronized` |
| `WL_USING_GETCLASS_RATHER_THAN_CLASS_LITERAL` | Lock on `getClass()` instead of class literal | Use `ClassName.class` for lock |
| `NN_NAKED_NOTIFY` | `notify()` without modifying state | Set condition variable before `notify()` |

### Security (Must Fix)

These are reported by the Find Security Bugs plugin:

| Bug Pattern | Description | Fix |
|-------------|-------------|-----|
| `SQL_INJECTION_JDBC` | SQL injection via string concatenation | Use parameterized queries |
| `COMMAND_INJECTION` | OS command injection | Validate/sanitize input, avoid `Runtime.exec` |
| `PATH_TRAVERSAL_IN` | Path traversal vulnerability | Canonicalize and validate file paths |
| `XSS_REQUEST_WRAPPER` | Cross-site scripting | Encode output, use framework XSS protection |
| `XXE_XMLSTREAMREADER` | XML External Entity injection | Disable external entity resolution |
| `WEAK_MESSAGE_DIGEST_SHA1` | Weak cryptographic hash (SHA-1, MD5) | Use SHA-256 or stronger |
| `HARD_CODE_PASSWORD` | Hardcoded password or secret | Use secrets management (vault, env vars) |
| `OBJECT_DESERIALIZATION` | Unsafe deserialization | Validate/restrict deserialization input |

### Bad Practice (Should Fix)

| Bug Pattern | Description | Fix |
|-------------|-------------|-----|
| `HE_EQUALS_USE_HASHCODE` | Class overrides `equals` but not `hashCode` | Implement both methods |
| `SE_NO_SERIALVERSIONID` | Serializable class missing `serialVersionUID` | Add `serialVersionUID` field |
| `DE_MIGHT_IGNORE` | Exception caught and ignored | Log or handle the exception |
| `OS_OPEN_STREAM` | Stream opened but not reliably closed | Use try-with-resources |
| `EI_EXPOSE_REP` | Returns reference to mutable object | Return defensive copy |
| `EI_EXPOSE_REP2` | Stores reference to mutable object | Store defensive copy |

### Performance (Review)

| Bug Pattern | Description | Fix |
|-------------|-------------|-----|
| `DM_BOXED_PRIMITIVE_FOR_PARSING` | Uses `new Integer(s)` instead of `Integer.parseInt(s)` | Use parse methods |
| `DM_NUMBER_CTOR` | Uses `new Integer(42)` instead of `Integer.valueOf(42)` | Use `valueOf` or autoboxing |
| `SBSC_USE_STRINGBUFFER_CONCATENATION` | String concatenation in loop | Use `StringBuilder` |
| `URF_UNREAD_FIELD` | Field written but never read | Remove dead field |
| `UPM_UNCALLED_PRIVATE_METHOD` | Private method never called | Remove dead method |

---

## Build Integration

### Maven: Fail on Violations

The `check` goal fails the build when bugs are found above the configured threshold:

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.9.7.0</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <failOnError>true</failOnError>
        <maxRank>14</maxRank>
        <excludeFilterFile>config/spotbugs/spotbugs-exclude.xml</excludeFilterFile>
        <plugins>
            <plugin>
                <groupId>com.h3xstream.findsecbugs</groupId>
                <artifactId>findsecbugs-plugin</artifactId>
                <version>1.12.0</version>
            </plugin>
        </plugins>
    </configuration>
    <executions>
        <execution>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**Key configuration:**
- `failOnError=true`: Build fails when bugs found
- `maxRank=14`: Only ranks 1-14 fail the build (Scariest + Scary + Troubling)
- `threshold=Medium`: Report medium and high confidence bugs
- `effort=Max`: Full interprocedural analysis

### Maven: Generate HTML Report

```xml
<reporting>
    <plugins>
        <plugin>
            <groupId>com.github.spotbugs</groupId>
            <artifactId>spotbugs-maven-plugin</artifactId>
            <version>4.9.7.0</version>
        </plugin>
    </plugins>
</reporting>
```

Run `mvn site` to generate an HTML report in `target/site/`.

### Gradle: Fail on Violations

```kotlin
plugins {
    id("com.github.spotbugs") version "6.1.3"
}

dependencies {
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.12.0")
}

spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/spotbugs-exclude.xml"))
    maxHeapSize.set("512m")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports {
        register("html") {
            required.set(true)
            outputLocation.set(file("${layout.buildDirectory.get()}/reports/spotbugs/${name}.html"))
        }
        register("xml") {
            required.set(false)
        }
    }
}
```

**Note**: SpotBugs Gradle plugin auto-generates tasks per source set: `spotbugsMain`, `spotbugsTest`. Connect `check` to `spotbugsMain` (not test) for CI.

### Gradle: Skip Test Analysis

```kotlin
tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsTest") {
    enabled = false
}
```

---

## CI/CD Integration

### Key CI Considerations

1. **SpotBugs runs after compilation** -- it requires compiled bytecode. Execute during `verify` (Maven) or after `compileJava` (Gradle).
2. **Include Find Security Bugs** -- always enable the security plugin in CI.
3. **Fail on rank 1-14** -- use `maxRank=14` to fail on Scariest, Scary, and Troubling bugs.
4. **Memory**: Large codebases may need increased heap: `-Xmx512m` for the SpotBugs process.
5. **Reports**: Generate XML for CI tools and HTML for human review.

### Example CI Configuration

**GitHub Actions:**
```yaml
- name: Build and compile
  run: ./gradlew compileJava

- name: Run SpotBugs
  run: ./gradlew spotbugsMain

- name: Upload SpotBugs report
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: spotbugs-report
    path: build/reports/spotbugs/
```

**Maven (CI profile):**
```xml
<profile>
    <id>ci</id>
    <build>
        <plugins>
            <plugin>
                <groupId>com.github.spotbugs</groupId>
                <artifactId>spotbugs-maven-plugin</artifactId>
                <configuration>
                    <effort>Max</effort>
                    <threshold>Medium</threshold>
                    <failOnError>true</failOnError>
                    <maxRank>14</maxRank>
                </configuration>
                <executions>
                    <execution>
                        <phase>verify</phase>
                        <goals>
                            <goal>check</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

---

## Incremental Adoption Strategy

### Phase 1: Security + Correctness (Week 1-2)

Start with the highest-value categories. These have the highest confidence and lowest false-positive rate.

**Enable:**
- `CORRECTNESS` category (null dereferences, infinite loops, type errors)
- `SECURITY` category (via Find Security Bugs plugin)
- `MT_CORRECTNESS` category (concurrency bugs)

**Configuration:**
- Effort: `Max`
- Threshold: `High` (only highest confidence)
- maxRank: `9` (only Scariest + Scary)

**Goal**: Zero new correctness/security bugs in PRs.

### Phase 2: Bad Practice + Malicious Code (Week 3-4)

Expand coverage to common code quality issues:

**Enable additionally:**
- `BAD_PRACTICE` category (equals/hashCode, exceptions, resource management)
- `MALICIOUS_CODE` category (mutable statics, exposed internals)

**Configuration:**
- Lower threshold to `Medium`
- maxRank: `14` (include Troubling)

**Goal**: All new code free of bad practice and security exposure bugs.

### Phase 3: Performance + Dodgy Code (Week 5+)

Add remaining categories:

**Enable additionally:**
- `PERFORMANCE` category (inefficient code)
- `STYLE` (Dodgy Code) category (confusing/error-prone code)

**Configuration:**
- maxRank stays at `14`
- Use exclusion filter to suppress known false positives

**Goal**: Full SpotBugs coverage. Exclusion filter contains only justified suppressions.

---

## Integration with Other Tools

### SpotBugs + PMD

SpotBugs and PMD are complementary with minimal overlap:

| Concern | PMD | SpotBugs |
|---------|-----|----------|
| Code style and conventions | Primary tool | Does not check |
| Complexity metrics | Primary tool | Does not check |
| Null dereference | Limited (`MissingBreakInSwitch`) | Full data-flow analysis |
| Concurrency | Limited | Full multithreading analysis |
| Security | Basic | 138 detectors (Find Security Bugs) |
| Performance | Identifies patterns | Identifies bytecode-level waste |
| Resource management | `CloseResource` rule | `OS_OPEN_STREAM` + data-flow |

**Run both**: PMD for source-level quality, SpotBugs for bytecode-level bug detection.

### SpotBugs + ArchUnit

These tools operate at completely different levels:

- **SpotBugs**: Detects individual bugs within classes (null, concurrency, security)
- **ArchUnit**: Enforces structural/architectural rules between classes (layers, cycles, dependencies)

They are independent and complementary. Use both for defense in depth.

### SpotBugs + Checkstyle

No overlap. Checkstyle enforces formatting and style. SpotBugs finds bugs. Use both.

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Running with `effort=Less` to speed up CI | Use `effort=Max` -- the additional time is negligible and catches more bugs |
| Setting `threshold=High` and missing real bugs | Use `threshold=Medium` for balanced coverage |
| Suppressing bugs without justification | Always provide `justification` in `@SuppressFBWarnings` |
| Not including Find Security Bugs plugin | Always include `findsecbugs-plugin` -- security bugs are the highest value |
| Analyzing test code and getting false positives | Exclude test classes in the exclusion filter or disable `spotbugsTest` task |
| Broad exclusion filter that suppresses entire categories | Use narrow matches (class + method + bug pattern) |
| Not updating SpotBugs version | Keep plugin and annotations versions in sync |
| Ignoring `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` | Fix with defensive copies or suppress with justification for DTOs |
| Running SpotBugs before compilation | SpotBugs needs bytecode -- run after `compile` phase |
| Not increasing heap for large codebases | Set `maxHeapSize` to 512m+ for codebases > 100k lines |

---

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Code quality standards that SpotBugs bug detection supports
- [SOLID_PRINCIPLES.md](./SOLID_PRINCIPLES.md) -- SOLID principles that SpotBugs helps enforce indirectly
- [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md) -- PMD, Checkstyle, detekt, ArchUnit standards (complementary to SpotBugs)
- [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) -- Java-specific conventions including SpotBugs integration
- [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md) -- Architecture testing standards (complementary to SpotBugs)
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Design patterns that inform code structure
- CI/CD pipeline integration guide (see issue #30)

---

**Last Updated**: February 19, 2026
**Version**: 1.0
