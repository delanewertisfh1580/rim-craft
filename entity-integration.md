# Entity integration

**Status: PLANNED/BLOCKED.** The repository contains Forge-shaped source under `infrastructure-forge`, but that module is not active and has no Forge dependency in `settings.gradle`.

## Intended boundary

- Core owns `Citizen` and domain state.
- Server runtime entity is a projection/transport anchor, not the aggregate.
- Client entity/rendering is a projection only.
- `CitizenId` is the stable Core binding; runtime entity UUID may change.
- Core emits intents; adapters materialize entities and physical effects.

## Required adapter responsibilities

When Forge/Fabric wiring is enabled, adapters must handle registration, spawn/despawn, ID binding, projection synchronization, packet codecs, server-thread execution, NBT runtime binding and unload/reconnect cleanup.

They must not calculate mood, needs, skills, goals, permissions or authoritative outcomes.

## Network/security rules

- Derive actor identity from the server connection.
- Validate world/dimension, entity existence, ownership, reach/line-of-sight, enum/length/range bounds, rate limits and replay IDs.
- Reject stale revisions.
- Never accept client-provided state as authoritative.
- Never broadcast full private aggregate state.

## Current files

The inactive module is `infrastructure-forge/src/main/java/com/rimworldcraft/infrastructure/forge/`. Do not treat its classes as verified runtime support.

## Activation prerequisites

1. Choose and configure ForgeGradle/mappings or a separate Fabric build.
2. Include the runtime module explicitly.
3. Add loader contract tests and a composition root.
4. Verify server/client lifecycle, entity registration, packets, projection revisions and persistence in a test world.
5. Update status documents only after the module passes those checks.

## References

- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`module-npc-core.md`](module-npc-core.md)
- [`module-player.md`](module-player.md)
- [`pathfinding-layer.md`](pathfinding-layer.md)
- [`implementation-status.md`](implementation-status.md)
