# Python Coding Standards

## Overview
This document outlines Python-specific coding standards that supplement the language-agnostic standards in `CODING_PRACTICES.md` and `CODING_STANDARDS.md`.

We support **Python 3.12+** and leverage modern Python features where appropriate. Choose the version that matches your project.

## Mandatory Rules

These rules are **non-negotiable** for all Python code. They supplement the language-agnostic rules in `CODING_PRACTICES.md`.

### Safety and Security

**Banned patterns -- never use these:**
- `eval()` / `exec()` -- arbitrary code execution
- `os.system()` -- shell injection risk
- `pickle` with untrusted input -- arbitrary code execution via deserialization
- Wildcard imports (`from x import *`) -- namespace pollution, implicit dependencies

**Safe alternatives:**
```python
# Bad: Shell injection risk
import os
os.system(f"ls {user_input}")

# Good: Safe subprocess with argument list (no shell string building)
import subprocess
result = subprocess.run(["ls", user_input], check=True, capture_output=True, text=True)
```

**Secrets and sensitive data:**
- Never hardcode secrets (tokens, passwords, private keys, API keys) in source code
- Never log secrets or sensitive fields; redact if necessary
- Use environment variables, secret managers, or config injection for credentials
- Use `secrets` module (not `random`) for security-sensitive token generation

**Input validation:**
- Treat all external inputs as untrusted
- Validate types, ranges, lengths, and formats before processing
- Sanitize data before use in queries, commands, or templates

### Dependency Control

- Do not introduce new third-party dependencies unless explicitly requested/approved
- Prefer Python standard library solutions when reasonable
- When a dependency is necessary, document the reason in the commit message
- Prefer `pathlib.Path` over `os.path` for file system operations
- Prefer `dataclasses` over third-party alternatives for simple structured data

**Version Selection Rule (MANDATORY):** When adding or updating any dependency, always select the latest version that has no known vulnerabilities.

1. **Identify the latest published version** on PyPI.
2. **Scan for vulnerabilities** using `pip-audit` (or `safety check`).
3. **If the latest version is vulnerable**, step back to the most-recent clean version and document why in the commit message.
4. **Never pin an older version out of habit** — always start from latest and work backwards only if forced by a CVE.

```bash
# Install pip-audit
pip install pip-audit

# Scan installed packages for known vulnerabilities
pip-audit

# Or scan a requirements file
pip-audit -r requirements.txt
```

**Commit message example when stepping back from latest:**
```
chore(deps): pin requests to 2.31.0 instead of 2.32.0

2.32.0 has CVE-2024-XXXXX (high severity, no fix yet).
2.31.0 is the latest clean version. Revisit when a patched version is released.
```

