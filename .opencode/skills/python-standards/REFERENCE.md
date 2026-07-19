# Python Standards — Reference Details

Companion reference for `python-standards/SKILL.md`. Contains detailed examples
and project structure extracted for progressive disclosure.

## Table of Contents

- [Collections and Comprehensions](#collections-and-comprehensions)
- [Async/Await Patterns](#asyncawait-patterns)
- [Domain-Driven Project Structure](#domain-driven-project-structure)
- [Tooling pyproject.toml](#tooling-pyprojecttoml)

---

## Collections and Comprehensions

```python
# ✅ Prefer comprehensions over imperative loops
active_orders = [o for o in orders if o.status == "active"]
total_by_customer = {o.customer_id: o.total for o in orders}

# ✅ Generators for large data (lazy evaluation)
def read_large_file(path: Path) -> Generator[str, None, None]:
    with open(path) as f:
        yield from f

# ✅ Built-in functions
total = sum(o.amount for o in orders)
any_overdue = any(o.is_overdue for o in orders)
```

---

## Async/Await Patterns

```python
import asyncio

# ✅ Structured concurrency (Python 3.11+)
async def process_orders(order_ids: list[str]) -> list[OrderResult]:
    async with asyncio.TaskGroup() as tg:
        tasks = [tg.create_task(process_one(oid)) for oid in order_ids]
    return [t.result() for t in tasks]

# ✅ Always await coroutines — never fire and forget without tracking
result = await fetch_order(order_id)
```

---

## Domain-Driven Project Structure

```
src/
  myproject/
    order/
      domain.py       # Order, OrderLine, OrderStatus (pure domain objects)
      repository.py   # OrderRepository Protocol
      service.py      # OrderService
      exceptions.py
    payment/
      ...
    config/
      settings.py
    common/
      logging.py
tests/
  order/
    test_order_service.py
    test_order_domain.py
  payment/
    ...
pyproject.toml
```

---

## Tooling pyproject.toml

Full recommended `pyproject.toml` configuration:

```toml
[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"

[tool.ruff]
line-length = 100
target-version = "py312"
select = ["E", "F", "I", "N", "UP", "S", "B", "A", "C4", "T20"]

[tool.mypy]
strict = true
python_version = "3.12"

[tool.pytest.ini_options]
testpaths = ["tests"]
```

**Required tools:**
- `ruff` — linting + formatting (primary, replaces flake8/isort/black)
- `mypy` with `strict = true` — type checking
- `bandit` — security scanning
- `pip-audit` — dependency vulnerability scanning
- `pre-commit` hooks enforcing all of the above
