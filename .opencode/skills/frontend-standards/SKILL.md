---
name: frontend-standards
description: >
  Use when building, reviewing, or testing React components — React 19 + Vitest 2 frontend
  standards: no explicit TypeScript type annotations (use inference, unknown only at
  boundaries), small components (under 100 lines), minimal props (max 5), finite state
  machine patterns for async state (status union: 'idle'|'loading'|'success'|'error'),
  React 19 hooks (use(), useOptimistic(), useActionState, useFormStatus), integration-style
  component tests that survive refactoring (no child mocking, only mock system boundaries),
  and Vitest 2 + React Testing Library conventions.
triggers:
  - "writing or modifying react components"
  - "reviewing frontend code for react standards"
  - "setting up vitest and react testing library"
  - "auditing component size props or async state"
  - "writing component tests and choosing mocks"
  - "using react nineteen hooks or form actions"
  - "implementing optimistic ui updates with useoptimistic"
not_for:
  - "backend or domain layer standards work"
  - "ui guidance for non react frontend stacks"
  - "browser automation or end to end verification"
---

# Frontend Standards (React 19 + Vitest 2)

## Table of Contents

- [Use when](#use-when)
- [Not for](#not-for)
- [Core Rules](#core-rules)
- [React 19 Hooks](#react-19-hooks)
- [Testing](#testing)
- [Definition of Done](#definition-of-done)

## Use when

- Writing or modifying React components
- Reviewing a frontend PR for standards compliance
- Setting up Vitest + React Testing Library for a new project
- Auditing component size, prop count, or async state patterns
- Writing component tests (choosing what to mock)

## Not for

- Backend, service, or domain-layer standards outside React component work
- UI guidance for non-React stacks such as Angular, Vue, or server-rendered templates
- Browser automation or end-to-end site verification — use Playwright for that

---

## Core Rules

1. **No explicit type annotations** — let TypeScript infer. Use `unknown` at system boundaries (API responses, `JSON.parse`). Never use `any`.
2. **Small components** — under 100 lines per file. One component per file. Multiple `useEffect` or unrelated state → split.
3. **Minimal props** — max 5 per component. Group related props into objects. Avoid prop drilling beyond 2 levels (use Context, Zustand, or Redux).
4. **Finite state machine for async** — use a `status` union (`'idle'|'loading'|'success'|'error'`) instead of paired boolean flags. Paired booleans create impossible states.

## React 19 Hooks

- `use(ctx)` — read context or unwrap a Promise in render (can be conditional, unlike `useContext`)
- `useOptimistic(items, reducer)` — add optimistic entry before server call for instant feedback
- `useActionState(fn, null)` — form actions with state; `isPending` is true while async fn runs
- `useFormStatus()` — read `pending` inside a submit button (must be a child of `<form>`)

## Testing

- **Tests verify behavior, not implementation** — rename internal state without touching tests → tests pass.
- **Integration-style** — let child components render through. Do NOT mock React components or custom hooks.
- **Mock only system boundaries**: `fetch`/HTTP clients, `localStorage`, browser APIs unavailable in JSDOM, third-party SDKs.
- **Query hierarchy** (accessibility-first): `getByRole` > `getByLabelText` > `getByPlaceholderText` > `getByText` > `getByTestId`
- **`userEvent` over `fireEvent`** — simulates real browser event chain (focus, blur, keyboard).

## Definition of Done

- [ ] No explicit type annotations where TypeScript can infer; no `any`
- [ ] Component files under 100 lines; max 5 props (use objects / context / store for more)
- [ ] Async state uses `status` union — no paired boolean flags
- [ ] Tests verify visible behavior; only system boundaries mocked
- [ ] `userEvent` for interactions; `getByRole`/`getByLabelText` queries
- [ ] All tests pass (`vitest run`)

For complete reference material, see [REFERENCE.md](REFERENCE.md) in this skill directory.
