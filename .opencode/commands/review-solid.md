---
description: Review code for SOLID principle violations
agent: standards-review
subtask: true
---

Review the following code for SOLID principle violations.

Target: $ARGUMENTS

Check for all five SOLID principles:
1. **SRP** - Single Responsibility: Classes with multiple reasons to change, > 2 private methods, God classes
2. **OCP** - Open/Closed: switch/when on types, hard-coded instantiation, if-else chains on object types
3. **LSP** - Liskov Substitution: UnsupportedOperationException in subclasses, type checking before casting
4. **ISP** - Interface Segregation: Fat interfaces (> 5 methods), empty interface method implementations
5. **DIP** - Dependency Inversion: Direct instantiation without injection, concrete class dependencies

Also check code quality metrics:
- Method length (language-specific limit, typically 15-20 lines; Go: 25 lines)
- Class length (max 300 lines)
- Parameter count (max 5)
- Duplicated code

Reference `docs/PRE_COMMIT_CHECKLIST.md` and `docs/CODING_STANDARDS.md` for detailed examples.

Provide a structured report with severity levels (Critical, Warning, Info) and specific fix recommendations.
