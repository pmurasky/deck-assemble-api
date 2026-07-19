# TypeScript / JavaScript Coding Standards

## Overview
This document outlines TypeScript and JavaScript-specific coding standards that supplement the language-agnostic standards in `CODING_PRACTICES.md` and `CODING_STANDARDS.md`.

We support **TypeScript 5.4+** (stay within N-1 of latest stable) with strict mode enabled. All new code MUST be written in TypeScript. JavaScript is accepted only for configuration files (e.g., `jest.config.js`, `.eslintrc.cjs`) and legacy code that has a migration plan.

## Mandatory Rules

These rules are **non-negotiable** for all TypeScript/JavaScript code. They supplement the language-agnostic rules in `CODING_PRACTICES.md`.

### Safety and Security

**Banned patterns -- never use these:**
- `eval()` / `new Function()` -- arbitrary code execution
- `any` type without documented justification -- disables type safety
- `@ts-ignore` without documented justification -- hides type errors (prefer `@ts-expect-error` with explanation)
- `innerHTML` / `outerHTML` with unsanitized input -- XSS vulnerability
- `document.write()` -- XSS risk and performance issues
- String concatenation for SQL / shell commands -- injection risk

**Safe alternatives:**
```typescript
// Bad: XSS risk
element.innerHTML = userInput;

// Good: Safe DOM manipulation
element.textContent = userInput;

// Bad: Shell injection risk
exec(`ls ${userInput}`);

// Good: Parameterized command
execFile("ls", [userInput]);
```

**Secrets and sensitive data:**
- Never hardcode secrets (tokens, passwords, private keys, API keys) in source code
- Never log secrets or sensitive fields; redact if necessary
- Use environment variables, secret managers, or config injection for credentials
- Use `crypto.randomUUID()` or `crypto.getRandomValues()` for security-sensitive token generation (not `Math.random()`)

**Input validation:**
- Treat all external inputs as untrusted
- Validate types, ranges, lengths, and formats before processing
- Use runtime validation libraries (Zod, io-ts, class-validator) at system boundaries
- Sanitize data before use in queries, commands, or templates

### Dependency Control

- Do not introduce new third-party dependencies unless explicitly requested/approved
- Prefer Node.js built-in modules and Web Platform APIs when reasonable
- When a dependency is necessary, document the reason in the commit message
- Audit dependencies for security vulnerabilities (`npm audit`)
- Pin major versions in `package.json` (use `^` for minor/patch flexibility)

**Version Selection Rule (MANDATORY):** When adding or updating any dependency, always select the latest version that has no known vulnerabilities.

1. **Identify the latest published version** on npm.
2. **Scan for vulnerabilities** using `npm audit` after installing.
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document why in the commit message.
4. **Never pin an older version out of habit** — always start from latest and work backwards only if forced by a CVE.

```bash
# Install the latest version and immediately audit
npm install <package>@latest
npm audit

# Or audit an existing install
npm audit --audit-level=high
```

**Commit message example when stepping back from latest:**
```
chore(deps): pin axios to 1.6.8 instead of 1.7.0

1.7.0 has CVE-2024-XXXXX (high severity, no fix yet).
1.6.8 is the latest clean version. Revisit when a patched version is released.
```

