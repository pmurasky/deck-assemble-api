# SOLID Principles & Design Patterns Reference

> Extracted from the pre-commit-checklist skill for detailed reference.
> Canonical owner: SOLID_PRINCIPLES, DESIGN_PATTERNS (see STANDARDS_OWNERSHIP_MATRIX).

---

## SOLID Principles Detailed Checklist

### Single Responsibility Principle (SRP)

**Question**: Does each class/method have ONE reason to change?

**Check for violations:**
- [ ] Class name contains "Manager", "Handler", "Utility", "Helper" (often God classes)
- [ ] Class has > 10 methods (likely doing too much)
- [ ] Method does multiple unrelated things (e.g., validates AND saves AND sends email)
- [ ] Class mixes business logic with infrastructure (e.g., database access)
- [ ] Method name contains "And" (e.g., `validateAndSave()`)

**Red flags:** Class > 300 lines, Method > 15-20 lines, Class imports from > 5 packages, Feature Envy

```kotlin
// ❌ BAD - Multiple responsibilities
class UserManager {
    fun createUser(data: UserData) {
        validateUser(data)           // validation
        val user = User(data)        // domain logic
        database.save(user)          // persistence
        emailService.sendWelcome(user) // notification
    }
}

// ✅ GOOD - Single responsibility
class UserCreator { fun create(data: UserData): User = User(data) }
class UserValidator { fun validate(data: UserData) }
class UserRepository { fun save(user: User) }
class WelcomeEmailSender { fun send(user: User) }
```

---

### Open/Closed Principle (OCP)

**Question**: Can I add new functionality WITHOUT modifying existing code?

**Check for violations:**
- [ ] Switch/when statements on type checks or enums
- [ ] If-else chains checking object types
- [ ] Hard-coded class instantiation (e.g., `val writer = MarkdownWriter()`)
- [ ] Adding new feature requires changing multiple existing classes

```kotlin
// ❌ BAD - Must modify to add new report type
fun generateReport(type: String) {
    when (type) {
        "markdown" -> MarkdownWriter().write()
        "json" -> JsonWriter().write()
        // Adding PDF requires modifying this method
    }
}

// ✅ GOOD - Open for extension
interface ReportWriter { fun write(data: Data) }
class ReportGenerator(private val writers: List<ReportWriter>) {
    fun generateAll(data: Data) { writers.forEach { it.write(data) } }
}
```

---

### Liskov Substitution Principle (LSP)

**Question**: Can I substitute any subclass for its parent without breaking functionality?

**Check for violations:**
- [ ] Subclass throws exceptions parent doesn't throw
- [ ] Subclass has stricter preconditions than parent
- [ ] Type checking before casting (`is` checks)

```kotlin
// ❌ BAD - Violates LSP
class Penguin : Bird() {
    override fun fly() { throw UnsupportedOperationException("Penguins can't fly!") }
}

// ✅ GOOD
interface FlyingBird : Bird { fun fly() }
class Sparrow : FlyingBird
class Penguin : Bird  // does not implement FlyingBird
```

---

### Interface Segregation Principle (ISP)

**Question**: Are interfaces focused and cohesive, or fat and bloated?

**Check for violations:**
- [ ] Interface has > 5 methods
- [ ] Classes implement interface but leave some methods empty or throw "not implemented"

```kotlin
// ❌ BAD — Fat interface
interface Worker { fun work(); fun eat(); fun sleep(); fun getPaid(); fun takeVacation(); fun attendMeeting() }

// ✅ GOOD — Segregated
interface Workable { fun work() }
interface Eatable { fun eat() }
interface Payable { fun getPaid() }
```

---

### Dependency Inversion Principle (DIP)

**Question**: Do high-level modules depend on abstractions, not concrete implementations?

**Check for violations:**
- [ ] Direct instantiation of dependencies inside classes (`val parser = CheckstyleParser()`)
- [ ] No constructor injection
- [ ] Cannot mock dependencies for testing

```kotlin
// ❌ BAD - Depends on concrete class
class Analyzer {
    private val parser = CheckstyleParser()  // Hard-coded dependency
    private val database = MySQLDatabase()
}

// ✅ GOOD - Depends on abstractions with injection
class Analyzer(
    private val parser: ViolationParser,     // Interface, injected
    private val database: Database
)
```

---

## Design Patterns Checklist

### Use Appropriate Patterns
- [ ] **Strategy Pattern** — multiple algorithms/implementations?
- [ ] **Factory Pattern** — complex object creation?
- [ ] **Template Method** — algorithms sharing common structure?
- [ ] **Decorator Pattern** — adding responsibilities dynamically?
- [ ] **Observer Pattern** — one-to-many dependencies?

### Avoid Anti-Patterns

**God Class / God Method**
- [ ] No class > 300 lines (class body)
- [ ] No method > language-specific limit (15-20 lines)
- [ ] No class with > 10 methods

**Feature Envy** — Methods primarily use data from OTHER classes

**Primitive Obsession** — Using `String userId` instead of `UserId` value class

**Long Parameter List** — No method with > 5 parameters

**Tight Coupling** — Classes don't directly instantiate their dependencies

**Duplicated Code** — No copy-pasted code blocks (DRY)

---

## Common Violations & Quick Fixes

**Hard-coded Dependencies:**
```kotlin
// ❌ BEFORE
class Orchestrator { private val parser = CheckstyleParser() }
// ✅ AFTER
class Orchestrator(private val parser: ViolationParser = CheckstyleParser())
```

**Switch on Type:**
```kotlin
// ❌ BEFORE
fun process(violation: Violation): Double = when (violation) {
    is CheckstyleViolation -> 2.0
    is PmdViolation -> 3.0
}
// ✅ AFTER
interface ScoredViolation { fun getPenalty(): Double }
// Each violation type implements getPenalty()
```
