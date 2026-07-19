# Kotlin Standards — Extended Reference

This companion file contains extended code examples referenced in [SKILL.md](./SKILL.md).

## Table of Contents

- [Coroutines — Full Examples](#coroutines--full-examples)
- [Collections & Sequences — Full Examples](#collections--sequences--full-examples)
- [Testing (JUnit 5 + MockK) — Full Example](#testing-junit-5--mockk--full-example)
- [Build (Gradle Kotlin DSL)](#build-gradle-kotlin-dsl)
- [SOLID Strategy and Singleton Patterns](#solid-strategy-and-singleton-patterns)

---

## Coroutines — Full Examples

```kotlin
// ✅ suspend functions over Deferred — idiomatic Kotlin coroutines
suspend fun fetchOrder(id: String): Order

// ✅ Structured concurrency with coroutineScope
suspend fun processOrders(ids: List<String>): List<Order> = coroutineScope {
    ids.map { id -> async { fetchOrder(id) } }.awaitAll()
}

// ✅ Handle cancellation — check isActive or use cancellable primitives
suspend fun longRunningTask(): Result {
    delay(1000)       // cancellable — automatically checks cancellation
    ensureActive()    // explicit cancellation check before expensive work
    return doWork()
}

// ✅ Use withContext for blocking I/O
val result = withContext(Dispatchers.IO) {
    file.readText()
}
```

Never launch a coroutine without a defined scope. Never use `GlobalScope` in production.

---

## Collections & Sequences — Full Examples

```kotlin
// ✅ Idiomatic collection operations
val activeOrders = orders.filter { it.status == OrderStatus.ACTIVE }
val totalByCustomer = orders.groupBy { it.customerId }
val (active, inactive) = orders.partition { it.isActive() }

// ✅ Sequences for large/chained operations (lazy evaluation)
val result = largeList.asSequence()
    .filter { it.isActive() }
    .map { it.total }
    .take(100)
    .toList()  // terminal — evaluated lazily

// ✅ Useful transformations
val lookup = orders.associateBy { it.id }   // Map<String, Order>
val ids = orders.mapNotNull { it.externalId } // null-safe mapping
```

---

## Testing (JUnit 5 + MockK) — Full Example

```kotlin
class OrderServiceTest {

    private val repository = mockk<OrderRepository>()
    private val emailer = mockk<EmailSender>(relaxed = true)
    private val service = OrderService(repository, emailer)

    @Test
    fun `should throw when order not found`() {
        // Given
        every { repository.findById("unknown") } returns null

        // When / Then
        assertThrows<OrderNotFoundException> {
            service.getOrder("unknown")
        }
    }

    @ParameterizedTest
    @CsvSource("ACTIVE, true", "CANCELLED, false", "PENDING, false")
    fun `should determine if order is active`(status: OrderStatus, expected: Boolean) {
        val order = Order("1", "cust", BigDecimal.TEN, status)
        assertThat(order.isActive()).isEqualTo(expected)
    }

    @Nested
    inner class `when creating an order` {
        @Test
        fun `should persist and return created order`() { ... }
    }
}
```

- Test names use backtick syntax: `` `should do X when Y` ``
- Given-When-Then structure with comment labels
- MockK for Kotlin-idiomatic mocking (`every`, `verify`, `coEvery` for coroutines)
- `relaxed = true` for mocks where you only care about some interactions

---

## Build (Gradle Kotlin DSL)

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    id("org.owasp.dependencycheck") version "10.0.3"
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
```

---

## SOLID Strategy and Singleton Patterns

### Strategy Pattern (OCP)

```kotlin
fun interface DiscountStrategy {
    fun calculate(order: Order): BigDecimal
}

class OrderService(private val discount: DiscountStrategy) {
    fun total(order: Order): BigDecimal = order.subtotal - discount.calculate(order)
}

// Callers can use lambdas thanks to fun interface (SAM conversion)
val service = OrderService { order -> order.subtotal * BigDecimal("0.10") }
```

### Singleton via `object`

```kotlin
object NoDiscount : DiscountStrategy {
    override fun calculate(order: Order): BigDecimal = BigDecimal.ZERO
}
```