See [CODING_PRACTICES.md](./CODING_PRACTICES.md#version-selection-rule-mandatory) for the full cross-ecosystem rule and audit tool table.

### Logging

- Use a structured logging library (e.g., pino, winston) for production code (not `console.log`)
- Logs must be actionable and include relevant context (IDs, operation names, etc.)
- Never log secrets, credentials, or sensitive user data
- Use appropriate log levels (see `CODING_PRACTICES.md` for level guidance)

```typescript
import { logger } from "./logger";

// Good: Structured, actionable log with context
logger.info({ orderId: order.id, total: order.total }, "Order processed");

// Good: Error with context for debugging
logger.error({ orderId: order.id, error }, "Payment failed");

// Bad: console.log in production code
console.log(`Processing order ${order.id}`);

// Bad: Secrets in logs
logger.debug({ token: apiToken }, "Connecting to API");
```

### Determinism and Reliability

- Avoid nondeterminism (time, randomness, concurrency) unless explicitly required and documented
- If randomness is used for deterministic outcomes (tests, reproducible outputs), seed it and document the seed
- Do not silently change behavior (function signatures, return types, exception semantics) -- highlight such changes explicitly in commit messages and PR descriptions

## Official Style Guide
We follow the project's ESLint configuration as the authoritative style guide. When no project config exists, default to the [TypeScript ESLint recommended rules](https://typescript-eslint.io/rules/) with the following project-specific additions and clarifications. Use Prettier for formatting.

## Project Organization

Follow domain-driven project structure:

```
project/
├── src/
│   ├── order/              # Order domain
│   │   ├── order.service.ts
│   │   ├── order.repository.ts
│   │   ├── order.model.ts
│   │   └── index.ts        # Public API barrel export
│   ├── payment/            # Payment processing
│   ├── inventory/          # Inventory management
│   ├── notification/       # Notification services
│   ├── config/             # Configuration
│   └── common/             # Shared utilities (keep minimal)
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── tsconfig.json
├── package.json
└── README.md
```

**Key Principles:**
- Package by domain (what it does), not by layer (what it is)
- Avoid `utils`, `helpers`, `managers` directories -- they become dumping grounds
- Each directory represents a cohesive functional area
- Minimize cross-domain imports
- Use barrel exports (`index.ts`) to define each domain's public API
- Keep barrel exports thin -- only re-export public interfaces, not internals

## Naming Conventions

- **Classes / Interfaces / Types / Enums**: PascalCase (`OrderService`, `PaymentResult`)
- **Functions / Methods / Variables**: camelCase (`calculateTotal`, `orderCount`)
- **Constants**: SCREAMING_SNAKE_CASE for true constants (`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Files**: kebab-case or dot-separated (`order.service.ts`, `payment-processor.ts`)
- **Directories**: kebab-case (`order-management/`) or lowercase (`order/`)
- **Type parameters**: Single uppercase letter or descriptive PascalCase (`T`, `TResult`, `TKey`)
- **Test files**: `.test.ts` or `.spec.ts` suffix (`order.service.test.ts`)
- **Interfaces**: Do NOT prefix with `I` (use `OrderRepository`, not `IOrderRepository`)
- **Type aliases**: Do NOT suffix with `Type` (use `OrderStatus`, not `OrderStatusType`)

**Naming anti-patterns to avoid:**
- `*Manager`, `*Handler`, `*Helper`, `*Utility` -- usually SRP violations
- `*Impl` suffix -- if there's only one implementation, drop the interface
- Abbreviations -- use `customer` not `cust`, `repository` not `repo`
- Single-letter variables outside arrow function parameters and generics

## TypeScript Strict Mode (Required)

**All projects MUST enable strict mode in `tsconfig.json`:**

```jsonc
{
  "compilerOptions": {
    "strict": true,                    // Enables all strict checks below:
    // "noImplicitAny": true,          // No implicit any
    // "strictNullChecks": true,       // Null/undefined must be explicit
    // "strictFunctionTypes": true,    // Strict function type checking
    // "strictPropertyInitialization": true, // Class properties must be initialized
    // "noImplicitThis": true,         // No implicit this
    // "alwaysStrict": true,           // Emit "use strict" in JS
    "noUncheckedIndexedAccess": true,  // Array/object index returns T | undefined
    "noImplicitReturns": true,         // All code paths must return
    "noFallthroughCasesInSwitch": true,// No fallthrough in switch
    "noUnusedLocals": true,            // No unused local variables
    "noUnusedParameters": true,        // No unused parameters
    "exactOptionalPropertyTypes": true // Distinguish undefined from missing
  }
}
```

**The `any` escape hatch:**
- Avoid `any` -- it disables type safety
- If absolutely necessary, add a comment explaining why
- Prefer `unknown` for truly unknown types and narrow with type guards
- Use `Record<string, unknown>` instead of `Record<string, any>` for generic objects

```typescript
// Bad: any disables type safety
function parse(data: any): Order {
  return data;
}

// Good: unknown forces type narrowing
function parse(data: unknown): Order {
  if (!isOrder(data)) {
    throw new ValidationError("Invalid order data");
  }
  return data;
}

// Good: Type guard for runtime validation
function isOrder(data: unknown): data is Order {
  return (
    typeof data === "object" &&
    data !== null &&
    "id" in data &&
    "total" in data
  );
}
```

## Type System Best Practices

### Prefer Interfaces for Object Shapes, Type Aliases for Unions/Utilities

```typescript
// Good: Interface for object shapes (extensible via declaration merging)
interface OrderRepository {
  save(order: Order): Promise<void>;
  findById(id: string): Promise<Order | null>;
}

// Good: Type alias for unions and computed types
type OrderStatus = "pending" | "confirmed" | "shipped" | "delivered";
type ReadonlyOrder = Readonly<Order>;
```

### Use Discriminated Unions for State Modeling

```typescript
// Good: Discriminated union -- compiler enforces exhaustive handling
type Result<T> =
  | { success: true; data: T }
  | { success: false; error: Error };

function handleResult(result: Result<Order>): void {
  if (result.success) {
    processOrder(result.data); // TypeScript knows data exists here
  } else {
    handleError(result.error); // TypeScript knows error exists here
  }
}

// Good: Domain state modeling
type OrderState =
  | { status: "draft"; items: LineItem[] }
  | { status: "submitted"; items: LineItem[]; submittedAt: Date }
  | { status: "shipped"; items: LineItem[]; submittedAt: Date; trackingId: string };
```

### Use Utility Types

```typescript
// Good: Use built-in utility types
type OrderUpdate = Partial<Order>;
type RequiredOrder = Required<Order>;
type OrderSummary = Pick<Order, "id" | "total" | "status">;
type PublicOrder = Omit<Order, "internalNotes">;
type StatusMap = Record<OrderStatus, string>;
```

### TypeScript 5.x Features

#### `satisfies` Operator (TS 4.9+)

Use `satisfies` to validate a value against a type while keeping the widest inferred type. Prefer over `as Type` assertions.

```typescript
// Bad: `as` assertion silences the compiler — no runtime safety
const config = { host: "localhost", port: 8080 } as Config;

// Good: satisfies validates the shape without narrowing
const config = {
  host: "localhost",
  port: 8080,
} satisfies Config;
// config.host is still typed as string literal "localhost", not string
```

**When to use `satisfies`:**
- Object literals that must conform to an interface but you want precise literal types
- Route maps, config objects, theme tokens — any "bag of values with a known shape"

#### `using` / `await using` — Explicit Resource Management (TS 5.2)

Automatically dispose of resources when they leave scope. Works with any object implementing `Symbol.dispose` or `Symbol.asyncDispose`.

```typescript
// Before: manual cleanup
const conn = openConnection();
try {
  conn.query("SELECT 1");
} finally {
  conn.close();
}

// With using: cleanup is guaranteed even on throw
using conn = openConnection(); // calls conn[Symbol.dispose]() on scope exit
conn.query("SELECT 1");

// Async version
await using conn = openAsyncConnection();
await conn.query("SELECT 1");
```

**Implementation pattern:**
```typescript
function openConnection(): Connection & Disposable {
  const conn = createConnection();
  return Object.assign(conn, {
    [Symbol.dispose]() { conn.close(); },
  });
}
```

#### `NoInfer<T>` Utility Type (TS 5.4)

Prevents TypeScript from using a type parameter as an inference site. Useful for constraining generic functions where one argument should not influence the inferred type.

```typescript
// Without NoInfer: TypeScript infers T from both arguments
function createState<T>(initial: T, fallback: T): T { return initial ?? fallback; }
// createState("hello", 42) infers T as string | number — not what you want

// With NoInfer: only `initial` drives inference; fallback must match
function createState<T>(initial: T, fallback: NoInfer<T>): T { return initial ?? fallback; }
// createState("hello", 42) — TypeScript error: 42 is not assignable to string
```

#### Const Type Parameters (TS 5.0)

Add `const` to type parameter declarations to infer the narrowest type, equivalent to `as const` on the call site.

```typescript
// Without const modifier: T inferred as string[] — caller must add 'as const' to get literal types
function toTuple<T extends readonly unknown[]>(arr: T) { return arr; }
toTuple(["a", "b"] as const); // type: readonly ["a", "b"] — requires caller annotation

// With const modifier: infers readonly ["a", "b"] automatically
function toTuple<const T extends readonly unknown[]>(arr: T) { return arr; }
toTuple(["a", "b"]); // type: readonly ["a", "b"] — no caller annotation needed
```

### Generics

```typescript
// Good: Generic with constraints
interface Repository<T extends { id: string }> {
  save(entity: T): Promise<void>;
  findById(id: string): Promise<T | null>;
  findAll(): Promise<T[]>;
}

// Good: Generic function with constraint
function groupBy<T, K extends string | number>(
  items: T[],
  keyFn: (item: T) => K,
): Record<K, T[]> {
  return items.reduce(
    (groups, item) => {
      const key = keyFn(item);
      (groups[key] ??= []).push(item);
      return groups;
    },
    {} as Record<K, T[]>,
  );
}
```

### Enums

**Prefer const objects or union types over TypeScript enums:**

```typescript
// Preferred: Union type (simple, no runtime overhead)
type OrderStatus = "pending" | "confirmed" | "shipped" | "delivered";

// Preferred: Const object (when you need runtime access to values)
const OrderStatus = {
  Pending: "pending",
  Confirmed: "confirmed",
  Shipped: "shipped",
  Delivered: "delivered",
} as const;
type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

// Acceptable: TypeScript enum (only if project convention requires it)
enum OrderStatus {
  Pending = "pending",
  Confirmed = "confirmed",
  Shipped = "shipped",
  Delivered = "delivered",
}
```

**Why avoid enums:**
- String unions are simpler and have zero runtime overhead
- Const objects provide runtime values without enum quirks (reverse mapping, numeric auto-increment)
- Enums create nominal types that don't work well with plain strings from JSON

## Imports

**Rules:**
- Use ES modules (`import`/`export`) exclusively -- never `require()` in TypeScript
- Use named exports by default; avoid default exports (they complicate refactoring and IDE support)
- Group imports in this order, separated by blank lines:
  1. Node.js built-in modules (prefix with `node:`)
  2. Third-party packages
  3. Local/project imports
- Use path aliases (configured in `tsconfig.json`) to avoid deep relative imports
- Let ESLint enforce import ordering automatically

```typescript
// Good: Organized imports with named exports
import { readFile } from "node:fs/promises";
import { join } from "node:path";

import { z } from "zod";
import pino from "pino";

import { OrderService } from "@/order/order.service";
import { OrderRepository } from "@/order/order.repository";
import type { Order } from "@/order/order.model";
```

**Use `import type` for type-only imports:**
```typescript
// Good: Explicit type-only import (erased at compile time)
import type { Order, OrderStatus } from "@/order/order.model";

// Good: Inline type import
import { OrderService, type OrderConfig } from "@/order/order.service";
```

## Immutability

**Prefer immutable data structures:**
- Use `readonly` for properties that should not change after construction
- Use `Readonly<T>`, `ReadonlyArray<T>`, `ReadonlyMap<K, V>`, `ReadonlySet<T>` for collections
- Use `as const` for literal types
- Prefer `const` over `let`; never use `var`
- Return new objects/arrays from functions instead of mutating inputs

```typescript
// Good: Immutable model
interface Order {
  readonly id: string;
  readonly items: readonly LineItem[];
  readonly total: number;
  readonly status: OrderStatus;
}

// Good: Immutable function -- returns new array
function addItem(items: readonly LineItem[], newItem: LineItem): readonly LineItem[] {
  return [...items, newItem];
}

// Good: as const for literal types
const CONFIG = {
  maxRetries: 3,
  timeout: 5000,
} as const;

// Bad: Mutable with direct mutation
function addItem(items: LineItem[], newItem: LineItem): void {
  items.push(newItem); // Mutates input
}
```

## Null Safety

**With `strictNullChecks` enabled (required), handle nullability explicitly:**

```typescript
// Good: Explicit null return type
function findOrder(id: string): Order | null {
  return orders.get(id) ?? null;
}

// Good: Early return with guard clause
function processOrder(id: string): OrderResult {
  const order = findOrder(id);
  if (order === null) {
    throw new OrderNotFoundError(id);
  }
  return process(order); // TypeScript knows order is not null here
}

// Good: Optional chaining and nullish coalescing
const city = order.address?.city ?? "Unknown";

// Good: Non-null assertion ONLY when you can prove it (rare, document why)
const element = document.getElementById("root")!; // Guaranteed to exist in index.html

// Bad: Implicit null handling
function findOrder(id: string): Order {
  return orders.get(id); // May return undefined but type says Order
}
```

## Function/Method Design

Apply the same standards from CODING_PRACTICES.md:
- **Maximum 20 lines per function** (excluding blank lines and braces)
- Single responsibility per function
- Maximum 5 parameters (use parameter objects)
- Prefer returning values over mutating state
- Use arrow functions for callbacks; use function declarations for top-level functions

```typescript
// Good: Small, focused functions
function calculateScore(findings: readonly Finding[]): number {
  const totalPenalty = findings.reduce((sum, f) => sum + penaltyFor(f), 0);
  return Math.max(0, 100 - totalPenalty);
}

// Good: Parameter object when > 3 params
interface ReportConfig {
  readonly scores: readonly Score[];
  readonly outputDir: string;
  readonly format?: ReportFormat;
  readonly includeDetails?: boolean;
}

function generateReport(config: ReportConfig): Report {
  // ...
}

// Bad: Too many parameters
function generateReport(
  scores: Score[],
  outputDir: string,
  format: ReportFormat,
  includeDetails: boolean,
  locale: string,
  title: string,
): Report {
  // ...
}
```

## Dependency Injection

**Use constructor injection:**

```typescript
// Good: Constructor injection (all dependencies visible, testable)
class OrderService {
  constructor(
    private readonly repository: OrderRepository,
    private readonly processor: PaymentProcessor,
  ) {}

  async process(order: Order): Promise<OrderResult> {
    await this.processor.charge(order.total);
    return this.repository.save(order);
  }
}

// Bad: Hidden dependencies (hard to test)
class OrderService {
  async process(order: Order): Promise<OrderResult> {
    const repository = new PostgresRepository(); // tight coupling
    const processor = new StripeProcessor();      // can't inject mocks
    // ...
  }
}
```

**For framework-based DI (NestJS, InversifyJS, etc.):**
- Follow the framework's conventions
- Keep the DI container configuration in a dedicated module
- Prefer constructor injection over property injection

## Interface Design

**Prefer focused interfaces (ISP):**

```typescript
// Good: Focused interfaces
interface ScoreCalculator {
  calculate(findings: readonly Finding[]): number;
}

interface ReportWriter {
  write(report: Report, outputDir: string): Promise<void>;
}

// Bad: Fat interface
interface CodeAnalyzer {
  parse(source: string): Finding[];
  calculateScore(findings: Finding[]): number;
  writeReport(report: Report, outputDir: string): Promise<void>;
  sendNotification(report: Report): Promise<void>;
  loadConfig(configPath: string): Config;
}
```

## Error Handling

**Best practices:**
- Define domain-specific error classes extending `Error`
- Use typed error responses (discriminated unions) where appropriate
- Never catch and silently swallow errors
- Use `try/catch` at boundaries (HTTP handlers, CLI entry points)
- Let errors propagate through the call stack -- don't catch mid-layer unless you add value

```typescript
// Good: Domain error with context
class OrderNotFoundError extends Error {
  constructor(public readonly orderId: string) {
    super(`Order not found: ${orderId}`);
    this.name = "OrderNotFoundError";
  }
}

// Good: Result type for expected failures
type Result<T, E = Error> =
  | { ok: true; value: T }
  | { ok: false; error: E };

function parseOrder(data: unknown): Result<Order, ValidationError> {
  const parsed = orderSchema.safeParse(data);
  if (!parsed.success) {
    return { ok: false, error: new ValidationError(parsed.error) };
  }
  return { ok: true, value: parsed.data };
}

// Good: Error handling at boundary
async function handleCreateOrder(req: Request, res: Response): Promise<void> {
  try {
    const order = await orderService.create(req.body);
    res.status(201).json(order);
  } catch (error) {
    if (error instanceof ValidationError) {
      res.status(400).json({ error: error.message });
      return;
    }
    logger.error({ error }, "Failed to create order");
    res.status(500).json({ error: "Internal server error" });
  }
}

// Bad: Swallowing errors
try {
  await process(data);
} catch {
  // silently ignored
}
```

## Async/Await

**Use `async`/`await` for all asynchronous code:**

```typescript
// Good: async/await for clarity
async function fetchAllScores(urls: readonly string[]): Promise<Score[]> {
  const results = await Promise.all(urls.map((url) => fetchScore(url)));
  return results;
}

// Good: Error handling with async/await
async function fetchScore(url: string): Promise<Score> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new FetchError(`Failed to fetch ${url}: ${response.status}`);
  }
  return response.json() as Promise<Score>;
}

