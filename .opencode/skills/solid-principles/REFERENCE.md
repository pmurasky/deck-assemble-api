# SOLID Principles

## Overview

SOLID is a set of five object-oriented design principles introduced by Robert C. Martin ("Uncle Bob") and named by Michael Feathers. These principles guide developers toward creating software that is easier to understand, maintain, extend, and test.

**Quick Reference:**

| Letter | Principle | One-Liner |
|--------|-----------|-----------|
| **S** | Single Responsibility | A class has one reason to change |
| **O** | Open/Closed | Open for extension, closed for modification |
| **L** | Liskov Substitution | Subtypes must be substitutable for base types |
| **I** | Interface Segregation | Prefer focused interfaces over fat ones |
| **D** | Dependency Inversion | Depend on abstractions, not concretions |

---

## Why SOLID Matters

Without SOLID, codebases develop these symptoms:

- **Rigidity** -- Small changes cascade through many files
- **Fragility** -- Fixing one bug introduces two new ones
- **Immobility** -- Components cannot be reused in other contexts
- **Viscosity** -- Doing things "the right way" is harder than hacking

With SOLID:

- Easier to test (components isolate cleanly)
- Easier to extend (new features = new code, not modified code)
- Easier to maintain (single place to change for each concern)
- Easier to understand (small, focused classes with clear names)

---

## S -- Single Responsibility Principle (SRP)

> A class should have only one reason to change.

### Real-World Analogy

A chef in a restaurant is responsible only for cooking -- not for taking orders, serving food, or washing dishes. Each role has a dedicated person so that changing how orders are taken does not affect how food is cooked.

### The Problem

When a class handles multiple responsibilities, a change to one responsibility risks breaking the others. The class becomes harder to test, harder to name, and harder to reason about.

### Violation Signals

- Class name contains "Manager", "Handler", "Utility", or "Helper"
- Class has more than 10 methods
- Method name contains "And" (e.g., `validateAndSave()`)
- Class mixes business logic with infrastructure (database, HTTP, file I/O)
- More than 2 private methods (hidden responsibilities wanting extraction)

### Examples

**Kotlin -- Before (multiple responsibilities):**

```kotlin
class ReportService {
    fun generateReport(data: ReportData) {
        // Data fetching logic (40 lines)
        // Formatting logic (35 lines)
        // PDF rendering logic (30 lines)
        // Email sending logic (25 lines)
    }
}
```

**Kotlin -- After (single responsibility each):**

```kotlin
class ReportService(
    private val dataFetcher: ReportDataFetcher,
    private val formatter: ReportFormatter,
    private val renderer: PdfRenderer,
    private val emailSender: EmailSender
) {
    fun generateReport(request: ReportRequest) {
        val data = dataFetcher.fetch(request)
        val formatted = formatter.format(data)
        val pdf = renderer.render(formatted)
        emailSender.send(pdf, request.recipient)
    }
}
```

**Java -- Before:**

```java
public class Book {
    private String name;
    private String author;
    private String text;

    public String replaceWordInText(String word, String replacement) {
        return text.replaceAll(word, replacement);
    }

    // Violation: printing is a separate responsibility
    void printTextToConsole() {
        System.out.println(text);
    }
}
```

**Java -- After:**

```java
public class Book {
    private String name;
    private String author;
    private String text;

    public String replaceWordInText(String word, String replacement) {
        return text.replaceAll(word, replacement);
    }
}

public class BookPrinter {
    void printTextToConsole(String text) {
        System.out.println(text);
    }

    void printTextToFile(String text, String filePath) {
        // write to file
    }
}
```

**Python -- Before:**

```python
class OrderProcessor:
    def process(self, order):
        # validate order
        # save to database
        # send confirmation email
        # log activity
        pass
```

**Python -- After:**

```python
class OrderProcessor:
    def __init__(self, validator, repository, notifier):
        self._validator = validator
        self._repository = repository
        self._notifier = notifier

    def process(self, order):
        self._validator.validate(order)
        self._repository.save(order)
        self._notifier.send_confirmation(order)

class OrderValidator:
    def validate(self, order): ...

class OrderRepository:
    def save(self, order): ...

class OrderNotifier:
    def send_confirmation(self, order): ...
```

