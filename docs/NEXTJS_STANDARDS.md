# Next.js Standards and Best Practices

## Overview
This document defines framework-specific standards for **Next.js 15+** projects using the App Router. It complements `CODING_PRACTICES.md` and `TYPESCRIPT_STANDARDS.md`.

## Baseline Stack

- Next.js App Router (`app/`)
- TypeScript strict mode
- Server Components by default
- ESLint + type-check required in CI

## App Architecture

### Route organization

Organize routes by domain and feature:

```
app/
├── (marketing)/
├── (dashboard)/
│   ├── orders/
│   │   ├── page.tsx
│   │   ├── loading.tsx
│   │   ├── error.tsx
│   │   └── [id]/page.tsx
│   └── layout.tsx
├── api/
│   └── orders/route.ts
└── layout.tsx
```

Rules:
- Keep route-level UI/state close to the route.
- Keep shared domain logic in `src/` (not inside route files).
- Avoid cross-segment imports that create tight coupling.

### Component boundaries

- Default to **Server Components**.
- Use `"use client"` only when needed (stateful UI, browser APIs, event handlers).
- Do not fetch sensitive server-only data in Client Components.

## Data Fetching and Caching

- Prefer server-side data fetching in Server Components or Route Handlers.
- Use Next.js caching intentionally:
  - Static/cached data: default `fetch` cache or `revalidate`.
  - Dynamic/user data: `cache: "no-store"` or segment-level dynamic config.
- Tag cache entries and use `revalidateTag`/`revalidatePath` after writes.

```ts
await fetch(url, {
  next: { tags: ["orders"], revalidate: 300 }
});
```

Avoid:
- Accidental over-caching of user-specific data.
- Client-side duplicate fetch when server already has data.

## Server Actions and Mutations

- Use Server Actions for simple form-driven mutations close to UI.
- Use Route Handlers for public APIs, third-party webhooks, or cross-client access.
- Validate all mutation inputs on the server.
- Revalidate affected paths/tags after mutation.

## API Route Handlers (`app/api/**/route.ts`)

- Validate request payloads and query params at runtime.
- Return typed, stable response shapes.
- Never leak internal error stacks to clients.
- Map errors to consistent HTTP status codes.

Recommended response shape:

```ts
type ApiResponse<T> =
  | { ok: true; data: T }
  | { ok: false; error: { code: string; message: string } };
```

## Security Requirements

- Keep secrets server-side only. Never expose secrets through `NEXT_PUBLIC_*`.
- Authorize server actions and route handlers explicitly.
- Protect against CSRF for cookie-based auth workflows.
- Sanitize and validate any user-controlled content before rendering.
- Use secure headers and content security policy where appropriate.

## Authentication and Authorization

- Perform auth checks on the server at route/action boundary.
- Enforce authorization in domain/service layer, not only UI.
- Do not trust client-provided role/permission flags.

## Performance Standards

- Optimize images with `next/image`.
- Use route-level `loading.tsx` and streaming for perceived performance.
- Use dynamic imports for heavy client-only components.
- Keep bundle size small; avoid large client libraries in `"use client"` trees.
- Track Core Web Vitals in production.

## SEO and Metadata

- Use metadata API (`generateMetadata`) per route where needed.
- Ensure canonical URLs and structured metadata for public pages.
- Use semantic headings and accessible markup.

## Error Handling and Observability

- Provide `error.tsx` boundaries for route segments.
- Log structured errors on server with request correlation data.
- Treat expected domain errors differently from unexpected exceptions.
- Never log secrets, tokens, or full PII.

## Environment and Configuration

- Validate environment variables at startup.
- Keep env parsing in one module (for example `src/config/env.ts`).
- Fail fast on missing required configuration.

## Testing Strategy for Next.js

### Unit tests
- Domain logic, helpers, pure transformations.
- Server actions with mocked dependencies.

### Integration tests
- Route handlers (`GET/POST/...`) with realistic request/response assertions.
- Data + cache invalidation behavior.

### E2E tests
- Critical user journeys (auth, checkout/payment, form submission, dashboards).
- Validate redirects, error boundaries, and loading states.

All tests must follow Given-When-Then and pass before commit.

## Accessibility

- Keyboard navigable UI and visible focus states.
- Correct labels/roles/alt text.
- Avoid click-only interactions without keyboard equivalents.

## Deployment and Runtime Guidance

- Declare runtime intentionally (`nodejs` vs `edge`) per route when needed.
- Do not use Node-only libraries in Edge runtime paths.
- Keep cold-start-sensitive code minimal.

## Anti-Patterns to Reject

- Blanket `"use client"` at high-level layout/page components
- Business logic embedded directly in React components
- Fetching secrets or privileged data from client components
- Route handlers without input validation
- Global mutable state for request-scoped server data
- Silent retries without timeout/backoff strategy

## Related Standards

- `./TYPESCRIPT_STANDARDS.md`
- `./CODING_PRACTICES.md`
- `./PRE_COMMIT_CHECKLIST.md`
- `./SOLID_PRINCIPLES.md`