// Good: Concurrent with controlled parallelism
async function processInBatches<T, R>(
  items: readonly T[],
  batchSize: number,
  processor: (item: T) => Promise<R>,
): Promise<R[]> {
  const results: R[] = [];
  for (let i = 0; i < items.length; i += batchSize) {
    const batch = items.slice(i, i + batchSize);
    const batchResults = await Promise.all(batch.map(processor));
    results.push(...batchResults);
  }
  return results;
}
```

**Guidelines:**
- Always use `async`/`await` -- never raw `.then()/.catch()` chains
- Use `Promise.all()` for concurrent independent operations
- Use `Promise.allSettled()` when you need all results regardless of failures
- Never use `Promise.race()` without a timeout
- Always handle promise rejections (unhandled rejections crash Node.js)
- Never use `void` promises without explicit fire-and-forget documentation

## Collections and Functional Patterns

**Prefer functional and immutable operations:**

```typescript
// Good: Functional transformations (returns new arrays)
const activeOrders = orders.filter((o) => o.status === "active");
const totals = orders.map((o) => o.total);
const grandTotal = totals.reduce((sum, t) => sum + t, 0);

// Good: Method chaining
const report = orders
  .filter((o) => o.status === "shipped")
  .map((o) => ({ id: o.id, total: o.total }))
  .sort((a, b) => b.total - a.total);