**PHP -- Before:**

```php
class AreaCalculator {
    public function sum($shapes) {
        foreach ($shapes as $shape) {
            if (is_a($shape, 'Square')) {
                $area[] = pow($shape->length, 2);
            } elseif (is_a($shape, 'Circle')) {
                $area[] = pi() * pow($shape->radius, 2);
            }
        }
        return array_sum($area);
    }

    // Violation: output formatting is a separate concern
    public function output() {
        return '<h1>Sum: ' . $this->sum() . '</h1>';
    }
}
```

**PHP -- After:**

```php
class AreaCalculator {
    public function sum($shapes) {
        foreach ($shapes as $shape) {
            $area[] = $shape->area(); // each shape knows its own area
        }
        return array_sum($area);
    }
}

class SumCalculatorOutputter {
    public function __construct(private AreaCalculator $calculator) {}

    public function JSON() {
        return json_encode(['sum' => $this->calculator->sum()]);
    }

    public function HTML() {
        return '<h1>Sum: ' . $this->calculator->sum() . '</h1>';
    }
}
```

### Checklist

- [ ] Can you describe the class's purpose in ONE sentence without using "and"?
- [ ] Does the class have 0-2 private methods?
- [ ] Does the class import from fewer than 5 packages?
- [ ] Is the class under 300 lines?

---

## O -- Open/Closed Principle (OCP)

> Software entities should be open for extension but closed for modification.

### Real-World Analogy

A graphic design application lets you install new brush plugins without changing the core drawing engine. The engine is *closed* for modification but *open* for extension via a plugin interface.

### The Problem

When adding a new feature requires modifying existing, tested code, you risk introducing bugs into working functionality. Every `when`/`switch` statement on a type is a sign that the next feature will require editing that block.

### Violation Signals

- `when`/`switch` statements checking object types or enums
- `if-else` chains that grow with each new variant
- Adding a feature requires modifying multiple existing classes
- Hard-coded class instantiation (e.g., `val writer = MarkdownWriter()`)

### Examples

**Kotlin -- Before (must modify to add new format):**

```kotlin
class ReportGenerator {
    fun generate(data: Data, format: String) {
        when (format) {
            "pdf" -> // PDF logic
            "html" -> // HTML logic
            "csv" -> // Adding CSV required modifying this method
        }
    }
}
```

**Kotlin -- After (extend without modifying):**

```kotlin
interface ReportStrategy {
    fun generate(data: Data): ByteArray
}

class PdfReportStrategy : ReportStrategy {
    override fun generate(data: Data) = // PDF logic
}

class CsvReportStrategy : ReportStrategy {
    override fun generate(data: Data) = // CSV logic -- no changes to existing code
}

class ReportGenerator(private val strategy: ReportStrategy) {
    fun generate(data: Data) = strategy.generate(data)
}
```

**Java -- Before:**

```java
public class Guitar {
    private String make;
    private String model;
    private int volume;
}
```

**Java -- After (extend via inheritance, no modification):**

```java
public class SuperCoolGuitarWithFlames extends Guitar {
    private String flameColor;
    // Extends behavior without touching Guitar
}
```

**PHP -- Before:**

```php
class AreaCalculator {
    public function sum($shapes) {
        foreach ($shapes as $shape) {
            if (is_a($shape, 'Square')) {
                $area[] = pow($shape->length, 2);
            } elseif (is_a($shape, 'Circle')) {
                $area[] = pi() * pow($shape->radius, 2);
            }
            // Adding Triangle requires modifying this method
        }
        return array_sum($area);
    }
}
```

**PHP -- After:**

```php
interface ShapeInterface {
    public function area();
}

class Square implements ShapeInterface {
    public function area() { return pow($this->length, 2); }
}

class Circle implements ShapeInterface {
    public function area() { return pi() * pow($this->radius, 2); }
}

class Triangle implements ShapeInterface {
    public function area() { return 0.5 * $this->base * $this->height; }
    // New shape -- no changes to AreaCalculator
}

class AreaCalculator {
    public function sum($shapes) {
        foreach ($shapes as $shape) {
            $area[] = $shape->area();
        }
        return array_sum($area);
    }
}
```

