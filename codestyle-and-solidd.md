# Code style, SOLID and design practices

## Required style

- Prefer clear names, small focused methods and explicit domain vocabulary.
- Keep one public top-level type per file where practical.
- Use records/final classes for immutable values and defensive copies for collections.
- Constructor-inject dependencies; avoid service locators, hidden globals and static mutable state.
- Use domain exceptions/results with stable reason codes; do not swallow errors.
- Use project logging seams; no `System.out`, `System.err` or `printStackTrace` in production.
- Use explicit `GameTick`/injected clocks, not wall-clock time in domain logic.
- Bound retries, queues, planner work and async execution; define cancellation/shutdown behavior.

## SOLID application

- **S:** one context/capability per class.
- **O:** policies and adapters are replaceable behind interfaces.
- **L:** implementations preserve port semantics and failure guarantees.
- **I:** prefer narrow context-specific ports.
- **D:** application/domain depend on abstractions, infrastructure depends inward.

## Review checklist

- Correct context and owner of state.
- No new legacy type or duplicate model.
- No foreign aggregate or platform dependency.
- Invariants protected at the aggregate/value-object boundary.
- Error, replay, world-scope and terminal-state paths tested.
- Public contract and relevant documentation updated.

## References

- [`AGENTS.md`](AGENTS.md)
- [`ddd-tactical-patterns.md`](ddd-tactical-patterns.md)
- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`definition-of-done-do-d.md`](definition-of-done-do-d.md)
