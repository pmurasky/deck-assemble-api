---
name: java-static-analysis
description: >
  Use when configuring or running Java static analysis — ArchUnit layer boundary enforcement
  (recommended rules by tier: critical, high-value, advanced), SpotBugs bug categories
  (correctness, concurrency, security) and rank system, and Checkstyle rule categories and
  enforcement (Javadoc, naming, imports, size limits).
triggers:
  - "configuring checkstyle for a java project"
  - "setting up spotbugs and security bug detection"
  - "writing or reviewing archunit architecture tests"
  - "diagnosing checkstyle spotbugs or archunit failures"
  - "reviewing static analysis suppressions in java code"
not_for:
  - "general java coding questions outside analysis tools"
  - "kotlin only static analysis without java tools"
  - "manual review with no related analysis concern"
---

# Java Static Analysis — ArchUnit, SpotBugs, Checkstyle

Use all three alongside PMD for comprehensive Java quality enforcement.

## Table of Contents

- [Use when](#use-when)
- [Checkstyle — Style Enforcement](#checkstyle--style-enforcement)
- [SpotBugs — Bytecode Bug Detection](#spotbugs--bytecode-bug-detection)
- [ArchUnit — Architecture Enforcement](#archunit--architecture-enforcement)
- [Suppression Strategy](#suppression-strategy)

## Use when

- Configuring Checkstyle, SpotBugs, or ArchUnit for a Java project
- Writing or reviewing ArchUnit tests (layer rules, cycle detection, naming)
- Diagnosing a Checkstyle, SpotBugs, or ArchUnit failure
- Reviewing a PR for suppression increases or missing justification comments

## Not for

- General Java language design outside static-analysis tooling
- Kotlin-only PMD or detekt — use `static-analysis`
- Manual code review with no related analysis tool concern

---

## Checkstyle — Style Enforcement

Java-only. Checkstyle 10.21.1+. Violations **must fail the build**.

**Key thresholds:**

| Check | Value |
|-------|-------|
| Method length | 20 lines (`countEmpty=false`) |
| File length | 500 lines |
| Parameter count | 5 max |
| Line length | 120 chars |
| Nesting depth (if/for/try) | 3/2/1 max |

**Critical rule categories:**
- **Javadoc**: `MissingJavadocType`, `MissingJavadocMethod`, `JavadocMethod` (complete tags), `NonEmptyAtclauseDescription`
- **Naming**: PascalCase types, camelCase methods/fields, `SCREAMING_SNAKE_CASE` constants; max 2 consecutive uppercase (`XmlParser` not `XMLParser`)
- **Imports**: no wildcards, no unused, static imports first
- **Coding**: `EqualsHashCode`, no magic numbers (except -1/0/1/2), `MissingSwitchDefault`

Test classes exempt from Javadoc for `@Test`, `@BeforeEach`, `@AfterEach`, `@Nested`.

→ [Full Maven & Gradle config](./REFERENCE.md#checkstyle--maven-configuration)

---

## SpotBugs — Bytecode Bug Detection

Java-only. SpotBugs 4.9.7+. Analyzes compiled bytecode — finds bugs PMD/Checkstyle cannot.

**Always include Find Security Bugs plugin** (138 security detectors: SQL injection, XSS, XXE, hardcoded secrets, path traversal).

**Standard config:** `effort=Max`, `threshold=Medium`, `maxRank=14`

| Rank | Category | Policy |
|------|----------|--------|
| 1–4 | Scariest | Build MUST fail |
| 5–9 | Scary | Build MUST fail |
| 10–14 | Troubling | Block in strict mode |
| 15–20 | Of Concern | Advisory |

**Always enable:** `CORRECTNESS`, `MT_CORRECTNESS`, `SECURITY`, `BAD_PRACTICE`

→ [Full Maven & Gradle config](./REFERENCE.md#spotbugs--maven-configuration)

---

## ArchUnit — Architecture Enforcement

**Critical tier (always enforce):**
- No cycles between packages
- Controller → Service → Repository dependency direction
- Domain layer has no dependency on infrastructure layer

**High-value tier:**
- `*Controller` classes only in `..controller..` packages
- `*Repository` classes only in `..repository..` packages
- No `java.util.Date` or `java.util.Calendar` (use `java.time`)

**Advanced tier:**
- No field injection (`@Autowired` on fields)
- All `@Service` classes have interfaces

→ [Full ArchUnit test examples](./REFERENCE.md#archunit--tier-1-critical-rules)

---

## Suppression Strategy

| Situation | Action |
|-----------|--------|
| Generated code | Exclude path in tool config |
| Legacy code | Baseline file + tech debt tracking |
| False positive | Suppress narrowest scope + justification |
| Wrong rule for project | Disable in config with rationale |

```java
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH") // findById contract guarantees non-null here
@SuppressWarnings("checkstyle:MagicNumber") // HTTP status codes are well-known constants
```

PR review: new suppressions need reviewer approval; suppression count must trend downward.

## Definition of Done

- [ ] Checkstyle passes (zero violations); all public APIs have Javadoc
- [ ] SpotBugs passes at `maxRank=14`; Find Security Bugs plugin enabled
- [ ] ArchUnit critical tier passes; no package cycles
- [ ] No new suppressions without justification comment
