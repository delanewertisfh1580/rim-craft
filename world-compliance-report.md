# World compliance report

**Status: PARTIAL.**

## Implemented

- Immutable world/region snapshots.
- Terrain, climate, hazard, resource, accessibility and bounds facts.
- World/region consistency and cross-world rejection.
- Read-only snapshot port and deterministic settlement validation.
- Focused World Context tests.

## Pending or blocked

Minecraft observation adapter, chunk/region discovery, durable snapshots, weather evolution, event handlers, pathfinding backend and runtime contract suites.

## Boundary guarantee

Core stores normalized facts only. `Level`, chunks, blocks, entities and loader-specific values remain outside Core.