**Python -- Before:**

```python
class PaymentProcessor:
    def process(self, payment_type, amount):
        if payment_type == "credit_card":
            # credit card logic
        elif payment_type == "paypal":
            # paypal logic
        # Adding new type requires modifying this method
```

**Python -- After:**

```python
from abc import ABC, abstractmethod

class Payment(ABC):
    @abstractmethod
    def process(self, amount): ...

class CreditCardPayment(Payment):
    def process(self, amount):
        # credit card logic

class PayPalPayment(Payment):
    def process(self, amount):
        # paypal logic

class CryptoPayment(Payment):
    def process(self, amount):
        # new payment type -- no changes to existing code

class PaymentProcessor:
    def __init__(self, payment: Payment):
        self._payment = payment

    def process(self, amount):
        self._payment.process(amount)
```

### Checklist

- [ ] Can you add a new variant without modifying existing classes?
- [ ] Are there any `when`/`switch` statements on type checks?
- [ ] Is the Strategy pattern used where multiple algorithms exist?

---

## L -- Liskov Substitution Principle (LSP)

> Subtypes must be substitutable for their base types without altering program correctness.

### Real-World Analogy

If you are driving a vehicle, you should be able to switch from a car to a truck and still drive without issues. Both honor the "vehicle" contract -- they start, steer, accelerate, and brake. An electric car that throws an error when you try to start the engine (because it has no engine) breaks this contract.

### The Problem

When a subclass changes the expected behavior of its parent -- by throwing unexpected exceptions, ignoring operations, or tightening preconditions -- code that works with the parent type breaks silently when given the subclass.

### Violation Signals

- Subclass throws exceptions the parent does not declare
- Subclass has stricter preconditions than the parent
- Subclass has weaker postconditions than the parent
- Subclass leaves methods empty or as no-ops
- Code uses `is`/`instanceof` checks before calling methods

### Examples

**Java -- Violation (ElectricCar breaks Car contract):**

```java
public interface Car {
    void turnOnEngine();
    void accelerate();
}

public class MotorCar implements Car {
    private Engine engine;

    public void turnOnEngine() { engine.on(); }
    public void accelerate() { engine.powerOn(1000); }
}

public class ElectricCar implements Car {
    public void turnOnEngine() {
        throw new AssertionError("I don't have an engine!");
        // Violation: caller expects turnOnEngine() to work
    }
    public void accelerate() { /* electric acceleration */ }
}
```

**Java -- Fixed (rework the abstraction):**

```java
public interface Vehicle {
    void start();
    void accelerate();
}

public class MotorCar implements Vehicle {
    public void start() { engine.on(); }
    public void accelerate() { engine.powerOn(1000); }
}

public class ElectricCar implements Vehicle {
    public void start() { battery.activate(); }
    public void accelerate() { motor.powerOn(1000); }
    // Both fulfill the Vehicle contract without surprises
}
```

**Kotlin -- Violation (Penguin breaks Bird.fly()):**

```kotlin
open class Bird {
    open fun fly() { /* fly */ }
}

class Penguin : Bird() {
    override fun fly() {
        throw UnsupportedOperationException("Penguins can't fly!")
    }
}
```

**Kotlin -- Fixed:**

```kotlin
interface Bird
interface FlyingBird : Bird {
    fun fly()
}
class Sparrow : FlyingBird {
    override fun fly() { /* soar */ }
}
class Penguin : Bird // No fly() to violate
```

**Python -- Violation (move() contract broken):**

```python
class Bird:
    def move(self):
        return "flying"

class Penguin(Bird):
    def move(self):
        raise NotImplementedError("Penguins can't fly")
        # Caller expects move() to return a string, not throw
```

**Python -- Fixed:**

