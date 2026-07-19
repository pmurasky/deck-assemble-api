# Static Analysis Standards

## Overview and Philosophy

Static analysis catches bugs, enforces consistency, and reduces code review burden -- all before a single test runs. It is the first automated quality gate in our development workflow.

### Why Static Analysis Matters

1. **Catches bugs before tests**: Null dereferences, resource leaks, and logic errors detected at analysis time
2. **Enforces consistency**: Naming, formatting, and structural rules applied uniformly across the codebase
3. **Reinforces engineering standards**: Complexity limits, method length, parameter counts, and DRY violations are enforced automatically -- not just documented
4. **Reduces review burden**: Reviewers focus on design and logic instead of style and metrics

### Relationship to Engineering Standards

Static analysis tools are configured to enforce the thresholds defined in our coding standards:

| Standard | Threshold | Enforced By |
|----------|-----------|-------------|
| Method length | 15-20 lines (language-specific) | PMD `NcssCount`, detekt `LongMethod` |
| Class length | 300 lines max | PMD `NcssCount` (class), detekt `LargeClass` |
| Cyclomatic complexity | 10 per method | PMD `CyclomaticComplexity`, detekt `CyclomaticComplexMethod` |
| Cognitive complexity | 15 per method | PMD `CognitiveComplexity`, detekt `CognitiveComplexMethod` |
| Parameter count | 5 max | PMD `ExcessiveParameterList`, detekt `LongParameterList` |
| Nesting depth | 3 levels | PMD, detekt `NestedBlockDepth` |
| No duplicated code | DRY principle | CPD (Copy-Paste Detection), detekt `StringLiteralDuplication` |

See [CODING_PRACTICES.md](./CODING_PRACTICES.md) for the full set of coding standards these tools enforce.

### Zero-Tolerance Policy

**PMD/detekt violations MUST fail the build.** Do not set `ignoreFailures = true` (Gradle) or `failOnViolation = false` (Maven) in production builds. Static analysis is a hard gate, not advisory.