See [CODING_PRACTICES.md](./CODING_PRACTICES.md#version-selection-rule-mandatory) for the full cross-ecosystem rule and audit tool table.

### Logging

- Use the `logging` module for all production code (not `print`)
- Logs must be actionable and include relevant context (IDs, operation names, etc.)
- Never log secrets, credentials, or sensitive user data
- Use appropriate log levels (see `CODING_PRACTICES.md` for level guidance)

```python
import logging

logger = logging.getLogger(__name__)

# Good: Structured, actionable log with context
logger.info("Order processed", extra={"order_id": order.id, "total": order.total})

# Good: Error with context for debugging
logger.error("Payment failed for order %s: %s", order.id, error, exc_info=True)

# Bad: print statements in production code
print(f"Processing order {order.id}")

# Bad: Secrets in logs
logger.debug(f"Connecting with token {api_token}")
```

### Determinism and Reliability

- Avoid nondeterminism (time, randomness, concurrency) unless explicitly required and documented
- If randomness is used for deterministic outcomes (tests, reproducible outputs), seed it and document the seed
- Do not silently change behavior (function signatures, return types, exception semantics) -- highlight such changes explicitly in commit messages and PR descriptions

## Official Style Guide
We follow [PEP 8](https://peps.python.org/pep-0008/) and [PEP 257](https://peps.python.org/pep-0257/) with the following project-specific additions and clarifications. Use a formatter (Ruff or Black) to enforce style automatically.

## Project Organization

Follow domain-driven project structure:

```
project/
├── src/
│   └── project_name/
│       ├── __init__.py
│       ├── order/            # Order domain
│       │   ├── __init__.py
│       │   ├── service.py
│       │   ├── repository.py
│       │   └── models.py
│       ├── payment/          # Payment processing
│       ├── inventory/        # Inventory management
│       ├── notification/     # Notification services
│       ├── config/           # Configuration
│       └── common/           # Shared utilities (keep minimal)
├── tests/
│   ├── unit/
│   ├── integration/
│   └── e2e/
├── pyproject.toml
└── README.md
```

**Key Principles:**
- Package by domain (what it does), not by layer (what it is)
- Avoid `utils`, `helpers`, `managers` packages -- they become dumping grounds
- Each package represents a cohesive functional area
- Minimize cross-package dependencies
- Use `src/` layout for installable packages

## Naming Conventions

Following PEP 8:
- **Classes**: PascalCase (`OrderService`, `PaymentProcessor`)
- **Functions/Methods/Variables**: snake_case (`calculate_total`, `order_count`)
- **Constants**: SCREAMING_SNAKE_CASE (`MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
- **Modules**: snake_case (`order_service.py`, `payment_processor.py`)
- **Packages**: lowercase, no underscores preferred (`order`, `payment`)
- **Private**: Single leading underscore (`_internal_method`, `_helper`)
- **Test Files**: `test_` prefix (`test_order_service.py`)
- **Test Functions**: `test_` prefix with descriptive name (`test_should_calculate_total_with_discounts`)

**Naming anti-patterns to avoid:**
- `*Manager`, `*Handler`, `*Helper`, `*Utility` -- usually SRP violations
- `*Impl` suffix -- if there's only one implementation, drop the interface
- Abbreviations -- use `customer` not `cust`, `repository` not `repo`
- Single-letter variables outside comprehensions and lambdas

## Imports

**Rules:**
- No wildcard imports (`from x import *`) -- ever
- Group imports in this order, separated by blank lines:
  1. Standard library
  2. Third-party packages
  3. Local/project imports
- Use absolute imports; avoid relative imports unless within a package's internal modules
- Let Ruff or isort enforce import ordering automatically

```python
# Good: Organized imports
import logging
from pathlib import Path

import httpx
from pydantic import BaseModel

from project.order.models import Order
from project.order.repository import OrderRepository
```

## Modern Python Features (3.12+)

### Type Hints (MUST for Public Interfaces; SHOULD Elsewhere)

All public functions, classes, and methods **must** include type hints. Internal/private code **should** include them.

```python
# Good: Full type annotations
def calculate_score(findings: list[Finding], weights: Weights) -> int:
    total_penalty = sum(penalty_for(f) for f in findings)
    return max(0, 100 - total_penalty)

# Good: Optional values -- prefer T | None over Optional[T]
def find_by_email(email: str) -> User | None:
    return repository.find(email)

# Good: Type aliases (PEP 695)
type ScoreMap = dict[str, int]
type Callback = Callable[[str], None]

# Bad: No type hints
def calculate_score(findings, weights):
    ...
```

**Type hint rules:**
- Prefer `T | None` over `Optional[T]` (they are equivalent; `T | None` is the modern style)
- Avoid `Any` unless unavoidable; if used, add a short comment explaining why
- Use modern generics (`list[str]`, `dict[str, int]`) instead of `typing.List`, `typing.Dict`
- Use `collections.abc` types (`Sequence`, `Mapping`, `Iterable`) in function parameters for flexibility

### Dataclasses and Named Tuples (Use for DTOs and Value Objects)

```python
from dataclasses import dataclass, field

# Good: Immutable value object
@dataclass(frozen=True)
class ScoreResult:
    class_name: str
    score: int
    penalties: tuple[Penalty, ...] = field(default_factory=tuple)

# Good: Mutable when needed, with defaults
@dataclass
class ReportConfig:
    scores: list[Score]
    output_dir: Path
    format: ReportFormat = ReportFormat.MARKDOWN
    include_details: bool = False

# Bad: Plain dict or class with manual __init__
result = {"class_name": "Foo", "score": 85}
```

### Structural Pattern Matching (3.10+)

```python
# Good: Pattern matching for complex dispatch
match result:
    case ParseResult(findings=findings) if findings:
        process(findings)
    case ParseResult(error=error):
        handle_error(error)
    case _:
        raise ValueError(f"Unexpected result: {result}")

# Good: Matching on type and structure
match command:
    case {"action": "create", "name": str(name)}:
        create_resource(name)
    case {"action": "delete", "id": int(resource_id)}:
        delete_resource(resource_id)
```

### Protocols (Structural Subtyping -- Preferred over ABCs)

```python
from typing import Protocol

# Good: Protocol defines the interface structurally
class ScoreCalculator(Protocol):
    def calculate(self, findings: list[Finding]) -> int: ...

class ReportWriter(Protocol):
    def write(self, report: Report, output: Path) -> None: ...

# Any class with matching methods satisfies the protocol -- no inheritance needed
class CheckstyleScorer:
    def calculate(self, findings: list[Finding]) -> int:
        return max(0, 100 - sum(f.penalty for f in findings))

# This works because CheckstyleScorer has a matching `calculate` method
def run_scoring(calculator: ScoreCalculator, findings: list[Finding]) -> int:
    return calculator.calculate(findings)
```

### Abstract Base Classes (When You Need Inheritance)

```python
from abc import ABC, abstractmethod

# Good: ABC for shared behavior with enforced contract
class Payment(ABC):
    @abstractmethod
    def process(self, amount: Decimal) -> PaymentResult: ...

    def validate_amount(self, amount: Decimal) -> None:
        if amount <= 0:
            raise ValueError(f"Amount must be positive: {amount}")

class CreditCardPayment(Payment):
    def process(self, amount: Decimal) -> PaymentResult:
        self.validate_amount(amount)
        # credit card logic
```

### F-Strings

```python
# Good: F-strings for formatting
msg = f"User {name} scored {score} points"
query = f"""
    SELECT name, score
    FROM class_scores
    WHERE score > {threshold}
    ORDER BY score DESC
"""

# Bad: String concatenation or .format()
msg = "User " + name + " scored " + str(score) + " points"
msg = "User {} scored {} points".format(name, score)
```

## Immutability

**Prefer immutable data structures:**
- Use `@dataclass(frozen=True)` for value objects
- Use `tuple` instead of `list` for fixed collections
- Use `frozenset` instead of `set` for fixed sets
- Use `types.MappingProxyType` for read-only dict views
- Return copies from methods to prevent mutation

```python
from dataclasses import dataclass, field

# Good: Immutable
@dataclass(frozen=True)
class OrderSummary:
    order_id: str
    items: tuple[LineItem, ...]
    total: Decimal

# Bad: Mutable with leaky references
class OrderSummary:
    def __init__(self):
        self.items: list[LineItem] = []

    def get_items(self) -> list[LineItem]:
        return self.items  # Leaks mutable reference
```

## Null Safety

**Avoid returning `None` implicitly:**
- Use explicit `T | None` return types
- Use `Optional[T]` for older codebases (pre-3.10)
- Fail fast with descriptive messages
- Use `typing.assert_never` for exhaustive checks

```python
# Good: Explicit None handling
def find_order(order_id: str) -> Order | None:
    return repository.find(order_id)

# Good: Early return with guard clause
def process_order(order_id: str) -> OrderResult:
    order = find_order(order_id)
    if order is None:
        raise OrderNotFoundError(order_id)
    return _process(order)

# Bad: Implicit None return
def find_order(order_id: str) -> Order:
    result = repository.find(order_id)
    if result:
        return result
    # Implicitly returns None but type hint says Order
```

## Function/Method Design

Apply the same standards from CODING_PRACTICES.md:
- **Maximum 20 lines per function** (excluding blanks and docstrings)
- Single responsibility per function
- Maximum 5 parameters (use dataclasses or parameter objects)
- Prefer returning values over mutating state

```python
# Good: Small, focused functions
def calculate_score(findings: list[Finding]) -> int:
    total_penalty = sum(penalty_for(f) for f in findings)
    return max(0, 100 - total_penalty)

# Good: Parameter object when > 3 params
@dataclass(frozen=True)
class ReportConfig:
    scores: list[Score]
    output_dir: Path
    format: ReportFormat = ReportFormat.MARKDOWN
    include_details: bool = False
    locale: str = "en_US"

def generate_report(config: ReportConfig) -> Report: ...

# Bad: Too many parameters
def generate_report(
    scores: list[Score],
    output_dir: Path,
    format: ReportFormat,
    include_details: bool,
    locale: str,
    title: str,
) -> Report: ...
```

## Dependency Injection

**Use constructor injection (via `__init__`):**

```python
# Good: Constructor injection (all dependencies visible, testable)
class OrderService:
    def __init__(
        self,
        repository: OrderRepository,
        processor: PaymentProcessor,
    ) -> None:
        self._repository = repository
        self._processor = processor

    def process(self, order: Order) -> OrderResult:
        self._processor.charge(order.total)
        return self._repository.save(order)

# Bad: Hidden dependencies (hard to test)
class OrderService:
    def process(self, order: Order) -> OrderResult:
        repository = PostgresRepository()  # tight coupling
        processor = StripeProcessor()      # can't inject mocks
        ...
```

## Interface Design

**Prefer focused protocols/ABCs (ISP):**

```python
# Good: Focused interfaces
class ScoreCalculator(Protocol):
    def calculate(self, findings: list[Finding]) -> int: ...

class ReportWriter(Protocol):
    def write(self, report: Report, output: Path) -> None: ...

# Bad: Fat interface
class CodeAnalyzer(Protocol):
    def parse(self, source: Path) -> list[Finding]: ...
    def calculate_score(self, findings: list[Finding]) -> int: ...
    def write_report(self, report: Report, output: Path) -> None: ...
    def send_notification(self, report: Report) -> None: ...
    def load_config(self, config_path: Path) -> Config: ...
```

## Exception Handling

**Best practices:**
- Define domain-specific exceptions inheriting from a base domain exception
- Never catch bare `Exception` unless re-raising
- Use context managers for resource management
- Log at the appropriate level and include context
- Don't use exceptions for control flow

```python
# Good: Domain exception with context
class OrderNotFoundError(Exception):
    def __init__(self, order_id: str) -> None:
        self.order_id = order_id
        super().__init__(f"Order not found: {order_id}")

# Good: Context manager for resources
def read_config(path: Path) -> Config:
    try:
        with path.open() as f:
            return json.load(f)
    except FileNotFoundError:
        raise ConfigError(f"Config file not found: {path}")
    except json.JSONDecodeError as e:
        raise ConfigError(f"Invalid config JSON in {path}: {e}")

# Bad: Swallowing exceptions
try:
    process(data)
except Exception:
    pass  # silently ignored
```

## Collections and Comprehensions

**Prefer functional and idiomatic operations:**

```python
# Good: Comprehensions and generators
scores_by_class = {
    class_name: calculate_score(findings)
    for class_name, findings in groupby(all_findings, key=attrgetter("class_name"))
}

# Good: Generator for lazy evaluation
def penalties(findings: Iterable[Finding]) -> Iterator[int]:
    return (penalty_for(f) for f in findings if f.severity == Severity.HIGH)

# Good: Built-in functions
total = sum(f.penalty for f in findings)
has_errors = any(f.severity == Severity.ERROR for f in findings)
all_valid = all(f.is_valid for f in findings)

# Bad: Imperative with mutation
scores_by_class = {}
for finding in findings:
    # manual grouping logic...
```

**Collection guidelines:**
- Use comprehensions for transformation and filtering
- Use generators for large or lazy sequences
- Use `itertools` for complex iteration patterns
- Break complex comprehensions into named intermediate steps
- Prefer `dict.get()` with defaults over `KeyError` handling

## Context Managers

**Use for resource management:**

```python
# Good: Context manager for cleanup
from contextlib import contextmanager

@contextmanager
def database_connection(url: str) -> Iterator[Connection]:
    conn = connect(url)
    try:
        yield conn
    finally:
        conn.close()

# Usage
with database_connection(db_url) as conn:
    conn.execute(query)
```

## Documentation (Docstrings)

**Requirements (PEP 257 + Google style):**
- All public classes, methods, and functions must have docstrings
- Use Google-style docstrings (superset of PEP 257)
- First line is the summary (imperative mood)
- Include `Args`, `Returns`, `Raises` sections
- Document expected formats, constraints, and important edge cases
- Document raised exceptions if they are part of the function's contract

```python
def calculate_score(findings: list[Finding], weights: Weights) -> int:
    """Calculate the quality score for a class based on code findings.

    The score starts at 100 and decreases based on the severity
    and count of findings. The minimum score is 0.

    Args:
        findings: List of code quality findings.
        weights: Scoring weights for each severity category.

    Returns:
        Score between 0 and 100 inclusive.

    Raises:
        ValueError: If weights contain negative values.
    """
    ...
```

## Testing in Python

**Framework:** pytest

**Mandatory rules:**
- Every change requires tests appropriate to the change
- Bug fixes **must** include a regression test that fails without the fix
- Tests must be deterministic and isolated -- no shared mutable state between tests
- Avoid real network calls or reliance on external systems; use mocks, fakes, or recorded responses
- If randomness is involved, seed it explicitly for reproducibility

**Structure:**
- Use `pytest` fixtures for setup/teardown
- Given-When-Then structure in every test
- Descriptive function names using `test_should_..._when_...` pattern
- Use `pytest.mark.parametrize` for data-driven tests
- Use `unittest.mock` or `pytest-mock` for mocking

```python
def test_should_calculate_correct_score_with_multiple_penalties(scorer):
    # Given
    findings = [
        Finding("Foo.py", line=10, severity=Severity.HIGH),
        Finding("Foo.py", line=20, severity=Severity.LOW),
    ]

    # When
    score = scorer.calculate_score(findings)

    # Then
    assert score == 82


class TestWhenNoFindingsExist:
    def test_should_return_perfect_score(self, scorer):
        assert scorer.calculate_score([]) == 100


@pytest.mark.parametrize(
    "severity, expected",
    [
        (Severity.HIGH, 30),
        (Severity.MEDIUM, 15),
        (Severity.LOW, 5),
    ],
)
def test_should_apply_correct_penalty_per_severity(scorer, severity, expected):
    finding = Finding("Foo.py", line=1, severity=severity)
    assert scorer.penalty_for(finding) == expected
```

**Fixtures:**

```python
@pytest.fixture
def scorer() -> ClassScorer:
    return ClassScorer(weights=Weights.defaults())

@pytest.fixture
def mock_repository(mocker) -> OrderRepository:
    return mocker.Mock(spec=OrderRepository)

@pytest.fixture
def order_service(mock_repository, mock_notifier) -> OrderService:
    return OrderService(repository=mock_repository, notifier=mock_notifier)
```

### Fluent Assertions with assertpy

Use **bare `assert`** for simple scalar equality checks — pytest's assertion rewriting gives good diffs. Use **assertpy** for collections, chaining, soft assertions, and when descriptive failure context is needed.

**When to use which:**
- Bare `assert` — `assert result == 42`, `assert user is not None`
- assertpy — collections, chained checks, soft assertions, failure labels

**Basic fluent chaining:**
```python
from assertpy import assert_that

assert_that('foobar').is_length(6).starts_with('foo').ends_with('bar')
assert_that(42).is_greater_than(0).is_less_than(100)
```

**Collection assertions:**
```python
assert_that(people).extracting('name').contains('Alice', 'Bob')
assert_that(results).is_length(3).contains_only('a', 'b', 'c')
assert_that(scores).is_sorted()
```

**Exception assertions:**
```python
assert_that(my_function).raises(ValueError).when_called_with(bad_input)
```

**Soft assertions** — collect all failures before reporting:
```python
from assertpy import soft_assertions

with soft_assertions():
    assert_that(response.status_code).is_equal_to(200)
    assert_that(response.json()['name']).is_equal_to('Alice')
    assert_that(response.headers['Content-Type']).contains('application/json')
```

**Failure context with `described_as`:**
```python
assert_that(invoice.total).described_as('invoice total after discount').is_equal_to(99.99)
```

## Package and Environment Management

### uv (Recommended — 2025/2026)

`uv` is the recommended Python package and environment manager. It replaces `pip`, `venv`, `pip-tools`, and virtual environment activation in one fast tool.

```bash
# Create and activate a virtual environment
uv venv
source .venv/bin/activate   # or .venv\Scripts\activate on Windows

# Install dependencies from pyproject.toml
uv sync

# Add a dependency
uv add pydantic>=2.0

# Add a dev dependency
uv add --dev pytest>=8.0

# Run a script in the project's venv
uv run pytest

# Lock dependencies (generates uv.lock)
uv lock

# Install a tool globally (e.g., for CI)
uv tool install ruff
```

**When to use uv vs pip:**
- New projects: always use `uv`
- CI pipelines: `uv sync --frozen` for reproducible installs
- Existing projects: `uv` is pip-compatible; switch by running `uv sync` once

## Build Tools and Project Configuration

**Use `pyproject.toml` (PEP 621):**

```toml
[project]
name = "project-name"
version = "1.0.0"
requires-python = ">=3.12"
dependencies = [
    "pydantic>=2.0",
    "httpx>=0.25",
]

[project.optional-dependencies]
dev = [
    "pytest>=8.3",
    "pytest-cov>=6.0",
    "pytest-mock>=3.12",
    "assertpy>=1.1",
    "mypy>=1.11",
    "ruff>=0.6",
]

[tool.pytest.ini_options]
testpaths = ["tests"]
addopts = "-v --strict-markers"

[tool.mypy]
python_version = "3.12"
strict = true
warn_return_any = true
disallow_untyped_defs = true

[tool.ruff]
target-version = "py312"
line-length = 88

[tool.ruff.lint]
select = ["E", "F", "W", "I", "N", "UP", "B", "SIM", "RUF"]
```

## Code Quality Tools

**Recommended toolchain:**
- **Ruff**: Fast linting and formatting (replaces flake8, isort, Black) -- primary recommendation
- **Black**: Acceptable alternative formatter if Ruff formatting is not adopted
- **mypy** or **Pyright**: Static type checking (use `strict = true` with mypy)
- **Bandit**: Security-focused static analysis
- **pytest**: Testing framework
- **uv**: Fast Python package and environment manager (replaces pip/venv/pip-tools)
- **pre-commit**: Hook automation for format check + lint + type check on every commit
- **CI pipeline**: Run format check + lint + type check + tests on each PR

**Same rules apply:**
- 20-line function maximum
- 0-2 private methods per class (SRP guideline)
- 80%+ unit test coverage (pytest-cov, unit tests only -- integration/E2E tests do not count toward coverage)
- No duplicated code

## Common Anti-Patterns to Avoid

### God Classes

```python
# Bad: Class doing too many things
class ApplicationManager:
    def process_order(self): ...
    def send_email(self): ...
    def generate_report(self): ...
    def sync_inventory(self): ...

# Good: Split by responsibility
class OrderProcessor: ...
class EmailNotifier: ...
class ReportGenerator: ...
class InventorySynchronizer: ...
```

### Service Locator (Anti-Pattern)

```python
# Bad: Hidden dependencies
class OrderService:
    def process(self, order: Order) -> None:
        repo = ServiceLocator.get(OrderRepository)  # Hidden!
        repo.save(order)

# Good: Explicit constructor injection
class OrderService:
    def __init__(self, repository: OrderRepository) -> None:
        self._repository = repository
```

### Mutable Default Arguments

```python
# Bad: Mutable default argument (shared across calls)
def add_item(item: str, items: list[str] = []) -> list[str]:
    items.append(item)
    return items

# Good: Use None and create inside
def add_item(item: str, items: list[str] | None = None) -> list[str]:
    if items is None:
        items = []
    items.append(item)
    return items
```

### Bare Except / Too-Broad Except

```python
# Bad: Catches everything including KeyboardInterrupt, SystemExit
try:
    process()
except:
    pass

# Bad: Too broad
try:
    process()
except Exception:
    log.error("Something went wrong")

# Good: Specific exceptions
try:
    process()
except (ConnectionError, TimeoutError) as e:
    log.error(f"Network error during processing: {e}")
    raise ProcessingError(f"Failed to process: {e}") from e
```

## SOLID Principles Notes

Use the guide in `docs/SOLID_PRINCIPLES.md` and apply these Python-specific practices:
- **SRP**: Use dataclasses for focused data carriers; extract responsibilities into separate classes with constructor injection.
- **OCP**: Use Protocols or ABCs with concrete implementations for known type hierarchies; use Strategy pattern (Protocol + implementations) for open extension.
- **LSP**: Protocols help enforce contracts structurally; avoid raising `NotImplementedError` in overrides -- redesign the abstraction instead.
- **ISP**: Python supports multiple inheritance natively; keep Protocols/ABCs small (1-3 methods) and compose them.
- **DIP**: Use constructor injection via `__init__` with Protocol/ABC types; enable test injection by depending on abstractions.

## Design Patterns Notes

Use the catalog in `docs/DESIGN_PATTERNS.md` and apply these Python-specific practices:
- **Strategy**: Prefer Protocols for strategy interfaces -- no inheritance required.
- **Factory Method/Abstract Factory**: Use classmethods or module-level factory functions; avoid complex factory hierarchies.
- **Builder**: Prefer dataclasses with default values and `**kwargs` before introducing a builder.
- **Singleton**: Use module-level instances or `functools.lru_cache`; avoid global mutable state.
- **Decorator/Proxy**: Use composition and Python decorators (`@decorator`); keep wrappers small and focused.
- **Observer**: Use callbacks, signals, or event libraries; avoid tight coupling.

## Async/Await (asyncio)

**Use for I/O-bound operations (if needed):**

```python
import asyncio

# Good: Async for concurrent I/O
async def fetch_all_scores(urls: list[str]) -> list[Score]:
    async with httpx.AsyncClient() as client:
        tasks = [fetch_score(client, url) for url in urls]
        return await asyncio.gather(*tasks)

async def fetch_score(client: httpx.AsyncClient, url: str) -> Score:
    response = await client.get(url)
    response.raise_for_status()
    return Score.from_dict(response.json())
```

**Guidelines:**
- Use `async`/`await` for I/O-bound concurrency (HTTP, database, file I/O)
- Use `asyncio.gather()` for concurrent tasks
- Use `asyncio.TaskGroup` (3.11+) for structured concurrency — prefer over `asyncio.gather()` for new code because TaskGroup propagates exceptions cleanly
- Don't use async for CPU-bound work (use `multiprocessing` instead)
- Prefer `httpx` over `aiohttp` for async HTTP

## Python Version Feature Summary

| Feature | Minimum Python Version | PEP |
|---------|----------------------|-----|
| Dataclasses | 3.7 | 557 |
| Walrus operator (`:=`) | 3.8 | 572 |
| `dict` union (`\|`) | 3.9 | 584 |
| `list[int]` generics in annotations | 3.9 | 585 |
| Structural Pattern Matching | 3.10 | 634 |
| `X \| Y` union types in annotations | 3.10 | 604 |
| `Self` type | 3.11 | 673 |
| Exception Groups / `except*` | 3.11 | 654 |
| `asyncio.TaskGroup` | 3.11 | -- |
| `type` statement (type aliases) | 3.12 | 695 |
| `@override` decorator | 3.12 | 698 |
| Improved f-string parsing | 3.12 | 701 |
| `TypeVar` defaults | 3.13 | 696 |
| `@deprecated` decorator | 3.13 | 702 |
| Improved error messages | 3.13 | -- |
| Free-threaded CPython (experimental) | 3.13 | 703 |

## Python Definition of Done Checklist

Every Python change must satisfy these criteria before it is considered complete. This checklist supplements the language-agnostic checklists in `PRE_COMMIT_CHECKLIST.md` and `AI_AGENT_WORKFLOW.md`.

- [ ] Runs on Python 3.12+
- [ ] Public interfaces have type hints and docstrings
- [ ] No use of `eval`, `exec`, `os.system`, or `pickle` on untrusted input
- [ ] External inputs validated (types, ranges, lengths, formats)
- [ ] No secrets or credentials in code or logs
- [ ] Uses `logging` module (not `print`) for production output
- [ ] No new third-party dependencies without explicit approval
- [ ] Tests added or updated; bug fixes include a regression test
- [ ] Tests are deterministic and isolated (no real network calls)
- [ ] All tests pass, build succeeds, no lint or type-check errors
- [ ] Assumptions and behavior changes are clearly documented
- [ ] Change is small and reviewable (or refactor was explicitly requested)

---

**Last Updated**: 2026-04-30
**Version**: 2.0
**Python Version**: 3.12+ / 3.13+
