---
name: python-standards
description: >
  Use when writing, reviewing, or setting up tooling for Python code — Python-specific
  conventions for Python 3.12+/3.13+: mandatory safety/security rules (banned functions,
  subprocess safety, secrets, input validation), dependency control, logging, type hints,
  dataclasses, Protocols, pattern matching, ABCs, pytest testing with fixtures and
  parametrize, async/await, uv package manager, pyproject.toml, Ruff, mypy strict mode, and SOLID/design
  pattern idioms.
triggers:
  - "writing new python code with safety standards"
  - "reviewing python pull requests for style and safety"
  - "setting up python project tooling and type checks"
  - "auditing python code for banned unsafe patterns"
  - "writing pytest tests with fixtures and parametrization"
  - "setting up python project with uv package manager"
not_for:
  - "non python codebases without python specific concerns"
  - "react frontend or browser automation guidance"
  - "security only review where broader security skill leads"
---

# Python Standards (Python 3.12+/3.13+)

## Table of Contents

- [Use when](#use-when)
- [Mandatory Safety Rules](#mandatory-safety-rules)
- [Type Hints & Null Safety](#type-hints--null-safety)
- [Immutability & Dataclasses](#immutability--dataclasses)
- [Function Design](#function-design)
- [Testing (pytest)](#testing-pytest)
- [Tooling (uv, Ruff, mypy)](#tooling-uv-ruff-mypy)

## Use when

- Writing new Python 3.12+/3.13+ code
- Reviewing Python code for safety, idiomatic usage, or type issues
- Configuring pyproject.toml, Ruff, mypy, or uv
- Writing pytest tests with fixtures and parametrize

## Not for

- Non-Python codebases with no Python-specific concerns
- React frontend or browser automation guidance
- Security-only reviews where the `security-standards` skill leads

---

## Mandatory Safety Rules

**HARD BLOCK — never use:**

| Banned | Safe Alternative |
|--------|-----------------|
| `eval()` / `exec()` | Never execute dynamic strings |
| `pickle` for untrusted data | `json` or `msgpack` |
| `shell=True` in subprocess | `subprocess.run([...])` with list args |
| `os.system()` | `subprocess.run()` |
| `input()` in production code | Parse argv or config files |
| String SQL concatenation | Parameterized queries / ORM |

**Secrets:** Never hardcode. Load from env vars or a secrets manager. Never commit `.env`.

**Input validation:** Use `pydantic` (or equivalent) at every external boundary.

## Type Hints & Null Safety

- Type hints on all public function signatures (Python 3.12+ generic syntax: `list[str]`, `dict[str, int]`)
- `X | None` over `Optional[X]`; `X | Y` over `Union[X, Y]`
- Use `mypy --strict`; fix all errors before committing
- No `# type: ignore` without a justification comment

## Immutability & Dataclasses

- `@dataclass(frozen=True)` for value objects; `@dataclass` for mutable models
- `tuple` / `frozenset` when collection must be immutable
- Class variables: `ClassVar[T]` annotation
- Never mutate function arguments

## Function Design

- Max 20 lines per function; max 5 parameters — use a `@dataclass` parameter object when exceeded
- Constructor injection only; no module-level globals for dependencies
- Use `Protocols` for structural subtyping over concrete ABC inheritance where flexibility matters

## Exception Handling

- Domain-specific exceptions extending `Exception` (not `BaseException`)
- Context managers (`with`) for all resources; never bare `except:` or `except Exception:` without logging
- `logging.exception()` in handlers to preserve tracebacks

## Testing (pytest)

- Given-When-Then structure; descriptive names (`test_should_do_x_when_y`)
- `@pytest.fixture` for setup; `@pytest.mark.parametrize` for data-driven tests
- Mock only at system boundaries (HTTP, DB, file I/O) — use `pytest-mock`
- Min 80% unit test coverage; 100% for critical paths

→ [Full test example](./REFERENCE.md#testing-pytest)

## Tooling (uv, Ruff, mypy)

- **uv**: dependency management (`uv add`, `uv sync`, `uv run`)
- **Ruff**: linting + formatting (replaces flake8, isort, black)
- **mypy**: strict type checking (`mypy --strict`)
- `pyproject.toml` as single config source; no `setup.py`

## Definition of Done

- [ ] No `eval`, `exec`, `pickle` on untrusted data, `shell=True`, or hardcoded secrets
- [ ] Type hints on all public APIs; `mypy --strict` passes
- [ ] `ruff check .` and `ruff format --check .` pass with zero errors
- [ ] pytest passes; coverage ≥ 80%
- [ ] `with` blocks for all resources; no bare `except:`
- [ ] Input boundaries validated with `pydantic` or equivalent
