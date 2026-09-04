# World context

**Status: PARTIAL.** The JVM model provides immutable world observations and settlement validation. Minecraft observation, chunk lifecycle and durable world persistence are not active.

## Ownership

World owns normalized region facts: terrain, climate, hazards, resources, accessibility, bounds and spawn candidates. Minecraft owns the physical `Level`, chunks, blocks and entities.

## Current code

- Models: `core-api/src/main/java/com/rimworldcraft/core/world/`.
- Service: `core-impl/src/main/java/com/rimworldcraft/core/world/WorldContextService.java`.
- Port: `core.ports.driven.WorldSnapshotPort`.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/world/WorldContextTest.java`.

## Implemented behavior

- Immutable `WorldRegion` and `WorldSnapshot`.
- Bounds, climate, terrain, hazard, resource and accessibility validation.
- World/region key consistency checks.
- Cross-world rejection.
- Deterministic, side-effect-free settlement validation.

## Not complete

Minecraft/Forge/Fabric observation adapter, region discovery, weather evolution, pathfinding backend, durable snapshots, event handlers and metrics.

## Agent constraints

Keep `Level`, `BlockPos`, chunks, entities and loader types outside Core. Pass `WorldId` with every positional/persistence operation.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`world-compliance-report.md`](world-compliance-report.md)
