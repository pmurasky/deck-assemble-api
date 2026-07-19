# TypeScript Standards — Extended Reference

This companion file contains extended code examples referenced in [SKILL.md](./SKILL.md).

## Table of Contents

- [Module Organization](#module-organization)
- [Domain-Driven Project Structure](#domain-driven-project-structure)
- [Testing (Jest / Vitest) — Full Example](#testing-jest--vitest--full-example)
- [Tooling Setup](#tooling-setup)
- [SOLID Strategy Pattern](#solid-strategy-pattern)

---

## Module Organization

```typescript
// ✅ Named imports — explicit, tree-shakeable
import { createOrder, cancelOrder } from './order-service';

// ✅ import type for type-only imports (erased at compile time)
import type { Order, OrderStatus } from './order-domain';

// ✅ Path aliases (configure in tsconfig)
import { OrderService } from '@/order/order-service';

// ❌ Avoid wildcard imports
import * as OrderModule from './order-service';
```

Barrel `index.ts` files only for public API of a feature module — not for internal use.

---

## Domain-Driven Project Structure

```
src/
  order/
    order-domain.ts       # Types, value objects, pure logic
    order-repository.ts   # Interface
    order-service.ts      # Use cases
    order-service.test.ts # Co-located tests
    index.ts              # Public API barrel
  payment/
    ...
  config/
    app-config.ts
  common/
    result.ts
    errors.ts
index.ts                  # Entry point
```

---

## Testing (Jest / Vitest) — Full Example

```typescript
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('OrderService', () => {
  let repository: FakeOrderRepository;
  let service: OrderService;

  beforeEach(() => {
    repository = new FakeOrderRepository();
    service = new OrderService(repository, new FakeEmailSender());
  });

  it('should throw OrderNotFoundError when order does not exist', async () => {
    // Given
    const unknownId = 'does-not-exist';

    // When / Then
    await expect(service.getOrder(unknownId))
      .rejects.toThrow(OrderNotFoundError);
  });

  it('should return active orders only', async () => {
    // Given
    repository.add(orderFactory({ status: 'active' }));
    repository.add(orderFactory({ status: 'cancelled' }));

    // When
    const result = await service.getActiveOrders();

    // Then
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe('active');
  });
});
```

- Test names: `should <behavior> when <condition>`
- Use factory functions for test data (`orderFactory(overrides)`)
- Mock only system boundaries (HTTP clients, `fetch`, DB connections)
- 80% unit test coverage minimum; 100% for critical paths

---

## Tooling Setup

```json
// package.json scripts
{
  "scripts": {
    "typecheck": "tsc --noEmit",
    "lint": "eslint src --ext .ts,.tsx",
    "format": "prettier --write src",
    "test": "vitest run",
    "test:coverage": "vitest run --coverage"
  }
}
```

**Required tools:**
- ESLint + `@typescript-eslint/recommended` + `@typescript-eslint/strict`
- Prettier for formatting (no style debates)
- Husky + lint-staged for pre-commit hooks
- `tsc --noEmit` in CI — must pass with zero errors

---

## SOLID Strategy Pattern

```typescript
interface PricingStrategy {
  calculate(order: Order): number;
}

class OrderService {
  constructor(private readonly pricing: PricingStrategy) {}
}
```