// Good: Use Map and Set for lookups
const orderMap = new Map<string, Order>(orders.map((o) => [o.id, o]));
const uniqueStatuses = new Set(orders.map((o) => o.status));

// Bad: Imperative with mutation
const activeOrders: Order[] = [];
for (const o of orders) {
  if (o.status === "active") {
    activeOrders.push(o);
  }
}
```

**Collection guidelines:**
- Use `.map()`, `.filter()`, `.reduce()` for transformations
- Break complex chains into named intermediate variables for readability
- Use `Map` and `Set` for O(1) lookups instead of arrays
- Prefer `for...of` over `for (let i = 0; ...)` when index is not needed
- Use `Object.entries()`, `Object.keys()`, `Object.values()` for object iteration

## Documentation (TSDoc / JSDoc)

**Requirements:**
- All public classes, methods, and functions must have TSDoc comments
- Use `/** ... */` block comments (TSDoc format)
- First line is the summary
- Include `@param`, `@returns`, `@throws` tags
- Document expected formats, constraints, and important edge cases

```typescript
/**
 * Calculate the quality score for a class based on code findings.
 *
 * The score starts at 100 and decreases based on the severity
 * and count of findings. The minimum score is 0.
 *
 * @param findings - List of code quality findings.
 * @param weights - Scoring weights for each severity category.
 * @returns Score between 0 and 100 inclusive.
 * @throws {ValidationError} If weights contain negative values.
 */
