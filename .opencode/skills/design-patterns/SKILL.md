---
name: design-patterns
description: >
  Use when choosing or reviewing a structural design — GoF design patterns catalog
  with intent, use-when, and avoid-when guidance for creational (Factory, Builder,
  Singleton), structural (Adapter, Decorator, Proxy), and behavioral (Strategy,
  Observer, Command) patterns. Includes anti-pattern warnings.
triggers:
  - "choosing a design pattern for new feature"
  - "refactoring branching logic or tight coupling"
  - "reviewing whether a pattern is overused"
  - "comparing structural options for extensible design"
not_for:
  - "basic style or naming questions without design"
  - "forcing patterns into simple stable code"
  - "framework specific details after pattern choice"
---

# Design Patterns

Use patterns to solve recurring design problems, not to decorate code. Start simple and introduce a pattern only when real change pressure appears (new variants, branching logic, duplication, tight coupling).

---

## Use when

- When choosing a design pattern for a new feature
- When refactoring code that has pattern-selection signals (branching logic, tight coupling, variant explosion)
- When reviewing whether a pattern is applied correctly or over-applied

## Not for

- Basic style, naming, or linting questions that do not involve structural design
- Forcing a pattern into simple code with one stable implementation and no change pressure
- Framework-specific implementation details after the architectural pattern is already chosen

---

## Creational Patterns

- **Factory Method** — delegate object creation to subclasses. Use when a class cannot anticipate the concrete types it must create.
- **Abstract Factory** — create families of related objects. Use for consistent variants across multiple products (e.g., UI themes).
- **Builder** — construct complex objects step-by-step. Use for many optional parameters or complex assembly.
- **Prototype** — create objects by cloning existing ones. Use when creation is expensive.
- **Singleton** — one instance, global access point. Use only when you truly need one shared instance. **Avoid** when it hides dependencies — prefer DI.

## Structural Patterns

- **Adapter** — make incompatible interfaces work together. Use when integrating legacy or third-party APIs.
- **Bridge** — decouple abstraction from implementation. Use when you have multiple dimensions of variation.
- **Composite** — treat individuals and compositions uniformly. Use for tree-like structures (files, UI components).
- **Decorator** — add behavior dynamically. Use when you need combinable features (e.g., stream filters).
- **Facade** — simplified interface to a complex subsystem. Use to provide a stable API over many components.
- **Flyweight** — share common state to reduce memory. Use when you have many similar objects with large shared state.
- **Proxy** — control access to an object (lazy loading, security, caching). Use when you need a stand-in to add cross-cutting behavior.

## Behavioral Patterns

- **Chain of Responsibility** — pass a request along a chain of handlers. Use when multiple handlers may apply in order.
- **Command** — encapsulate a request as an object. Use when you need undo/redo, queues, or auditability.
- **Observer** — notify multiple dependents on subject change. Use for event-driven updates.
- **State** — change behavior when internal state changes. Use when many conditionals depend on state.
- **Strategy** — encapsulate interchangeable algorithms behind a common interface. Use to swap behavior at runtime or add variants easily.
- **Template Method** — algorithm skeleton in base class with hook methods. Use for a stable process with varying steps.
- **Visitor** — add operations to object structures without modifying classes. Use for many unrelated operations over a stable structure. Avoid when the structure changes frequently.
- **Iterator**, **Mediator**, **Memento**, **Interpreter** — see REFERENCE.md for details.

## Pattern Selection Signals

- Variation in behavior across a dimension → **Strategy** or **State**
- Many object families that must stay consistent → **Abstract Factory**
- Expensive construction with optional pieces → **Builder** or **Prototype**
- Cross-cutting access control or laziness → **Proxy** or **Decorator**
- Tree structures with uniform operations → **Composite**

## Anti-Patterns and Misuse Warnings

- **Singleton as global state**: prefer constructor injection and explicit lifecycles
- **God mediator/facade**: split responsibilities or refine boundaries
- **Over-abstracted factories**: remove layers if only one concrete type exists
- **Visitor on unstable models**: avoid if class hierarchy changes often

For complete reference material including code examples, see [REFERENCE.md](REFERENCE.md) in this skill directory.