```python
class Bird:
    def move(self):
        raise NotImplementedError

class Sparrow(Bird):
    def move(self):
        return "flying"

class Penguin(Bird):
    def move(self):
        return "swimming"  # Still honors the contract
```

**PHP -- Violation (VolumeCalculator returns wrong type):**

```php
class AreaCalculator {
    public function sum() {
        // returns a number
        return array_sum($area);
    }
}

class VolumeCalculator extends AreaCalculator {
    public function sum() {
        // Violation: returns an array instead of a number
        return [$summedData];
    }
}
```

**PHP -- Fixed:**

```php
class VolumeCalculator extends AreaCalculator {
    public function sum() {
        // Returns a number, just like the parent
        return $summedData;
    }
}
```

### Checklist

- [ ] Do all subtypes honor the parent's contract (same return types, no surprise exceptions)?
- [ ] Can you loop over a `List<Base>` with mixed subtypes and get correct behavior?
- [ ] Are there any `instanceof`/`is` checks before calling methods?

---

## I -- Interface Segregation Principle (ISP)

> Clients should not be forced to depend on interfaces they do not use.

### Real-World Analogy

A universal remote control with 50 buttons is confusing when you only need power, volume, and channel. A simpler remote with just the buttons you need is easier to use and less error-prone.

### The Problem

When a "fat" interface forces implementing classes to provide methods they do not need, those classes end up with empty stubs, `throw NotImplementedError`, or dead code. Clients that depend on the fat interface are coupled to methods they never call.

### Violation Signals

- Interface has more than 5 methods
- Implementing classes throw "not implemented" for some methods
- Implementing classes leave some methods as empty no-ops
- Clients import an interface but only use 1-2 of its methods

### Examples

**Kotlin -- Before (fat interface):**

```kotlin
interface DataProcessor {
    fun parse(input: InputStream): List<Record>
    fun validate(records: List<Record>): ValidationResult
    fun transform(records: List<Record>): List<Record>
    fun export(records: List<Record>): ExportResult
}

// CsvParser is forced to implement validate, transform, export
class CsvParser : DataProcessor {
    override fun parse(input: InputStream) = // parsing logic
    override fun validate(records: List<Record>) = TODO("Not needed")
    override fun transform(records: List<Record>) = TODO("Not needed")
    override fun export(records: List<Record>) = TODO("Not needed")
}
```

**Kotlin -- After (segregated interfaces):**

```kotlin
interface Parser {
    fun parse(input: InputStream): List<Record>
}

interface Validator {
    fun validate(records: List<Record>): ValidationResult
}

interface Transformer {
    fun transform(records: List<Record>): List<Record>
}

class CsvParser : Parser {
    override fun parse(input: InputStream) = // Only parsing logic
}
```

**Java -- Before (BearKeeper forced to pet bears):**

```java
public interface BearKeeper {
    void washTheBear();
    void feedTheBear();
    void petTheBear();
}

// All keepers must implement petTheBear(), even if dangerous
```

**Java -- After:**

```java
public interface BearCleaner {
    void washTheBear();
}

public interface BearFeeder {
    void feedTheBear();
}

public interface BearPetter {
    void petTheBear();
}

public class BearCarer implements BearCleaner, BearFeeder {
    public void washTheBear() { /* safe */ }
    public void feedTheBear() { /* safe */ }
    // No petTheBear() -- not forced to risk it
}

public class CrazyPerson implements BearPetter {
    public void petTheBear() { /* good luck */ }
}
```

**PHP -- Before (forced to implement volume for 2D shapes):**

```php
interface ShapeInterface {
    public function area();
    public function volume();
}

class Square implements ShapeInterface {
    public function area() { return pow($this->length, 2); }
    public function volume() {
        // Squares don't have volume -- forced to implement anyway
        return null;
    }
}
```

**PHP -- After:**

```php
interface ShapeInterface {
    public function area();
}

interface ThreeDimensionalShapeInterface {
    public function volume();
}

class Square implements ShapeInterface {
    public function area() { return pow($this->length, 2); }
    // No volume() needed
}

class Cuboid implements ShapeInterface, ThreeDimensionalShapeInterface {
    public function area() { /* surface area */ }
    public function volume() { /* volume */ }
}
```