- **Local development**: Violations fail the build immediately
- **CI/CD pipelines**: Violations block the PR from merging
- **No exceptions** without a documented suppression (see [Suppression Strategy](#suppression-strategy))

---

## PMD 7 Configuration Best Practices

PMD 7 is our primary static analysis tool for Java. It detects code quality issues, complexity violations, best-practice deviations, and potential bugs.

### Always Use Custom Rulesets

Never rely on PMD's default category imports. Instead, reference rules individually in a custom ruleset file. This provides:

- **Stability across PMD version upgrades**: New rules added to a category won't surprise you
- **Intentional rule selection**: Every enabled rule has a documented reason
- **Clear exclusion rationale**: Every excluded rule has a documented justification

This repo provides curated rulesets:
- **Java**: `config/pmd/java-ruleset.xml`
- **Kotlin**: `config/pmd/kotlin-ruleset.xml`

### Recommended Thresholds

Configure PMD thresholds to match our engineering standards:

| Rule | Property | Threshold | Rationale |
|------|----------|-----------|-----------|
| `CyclomaticComplexity` | `reportLevel` (method) | 10 | Keeps methods testable and readable |
| `CyclomaticComplexity` | `reportLevel` (class) | 40 | Flags God classes |
| `CognitiveComplexity` | `reportLevel` | 15 | Measures human comprehension difficulty |
| `NcssCount` | method threshold | 20 | Aligns with 15-20 line method max |
| `NcssCount` | class threshold | 300 | Aligns with 300 line class max |
| `ExcessiveParameterList` | `minimum` | 5 | Aligns with max params standard |
| `TooManyFields` | `maxfields` | 15 | Signals SRP violation |
| `TooManyMethods` | `maxmethods` | 20 | Signals God class |
| `NPathComplexity` | `reportLevel` | 200 | Limits number of execution paths |
| `AvoidDeeplyNestedIfStmts` | `problemDepth` | 3 | Keeps nesting manageable |

### Document Rule Inclusion/Exclusion

Every rule in the custom ruleset should have a comment explaining **why** it is included or excluded. The `config/pmd/java-ruleset.xml` file in this repo follows this practice. Example:

```xml
<!-- Included: Enforces our 20-line method max from CODING_PRACTICES.md -->
<rule ref="category/java/design.xml/NcssCount">
    <properties>
        <property name="methodReportLevel" value="20" />
        <property name="classReportLevel" value="300" />
    </properties>
</rule>

<!-- Excluded: OnlyOneReturn - multiple returns improve readability in guard clauses -->
<!-- <rule ref="category/java/codestyle.xml/OnlyOneReturn" /> -->
```

For the complete Java ruleset with all inclusions and exclusions documented, see `config/pmd/java-ruleset.xml`. For the complete Kotlin ruleset, see `config/pmd/kotlin-ruleset.xml`.

### Build Integration

For Maven and Gradle integration snippets, see:
- **Java**: [JAVA_STANDARDS.md -- PMD Maven Integration](./JAVA_STANDARDS.md#pmd-maven-integration) and [PMD Gradle Integration](./JAVA_STANDARDS.md#pmd-gradle-integration)
- **Kotlin**: [KOTLIN_STANDARDS.md -- Gradle Integration](./KOTLIN_STANDARDS.md#gradle-integration)

---

## CPD (Copy-Paste Detection)

CPD (part of the PMD toolset) detects duplicated code blocks across files, enforcing the DRY principle automatically.

### Minimum Token Threshold

The token threshold determines the minimum size of a duplicated block before CPD reports it. A lower threshold catches more duplications but may produce noise.

| Setting | Token Count | Use Case |
|---------|-------------|----------|
| Strict | 75 | Small, well-factored codebases |
| **Default (recommended)** | **100** | **Most projects** |
| Lenient | 150 | Large legacy codebases during initial adoption |

### Language-Specific CPD Notes

| Language | CPD Support | Notes |
|----------|-------------|-------|
| Java | Full | Tokenizes Java syntax accurately |
| Kotlin | Full | Uses Kotlin grammar module |
| JavaScript/TypeScript | Full | Via PMD JavaScript module |
| Apex | Full | Via PMD Apex module |
| XML/JSP | Full | Via PMD XML/JSP modules |
| Python | Full | Via PMD Python module |
| Go | Partial | Basic token matching |

### CPD Integration

CPD is bundled with PMD. No separate installation required.

**Maven**: Add the `cpd-check` goal to the PMD plugin (see [JAVA_STANDARDS.md -- CPD](./JAVA_STANDARDS.md#cpd-copy-paste-detection)):

```xml
<configuration>
    <minimumTokens>100</minimumTokens>
</configuration>
<executions>
    <execution>
        <goals>
            <goal>check</goal>
            <goal>cpd-check</goal>
        </goals>
    </execution>
</executions>
```

**Gradle**: CPD is included automatically with the `pmd` plugin (see [JAVA_STANDARDS.md -- CPD](./JAVA_STANDARDS.md#cpd-copy-paste-detection)):

```kotlin
tasks.withType<Cpd> {
    minimumTokenCount.set(100)
}
```

### Handling CPD Violations

When CPD detects duplication:

1. **Extract shared logic** into a common method or class
2. **Use inheritance or composition** if duplication spans related classes
3. **Use templates or generics** if duplication differs only by type
4. **Suppress only if justified** (e.g., test fixtures that are intentionally similar)

---

## Suppression Strategy

Suppressions are sometimes necessary, but they must be intentional, justified, and tracked.

### When Suppression Is Acceptable

- **False positives**: The tool flags code that is actually correct (e.g., LawOfDemeter on fluent APIs)
- **Framework constraints**: Framework-generated code or required patterns that violate rules (e.g., Spring Boot entry points)
- **Intentional design decisions**: Documented decisions where the rule doesn't apply (e.g., orchestrator classes that coordinate many dependencies)

### When Suppression Is NOT Acceptable

- To hide real violations that should be fixed
- To meet a deadline -- fix the code or negotiate scope
- As a blanket global disable -- suppressions must be scoped to the narrowest target

### Suppression Mechanisms by Tool

#### PMD (Java)

```java
// Preferred: Annotation with justification
@SuppressWarnings("PMD.GodClass")  // Justified: orchestration class, refactoring tracked in JIRA-1234
public class OrderWorkflow { ... }

// Alternative: Inline comment (use sparingly)
String output = System.getenv("HOME"); // NOPMD - required for environment detection
```

#### detekt (Kotlin)

```kotlin
// Preferred: Annotation with justification
@Suppress("TooGenericExceptionCaught")  // Justified: top-level error handler must catch all
fun handleRequest(request: Request): Response { ... }
```

#### Checkstyle (Java)

```java
// Preferred: Annotation with justification
@SuppressWarnings("checkstyle:MagicNumber")  // Justified: HTTP status codes are well-known constants
public static final int HTTP_OK = 200;
```

### Suppression Guidelines

1. **Always include a justification comment** explaining WHY the suppression is needed
2. **Suppress at the narrowest scope** possible (method > class > file > global)
3. **Never suppress globally** in the ruleset or config to hide violations -- fix the code instead
4. **Prefer annotations** over inline comments for traceability
5. **Track suppression count** as a metric; increasing suppressions require review
6. **Review suppressions** in code reviews -- a new suppression should be scrutinized as closely as new code

### Suppression Metrics

Track the number of active suppressions over time:

```bash
# Count PMD suppressions in Java files
grep -rn "NOPMD\|@SuppressWarnings(\"PMD" src/main/java/ | wc -l

# Count detekt suppressions in Kotlin files
grep -rn "@Suppress(" src/main/kotlin/ | wc -l

# Count Checkstyle suppressions
grep -rn "@SuppressWarnings(\"checkstyle" src/main/java/ | wc -l
```

**Quality gate**: If the suppression count increases in a PR, the reviewer must verify each new suppression is justified.

---

## Tool-Specific Sections

### PMD for Java (Primary)

PMD 7 is the primary static analysis tool for Java. It provides comprehensive rule coverage across best practices, code style, design, error-prone patterns, multithreading, performance, and security.

- **Ruleset**: `config/pmd/java-ruleset.xml`
- **Full documentation**: [JAVA_STANDARDS.md -- PMD 7 Configuration](./JAVA_STANDARDS.md#pmd-7-configuration)
- **Version**: PMD 7.9.0+

### Checkstyle for Java

Checkstyle enforces formatting, naming, imports, Javadoc conventions, and size limits based on the Google Java Style Guide with thresholds aligned to our engineering standards.

- **Configuration**: `config/checkstyle/checkstyle.xml`
- **Full documentation**: [CHECKSTYLE_STANDARDS.md](./CHECKSTYLE_STANDARDS.md)
- **Version**: Checkstyle 10.21.1+

**Complementary to PMD**: Checkstyle handles formatting and Javadoc; PMD handles complexity, design, and bugs. PMD's `CommentRequired` rule is intentionally excluded in favor of Checkstyle's more configurable Javadoc checks.

### detekt for Kotlin

detekt is the primary static analysis tool for Kotlin. It provides full Kotlin AST support with configurable thresholds aligned to our standards.

- **Configuration**: `config/detekt/detekt.yml`
- **Full documentation**: [KOTLIN_STANDARDS.md -- detekt Configuration](./KOTLIN_STANDARDS.md#detekt-configuration)
- **Version**: detekt 1.23.8+

### PMD for Kotlin (Supplementary)

PMD 7 has limited Kotlin support (2 rules). It supplements detekt for edge cases.

- **Ruleset**: `config/pmd/kotlin-ruleset.xml`
- **Full documentation**: [KOTLIN_STANDARDS.md -- PMD 7 Kotlin Configuration](./KOTLIN_STANDARDS.md#pmd-7-kotlin-configuration)
- **Rules**: `FunctionNameTooShort`, `OverrideBothEqualsAndHashcode`

### PMD for Apex (Salesforce)

PMD provides comprehensive Apex analysis. Use custom rulesets following the same principles as Java.

- **Category coverage**: Best Practices, Code Style, Design, Error Prone, Multithreading, Performance, Security
- **Key rules**: `CyclomaticComplexity`, `CognitiveComplexity`, `ExcessiveParameterList`, `AvoidGlobalModifier`
- **Suppression**: Use `// NOPMD` inline comments (Apex does not support annotations for PMD)

### PMD for JavaScript

PMD provides basic JavaScript analysis. For comprehensive JavaScript/TypeScript coverage, use ESLint as the primary tool with PMD as a supplement.

- **Category coverage**: Best Practices, Code Style, Error Prone
- **Key rules**: `AssignmentInOperand`, `GlobalVariable`, `AvoidTrailingComma`
- **Recommendation**: Use ESLint for primary JavaScript/TypeScript analysis; PMD for supplementary checks

### ArchUnit for Java and Kotlin (Architecture Testing)

ArchUnit enforces architectural constraints that code-level static analysis tools (PMD, Checkstyle, detekt) cannot detect: layer violations, package cycles, dependency direction, and module boundaries.

- **Configuration**: `config/archunit/archunit.properties`
- **Full documentation**: [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md)
- **Version**: ArchUnit 1.4.1+

**Complementary to PMD/detekt**: PMD and detekt handle code-level quality (complexity, style, method length). ArchUnit handles structural/architectural quality (layer boundaries, cycles, dependency direction). Use both.

### SpotBugs for Java (Bytecode Bug Detection)

SpotBugs analyzes compiled Java bytecode to find bugs that source-level tools (PMD, Checkstyle) cannot detect: null pointer dereferences, concurrency issues, security vulnerabilities, and resource leaks.

- **Configuration**: `config/spotbugs/spotbugs-exclude.xml`
- **Full documentation**: [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md)
- **Version**: SpotBugs 4.9.7+, Maven plugin 4.9.7.0
- **Security plugin**: Find Security Bugs 1.12.0 (138 detectors for OWASP Top 10)

**Complementary to PMD/Checkstyle**: PMD and Checkstyle analyze source code for quality and style. SpotBugs analyzes bytecode for actual bugs (null dereferences, race conditions, security flaws). Use both.

**Java-only**: SpotBugs does not support Kotlin or other JVM languages. For Kotlin static analysis, use detekt.

### Language-Specific Tool Matrix

| Language | Primary Tool | Secondary Tool | Architecture Testing | CPD | Config File |
|----------|-------------|----------------|---------------------|-----|-------------|
| Java | PMD 7 + Checkstyle | SpotBugs, Error Prone | ArchUnit | Yes | `config/pmd/java-ruleset.xml`, `config/checkstyle/checkstyle.xml`, `config/spotbugs/spotbugs-exclude.xml` |
| Kotlin | detekt | PMD 7 (2 rules) | ArchUnit | Yes | `config/detekt/detekt.yml`, `config/pmd/kotlin-ruleset.xml` |
| Apex | PMD 7 | -- | -- | Yes | Custom ruleset (see issue #28) |
| JavaScript | ESLint | PMD 7 | -- | Yes | Custom ruleset (see issue #28) |
| Python | Ruff, Pylint | -- | -- | Yes (via PMD) | Language-specific config |
| Go | golangci-lint | -- | -- | -- | `.golangci.yml` |

---

## Incremental Adoption Strategy

For existing projects that don't currently use static analysis, adopt rules in phases to avoid an overwhelming initial violation count.

### Phase 1: Error Prone + Security (Week 1-2)

Start with rules that catch actual bugs and security issues. These have the highest value and lowest false-positive rate.

**PMD rules to enable:**
- `category/java/errorprone.xml` (selected rules: `NullAssignment`, `MissingBreakInSwitch`, `CloseResource`, etc.)
- `category/java/security.xml` (all rules)

**detekt rules to enable:**
- `potential-bugs` rule set
- `exceptions` rule set

**Goal**: Zero new bug-class violations in PRs.

### Phase 2: Best Practices + Design (Week 3-4)

Add rules that enforce good design and coding practices.

**PMD rules to enable:**
- `category/java/bestpractices.xml` (selected rules: `UnusedPrivateField`, `LooseCoupling`, etc.)
- `category/java/design.xml` (selected rules: `CyclomaticComplexity`, `CognitiveComplexity`, `NcssCount`, etc.)

**detekt rules to enable:**
- `complexity` rule set
- `style` rule set (selected rules)

**Goal**: Complexity thresholds enforced; no new design violations.

### Phase 3: Code Style + Performance (Week 5-6)

Add rules that enforce consistency and performance best practices.

**PMD rules to enable:**
- `category/java/codestyle.xml` (selected rules)
- `category/java/performance.xml` (selected rules)

**detekt rules to enable:**
- `performance` rule set
- `naming` rule set
- `comments` rule set

**Goal**: Full ruleset active; all new code is clean.

### Baseline File Support

For legacy violations that exist before adoption, use baseline files to track them without blocking CI:

**detekt baseline**:
```bash
# Generate baseline of current violations
./gradlew detektBaseline

# Configure in build.gradle.kts
detekt {
    baseline = file("config/detekt/baseline.xml")
}
```

**PMD baseline** (manual approach):
1. Run PMD and record the current violation count
2. Set a quality gate: "no new violations" (total count must not increase)
3. Gradually reduce the count over time

**Important**: Baseline files are a **migration tool**, not a permanent suppression mechanism. Plan to eliminate baseline violations over time. Track baseline violation count as a metric and set targets for reduction.

---

## Metrics and Reporting

### Key Metrics to Track

| Metric | Target | Why |
|--------|--------|-----|
| Total violations | Trending to zero | Overall code health |
| New violations per PR | Zero | Quality gate for new code |
| Suppression count | Stable or decreasing | Prevents suppression abuse |
| Baseline violations | Decreasing over time | Legacy debt reduction |
| Complexity hotspots | Under thresholds | Identifies refactoring targets |
| CPD duplicate blocks | Zero new | DRY enforcement |

### Quality Gates

Enforce these quality gates in CI/CD:

1. **Hard gate (blocks merge)**: Zero new PMD/detekt/Checkstyle violations
2. **Hard gate (blocks merge)**: Zero new CPD violations above token threshold
3. **Soft gate (warning)**: Suppression count increased -- reviewer must verify justification
4. **Trend gate**: Baseline violation count must not increase between sprints

### Dashboard Recommendations

For visibility into static analysis trends:

- **SonarQube / SonarCloud**: Comprehensive dashboard with historical trends, quality gates, and IDE integration. Recommended for teams with dedicated infrastructure.
- **GitHub Actions + SARIF**: Upload PMD/detekt results as SARIF to GitHub Security tab for inline PR annotations. Lightweight and free for public repos.
- **Custom reports**: Use PMD XML/HTML reports and detekt HTML/SARIF reports for team review. Store in CI artifacts.

### Report Formats

Configure tools to generate both machine-readable and human-readable reports:

```kotlin
// Gradle: PMD reports
tasks.withType<Pmd> {
    reports {
        xml.required.set(true)   // For CI/CD integration
        html.required.set(true)  // For human review
    }
}

// Gradle: detekt reports
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        xml.required.set(true)   // For CI/CD integration
        html.required.set(true)  // For human review
        sarif.required.set(true) // For GitHub Security tab
    }
}
```

---

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Code quality thresholds that static analysis enforces
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) -- Standards index
- [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md) -- ArchUnit architecture testing standards (layer boundaries, cycles, dependency direction)
- [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) -- Java-specific PMD, Checkstyle, and CPD configuration
- [KOTLIN_STANDARDS.md](./KOTLIN_STANDARDS.md) -- Kotlin-specific detekt and PMD configuration
- [PRE_COMMIT_CHECKLIST.md](./PRE_COMMIT_CHECKLIST.md) -- Pre-commit quality checklist including static analysis
- CI/CD pipeline integration guide (see issue #30)

---

**Last Updated**: February 19, 2026
**Version**: 1.0
