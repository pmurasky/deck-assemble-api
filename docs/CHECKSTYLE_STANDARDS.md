# Checkstyle Style Enforcement Standards

## Overview

Checkstyle is a static analysis tool that enforces coding style and formatting conventions in Java source code. It verifies Javadoc completeness, naming conventions, import ordering, whitespace formatting, size limits, and coding practices -- all based on configurable rules.

### Why Checkstyle

Checkstyle operates at the source-code level, enforcing consistent style that makes code readable and reviewable. Other static analysis tools target different concerns:

| Concern | Checkstyle | PMD | SpotBugs |
|---------|-----------|-----|----------|
| Formatting (braces, whitespace, indentation) | **Primary tool** | Does not check | Does not check |
| Naming conventions (camelCase, PascalCase, SCREAMING_SNAKE) | **Primary tool** | Limited checks | Does not check |
| Javadoc completeness and formatting | **Primary tool** | `CommentRequired` (disabled in favor of Checkstyle) | Does not check |
| Import ordering and cleanup | **Primary tool** | Basic checks | Does not check |
| Size limits (method length, parameter count) | Enforces thresholds | Also enforces (NCSS-based) | Does not check |
| Complexity metrics (cyclomatic, cognitive) | Boolean expression complexity only | **Primary tool** | Does not check |
| Code quality patterns (design, coupling) | Limited | **Primary tool** | Does not check |
| Bug detection (null, concurrency, security) | Does not check | Limited | **Primary tool** |

**Use both Checkstyle and PMD**: Checkstyle handles formatting and Javadoc. PMD handles complexity, design, and bug patterns. Their overlap is minimal and intentionally coordinated -- PMD's `CommentRequired` rule is disabled in favor of Checkstyle's more configurable Javadoc checks.

### Relationship to Engineering Standards

Checkstyle enforces the style and size standards defined in our engineering practices:

| Standard | Checkstyle Enforcement |
|----------|----------------------|
| **Method length (20 lines max)** | `MethodLength` check with `max=20`, `countEmpty=false` |
| **Parameter count (5 max)** | `ParameterNumber` check with `max=5` |
| **File length (500 lines max)** | `FileLength` check with `max=500` |
| **Line length (120 chars)** | `LineLength` check with `max=120` |
| **Nesting depth (3 max)** | `NestedIfDepth`, `NestedForDepth`, `NestedTryDepth` checks |
| **No duplicated code** | Handled by CPD (not Checkstyle) |
| **Naming conventions** | Full suite: packages, types, methods, fields, parameters, lambdas, type parameters |
| **Javadoc on public APIs** | `MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod` checks |

See [CODING_PRACTICES.md](./CODING_PRACTICES.md) for the full set of coding standards and [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) for Java-specific conventions including build integration examples.

### Applicability

**Checkstyle is Java-only.** It analyzes Java source files (`.java`). It does not support Kotlin, Groovy, or other JVM languages. For Kotlin style enforcement, use detekt (see [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md)).

### Zero-Tolerance Policy

**Checkstyle violations MUST fail the build.** Style violations are a hard gate, not advisory.

