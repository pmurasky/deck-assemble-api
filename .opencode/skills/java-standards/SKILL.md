---
name: java-standards
description: >
  Use when writing, reviewing, or setting up tooling for Java code — Java 21/25
  conventions: records, sealed classes, pattern matching, text blocks, Optional, unnamed
  variables, Markdown Javadoc, Stream Gatherers, module imports, flexible constructors,
  null safety, immutability, DI, JUnit 5 testing, and SOLID/design pattern idioms.
triggers:
  - "writing modern java code with current conventions"
  - "reviewing java code for idiomatic feature usage"
  - "designing immutable records sealed types or optionals"
  - "setting up junit five testing and dependency injection"
  - "configuring static analysis for a java project"
  - "using unnamed variables or underscore bindings in java"
  - "writing markdown javadoc with triple slash syntax"
not_for:
  - "legacy java codebases stuck below java twenty one"
  - "deep checkstyle or spotbugs tuning work"
  - "kotlin or generic jvm standards guidance"
---

# Java Standards (Java 21 / 25)

## Table of Contents

- [Use when](#use-when)
- [Java Version Feature Matrix](#java-version-feature-matrix)
- [Records (Immutable Data)](#records-immutable-data)
- [Sealed Classes / Interfaces](#sealed-classes--interfaces)
- [Pattern Matching](#pattern-matching)
- [Null Safety](#null-safety)
- [Exception Handling](#exception-handling)
- [Testing (JUnit 5 + AssertJ + Mockito)](#testing-junit-5--assertj--mockito)
- [Static Analysis Tools](#static-analysis-tools)

## Use when

- Writing new Java code targeting Java 21 or Java 25
- Reviewing Java code for idiomatic usage of modern features
- Designing immutable data models or implementing null safety patterns
- Setting up constructor injection or writing JUnit 5 tests with AssertJ and Mockito
- Applying SOLID principles idiomatically in Java

## Not for

- Pre-Java-21 codebases that cannot use the modern language features referenced here
- Deep Checkstyle, SpotBugs, or ArchUnit tuning — use `java-static-analysis`
- Kotlin or general JVM guidance not specifically about Java coding standards

---

## Java Version Feature Matrix

Java 21 (LTS): records, sealed classes, pattern matching, text blocks, `Optional`. Java 22: unnamed variables (`_`). Java 23: Markdown Javadoc (`///`). Java 24: Stream Gatherers. Java 25 (LTS): module imports, flexible constructors. **String templates: WITHDRAWN — use `String.formatted()` instead.**

→ [Full feature matrix](./REFERENCE.md#java-version-feature-matrix)

## Naming & Style

- PascalCase for classes/records; camelCase for methods/fields; `SCREAMING_SNAKE_CASE` for constants
- No `I` prefix on interfaces; no `*Manager`/`*Handler`; Google Java Style Guide baseline

## Records (Immutable Data)

Use records for DTOs, value objects, command objects. Compact constructors for validation. Java 25 flexible constructors allow mutations before canonical assignment.

→ [Full record examples](./REFERENCE.md#records-immutable-data)

## Sealed Classes / Interfaces

Use for restricted type hierarchies requiring exhaustive `switch`. Prefer `sealed interface`. Enables compile-enforced exhaustive dispatch.

## Pattern Matching & Unnamed Variables

- `instanceof` pattern matching (Java 16+): `if (shape instanceof Circle c)`
- Switch expressions with patterns (Java 21+)
- Unnamed variables `_` (Java 22+) for discarded bindings in catch/switch/for-each/lambda

## Null Safety & Immutability

- `Optional<T>` for absent return values — never return `null`
- `Objects.requireNonNull()` in constructors; `@NonNull`/`@Nullable` on params and fields
- `List.of()` / `List.copyOf()` — never expose mutable collections in public API

## Exception Handling

- Domain-specific exceptions extending `RuntimeException`
- `try`-with-resources for all `AutoCloseable`; never swallow exceptions silently

## Testing (JUnit 5 + AssertJ + Mockito)

- Test names: `shouldDoSomethingWhenCondition` (camelCase)
- Given-When-Then structure; AssertJ for fluent assertions
- `@ParameterizedTest` + `@CsvSource`/`@MethodSource` for data-driven tests
- `@Nested` for grouping related scenarios

→ [Full test class example](./REFERENCE.md#testing-junit-5--assertj--mockito)

## Static Analysis Tools

| Tool | Purpose | Key Thresholds |
|------|---------|----------------|
| PMD 7 | Code quality | Cyclomatic ≤ 10, method ≤ 20 lines, class ≤ 300 lines |
| Checkstyle 10.x | Style | Google style + project thresholds |
| SpotBugs | Bytecode bugs | Effort: Max, threshold: Medium, rank ≤ 14 |
| ArchUnit | Architecture | Layer boundaries, no package cycles |

Suppressions: always narrowest scope; always include justification comment.

Run `./gradlew dependencyCheckAnalyze` before pushing (fail on CVE ≥ 7).

## Definition of Done

- [ ] Records for DTOs; sealed classes for restricted hierarchies; pattern matching over `instanceof` + cast
- [ ] `Optional<T>` over `null`; constructor injection with `requireNonNull`
- [ ] Methods ≤ 20 lines; classes ≤ 300 lines; max 5 params
- [ ] Javadoc on all public APIs (`///` on Java 23+)
- [ ] JUnit 5 + AssertJ tests pass; coverage ≥ 80%
- [ ] PMD + Checkstyle + SpotBugs zero violations; no CVE ≥ 7
- [ ] `try`-with-resources for all `AutoCloseable`
