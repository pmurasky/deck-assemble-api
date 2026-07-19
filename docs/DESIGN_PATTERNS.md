# Design Patterns

## Overview
This document provides a concise, practical reference to the GoF design patterns as cataloged on https://refactoring.guru/design-patterns.

Use patterns to solve recurring design problems, not to decorate code. Start simple and introduce a pattern only when real change pressure appears (new variants, branching logic, duplication, tight coupling).

## How To Use This Guide
- Intent: what problem the pattern solves.
- Use when: conditions that indicate the pattern is appropriate.
- Avoid when: signals that the pattern adds unnecessary complexity.

## Creational Patterns

### Factory Method
- Intent: delegate object creation to subclasses or collaborators.
- Use when: a class cannot anticipate the concrete types it must create.
- Avoid when: a simple constructor call is sufficient.

### Abstract Factory
- Intent: create families of related objects without specifying concrete classes.
- Use when: you need consistent variants across multiple products (e.g., UI themes).
- Avoid when: you only create one product type.

### Builder
- Intent: construct complex objects step-by-step with readable configuration.
- Use when: object creation has many optional parameters or complex assembly.
- Avoid when: the object has a small, stable constructor.

### Prototype
- Intent: create new objects by cloning existing ones.
- Use when: object creation is expensive or requires complex setup.
- Avoid when: cloning is shallow or error-prone for your model.

### Singleton
- Intent: ensure a class has only one instance and provide a global access point.
- Use when: you truly need a single shared instance with controlled lifecycle.
- Avoid when: it hides dependencies or makes testing hard (prefer DI).

## Structural Patterns

### Adapter
- Intent: make incompatible interfaces work together.
- Use when: integrating legacy or third-party APIs.
- Avoid when: you can change one side of the interface.

### Bridge
- Intent: decouple abstraction from implementation so both can vary.
- Use when: you have multiple dimensions of variation (e.g., shape x renderer).
- Avoid when: a simple inheritance tree is stable and small.

### Composite
- Intent: treat individual objects and compositions uniformly.
- Use when: working with tree-like structures (files, UI components).
- Avoid when: the hierarchy is shallow and unlikely to grow.

### Decorator
- Intent: add behavior dynamically without changing the original class.
- Use when: you need combinable features (e.g., stream filters).
- Avoid when: simple inheritance or configuration is enough.

### Facade
- Intent: provide a simplified interface to a complex subsystem.
- Use when: you need a stable, easy-to-use API over many components.
- Avoid when: it merely hides poor subsystem design.

### Flyweight
- Intent: share common state to reduce memory usage.
- Use when: you have many similar objects with large shared state.
- Avoid when: shared state management adds more complexity than it saves.

### Proxy
- Intent: control access to an object (lazy loading, security, caching).
- Use when: you need a stand-in to add cross-cutting behavior.
- Avoid when: it obscures performance or error handling.

## Behavioral Patterns

### Chain of Responsibility
- Intent: pass a request along a chain of handlers.
- Use when: multiple handlers may process a request in order.
- Avoid when: the order is fixed and only one handler will ever apply.

### Command
- Intent: encapsulate a request as an object.
- Use when: you need undo/redo, queues, or auditability.
- Avoid when: a direct method call is sufficient.

### Interpreter
- Intent: define a grammar and interpret sentences in the language.
- Use when: you need to evaluate or transform simple, repeated expressions.
- Avoid when: the grammar is complex (prefer a parser generator).

### Iterator
- Intent: access elements of a collection without exposing its internals.
- Use when: you need a stable traversal API across collections.
- Avoid when: the language already provides a clear iterable abstraction.

### Mediator
- Intent: centralize complex communications between objects.
- Use when: many objects are tightly coupled through direct references.
- Avoid when: the mediator becomes a god object.

### Memento
- Intent: capture and restore object state without violating encapsulation.
- Use when: you need snapshots for undo/rollback.
- Avoid when: state is large or security-sensitive.

### Observer
- Intent: notify multiple dependents when a subject changes.
- Use when: you need event-driven updates.
- Avoid when: ordering and lifecycle complexity outweigh benefits.

### State
- Intent: change behavior when internal state changes.
- Use when: many conditional branches depend on state.
- Avoid when: a simple enum and switch is stable and small.

### Strategy
- Intent: encapsulate interchangeable algorithms behind a common interface.
- Use when: you need to swap behavior at runtime or add variants easily.
- Avoid when: the algorithm will never change.

### Template Method
- Intent: define algorithm skeleton in a base class with hook methods.
- Use when: you have a stable process with varying steps.
- Avoid when: composition is more flexible than inheritance.

### Visitor
- Intent: add operations to object structures without modifying the classes.
- Use when: you need many unrelated operations over a stable structure.
- Avoid when: the object structure changes frequently.

## Pattern Selection Signals
- Variation in behavior across a dimension: Strategy or State.
- Many object families that must stay consistent: Abstract Factory.
- Expensive construction with optional pieces: Builder or Prototype.
- Cross-cutting access control or laziness: Proxy or Decorator.
- Tree structures with uniform operations: Composite.

## Anti-Patterns and Misuse Warnings
- Singleton as global state: prefer constructor injection and explicit lifecycles.
- God mediator/facade: split responsibilities or refine boundaries.
- Over-abstracted factories: remove layers if only one concrete type exists.
- Visitor on unstable models: avoid if class hierarchy changes often.

## References
- Refactoring.Guru Design Patterns: https://refactoring.guru/design-patterns

---

**Last Updated**: February 16, 2026
**Version**: 1.0
