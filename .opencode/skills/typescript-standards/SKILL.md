---
name: typescript-standards
description: >
  Use when writing, reviewing, or setting up tooling for TypeScript or JavaScript code —
  TypeScript 5.4+ with strict mode: mandatory safety rules (banned patterns, secrets, input
  validation), strict tsconfig, type system best practices (discriminated unions, utility
  types, generics, type guards, satisfies operator, NoInfer, using declarations), null safety,
  immutability, async/await, dependency injection, domain-driven project structure, testing
  with Jest/Vitest, ESLint/Prettier/Husky tooling, and SOLID/design pattern idioms.
triggers:
  - "writing new typescript or javascript code safely"
  - "reviewing typescript code for null safety issues"
  - "configuring tsconfig eslint prettier or husky"
  - "designing discriminated unions generics or utility types"
  - "writing tests with jest or vitest"
  - "using typescript five satisfies or noinfer features"
not_for:
  - "react component ui conventions better handled elsewhere"
  - "non typescript languages without ts or js scope"
  - "browser automation or visual qa with playwright"
---

# TypeScript Standards (TypeScript 5.4+)

## Table of Contents

- [Use when](#use-when)
- [Mandatory Safety & Security Rules](#mandatory-safety--security-rules)
- [Strict tsconfig.json](#strict-tsconfigjson)
- [Type System](#type-system)
- [TypeScript 5.x Features](#typescript-5x-features)
- [Naming Conventions](#naming-conventions)
- [Null Safety & Immutability](#null-safety--immutability)
- [Async/Await](#asyncawait)
- [Testing (Jest / Vitest)](#testing-jest--vitest)
- [Tooling](#tooling)

## Use when

- Writing new TypeScript or JavaScript code in any project
- Reviewing TypeScript code for type safety, null safety, or architectural issues
- Configuring `tsconfig.json`, ESLint, Prettier, or Husky
- Designing types — discriminated unions, generics, utility types
- Writing tests with Jest or Vitest

## Not for

- React component-specific UI conventions — use `frontend-standards`
- Non-TypeScript/JavaScript languages with no TS/JS scope
- Browser automation or visual QA needing Playwright

---

## Mandatory Safety & Security Rules

**HARD BLOCK — never use:**

| Banned | Safe Alternative |
|--------|-----------------|
| `eval(code)` / `new Function(code)` | Never execute dynamic strings |
| `any` without justification | `unknown` — narrow with type guards |
| `@ts-ignore` | Fix the type error |
| `innerHTML = userInput` | `textContent` or DOMPurify |
| `Math.random()` for tokens | `crypto.randomUUID()` |
| String SQL concatenation | Parameterized queries / ORMs |

**Secrets:** Never hardcode. Load from env vars. Never commit `.env` files.

**Input validation at boundaries:** Use `zod` (or equivalent) at every external boundary (API, form, config).

## Strict tsconfig.json

Enable: `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noImplicitReturns`, `noFallthroughCasesInSwitch`.

→ [Full tsconfig.json example](./REFERENCE.md#strict-tsconfigjson)

## Type System

- Discriminated unions over inheritance for sum types
- `unknown` at external boundaries; narrow with type guards before use
- `satisfies` operator for extra type checking without widening
- `NoInfer<T>` to prevent unwanted type argument inference
- Avoid `as` casts — use narrowing or type guards instead

## TypeScript 5.x Features

- **`satisfies`** (5.0): validate an expression against a type without widening
- **`NoInfer<T>`** (5.4): prevent unintended cross-inference in generics
- **`using` declarations** (5.2): explicit resource disposal (`Symbol.dispose`)

## Naming Conventions

- Types/interfaces: PascalCase; functions/variables: camelCase; constants: `SCREAMING_SNAKE_CASE`
- No `I` prefix on interfaces; no `*Manager`/`*Handler`

## Null Safety & Immutability

- Enable `strictNullChecks`; use optional chaining `?.` and nullish coalescing `??`
- `const` over `let`; `readonly` on properties; `ReadonlyArray<T>` for arrays
- Never mutate function arguments

## Async/Await

- `async`/`await` over raw promises; always `await` before returning
- Typed error handling: `try/catch` with `unknown`, narrow before use
- No floating promises — always handle or `void` explicitly

## Testing (Jest / Vitest)

- Given-When-Then structure; test names describe behavior
- Mock only at system boundaries (HTTP, DB, file I/O) — no mocking internal modules
- Min 80% unit test coverage; 100% for critical paths

→ [Full test example](./REFERENCE.md#testing-jest--vitest)

## Tooling

- ESLint with `@typescript-eslint/recommended-type-checked`
- Prettier for formatting; Husky pre-commit hook to enforce
- Vitest preferred for new projects (faster, native ESM)

## Definition of Done

- [ ] No `any`, `eval`, `@ts-ignore`, or hardcoded secrets
- [ ] `strict` tsconfig enabled; all boundaries validated with `zod`
- [ ] Discriminated unions over inheritance for sum types
- [ ] `const`/`readonly` preferred; no argument mutation
- [ ] Jest/Vitest tests pass; coverage ≥ 80%
- [ ] ESLint + Prettier pass with zero errors
