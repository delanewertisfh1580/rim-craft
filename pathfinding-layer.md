# Pathfinding layer

**Status: PLANNED/BLOCKED.** `IPathfinderPort`-style design exists in documentation, but Baritone/Automatone is not configured or active in the build.

## Responsibility

Pathfinding answers whether and how an entity can reach a target. Goal AI decides why to move; entity integration performs movement; pathfinding does not own domain aggregates or goals.

## Core contract requirements

A future path port must use platform-neutral values (`GridPosition`, world-scoped IDs, traversal context and immutable path results) and provide bounded reachability/path requests with:

- same-world validation;
- timeout and cancellation;
- stale-request protection;
- bounded concurrency/queue;
- deterministic mapping for fixed inputs;
- explicit result status (`FOUND`, `NOT_FOUND`, `TIMEOUT`, `CANCELLED`, `FAILED`).

## Adapter rules

- Baritone/Minecraft types stay in Infrastructure.
- Per-request settings are scoped; do not mutate global backend settings unsafely.
- World reads and entity/world mutations obey loader thread rules.
- Fallback backends must preserve the same safety policy and pass shared contract tests.
- No block breaking/placing is implied by a path request.

## Activation prerequisites

1. Confirm artifact coordinates, license, loader and mappings.
2. Add the dependency only to the runtime adapter module.
3. Implement mapper/adapter and bounded request registry.
4. Add fake-port unit tests plus loader integration tests.
5. Measure timeout, queue, cancellation and server-tick impact.

## References

- [`module-goal-ai.md`](module-goal-ai.md)
- [`entity-integration.md`](entity-integration.md)
- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`implementation-status.md`](implementation-status.md)
