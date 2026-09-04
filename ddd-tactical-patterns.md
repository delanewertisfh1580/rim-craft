# DDD tactical patterns

## Pattern ownership

| Pattern | RimWorldCraft use | Rule |
|---|---|---|
| Entity | `Citizen`, `Colony`, `BuildOrder`, `Storyteller` | stable identity and lifecycle |
| Value object | IDs, `GridPosition`, `GameTick`, state values | immutable, validated, value equality |
| Aggregate | context root plus owned state | one consistency boundary; root-only mutation |
| Factory | Citizen/Blueprint/BuildOrder/Incident creation | create valid state; no persistence side effects |
| Repository | context-owned driven interface | load/save aggregate or snapshot contract; no null |
| Domain service | policies/algorithms crossing objects | stateless or explicit bounded state |
| Domain event | post-transition fact | immutable, versioned, idempotent consumers |

## Invariants

- Aggregates reject invalid transitions themselves.
- Public APIs expose behavior methods, not public setters or mutable collections.
- Cross-aggregate references use IDs/summaries, never object graphs.
- Repository access belongs in application services.
- Events are published after successful state transition/persistence boundary.
- A value object must validate its own range/null/format constraints.

## Current code mapping

- Shared IDs and coordinates: `core-api/.../core/shared`.
- Citizen domain: `core-api/.../core/npc/domain`.
- Colony/building implementations: `core-impl/.../core/colony`, `core/building`.
- Goal AI: `core-api/.../core/goal` plus `core-impl/.../core/goal`.
- World/Storyteller/Player contracts: `core-api/...` with application services in `core-impl`.
- Compatibility aggregates and events remain under `core.api.*` and broad packages.

## Anti-patterns to avoid

- Minecraft types in Core.
- Repository/file access inside aggregates.
- A second copy of Citizen or Colony state in another context.
- Mutable singleton/global state.
- Event handlers that directly mutate foreign aggregates.
- Unbounded retries, queues, planner depth or history.
- Treating design examples as implemented APIs.

## Test expectations

Test behavior and failure paths: invalid values, duplicate membership, terminal states, insufficient resources, replayed commands/events, world mismatch, planning limits and persistence recovery. Use fake ports and deterministic time/random.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`data-interfaces.md`](data-interfaces.md)
- [`event-system-api.md`](event-system-api.md)
- [`testing-strategy.md`](testing-strategy.md)
- [`codestyle-and-solidd.md`](codestyle-and-solidd.md)
