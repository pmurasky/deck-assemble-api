---
name: kotlin-standards
description: >
  Use when writing, reviewing, or setting up tooling for Kotlin code — Kotlin 2.3 / K2 compiler
  conventions: null safety (no !!), immutability (val, data class), extension functions,
  sealed classes, structured concurrency (coroutineScope, supervisorScope, Flow, StateFlow),
  Power Assert plugin, KDoc, JUnit 5 testing with MockK, Gradle Kotlin DSL,
  and SOLID/design pattern idioms.
triggers:
  - "writing new kotlin code with idiomatic standards"
  - "reviewing kotlin code for null safety issues"
  - "choosing between data classes coroutines and sealed types"
  - "configuring gradle kotlin dsl or detekt"
  - "writing kotlin tests with junit and mockk"
  - "migrating kotlin project to k2 compiler"
not_for:
  - "java specific language feature decisions"
  - "deep spotbugs or checkstyle configuration work"
  - "react frontend conventions unrelated to kotlin"
---

# Kotlin Standards (Kotlin 2.3 / K2 Compiler)

## Table of Contents

- [Use when](#use-when)
- [Naming Conventions](#naming-conventions)
- [Immutability](#immutability)
- [Null Safety](#null-safety)
- [Function Design](#function-design)
- [Sealed Classes / Interfaces](#sealed-classes--interfaces)
- [Coroutines](#coroutines)
- [Exception Handling](#exception-handling)
- [Testing (JUnit 5 + MockK)](#testing-junit-5--mockk)
- [Build (Gradle Kotlin DSL)](#build-gradle-kotlin-dsl)

## K2 Compiler Notes

- Default since Kotlin 2.0 — 2× faster compilation, improved smart casts
- Upgrade `kotlinx.coroutines` to 1.8.0+ before migrating to K2
- Address K2 warnings — they are correctness improvements, not noise

## Use when

- Writing new Kotlin code (JVM, Android, Multiplatform)
- Reviewing for null safety, immutability, or idiomatic usage issues
- Configuring detekt or Gradle Kotlin DSL
- Writing tests with JUnit 5 and MockK
- Applying SOLID principles idiomatically in Kotlin

## Not for

- Java-specific feature decisions (records, `Optional`, module imports)
- Deep SpotBugs or Checkstyle configuration — use `java-static-analysis`
- Frontend React/Vitest conventions unrelated to Kotlin

---

## Naming Conventions

- Classes/interfaces/objects: PascalCase (`OrderService`)
- Functions/properties/variables: camelCase (`calculateTotal`)
- Constants: `SCREAMING_SNAKE_CASE` (`MAX_RETRIES`)
- No `I` prefix on interfaces; no `*Manager`/`*Handler` (SRP signal)

## Immutability

- `val` over `var` — always prefer immutable bindings
- `data class` for DTOs, config, and value objects
- Immutable collections by default: `listOf`, `mapOf`; never expose `MutableList` in public API
- Use `copy()` for immutable updates

## Null Safety

**HARD BLOCK: Never use `!!` in production code.**

- Use `?.`, `?:` (Elvis), `requireNotNull()`, or early return guards
- `!!` only in test code where null is structurally impossible

## Function Design

- Max 20 lines per function (excluding blank lines and braces)
- Max 5 parameters — use a `data class` when exceeded
- Ktor routes taking injected services: up to 7 params acceptable; above 7, use a dependencies object
- Use expression bodies for simple single-expression functions

## Extension Functions

- Use when the operation is conceptually "on" the receiver type
- Keep discoverable — file names match receiver type (`StringExtensions.kt`)
- Avoid when the function has no semantic connection to the receiver

## Sealed Classes / Interfaces

- Use for restricted type hierarchies requiring exhaustive `when`
- Prefer `sealed interface` over `sealed class` for flexibility
- Use `data object` for singleton variants

## Coroutines

- `suspend` functions over `Deferred`; `coroutineScope` for structured concurrency
- `supervisorScope` when children fail independently
- `withContext(Dispatchers.IO)` for blocking I/O; never `GlobalScope` in production
- Use `kotlinx.coroutines 1.9.x` (min 1.8.0 for K2)

## Exception Handling

- `runCatching` for operations that may fail
- `use {}` for all `AutoCloseable` resources
- Domain-specific exceptions extending `RuntimeException`
- Never catch `Exception`/`Throwable` without re-throwing or logging

## Testing (JUnit 5 + MockK)

- Test names: backtick syntax `` `should do X when Y` ``
- Given-When-Then structure
- MockK for Kotlin-idiomatic mocking: `every`, `verify`, `coEvery` for coroutines

→ [Full test class example](./REFERENCE.md#testing-junit-5--mockk--full-example)

## Build (Gradle Kotlin DSL)

`build.gradle.kts` — enable `useJUnitPlatform()`, Jacoco, OWASP dependency check with `failBuildOnCVSS = 7.0f`.

→ [Full build.gradle.kts example](./REFERENCE.md#build-gradle-kotlin-dsl)

## Static Analysis

- **detekt**: Cyclomatic ≤ 10, function ≤ 20 lines, class ≤ 300 lines, params ≤ 5, nesting ≤ 3
- **PMD 7**: `FunctionNameTooShort` + `OverrideBothEqualsAndHashcode` only
- **ArchUnit**: layer boundary enforcement

Suppressions: always narrowest scope; always include justification comment.

## Definition of Done

- [ ] `val` over `var`; no `!!` in production; no `GlobalScope`
- [ ] All public APIs have KDoc
- [ ] Functions ≤ 20 lines; classes ≤ 300 lines; max 5 params
- [ ] `detekt` passes; JUnit 5 + MockK tests pass; coverage ≥ 80%
- [ ] `dependencyCheckAnalyze` passes (no CVE ≥ 7)
- [ ] `use {}` for all `AutoCloseable` resources