function calculateScore(findings: readonly Finding[], weights: Weights): number {
  // ...
}
```

## Testing

**Framework:** Jest or Vitest (Vitest preferred for new projects due to native ESM and TypeScript support).

**Mandatory rules:**
- Every change requires tests appropriate to the change
- Bug fixes **must** include a regression test that fails without the fix
- Tests must be deterministic and isolated -- no shared mutable state between tests
- Avoid real network calls or reliance on external systems; use mocks, fakes, or recorded responses
- If randomness is involved, seed it explicitly for reproducibility

**Structure:**
- Given-When-Then structure in every test
- Descriptive test names using `should...when...` pattern
- Use `describe` blocks to group related tests
- Use `beforeEach` / `afterEach` for setup/teardown
- Use `jest.fn()` / `vi.fn()` for mocking

```typescript
describe("OrderService", () => {
  let service: OrderService;
  let mockRepository: jest.Mocked<OrderRepository>;
  let mockProcessor: jest.Mocked<PaymentProcessor>;

  beforeEach(() => {
    mockRepository = {
      save: jest.fn(),
      findById: jest.fn(),
    };
    mockProcessor = {
      charge: jest.fn(),
    };
    service = new OrderService(mockRepository, mockProcessor);
  });

  describe("process", () => {
    it("should process order and save when payment succeeds", async () => {
      // Given
      const order = createTestOrder({ total: 100 });
      mockProcessor.charge.mockResolvedValue({ success: true });
      mockRepository.save.mockResolvedValue(undefined);

      // When
      const result = await service.process(order);

      // Then
      expect(mockProcessor.charge).toHaveBeenCalledWith(100);
      expect(mockRepository.save).toHaveBeenCalledWith(order);
      expect(result.status).toBe("completed");
    });

    it("should throw PaymentError when payment fails", async () => {
      // Given
      const order = createTestOrder({ total: 100 });
      mockProcessor.charge.mockRejectedValue(new PaymentError("Declined"));

      // When / Then
      await expect(service.process(order)).rejects.toThrow(PaymentError);
      expect(mockRepository.save).not.toHaveBeenCalled();
    });
  });
});
```

**Test helpers:**

```typescript
// Good: Factory functions for test data
function createTestOrder(overrides: Partial<Order> = {}): Order {
  return {
    id: "test-order-1",
    items: [],
    total: 0,
    status: "pending",
    ...overrides,
  };
}
```

### Vitest Assertions — Key Patterns

**`toBe` vs `toEqual` — the most common confusion:**
```typescript
// toBe — uses === (reference / primitive equality)
expect(42).toBe(42)               // ✅ primitives
expect(obj).toBe(obj)             // ✅ same reference

