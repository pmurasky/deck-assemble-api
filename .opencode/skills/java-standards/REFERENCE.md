# Java Standards — Reference Details

Companion reference for `java-standards/SKILL.md`. Contains the Java version feature
matrix and detailed code examples extracted for progressive disclosure.

## Table of Contents

- [Java Version Feature Matrix](#java-version-feature-matrix)
- [Streams and Collections](#streams-and-collections)
- [Markdown Javadoc (Java 23+)](#markdown-javadoc-java-23)
- [Module Import Declarations (Java 25)](#module-import-declarations-java-25)

---

## Java Version Feature Matrix

| Feature | Available Since |
|---------|----------------|
| Records, sealed classes, pattern matching (`instanceof`), text blocks, `Optional` | Java 21 (LTS) |
| Unnamed variables (`_`), unnamed patterns | Java 22 |
| Markdown Javadoc (`///`) | Java 23 |
| Stream Gatherers (`Gatherers.*`) | Java 24 |
| Module import declarations, flexible constructors, compact source files | Java 25 (LTS) |
| **String templates** | **WITHDRAWN — use `String.formatted()` instead** |

---

## Streams and Collections

```java
// ✅ Prefer functional stream operations
var activeOrders = orders.stream()
    .filter(o -> o.status() == OrderStatus.ACTIVE)
    .sorted(Comparator.comparing(Order::createdAt))
    .toList(); // Java 16+ — returns unmodifiable list

// ✅ Group by with collectors
var byStatus = orders.stream()
    .collect(Collectors.groupingBy(Order::status));

// ✅ Java 24: Stream Gatherers for custom intermediate operations
var windowed = orders.stream()
    .gather(Gatherers.windowFixed(3))
    .toList(); // [[o1,o2,o3], [o4,o5,o6], ...]
```

---

## Markdown Javadoc (Java 23+)

```java
/// Returns the order with the given ID.
///
/// ## Parameters
/// - `id` — the unique order identifier (must not be null)
///
/// ## Returns
/// `Optional.empty()` if no order exists with this ID.
///
/// ## Throws
/// - `IllegalArgumentException` if `id` is blank
public Optional<Order> findById(String id) { ... }
```

---

## Module Import Declarations (Java 25)

```java
// Import all public types from a module — replaces many individual imports
import module java.base;         // imports String, List, Optional, Objects, etc.
import module java.logging;      // imports Logger, Level, etc.
```
