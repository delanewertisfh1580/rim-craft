# RimWorldCraft — World Context

## Scope

World Context owns bounded, platform-neutral observations needed by Core: regions, terrain, climate, hazards, resources, accessibility, spawn-entry candidates, and settlement validation. Minecraft remains the owner of the physical world; `Level`, chunks, blocks, and entities never enter Core.

## Canonical model

Package: `com.rimworldcraft.core.world`.

- `WorldRegion` — immutable region snapshot scoped by `WorldId` and `RegionId`;
- `WorldSnapshot` — immutable publication boundary containing same-world regions;
- `RegionBounds` — inclusive coordinate bounds;
- `TerrainFacts`, `ClimateFacts`, `HazardFacts`, `ResourceFacts` — validated facts;
- `Accessibility` — read-only accessibility result;
- `SettlementValidationResult` — typed validation result with a diagnostic reason.

Coordinates use shared `GridPosition`. Every positional query carries `WorldId`; a snapshot from another world is rejected.

## Ports and flow

`WorldSnapshotPort` is the driven Core port. Its adapter supplies immutable snapshots and may use platform observation internally. `validateSettlement` checks, in order:

1. world snapshot availability and world scope;
2. position containment in a region;
3. terrain buildability;
4. region accessibility;
5. hazard severity threshold.

`WorldContextService` is the application facade for snapshot reads and settlement validation.

## Invariants

- bounds have ordered minimum/maximum coordinates;
- climate, hazard, resource, and accessibility values are bounded and validated;
- spawn entry points belong to the region bounds;
- all regions in a `WorldSnapshot` match both its `WorldId` and their map key;
- published snapshots and nested collections are immutable;
- validation has no side effects.

## Configuration and persistence

World configuration and durable snapshot persistence are **Pending**. The current JVM boundary is sufficient for deterministic policy tests; no Minecraft observation adapter or NBT mapping is claimed.

## Evidence

- `core-api/src/main/java/com/rimworldcraft/core/world/`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driven/WorldSnapshotPort.java`
- `core-impl/src/main/java/com/rimworldcraft/core/world/WorldContextService.java`
- `core-impl/src/test/java/com/rimworldcraft/core/world/WorldContextTest.java`
- `world-compliance-report.md`