// toEqual — deep equality (use for objects and arrays)
expect({ name: 'Alice' }).toEqual({ name: 'Alice' })   // ✅
expect([1, 2, 3]).toEqual([1, 2, 3])                   // ✅
```

**Partial matching with `toMatchObject`:**
```typescript
expect(response).toMatchObject({
    status: 'success',
    user: { name: 'Alice' }
    // other fields are ignored
})
```

**Exception assertions:**
```typescript
expect(() => service.process(null)).toThrowError('input must not be null')
expect(() => service.process(null)).toThrowError(/must not be null/)
```

**Async — `resolves` and `rejects`:**
```typescript
await expect(fetchUser(42)).resolves.toEqual({ id: 42, name: 'Alice' })
await expect(fetchUser(-1)).rejects.toThrowError('User not found')
```

**Soft assertions with `expect.soft`** — continues after failure, all failures reported at end:
```typescript
test('validates response shape', () => {
    expect.soft(response.status).toBe(200)
    expect.soft(response.body.name).toBe('Alice')
    expect.soft(response.headers['Content-Type']).toContain('application/json')
})
```

**Asymmetric matchers** — for dynamic or partial data:
```typescript
expect(response).toEqual({
    id: expect.any(Number),
    name: expect.stringContaining('Alice'),
    tags: expect.arrayContaining(['admin']),
    meta: expect.objectContaining({ active: true })
})
```

**Spy / mock assertions:**
```typescript
expect(mockFn).toHaveBeenCalledTimes(1)
expect(mockFn).toHaveBeenCalledWith('expected-arg')
expect(mockFn).not.toHaveBeenCalled()
```

## Build Tools and Project Configuration

**Use `tsconfig.json` with strict settings (see TypeScript Strict Mode section above).**

**Recommended `tsconfig.json` (Node.js):**
```jsonc
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "NodeNext",
    "moduleResolution": "NodeNext",
    "strict": true,
    "noUncheckedIndexedAccess": true,
    "noImplicitReturns": true,
    "noFallthroughCasesInSwitch": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "exactOptionalPropertyTypes": true,
    "declaration": true,
    "declarationMap": true,
    "sourceMap": true,
    "outDir": "./dist",
    "rootDir": "./src",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["src/**/*.ts"],
  "exclude": ["node_modules", "dist"]
}
```

**Recommended `package.json` scripts:**
```json
{
  "scripts": {
    "build": "tsc",
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage",
    "lint": "eslint src/ tests/",
    "lint:fix": "eslint src/ tests/ --fix",
    "format": "prettier --write .",
    "format:check": "prettier --check .",
    "typecheck": "tsc --noEmit"
  }
}
```

## Code Quality Tools

**Recommended toolchain:**
- **ESLint** with `@typescript-eslint/eslint-plugin`: Primary linting and static analysis
- **Prettier**: Code formatting (handles style so ESLint focuses on logic)
- **TypeScript compiler** (`tsc --noEmit`): Type checking
- **Vitest** or **Jest**: Testing framework
- **vitest --coverage** or **jest --coverage** (with `v8` or `istanbul`): Coverage reporting
- **pre-commit hooks** (husky + lint-staged): Format check + lint + type check on every commit
- **CI pipeline**: Run format check + lint + type check + tests on each PR

**Recommended ESLint configuration (flat config, `eslint.config.mjs`):**

> **ESLint Flat Config**: ESLint 9.x uses `eslint.config.mjs` by default. Use `eslint.config.ts` only if your project has a bundler or ts-node/tsx runner configured — it requires `--flag unstable_ts_config` (ESLint 9.9+, still experimental). Prefer `.mjs` for new projects.

```javascript
import eslint from "@eslint/js";
import tseslint from "typescript-eslint";

