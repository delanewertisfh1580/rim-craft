# World Context compliance report

## Status

**PARTIAL / JVM MVP.** World Context is implemented as a platform-neutral immutable snapshot boundary. Minecraft, Forge, Fabric, chunk loading, and block observation adapters remain outside the active JVM modules.

## Implemented

- `WorldRegion` aggregate snapshot scoped by `WorldId` and `RegionId`.
- Immutable terrain, climate, hazard, resource, accessibility, bounds, and spawn-entry facts.
- `WorldSnapshot` publication boundary with region/world consistency checks.
- `WorldSnapshotPort` for read-only snapshot access and settlement validation.
- `WorldContextService` application facade.
- Typed settlement results with explicit reasons for unavailable world, outside region, inaccessible terrain, and excessive hazards.
- World scope validation prevents regions from another `WorldId` entering a snapshot.

## Evidence

| Requirement | Evidence | Status |
|---|---|---|
| WorldRegion and RegionId | `core-api/.../world/WorldRegion.java`, `core-api/.../shared/RegionId.java` | DONE |
| Terrain/climate/hazard/resource facts | `TerrainFacts`, `ClimateFacts`, `HazardFacts`, `ResourceFacts` | DONE |
| Accessibility | `core-api/.../world/Accessibility.java` | DONE |
| Settlement validation | `WorldSnapshotPort.validateSettlement`, `WorldContextTest` | DONE |
| Immutable world snapshots | `WorldSnapshot`, `WorldContextTest.snapshotIsImmutableAndWorldScoped` | DONE |
| WorldId scope | `WorldSnapshot` constructor and validation tests | DONE |
| Minecraft observation adapter | Not present in active JVM scope | PENDING |

## Boundary guarantees

- Core stores facts, not `Level`, `BlockPos`, chunks, entities, or loader-specific objects.
- World snapshot queries are read-only and do not mutate Colony, NPC, or Storyteller aggregates.
- Spawn entry points are immutable positions; materialization is delegated to an external intent/adapter boundary.
- Settlement validation is deterministic for a given snapshot.

## Verification

```bash
./gradlew :core-api:compileJava :core-impl:compileJava --no-daemon
./gradlew :core-api:test :core-impl:test --no-daemon
```

The focused World Context tests cover immutable publication, cross-world rejection, terrain/accessibility/hazard decisions, and out-of-region handling.

## Remaining gaps

- Real Minecraft observation adapter and Forge/Fabric contract suites are blocked until the runtime modules and mappings are explicitly approved.
- Durable world snapshot persistence and event handlers are not yet wired.
- Full region discovery, weather evolution, pathfinding backend integration, and metrics remain Planned.
