# Frontend Coding Standards (React 19 + Vitest 2)

## Overview

This document defines frontend-specific coding standards for React projects. It supplements `TYPESCRIPT_STANDARDS.md` and `CODING_PRACTICES.md` with rules that apply specifically to React component development.

**Tech Stack: React 19 with Vitest 2**

---

## Table of Contents

1. [TypeScript Usage](#1-typescript-usage)
2. [Component Design](#2-component-design)
3. [Props Design](#3-props-design)
4. [State Management with Finite State Machines](#4-state-management-with-finite-state-machines)
5. [React 19 Features](#5-react-19-features)
6. [Testing Strategy](#6-testing-strategy)
7. [Project Structure](#7-project-structure)
8. [Definition of Done](#8-definition-of-done)

---

## 1. TypeScript Usage

### Do Not Use Types

**Rule: Do not ever use explicit type annotations in TypeScript when writing frontend code. If a type annotation is absolutely necessary, use `unknown`.**

This rule promotes writing clean, minimal TypeScript where inference does the work. It avoids verbose type signatures that create coupling and resist refactoring.

```typescript
// Bad: explicit type annotations
const userName: string = "Alice";
const count: number = 0;
const onClick: (event: React.MouseEvent<HTMLButtonElement>) => void = (e) => { /* ... */ };

// Good: let TypeScript infer
const userName = "Alice";
const count = 0;
const onClick = (e) => { /* ... */ };
```

```typescript
// Bad: typed function parameters
function greet(name: string): string {
  return `Hello, ${name}`;
}

// Good: inferred
const greet = (name) => `Hello, ${name}`;
```

```typescript
// Bad: typed state
const [loading, setLoading] = useState<boolean>(false);

// Good: inferred
const [loading, setLoading] = useState(false);
```

**When `unknown` is necessary:**

```typescript
// Acceptable: boundary data from external API
const parseApiResponse = (data: unknown) => {
  // narrow with runtime checks before use
  if (typeof data === "object" && data !== null && "id" in data) {
    return data;
  }
  throw new Error("Unexpected API shape");
};
```

**Never use:**
- `any` — disables type safety entirely
- Explicit type annotations unless TypeScript cannot infer the type
- Type-casting (`as SomeType`) without a narrowing guard

---

## 2. Component Design

### Keep Components Small

Components should do ONE thing. If a component is getting long, it is doing too much — split it.

**Guidelines:**
- A component render function should fit in ~20 lines or fewer
- A component file should stay under 100 lines (excluding tests)
- If a component needs more than one `useEffect` or more than one piece of local state, consider splitting

```tsx
// Bad: one large component doing too much
function ProductPage({ productId }) {
  const [product, setProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [cart, setCart] = useState([]);
  const [relatedProducts, setRelatedProducts] = useState([]);
  // ... dozens of lines of logic and JSX
}

// Good: small, focused components composed together
function ProductPage({ productId }) {
  return (
    <div>
      <ProductDetails productId={productId} />
      <ReviewList productId={productId} />
      <RelatedProducts productId={productId} />
      <AddToCartButton productId={productId} />
    </div>
  );
}
```

### Component File Naming

- Component files: PascalCase (`UserProfile.tsx`, `OrderSummary.tsx`)
- Test files: co-located or in `__tests__/`, with `.test.tsx` suffix (`UserProfile.test.tsx`)
- One component per file (default export is the component)

---

## 3. Props Design

### Keep Props Count Small

**Maximum 5 props per component.** If a component needs more props, group related props into an object or move state out of props entirely.

**Strategies for reducing props:**

#### 1. Group related props into an object

```tsx
// Bad: 4 separate props for one concept
function UserCard({ name, age, height, weight }) {
  return <div>{name} — {age} years old</div>;
}

// Good: one cohesive prop object
function UserCard({ person }) {
  return <div>{person.name} — {person.age} years old</div>;
}
```

#### 2. Use state management (Zustand, Redux, Context)

When multiple sibling or distant components need the same data, lift it into a store or context rather than passing it as props through many layers.

```tsx
// Bad: prop drilling through intermediate components
function App() {
  const [user, setUser] = useState(null);
  return <Layout user={user}><Dashboard user={user} /></Layout>;
}

// Good: use context or a store
function App() {
  return (
    <UserProvider>
      <Layout><Dashboard /></Layout>
    </UserProvider>
  );
}

function Dashboard() {
  const user = useUser(); // reads from context or store
  return <div>Welcome, {user.name}</div>;
}
```

#### 3. Use React Context for cross-cutting concerns

```tsx
// Create a focused context for a single concern
const ThemeContext = createContext("light");

function ThemeProvider({ children }) {
  const [theme, setTheme] = useState("light");
  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

const useTheme = () => useContext(ThemeContext);
```

### Props Anti-Patterns to Avoid

- Boolean flags that control fundamentally different rendering (`isAdmin`, `isGuest`, `isOwner` on one component — split into separate components instead)
- Passing setter functions deep into the tree (use context or a store)
- Passing raw data that could be derived (compute derived values in the component or a hook)

---

## 4. State Management with Finite State Machines

### Use FSM Patterns for Async State

When a component has state that can transition between phases (loading, success, error, etc.), model it as a **finite state machine** rather than multiple boolean flags.

**Rule: Never use multiple `isLoading`/`isError` boolean flags together. Use a single state union type instead.**

```tsx
// Bad: boolean flags create impossible states (isLoading=true AND isError=true)
const [isLoading, setIsLoading] = useState(false);
const [isError, setIsError] = useState(false);
const [data, setData] = useState(null);

// Good: a single state variable with a union of valid states
const [status, setStatus] = useState("idle");
// status is one of: 'idle' | 'loading' | 'success' | 'error'
```

### Standard Status Union Pattern

Define status unions to model component lifecycle:

```tsx
// Define the possible states
// type LoadingState = 'idle' | 'loading' | 'success' | 'error'

function DataWidget({ id }) {
  const [status, setStatus] = useState("idle");
  const [data, setData] = useState(null);
  const [errorMessage, setErrorMessage] = useState("");

  const load = async () => {
    setStatus("loading");
    try {
      const result = await fetchData(id);
      setData(result);
      setStatus("success");
    } catch (err) {
      setErrorMessage(err.message);
      setStatus("error");
    }
  };

  if (status === "idle") return <button onClick={load}>Load</button>;
  if (status === "loading") return <Spinner />;
  if (status === "error") return <ErrorMessage message={errorMessage} />;
  return <DataDisplay data={data} />;
}
```

### FSM State Names — Standard Vocabulary

Use these standard state names consistently across the codebase:

| State | Meaning |
|-------|---------|
| `'idle'` | Initial state, no action taken |
| `'loading'` | Async operation in progress |
| `'success'` | Operation completed successfully |
| `'error'` | Operation failed |
| `'submitting'` | Form or mutation in flight |
| `'redirecting'` | Navigation in progress |

### When to Use xstate

The simple union pattern above is sufficient for most cases. Use a full [xstate](https://xstate.js.org/) state machine when:

- There are more than 4-5 distinct states
- State transitions have guards or side effects that depend on the current state
- The component has complex parallel states

---

## 5. React 19 Features

### `use()` Hook

The `use()` hook reads the value of a resource (a Promise or Context) during render. Unlike `useContext`, `use()` can be called conditionally.

```tsx
import { use } from "react";

// Reading context with use() — can be called conditionally
function ThemeIcon({ showIcon }) {
  if (!showIcon) return null;
  const theme = use(ThemeContext);   // ✅ conditional — allowed with use()
  return <Icon color={theme.primary} />;
}

// Unwrapping a Promise with use() + Suspense
function UserProfile({ userPromise }) {
  const user = use(userPromise);     // suspends until resolved
  return <div>{user.name}</div>;
}

// Wrap the consuming component in Suspense
function Page({ userPromise }) {
  return (
    <Suspense fallback={<Spinner />}>
      <UserProfile userPromise={userPromise} />
    </Suspense>
  );
}
```

**Rules:**
- `use()` must be called inside a component or hook (like all hooks)
- Unlike other hooks, `use()` CAN be called inside loops and conditionals
- For Promises: the component suspends until the Promise resolves; wrap with `<Suspense>`

---

### `useOptimistic()` — Optimistic UI

Apply optimistic updates while an async operation is in flight. React rolls back automatically if the operation fails.

```tsx
import { useOptimistic } from "react";

function TodoList({ todos, onAddTodo }) {
  const [optimisticTodos, addOptimisticTodo] = useOptimistic(
    todos,
    (currentTodos, newTodo) => [...currentTodos, { ...newTodo, pending: true }]
  );

  async function handleSubmit(formData) {
    const title = formData.get("title") as string;
    addOptimisticTodo({ id: crypto.randomUUID(), title }); // instant UI
    await onAddTodo(title);  // actual server call
  }

  return (
    <>
      <ul>
        {optimisticTodos.map(todo => (
          <li key={todo.id} style={{ opacity: todo.pending ? 0.5 : 1 }}>
            {todo.title}
          </li>
        ))}
      </ul>
      <form action={handleSubmit}>
        <input name="title" />
        <button type="submit">Add</button>
      </form>
    </>
  );
}
```

---

### React 19 Actions — Async Form Handlers

Pass an async function directly to `<form action={...}>`. React manages pending state, errors, and optimistic updates.

```tsx
import { useActionState } from "react";

// useActionState: manage form action state (replaces useFormState from react-dom)
function ContactForm() {
  const [state, submitAction, isPending] = useActionState(
    async (prevState, formData) => {
      const result = await submitContact(formData.get("email") as string);
      return result.ok ? { success: true } : { error: result.error };
    },
    null // initial state
  );

  return (
    <form action={submitAction}>
      <input name="email" type="email" required />
      <button disabled={isPending}>
        {isPending ? "Submitting..." : "Submit"}
      </button>
      {state?.error && <p role="alert">{state.error}</p>}
      {state?.success && <p>Sent!</p>}
    </form>
  );
}
```

---

### `useFormStatus()` — Submit Button State

Read the pending state of the enclosing `<form>` action from any child component — no prop drilling needed.

```tsx
import { useFormStatus } from "react-dom";

// SubmitButton reads the form's pending state directly
function SubmitButton() {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {pending ? "Saving..." : "Save"}
    </button>
  );
}

// Use inside any form — no props needed
function ProfileForm() {
  return (
    <form action={updateProfile}>
      <input name="name" />
      <SubmitButton />  {/* reads form's pending state automatically */}
    </form>
  );
}
```

**Note:** `useFormStatus` must be used in a component that is a *child* of the `<form>` element, not in the same component that renders the form.

---

### Migration Notes: React 18 → React 19

| React 18 | React 19 |
|----------|----------|
| `useContext(Ctx)` always | `use(Ctx)` when conditional context read needed |
| Manual optimistic state | `useOptimistic()` |
| `useFormState` (react-dom) | `useActionState` (react, preferred) |
| `startTransition` wrapper | `<form action={asyncFn}>` — built-in transitions |

React 18 patterns remain valid. Use React 19 patterns for new code; migrate incrementally.

---

## 6. Testing Strategy

### Philosophy: Tests That Survive Refactoring

**Tests should verify behavior, not implementation.**

Write tests that remain valid even when you refactor the internals of a component. If renaming a prop, extracting a sub-component, or changing how state is stored causes tests to fail, the tests are testing the wrong thing.

**Good tests:**
- Render a component tree and interact with it as a user would
- Assert on visible output (text, accessibility attributes, DOM state)
- Do NOT assert on internal state, prop names, or implementation details

**Bad tests:**
- Assert on `component.state.isLoading`
- Mock every child component
- Test that `setState` was called with a specific value

### Let Components Call Through

It is acceptable — and often preferable — to let components render and call their child components naturally during tests. This is integration-style component testing.

```tsx
// Good: renders the full component tree
it("shows user name after loading", async () => {
  render(<UserProfile userId="123" />);

  // Initially shows loading state
  expect(screen.getByRole("status")).toBeInTheDocument();

  // After data loads, shows user name
  await screen.findByText("Alice");
  expect(screen.queryByRole("status")).not.toBeInTheDocument();
});
```

```tsx
// Acceptable to let child components render through
it("shows order total in checkout", async () => {
  render(<CheckoutPage cartId="cart-1" />);
  // CartSummary and PricingBreakdown both render as real components
  expect(await screen.findByText("Total: $42.00")).toBeInTheDocument();
});
```

### Only Mock at System Boundaries

Mock only what crosses a system boundary — network calls, browser APIs, external services:

```tsx
// Mock the API layer, not the components
vi.mock("../api/users", () => ({
  fetchUser: vi.fn().mockResolvedValue({ id: "123", name: "Alice" }),
}));

it("displays user profile", async () => {
  render(<UserProfile userId="123" />);
  expect(await screen.findByText("Alice")).toBeInTheDocument();
});
```

**Do NOT mock:**
- Child components (let them render)
- Custom hooks (test through the component)
- Internal state or reducers

**DO mock:**
- `fetch` / API clients
- `localStorage` / `sessionStorage`
- Browser APIs (`window.location`, `navigator`)
- Third-party SDKs

### Test File Structure (Given-When-Then)

```tsx
describe("UserProfile", () => {
  describe("when loading", () => {
    it("shows a loading spinner", () => {
      // Given
      vi.mocked(fetchUser).mockReturnValue(new Promise(() => {})); // never resolves
      // When
      render(<UserProfile userId="123" />);
      // Then
      expect(screen.getByRole("status", { name: /loading/i })).toBeInTheDocument();
    });
  });

  describe("when data loads successfully", () => {
    it("displays the user name", async () => {
      // Given
      vi.mocked(fetchUser).mockResolvedValue({ id: "123", name: "Alice" });
      // When
      render(<UserProfile userId="123" />);
      // Then
      expect(await screen.findByText("Alice")).toBeInTheDocument();
    });
  });

  describe("when the request fails", () => {
    it("shows an error message", async () => {
      // Given
      vi.mocked(fetchUser).mockRejectedValue(new Error("Not found"));
      // When
      render(<UserProfile userId="123" />);
      // Then
      expect(await screen.findByText(/not found/i)).toBeInTheDocument();
    });
  });
});
```

### Vitest + React Testing Library Setup

> Vitest 2.x is the current stable release. The configuration below targets Vitest 2.x. Vitest 1.x configs are largely compatible but should be updated.

<!-- Vitest 2.x + React Testing Library -->

Standard test utilities to use:

```tsx
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { vi, describe, it, expect, beforeEach } from "vitest";
```

Prefer `userEvent` over `fireEvent` for realistic interaction simulation.

### Accessibility in Tests

Prefer queries that reflect accessibility — they test the right things and encourage accessible markup:

```tsx
// Good: accessibility-aligned queries
screen.getByRole("button", { name: /submit/i });
screen.getByLabelText("Email address");
screen.getByText("Order confirmed");

// Avoid: implementation-detail queries
screen.getByTestId("submit-btn");         // fragile, no a11y signal
container.querySelector(".submit-btn");   // CSS coupling
```

---

## 7. Project Structure

```
src/
├── components/           # Shared, reusable UI components
│   ├── Button/
│   │   ├── Button.tsx
│   │   ├── Button.test.tsx
│   │   └── index.ts
│   └── ...
├── features/             # Feature-scoped components and logic
│   ├── checkout/
│   │   ├── CheckoutPage.tsx
│   │   ├── CheckoutPage.test.tsx
│   │   ├── CartSummary.tsx
│   │   └── useCheckout.ts
│   └── ...
├── hooks/                # Shared custom hooks
├── store/                # Global state (Zustand/Redux slices)
├── api/                  # API client functions (the system boundary)
└── App.tsx
```

**Principles:**
- Co-locate tests with the component they test
- Keep the `api/` layer thin — just fetch calls and response mapping
- Custom hooks extract stateful logic from components; co-locate with the feature unless shared

---

## 8. Definition of Done

Before committing frontend code, verify:

- [ ] No explicit type annotations (TypeScript inference is used throughout)
- [ ] No `any` type — use `unknown` with narrowing if unavoidable
- [ ] Component file is under 100 lines
- [ ] Component has 5 or fewer props — related props are grouped into objects
- [ ] Async state uses a `status` union (`'idle' | 'loading' | 'success' | 'error'`) — no boolean flag pairs
- [ ] Tests verify behavior from the user's perspective, not implementation details
- [ ] Tests do not mock child components — only system boundaries (API, browser APIs)
- [ ] Tests use Given-When-Then structure with descriptive names
- [ ] Accessibility-aligned queries used in tests (`getByRole`, `getByLabelText`, `getByText`)
- [ ] All Vitest tests pass
- [ ] React 19 hooks used where appropriate (use(), useOptimistic(), useActionState for forms)

## Related Documents

- `docs/TYPESCRIPT_STANDARDS.md` — TypeScript-specific conventions
- `docs/TESTING_STANDARDS.md` — Cross-language testing standards (test pyramid, doubles, mutation)
- `docs/CODING_PRACTICES.md` — Language-agnostic practices, SOLID, TDD
- `docs/SOLID_PRINCIPLES.md` — SOLID principles with TypeScript examples

---

*Last updated: 2026-04-30*