export default tseslint.config(
  eslint.configs.recommended,
  ...tseslint.configs.strictTypeChecked,
  {
    languageOptions: {
      parserOptions: {
        projectService: true,
      },
    },
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
      "@typescript-eslint/explicit-function-return-type": ["error", {
        allowExpressions: true,
      }],
      "@typescript-eslint/no-floating-promises": "error",
      "@typescript-eslint/no-misused-promises": "error",
      "@typescript-eslint/prefer-readonly": "error",
      "@typescript-eslint/strict-boolean-expressions": "error",
    },
  },
);
```

**Same rules apply:**
- 20-line function maximum
- 0-2 private methods per class (SRP guideline)
- 80%+ unit test coverage (unit tests only -- integration/E2E tests do not count toward coverage)
- No duplicated code

## Common Anti-Patterns to Avoid

### God Classes

```typescript
// Bad: Class doing too many things
class ApplicationManager {
  processOrder(): void { /* ... */ }
  sendEmail(): void { /* ... */ }
  generateReport(): void { /* ... */ }
  syncInventory(): void { /* ... */ }
}

// Good: Split by responsibility
class OrderProcessor { /* ... */ }
class EmailNotifier { /* ... */ }
class ReportGenerator { /* ... */ }
class InventorySynchronizer { /* ... */ }
```

### Barrel Export Bloat

```typescript
// Bad: Re-exporting everything (slows builds, creates circular deps)
// src/index.ts
export * from "./order";
export * from "./payment";
export * from "./inventory";
export * from "./notification";

