# RimWorldCraft

RimWorldCraft is currently a **platform-neutral Java 17 JVM skeleton** for a future Minecraft/Forge mod. The repository establishes domain contracts and early aggregates without claiming a working Minecraft runtime.

## Active Gradle modules

The default build includes:

- `core-api` — immutable API types, domain events, handlers, and platform-neutral ports.
- `core-impl` — early Colony, Citizen, Storyteller, Goal, and Building implementations plus unit tests.
- `infrastructure-common` — JSON/config validation, JSON save/repository adapters, and logging seams.

`infrastructure-forge`, `test-common`, `test-core`, and `test-integration` are present in the repository but are not active in `settings.gradle`; Forge wiring is intentionally pending confirmed ForgeGradle/mappings coordinates.

## Canonical terminology

- NPC aggregate: `Citizen`; identifier: `CitizenId`.
- Position: `GridPosition` for new contracts; legacy `Position` remains for compatibility.
- Shared value objects live in `com.rimworldcraft.core.api.types` until the planned package migration to `core.shared`.
- Configuration envelope uses positive integer `schemaVersion`; `$schema` is metadata, not a second version field.
- Storyteller terminology is canonical; the legacy `core.story` package remains a compatibility location for now.

## Implemented now

- Immutable domain events and platform-neutral ports.
- Validated shared identifiers: `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `RegionId`, `IncidentId`, `ContentId`, `CommandId`.
- `GameTick`, `SchemaVersion`, and canonical `GridPosition` value objects.
- Early Colony/Citizen/Storyteller/Building aggregates and tests.
- JSON syntax/schemaVersion validation and JSON save seams.
- Java 17 toolchain configuration.

## Known stubs and boundaries

Forge/Minecraft entities, NBT round-tripping, Baritone pathfinding, multiplayer packets, rendering, mod metadata, and runtime launch tasks are **Pending/Blocked** until platform wiring is explicitly requested and dependency/mappings are confirmed. The current code must not be described as a complete Forge mod.

## Build and test

Use the committed wrapper when available:

```bash
./gradlew clean build --no-daemon
./gradlew test --no-daemon
./gradlew architectureTest --no-daemon
./gradlew jacocoTestReport --no-daemon
```

The environment used for this stabilization did not provide a system Gradle executable, so wrapper generation/verification may remain a delivery gap if the wrapper cannot be bootstrapped locally.

## Roadmap

- **P0:** commit/verify Gradle 8 wrapper, split multi-public-type files, stabilize typed repositories and atomic resource reservation, add safe architecture/config/event checks.
- **P1:** migrate packages to `core.shared` and bounded-context `port.in`/`port.out`, add complete contract tests and coverage baselines.
- **P2:** confirm ForgeGradle and mappings, then implement infrastructure-forge wiring, NBT persistence, entity binding, Baritone integration, and multiplayer validation.

See `implementation-status.md` for the evidence-based status matrix and limitations.