**Python -- Before:**

```python
class Worker:
    def work(self): ...
    def eat(self): ...
    def sleep(self): ...

class Robot(Worker):
    def work(self): # fine
    def eat(self): raise NotImplementedError  # robots don't eat
    def sleep(self): raise NotImplementedError  # robots don't sleep
```

**Python -- After:**

```python
from abc import ABC, abstractmethod

class Workable(ABC):
    @abstractmethod
    def work(self): ...

class Eatable(ABC):
    @abstractmethod
    def eat(self): ...

class HumanWorker(Workable, Eatable):
    def work(self): ...
    def eat(self): ...

class Robot(Workable):
    def work(self): ...
    # No eat() or sleep() needed
```

### Checklist

- [ ] Does every interface have 5 or fewer methods?
- [ ] Do all implementing classes use every method in the interface?
- [ ] Are there any `TODO("Not needed")` or empty method stubs?

---

## D -- Dependency Inversion Principle (DIP)

> High-level modules should not depend on low-level modules. Both should depend on abstractions. Abstractions should not depend on details; details should depend on abstractions.

### Real-World Analogy

A standard power socket does not know what device will be plugged in -- it defines a contract (voltage, shape). Devices (low-level modules) are designed to fit the socket. You can plug in a lamp, a laptop, or a phone charger without rewiring your house.

### The Problem

When high-level business logic directly instantiates low-level infrastructure classes (database connections, HTTP clients, file writers), the business logic becomes:

- **Untestable** -- cannot substitute mocks
- **Inflexible** -- cannot swap implementations
- **Tightly coupled** -- changing infrastructure forces changes to business logic

### Violation Signals

- Direct instantiation of dependencies inside classes (`val parser = CheckstyleParser()`)
- Importing concrete classes instead of interfaces
- No constructor injection
- Cannot mock dependencies for testing
- Using `new ConcreteClass()` in constructors

### Examples

**Kotlin -- Before (depends on concrete classes):**

```kotlin
class OrderProcessor(private val config: Config) {
    private val repository = PostgresRepository(config.dbUrl) // tight coupling
    private val notifier = SmtpNotifier()                     // can't inject mocks
}
```

**Kotlin -- After (depends on abstractions):**

```kotlin
class OrderProcessor(
    private val repository: OrderRepository,
    private val notifier: Notifier
) {
    fun process(order: Order) {
        repository.save(order)
        notifier.notify(order)
    }
}

// Testing with mocks
val mockRepo = mock<OrderRepository>()
val mockNotifier = mock<Notifier>()
val processor = OrderProcessor(mockRepo, mockNotifier)
```

**Java -- Before (Windows98Machine depends on concrete classes):**

```java
public class Windows98Machine {
    private final StandardKeyboard keyboard;
    private final Monitor monitor;

    public Windows98Machine() {
        keyboard = new StandardKeyboard(); // tight coupling
        monitor = new Monitor();           // can't swap
    }
}
```

**Java -- After:**

```java
public interface Keyboard {}
public class StandardKeyboard implements Keyboard {}

public class Windows98Machine {
    private final Keyboard keyboard;
    private final Monitor monitor;

    public Windows98Machine(Keyboard keyboard, Monitor monitor) {
        this.keyboard = keyboard; // injected abstraction
        this.monitor = monitor;
    }
}
```

**PHP -- Before (PasswordReminder depends on MySQLConnection):**

```php
class MySQLConnection {
    public function connect() {
        return 'Database connection';
    }
}

class PasswordReminder {
    private $dbConnection;

    public function __construct(MySQLConnection $dbConnection) {
        $this->dbConnection = $dbConnection;
        // Cannot switch to PostgreSQL without modifying this class
    }
}
```

**PHP -- After:**