// Good: Explicit, focused exports
// src/order/index.ts
export { OrderService } from "./order.service";
export type { Order, OrderStatus } from "./order.model";
```

### Type Assertions Instead of Type Guards

```typescript
// Bad: Unsafe type assertion
const order = data as Order; // No runtime validation

// Good: Type guard with runtime check
function isOrder(data: unknown): data is Order {
  return (
    typeof data === "object" &&
    data !== null &&
    "id" in data &&
    typeof (data as Record<string, unknown>).id === "string"
  );
}

if (isOrder(data)) {
  processOrder(data); // Safe -- runtime-validated
}
```

### Callback Hell / Promise Chains

```typescript
// Bad: Nested callbacks
getData((err, data) => {
  if (err) return handleError(err);
  processData(data, (err, result) => {
    if (err) return handleError(err);
    saveResult(result, (err) => {
      // ...
    });
  });
});

// Bad: Long promise chains
getData()
  .then((data) => processData(data))
  .then((result) => saveResult(result))
  .catch((err) => handleError(err));

// Good: async/await
async function run(): Promise<void> {
  const data = await getData();
  const result = await processData(data);
  await saveResult(result);
}
```

### Mutable Default Arguments (JavaScript)

```typescript
// Bad: Mutable default (shared across calls in JavaScript)
function addTag(tag: string, tags: string[] = []): string[] {
  tags.push(tag);
  return tags;
}

// Good: Create new array each call
function addTag(tag: string, tags: readonly string[] = []): readonly string[] {
  return [...tags, tag];
}
```

## SOLID Principles Notes

Use the guide in `docs/SOLID_PRINCIPLES.md` and apply these TypeScript-specific practices:
- **SRP**: Use interfaces and classes for focused responsibilities; extract responsibilities into separate classes with constructor injection.
- **OCP**: Use interfaces with concrete implementations; use Strategy pattern (interface + implementations) for open extension.
- **LSP**: Interfaces enforce contracts; avoid throwing `NotImplementedError` in implementations -- redesign the abstraction instead.
- **ISP**: TypeScript supports interface composition natively; keep interfaces small (1-3 methods) and compose them with `extends`.
- **DIP**: Use constructor injection with interface types; enable test injection by depending on abstractions, not concrete classes.

## Design Patterns Notes

Use the catalog in `docs/DESIGN_PATTERNS.md` and apply these TypeScript-specific practices:
- **Strategy**: Use interfaces for strategy contracts; pass implementations via constructor injection.
- **Factory Method/Abstract Factory**: Use factory functions or static factory methods; avoid complex factory class hierarchies.
- **Builder**: Use parameter objects with optional properties or the builder pattern for complex construction; leverage TypeScript's type system for compile-time validation.
- **Singleton**: Use module-level instances (ES modules are singletons by default); avoid global mutable state.
- **Decorator/Proxy**: Use composition and TypeScript decorators (experimental) or higher-order functions; keep wrappers small and focused.
- **Observer**: Use typed event emitters, RxJS observables, or callback registries; avoid tight coupling.

## TypeScript / JavaScript Definition of Done Checklist

Every TypeScript/JavaScript change must satisfy these criteria before it is considered complete. This checklist supplements the language-agnostic checklists in `PRE_COMMIT_CHECKLIST.md` and `AI_AGENT_WORKFLOW.md`.

- [ ] TypeScript strict mode enabled (`strict: true` in `tsconfig.json`)
- [ ] No `any` types without documented justification
- [ ] No `@ts-ignore` (use `@ts-expect-error` with explanation if absolutely needed)
- [ ] No use of `eval()`, `new Function()`, `innerHTML` with unsanitized input
- [ ] External inputs validated at system boundaries (Zod, io-ts, or custom type guards)
- [ ] No secrets or credentials in code or logs
- [ ] Uses structured logging (not `console.log`) for production output
- [ ] No new third-party dependencies without explicit approval
- [ ] Tests added or updated; bug fixes include a regression test
- [ ] Tests are deterministic and isolated (no real network calls)
- [ ] All tests pass, build succeeds, no lint or type-check errors
- [ ] All promises are properly awaited or explicitly documented as fire-and-forget
- [ ] Assumptions and behavior changes are clearly documented
- [ ] Change is small and reviewable (or refactor was explicitly requested)

---

**Last Updated**: 2026-04-30
**Version**: 1.0
**TypeScript Version**: 5.4+ (strict mode)
