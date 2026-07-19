---
name: solid-principles
description: >
  Use when designing classes, reviewing code for SOLID violations, or refactoring
  tightly coupled code — deep-dive with real-world analogies, violation signals, and
  before/after code examples in Kotlin, Java, Python, and PHP. Includes checklists
  for each principle and a quick-reference violations table.
triggers:
  - "designing class hierarchies or interfaces with solid"
  - "reviewing code for solid principle violations"
  - "refactoring tightly coupled code toward better design"
  - "investigating solid issues from pre commit review"
not_for:
  - "syntax formatting or linter questions only"
  - "premature abstraction in tiny stable code"
  - "security testing or analysis configuration work"
---

# SOLID Principles

Apply these five principles to ALL code. They make codebases easier to test, extend, and maintain.

Canonical owner: SOLID_PRINCIPLES (see STANDARDS_OWNERSHIP_MATRIX).

| Letter | Principle | One-Liner |
|--------|-----------|-----------|
| **S** | Single Responsibility | A class has one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subtypes must be substitutable for base types |
| **I** | Interface Segregation | Prefer focused interfaces over fat ones |
| **D** | Dependency Inversion | Depend on abstractions, not concretions |

---

## Use when

- When designing class hierarchies or interfaces
- When reviewing code for SOLID violations
- When refactoring existing code toward better design
- When the pre-commit checklist flags SOLID issues

## Not for

- Syntax, formatting, or linter questions that do not involve object design
- Premature abstraction in tiny, stable code with no real extension pressure
- Security, testing, or static-analysis configuration that belongs to dedicated skills

---

## S — Single Responsibility Principle (SRP)

> A class should have only one reason to change.

**Violation signals:** Class name contains "Manager", "Handler", "Utility", or "Helper"; more than 10 methods; method name contains "And" (e.g., `validateAndSave()`); mixes business logic with infrastructure; more than 2 private methods.

**Fix:** Extract each responsibility into its own class; inject via constructor. Describe the class in ONE sentence without "and".

## O — Open/Closed Principle (OCP)

> Software entities should be open for extension but closed for modification.

**Violation signals:** `when`/`switch` on types or enums; `if-else` chains that grow with each new variant; adding a feature requires modifying multiple existing classes.

**Fix:** **Strategy pattern** — define an interface, implement per variant, inject the strategy. Add new variants as new classes only.

## L — Liskov Substitution Principle (LSP)

> Subtypes must be substitutable for their base types without altering program correctness.

**Violation signals:** Subclass throws exceptions the parent does not declare; subclass leaves methods empty or as no-ops; `instanceof`/`is` checks before calling methods.

**Fix:** Rework the abstraction hierarchy. Split the interface to remove capabilities that don't apply (e.g., `FlyingBird` vs `Bird`).

## I — Interface Segregation Principle (ISP)

> Clients should not be forced to depend on interfaces they do not use.

**Violation signals:** Interface has more than 5 methods; implementing classes throw "not implemented"; clients use only 1-2 of the interface's methods.

**Fix:** Split the fat interface into focused interfaces by caller need.

## D — Dependency Inversion Principle (DIP)

> High-level modules should not depend on low-level modules. Both should depend on abstractions.

**Violation signals:** Direct instantiation inside classes (`val parser = CheckstyleParser()`); no constructor injection; cannot mock dependencies for testing.

**Fix:** Inject all dependencies via constructor using interface types.

---

## Common Violations Quick Reference

| Violation | Principle | Fix |
|-----------|-----------|-----|
| God Class (300+ lines, 10+ methods) | SRP | Extract focused classes |
| `when`/`switch` on type | OCP | Strategy pattern or polymorphism |
| Subclass throws `UnsupportedOperationException` | LSP | Rework the abstraction hierarchy |
| Interface with `TODO("Not needed")` stubs | ISP | Split into focused interfaces |
| `private val dep = ConcreteClass()` | DIP | Constructor injection with interface |
| Method name contains "And" | SRP | Split into separate methods/classes |
| `instanceof`/`is` checks before method calls | LSP | Fix the type hierarchy |

For complete reference material with before/after code examples in Kotlin, Java, and Python, see [REFERENCE.md](REFERENCE.md) in this skill directory.
