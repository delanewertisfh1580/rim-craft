# Bounded-context migration report

## Status

**PARTIAL.** Typed shared values, summaries and target ports are present. Legacy aggregates/packages remain active for compatibility; platform runtime is not wired.

## Confirmed implemented

- `core.shared` typed IDs, `GameTick`, `SchemaVersion`, `GridPosition`.
- `core.contracts` summaries and integration contracts.
- `core.ports.driving` and `core.ports.driven` interfaces.
- Core isolation from Minecraft/Forge/Fabric/Baritone in source.
- JSON `SaveDocument` boundary and target context services.
- JVM tests for Colony, Citizen, Goal AI, Building, World, Storyteller, Player, events and JSON infrastructure.

## Compatibility state

Legacy `core.api.*`, `core.colony`, `core.npc`, `core.story`, `core.goal`, `core.building`, raw UUID and `Position` contracts remain. They are not the target for new code.

## Remaining work

- Move context implementation packages without breaking consumers.
- Finish typed event envelope migration and production handlers.
- Add full context snapshot mappers/repositories.
- Add Forge/NBT/network/pathfinding adapters after platform decision.
- Fix `ArchitectureTest.java` API mismatch. The earlier claim that ArchUnit was not configured is obsolete: ArchUnit is configured, but its test source currently fails to compile.

## Verification

```text
./gradlew :core-api:test :infrastructure-common:test --no-daemon  → BUILD SUCCESSFUL
./gradlew check --no-daemon                                      → FAILED in ArchitectureTest compilation
```