- **Local development**: Violations fail the build immediately
- **CI/CD pipelines**: Violations block the PR from merging
- **Legacy codebases**: Use [incremental adoption](#incremental-adoption-strategy) to phase in rules -- never disable the tool

---

## Installation

### Version

Use Checkstyle **10.21.1+** (latest stable as of this writing).

### Maven

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
mvn checkstyle:check      # Run analysis (fails on violations)
mvn checkstyle:checkstyle # Generate report without failing
mvn verify                # Runs Checkstyle as part of the build lifecycle
```

### Gradle (Kotlin DSL)

```kotlin
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

### Gradle (Groovy DSL)

```groovy
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
./gradlew checkstyleMain  # Run on main sources
./gradlew check           # Runs as part of the check lifecycle
```

---

## Configuration

This repo provides a curated Checkstyle 10.x configuration at `config/checkstyle/checkstyle.xml`. It is based on the Google Java Style Guide with thresholds aligned to our engineering standards.

### Configuration Structure

The configuration is organized into these sections:

| Section | Purpose | Key Checks |
|---------|---------|------------|
| File-level checks | Checks applied to the file as a whole | `NewlineAtEndOfFile`, `FileTabCharacter`, `LineLength`, `FileLength` |
| Javadoc | Documentation completeness and formatting | `MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod`, `AtclauseOrder` |
| Naming conventions | Identifier naming patterns | `TypeName`, `MethodName`, `MemberName`, `ConstantName`, `ParameterName` |
| Imports | Import organization and cleanup | `AvoidStarImport`, `UnusedImports`, `CustomImportOrder` |
| Formatting and whitespace | Code layout and spacing | `Indentation`, `LeftCurly`, `RightCurly`, `NeedBraces`, `WhitespaceAround` |
| Size limits | Method length, parameter count, complexity | `MethodLength`, `ParameterNumber`, `BooleanExpressionComplexity` |
| Coding practices | Common coding rules | `EqualsHashCode`, `MagicNumber`, `FallThrough`, `NestedIfDepth` |
| Class design | Class structure and visibility | `OneTopLevelClass`, `HideUtilityClassConstructor`, `VisibilityModifier` |
| Miscellaneous | Annotations, file structure | `MissingOverride`, `OuterTypeFilename`, `AnnotationLocation` |

### Key Thresholds

All thresholds are aligned with the engineering standards:

| Threshold | Value | Standard Reference |
|-----------|-------|-------------------|
| Line length | 120 characters | Google Style 4.4 (relaxed from 100) |
| Method length | 20 lines (empty lines excluded) | CODING_PRACTICES.md |
| File length | 500 lines | JAVA_STANDARDS.md |
| Parameter count | 5 max (overrides exempted) | CODING_PRACTICES.md |
| Boolean expression complexity | 4 operators max | CODING_PRACTICES.md |
| Nesting depth (if) | 3 levels max | CODING_PRACTICES.md |
| Nesting depth (for) | 2 levels max | CODING_PRACTICES.md |
| Nesting depth (try) | 1 level max | CODING_PRACTICES.md |
| Variable usage distance | 5 lines max | Readability |
| Abbreviation length | 2 consecutive uppercase max | Google Style 5.2.9 |

---

## Rule Categories

### Javadoc

Checkstyle is the primary tool for Javadoc enforcement. PMD's `CommentRequired` rule is intentionally disabled in favor of Checkstyle's more configurable checks.

| Check | What It Enforces |
|-------|-----------------|
| `MissingJavadocType` | All public classes, interfaces, enums, annotations, and records must have Javadoc |
| `MissingJavadocMethod` | All public methods and constructors must have Javadoc |
| `JavadocMethod` | When Javadoc is present, `@param`, `@return`, and `@throws` must be complete |
| `AtclauseOrder` | Tags ordered: `@param`, `@return`, `@throws`, `@deprecated` |
| `NonEmptyAtclauseDescription` | Tag descriptions must not be empty |
| `JavadocParagraph` | Blank line before `<p>` tags in multi-paragraph Javadoc |
| `JavadocStyle` | Proper HTML formatting in Javadoc |
| `JavadocType` | Type-level Javadoc must have `@param` for type parameters |

**Test exemptions**: Methods annotated with `@Test`, `@ParameterizedTest`, `@BeforeEach`, `@AfterEach`, `@BeforeAll`, `@AfterAll`, and `@Nested` are exempt from Javadoc requirements. These exemptions are configured in the `MissingJavadocMethod` check.

### Naming Conventions

| Check | Pattern | Applies To |
|-------|---------|-----------|
| `PackageName` | `^[a-z]+(\.[a-z][a-z0-9]*)*$` | Package declarations |
| `TypeName` | `^[A-Z][a-zA-Z0-9]*$` | Classes, interfaces, enums, annotations, records |
| `MethodName` | `^[a-z][a-zA-Z0-9]*$` | Methods |
| `MemberName` | `^[a-z][a-zA-Z0-9]*$` | Instance fields |
| `ConstantName` | `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$` | `static final` constants |
| `StaticVariableName` | `^[a-z][a-zA-Z0-9]*$` | Non-final static fields |
| `ParameterName` | `^[a-z][a-zA-Z0-9]*$` | Method/constructor parameters |
| `LocalVariableName` | `^[a-z_][a-zA-Z0-9]*$` | Local variables |
| `LambdaParameterName` | `^[a-z_][a-zA-Z0-9]*$` | Lambda parameters |
| `PatternVariableName` | `^[a-z][a-zA-Z0-9]*$` | Pattern matching variables (Java 16+) |
| `RecordComponentName` | `^[a-z][a-zA-Z0-9]*$` | Record components |
| Type parameters | `^[A-Z][A-Z0-9]*$` | Class, interface, method, record type parameters |
| `AbbreviationAsWordInName` | Max 2 consecutive uppercase | Prevents `XMLParser` (use `XmlParser`) |

### Imports

| Check | What It Enforces |
|-------|-----------------|
| `AvoidStarImport` | No wildcard imports (`import java.util.*`) -- always use explicit imports |
| `RedundantImport` | No duplicate imports |
| `UnusedImports` | No unused imports |
| `CustomImportOrder` | Static imports first, then third-party, alphabetically sorted |

### Formatting and Whitespace

| Check | What It Enforces |
|-------|-----------------|
| `Indentation` | 4-space indentation (8-space continuation) |
| `LeftCurly` | Opening braces at end of line (K&R style) |
| `RightCurly` | Closing braces on same line for `if/else/try/catch/finally/do` |
| `NeedBraces` | Braces required for all control statements (no single-line `if`) |
| `WhitespaceAfter` | Space after commas, semicolons, typecasts, keywords |
| `WhitespaceAround` | Space around operators and keywords (empty constructors/methods exempted) |
| `NoWhitespaceBefore` | No space before commas, semicolons, postfix operators |
| `OneStatementPerLine` | One statement per line (no `a = 1; b = 2;`) |
| `MultipleVariableDeclarations` | One variable declaration per statement |
| `EmptyLineSeparator` | Blank lines between class members (fields may be grouped) |
| `OperatorWrap` | Line breaks before operators in wrapped expressions |
| `ArrayTypeStyle` | Java style arrays (`String[] args`, not `String args[]`) |

### Size Limits

| Check | Threshold | Notes |
|-------|-----------|-------|
| `MethodLength` | 20 lines max | Empty lines excluded (`countEmpty=false`) |
| `ParameterNumber` | 5 max | Overridden methods exempted (`ignoreOverriddenMethods=true`) |
| `BooleanExpressionComplexity` | 4 operators max | Applies to `&&`, `\|\|`, `&`, `\|`, `^` |
| `FileLength` | 500 lines max | File-level check |
| `LineLength` | 120 chars max | Package, import, URL lines exempted |

### Coding Practices

| Check | What It Enforces |
|-------|-----------------|
| `EqualsHashCode` | Classes overriding `equals()` must also override `hashCode()` |
| `EqualsAvoidNull` | String literal on left side of `equals()` (`"value".equals(x)`) |
| `FallThrough` | No fall-through in switch statements without explicit comment |
| `MissingSwitchDefault` | Switch statements must have a `default` case |
| `DefaultComesLast` | `default` case must be the last case in switch |
| `MagicNumber` | No magic numbers in code (-1, 0, 1, 2 allowed; hashCode exempted) |
| `StringLiteralEquality` | Use `equals()` for string comparison, not `==` |
| `SimplifyBooleanExpression` | No unnecessarily complex boolean expressions |
| `SimplifyBooleanReturn` | No `if (x) return true; else return false;` |
| `NestedIfDepth` | Max 3 levels of nested `if` |
| `NestedForDepth` | Max 2 levels of nested `for` |
| `NestedTryDepth` | Max 1 level of nested `try` |
| `NoFinalizer` | No `finalize()` methods |
| `OverloadMethodsDeclarationOrder` | Overloaded methods grouped together |
| `VariableDeclarationUsageDistance` | Variables declared close to first use (5 lines max) |

### Class Design

| Check | What It Enforces |
|-------|-----------------|
| `OneTopLevelClass` | One top-level class per file |
| `HideUtilityClassConstructor` | Utility classes (all static methods) must have private constructor |
| `DeclarationOrder` | Fields at top of class, then constructors, then methods |
| `VisibilityModifier` | Fields should be private (public final and immutable fields allowed) |
| `FinalClass` | Classes with only private constructors should be `final` |

---

## Suppression Strategy

### `@SuppressWarnings` Annotation

For suppressing specific false positives in source code:

```java
// Suppress a specific check on a single element
@SuppressWarnings("checkstyle:MagicNumber")  // Justified: HTTP status codes are well-known
public static final int HTTP_OK = 200;

// Suppress Javadoc requirement on a specific method
@SuppressWarnings("checkstyle:MissingJavadocMethod")  // Internal helper, self-documenting name
int calculatePenalty(Finding finding) { ... }

// Suppress multiple checks (when genuinely needed)
@SuppressWarnings({"checkstyle:MethodLength", "checkstyle:ParameterNumber"})  // Justified: builder pattern
public Builder withAllOptions(String a, String b, String c, String d, String e, String f) { ... }
```

### Suppression Rules

| Rule | Rationale |
|------|-----------|
| **Justification comment is mandatory** | Documents why the suppression is safe |
| **Suppress at narrowest scope** | Apply to the method or field, not the class |
| **Use `checkstyle:` prefix** | Distinguishes from PMD or compiler suppressions |
| **Never suppress globally** | Fix the code instead of disabling the rule |
| **Review suppressions during code review** | Suppressions must be justified and approved |

### Suppression Filter Configuration

The `SuppressWarningsFilter` and `SuppressWarningsHolder` modules are enabled in the configuration to support `@SuppressWarnings` annotations. No additional suppression filter files are needed for most projects.

For advanced suppression (e.g., generated code), create a `checkstyle-suppressions.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE suppressions PUBLIC
    "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
    "https://checkstyle.org/dtds/suppressions_1_2.dtd">
<suppressions>
    <!-- Suppress all checks for generated code -->
    <suppress files="[\\/]generated[\\/]" checks=".*"/>

    <!-- Suppress Javadoc for test classes -->
    <suppress files=".*Test\.java$" checks="MissingJavadocType|MissingJavadocMethod"/>

    <!-- Suppress magic number in specific file -->
    <suppress files="HttpStatus\.java$" checks="MagicNumber"/>
</suppressions>
```

Reference it in the Maven plugin:
```xml
<configuration>
    <configLocation>config/checkstyle/checkstyle.xml</configLocation>
    <suppressionsLocation>config/checkstyle/checkstyle-suppressions.xml</suppressionsLocation>
</configuration>
```

---

## Intentionally Excluded Checks

The following checks are intentionally omitted from the configuration with documented rationale:

| Excluded Check | Rationale |
|----------------|-----------|
| `FinalParameters` | Too noisy; matches PMD's exclusion of `MethodArgumentCouldBeFinal` |
| `FinalLocalVariable` | Too noisy; matches PMD's exclusion of `LocalVariableCouldBeFinal` |
| `JavadocVariable` | Javadoc on fields is not required; class and method Javadoc is sufficient |
| `AvoidInlineConditionals` | Ternary expressions are acceptable when simple and readable |
| `TrailingComment` | Inline comments after code are acceptable in moderation |
| `TodoComment` | TODOs are tracked by project management tools, not static analysis |
| `RegexpSingleline` | Trailing whitespace is handled by formatters (IDE, git hooks) |
| `Header` / `RegexpHeader` | No license header requirement in this standard |

---

## Checkstyle vs. PMD Overlap

Checkstyle and PMD have some overlapping checks. Our configuration intentionally coordinates them to avoid duplicate violations:

| Check Area | Checkstyle | PMD | Who Handles It |
|-----------|-----------|-----|---------------|
| Javadoc completeness | `MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod` | `CommentRequired` (disabled) | **Checkstyle** |
| Method length | `MethodLength` (lines) | `NcssCount` (NCSS statements) | **Both** (complementary metrics) |
| Parameter count | `ParameterNumber` | `ExcessiveParameterList` | **Both** (same threshold) |
| Naming conventions | Full suite (15+ checks) | Limited checks | **Checkstyle** |
| Imports | `AvoidStarImport`, `UnusedImports` | `UnnecessaryImport` | **Both** (minimal overlap) |
| Magic numbers | `MagicNumber` | `AvoidLiteralsInIfCondition` | **Both** (different scopes) |
| Boolean complexity | `BooleanExpressionComplexity` | `CyclomaticComplexity` | **Both** (different metrics) |
| Empty statements | `EmptyStatement` | `EmptyIfStmt`, `EmptyWhileStmt`, etc. | **Both** (Checkstyle is broader) |
| Switch default | `MissingSwitchDefault` | `SwitchStmtsShouldHaveDefault` | **Both** (acceptable) |
| Equals/hashCode | `EqualsHashCode` | `OverrideBothEqualsAndHashcode` | **Both** (acceptable) |

**Coordination principle**: Where checks overlap, both tools report. The duplicate signal is acceptable because each tool provides slightly different context in its error message. Where one tool has clearly better coverage (Javadoc, naming), the other defers.

---

## IDE Integration

### IntelliJ IDEA

1. Install the **CheckStyle-IDEA** plugin (Settings > Plugins > Marketplace)
2. Configure: Settings > Tools > Checkstyle
3. Add configuration file: `config/checkstyle/checkstyle.xml`
4. Set scan scope to "Only Java sources (but not tests)"
5. Enable real-time scanning for immediate feedback

Also import the code style to match Checkstyle formatting:
- Settings > Editor > Code Style > Java > Import Scheme > Checkstyle Configuration
- Point to `config/checkstyle/checkstyle.xml`

### Eclipse

1. Install the **eclipse-cs** plugin from the Eclipse Marketplace
2. Configure: Project Properties > Checkstyle
3. Select "External Configuration File" and point to `config/checkstyle/checkstyle.xml`

### VS Code

1. Install the **Checkstyle for Java** extension
2. Set `java.checkstyle.configuration` to `config/checkstyle/checkstyle.xml`

---

## CI/CD Integration

### Key CI Considerations

1. **Checkstyle runs on source code** -- no compilation needed. It can run early in the pipeline.
2. **Fail fast**: Configure `failsOnError=true` (Maven) or `isIgnoreFailures=false` (Gradle) to fail the build on any violation.
3. **Zero warnings**: Set `maxWarnings=0` in Gradle to treat warnings as errors.
4. **Exclude test sources**: Set `includeTestSourceRoots=false` (Maven) or `exclude("**/test/**")` (Gradle).
5. **Reports**: Generate XML for CI tools (SonarQube, GitHub annotations) and HTML for human review.

### Example CI Configuration

**GitHub Actions:**
```yaml
- name: Run Checkstyle
  run: ./gradlew checkstyleMain

- name: Upload Checkstyle report
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: checkstyle-report
    path: build/reports/checkstyle/
```

**GitHub Actions with annotations:**
```yaml
- name: Run Checkstyle
  run: mvn checkstyle:check

- name: Annotate PR with Checkstyle results
  if: failure()
  uses: jwgmeligmeyling/checkstyle-github-action@master
  with:
    path: '**/checkstyle-result.xml'
```

**Maven CI profile:**
```xml
<profile>
    <id>ci</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-checkstyle-plugin</artifactId>
                <configuration>
                    <configLocation>config/checkstyle/checkstyle.xml</configLocation>
                    <failsOnError>true</failsOnError>
                    <consoleOutput>true</consoleOutput>
                </configuration>
                <executions>
                    <execution>
                        <phase>validate</phase>
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

### Phase 1: Formatting Foundation (Week 1)

Start with formatting rules that auto-formatters can fix. These have zero false positives.

**Enable:**
- File-level checks (`NewlineAtEndOfFile`, `FileTabCharacter`, `LineLength`)
- Import checks (`AvoidStarImport`, `UnusedImports`, `CustomImportOrder`)
- Braces and whitespace (`LeftCurly`, `RightCurly`, `NeedBraces`, `WhitespaceAround`)
- `OneStatementPerLine`, `ArrayTypeStyle`

**Fix strategy**: Run IDE auto-formatter with Checkstyle configuration imported. Most violations auto-fix.

**Goal**: Consistent formatting across the codebase. Zero new formatting violations.

### Phase 2: Naming + Size Limits (Week 2-3)

Add naming conventions and size-based rules:

**Enable additionally:**
- All naming checks (`TypeName`, `MethodName`, `MemberName`, `ConstantName`, etc.)
- Size limits (`MethodLength`, `ParameterNumber`, `FileLength`)
- Nesting depth checks

**Fix strategy**: Rename violations are straightforward IDE refactors. Size limit violations require actual refactoring -- track these as tech debt if not immediately fixable.

**Goal**: Consistent naming. No new oversized methods.

### Phase 3: Javadoc + Coding Practices (Week 4+)

Add documentation and coding practice rules:

**Enable additionally:**
- Javadoc checks (`MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod`)
- Coding practices (`EqualsHashCode`, `MagicNumber`, `FallThrough`)
- Class design (`HideUtilityClassConstructor`, `VisibilityModifier`)

**Fix strategy**: Javadoc is the most time-consuming to remediate. Use a suppression filter for existing code and require Javadoc on all new/modified code.

**Goal**: Full Checkstyle coverage. All new code fully compliant.

---

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Not pinning the Checkstyle version | Always specify exact version in Maven/Gradle dependency to avoid rule behavior changes |
| Using default Google Style without adjustments | Import our `config/checkstyle/checkstyle.xml` which aligns thresholds with engineering standards |
| Suppressing rules globally to pass CI | Fix the code; only suppress specific false positives with justification |
| Running Checkstyle on test sources | Set `includeTestSourceRoots=false` (Maven) or exclude test patterns (Gradle) |
| Conflicting with IDE auto-formatter | Import the Checkstyle config into your IDE's code style settings |
| Duplicating Javadoc enforcement with PMD | PMD's `CommentRequired` is intentionally disabled; Checkstyle handles Javadoc |
| Ignoring `MagicNumber` violations | Extract constants with descriptive names; suppress only for well-known values (HTTP status codes) |
| Setting `maxWarnings` > 0 in Gradle | Set `maxWarnings=0` to enforce zero-tolerance on warnings |
| Not excluding generated code | Use suppression filters to exclude generated source directories |
| Treating Checkstyle as optional | Checkstyle is a hard gate -- configure `failsOnError=true`, never `false` |

---

## Related Documents

- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- Code quality standards with thresholds that Checkstyle enforces
- [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) -- Java-specific conventions including Checkstyle build integration examples
- [STATIC_ANALYSIS_STANDARDS.md](./STATIC_ANALYSIS_STANDARDS.md) -- PMD, detekt, SpotBugs, ArchUnit standards (complementary to Checkstyle)
- [SPOTBUGS_STANDARDS.md](./SPOTBUGS_STANDARDS.md) -- SpotBugs bytecode analysis (catches bugs Checkstyle cannot)
- [ARCHUNIT_STANDARDS.md](./ARCHUNIT_STANDARDS.md) -- Architecture testing (enforces structural rules Checkstyle cannot)
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Design patterns that inform code structure
- CI/CD pipeline integration guide (see issue #30)

---

**Last Updated**: February 19, 2026
**Version**: 1.0