```php
interface DBConnectionInterface {
    public function connect();
}

class MySQLConnection implements DBConnectionInterface {
    public function connect() {
        return 'MySQL Database connection';
    }
}

class PostgreSQLConnection implements DBConnectionInterface {
    public function connect() {
        return 'PostgreSQL Database connection';
    }
}

class PasswordReminder {
    public function __construct(private DBConnectionInterface $dbConnection) {}

    public function remind() {
        $this->dbConnection->connect();
        // Works with ANY database -- MySQL, PostgreSQL, or a mock
    }
}
```

**Python -- Before:**

```python
class SMTPService:
    def send(self, message):
        # send via SMTP

class EmailSender:
    def __init__(self):
        self._service = SMTPService()  # tight coupling

    def send(self, message):
        self._service.send(message)
```

**Python -- After:**

```python
from abc import ABC, abstractmethod

class EmailService(ABC):
    @abstractmethod
    def send(self, message): ...

class SMTPService(EmailService):
    def send(self, message):
        # send via SMTP

class MockEmailService(EmailService):
    def send(self, message):
        # capture for testing

class EmailSender:
    def __init__(self, service: EmailService):
        self._service = service  # injected abstraction

    def send(self, message):
        self._service.send(message)

# Production
sender = EmailSender(SMTPService())
# Testing
test_sender = EmailSender(MockEmailService())
```

### Checklist

- [ ] Are all dependencies injected via constructor (no `new ConcreteClass()` inside)?
- [ ] Do classes depend on interfaces/abstract classes, not concrete implementations?
- [ ] Can you swap any dependency for a mock in tests?

---

## How the Principles Relate

The SOLID principles are not independent rules -- they reinforce each other:

```
DIP enables OCP
  |           |
  v           v
Abstractions let you      New implementations extend
swap implementations  --> behavior without modifying
without changing          existing code
callers

SRP enables ISP
  |           |
  v           v
Focused classes       --> Focused interfaces
(one responsibility)      (one capability)

LSP validates OCP
  |           |
  v           v
If subtypes honor the --> Then polymorphic extension
base contract            actually works correctly
```

**Key relationships:**

- **DIP enables OCP**: By depending on abstractions (DIP), you can add new implementations (OCP) without modifying the code that uses them.
- **SRP enables ISP**: A class with one responsibility naturally implements focused interfaces, not fat ones.
- **LSP validates OCP**: OCP relies on polymorphism -- substituting implementations. LSP ensures that substitution actually works correctly.
- **ISP supports DIP**: Small, focused interfaces make it easier to define the abstractions that DIP depends on.

---

## Common Violations Quick Reference

| Violation | Principle | Fix |
|-----------|-----------|-----|
| God Class (300+ lines, 10+ methods) | SRP | Extract focused classes |
| `when`/`switch` on type | OCP | Strategy pattern or polymorphism |
| Subclass throws `UnsupportedOperationException` | LSP | Rework the abstraction hierarchy |
| Interface with `TODO("Not needed")` stubs | ISP | Split into focused interfaces |
| `private val dep = ConcreteClass()` | DIP | Constructor injection with interface |
| Method name contains "And" | SRP | Split into separate methods/classes |
| `instanceof`/`is` checks before method calls | LSP | Fix the type hierarchy |
| Class imports 10+ packages | SRP | Extract responsibilities |

---

## Further Reading

- [PRE_COMMIT_CHECKLIST.md](./PRE_COMMIT_CHECKLIST.md) -- SOLID violation checklist with code examples
- [CODING_PRACTICES.md](./CODING_PRACTICES.md) -- General coding practices and SRP guidelines
- [DESIGN_PATTERNS.md](./DESIGN_PATTERNS.md) -- Patterns that implement SOLID (Strategy, Factory, etc.)
- [GO_STANDARDS.md](./GO_STANDARDS.md) -- Go-specific SOLID idioms
- [JAVA_STANDARDS.md](./JAVA_STANDARDS.md) -- Java-specific SOLID idioms
- [KOTLIN_STANDARDS.md](./KOTLIN_STANDARDS.md) -- Kotlin-specific SOLID idioms
- [PYTHON_STANDARDS.md](./PYTHON_STANDARDS.md) -- Python-specific SOLID idioms

---

**Last Updated**: February 16, 2026
**Version**: 1.0
