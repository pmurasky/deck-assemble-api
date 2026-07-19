# Java Static Analysis — Extended Reference

This companion file contains the full configuration examples and rule catalogs referenced in [SKILL.md](./SKILL.md).

## Table of Contents

- [Checkstyle — Maven Configuration](#checkstyle--maven-configuration)
- [Checkstyle — Gradle Configuration](#checkstyle--gradle-configuration)
- [SpotBugs — Maven Configuration](#spotbugs--maven-configuration)
- [SpotBugs — Gradle Configuration](#spotbugs--gradle-configuration)
- [ArchUnit — Tier 1: Critical Rules](#archunit--tier-1-critical-rules)
- [ArchUnit — Tier 2: High Value Rules](#archunit--tier-2-high-value-rules)
- [ArchUnit — Tier 3: Advanced (DDD / Large Projects)](#archunit--tier-3-advanced-ddd--large-projects)
- [ArchUnit — Kotlin Usage](#archunit--kotlin-usage)
- [ArchUnit — Test Organization](#archunit--test-organization)
- [ArchUnit — Freezing for Legacy Codebases](#archunit--freezing-for-legacy-codebases)
- [ArchUnit — CI Integration](#archunit--ci-integration)

---

## Checkstyle — Maven Configuration

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
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Checkstyle — Gradle Configuration

```kotlin
plugins { checkstyle }

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
    exclude("**/test/**")
}
```

---

## SpotBugs — Maven Configuration

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
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

## SpotBugs — Gradle Configuration

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
```

Always include the Find Security Bugs plugin — it adds 138 security detectors (SQL injection, XSS, path traversal, XXE, hardcoded secrets, and more).

---

## ArchUnit — Tier 1: Critical Rules

### Layered Architecture

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
        .because("layer boundaries enforce SRP and DIP");
```

### Package Cycle Detection

```java
@ArchTest
static final ArchRule no_package_cycles =
    slices().matching("com.myapp.(*)..").should().beFreeOfCycles()
        .because("package cycles create tight coupling and make refactoring impossible");
```

### General Coding Rules

```java
@ArchTest
static final ArchRule no_standard_streams =
    GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
        .because("use SLF4J/Logback instead of System.out/err");

@ArchTest
static final ArchRule no_field_injection =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION
        .because("use constructor injection for testability (DIP)");

@ArchTest
static final ArchRule no_java_util_logging =
    GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
        .because("use SLF4J as the logging facade");
```

---

## ArchUnit — Tier 2: High Value Rules

### Dependency Direction Rules

```java
@ArchTest
static final ArchRule services_should_not_depend_on_controllers =
    noClasses().that().resideInAPackage("..service..")
        .should().dependOnClassesThat().resideInAPackage("..controller..")
        .because("services must not know about the presentation layer (DIP)");
```

> **Use `dependOnClassesThat`**, not `accessClassesThat`. `dependOn` catches field types, method parameter/return types, extends/implements, and annotations — not just runtime method calls.

### Naming Convention Enforcement

```java
@ArchTest
static final ArchRule controllers_should_be_suffixed =
    classes().that().resideInAPackage("..controller..")
        .should().haveSimpleNameEndingWith("Controller")
        .because("naming consistency makes the codebase navigable");

@ArchTest
static final ArchRule logger_fields_should_be_private_static_final =
    fields().that().haveRawType(org.slf4j.Logger.class)
        .should().bePrivate().andShould().beStatic().andShould().beFinal()
        .because("loggers must follow the standard private static final pattern");
```

---

## ArchUnit — Tier 3: Advanced (DDD / Large Projects)

### Onion Architecture

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

---

## ArchUnit — Kotlin Usage

```kotlin
@AnalyzeClasses(packagesOf = [MyApplication::class],
    importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {

    @ArchTest
    val layered_architecture_is_respected: ArchRule =
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Persistence").definedBy("..persistence..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Service")
            .because("layer boundaries enforce SRP and DIP")
}
```

---

## ArchUnit — Test Organization

```
src/test/java/com/myapp/architecture/
    ArchitectureTest.java          -- master test composing rule groups
    CodingRulesTest.java           -- GeneralCodingRules, logging, exceptions
    LayerDependencyRulesTest.java  -- layer access and direction rules
    NamingConventionTest.java      -- naming suffix/prefix and location rules
    CycleFreeTest.java             -- slice and package cycle checks
```

---

## ArchUnit — Freezing for Legacy Codebases

```java
import com.tngtech.archunit.library.freeze.FreezingArchRule;

@ArchTest
static final ArchRule layered_architecture =
    FreezingArchRule.freeze(
        layeredArchitecture()
            .consideringAllDependencies()
            // ... layers and constraints ...
    );
```

Commit `archunit_store/` to VCS. Violation count ratchets down — it can only decrease.

---

## ArchUnit — CI Integration

```yaml
# GitHub Actions
- name: Run tests (includes ArchUnit)
  run: ./gradlew test
  env:
    JAVA_OPTS: "-Darchunit.freeze.store.default.allowStoreCreation=false"
```
