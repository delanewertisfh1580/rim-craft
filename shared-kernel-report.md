# Shared Kernel implementation report

## Status

**PARTIAL** — the canonical Shared Kernel value objects and boundary mapper are implemented and tested, while the existing skeleton still has legacy aggregate and repository APIs that require a staged migration.

## Canonical terminology

- `Citizen` / `CitizenId`
- `GridPosition`
- `com.rimworldcraft.core.api.types` as the current shared Core API location
- `core.api.ports` as current compatibility ports package; `core.ports` remains the target architecture
- positive `SchemaVersion`
- non-negative `GameTick`
- namespaced `ContentId` in `namespace:path` format
- `core.api.types.Gender` as the sole gender enum

## Implemented value objects

| Type | Implementation | Validation |
|---|---|---|
| `WorldId` | Java 17 record | non-null UUID |
| `ColonyId` | Java 17 record | non-null UUID |
| `CitizenId` | Java 17 record | non-null UUID |
| `PlayerId` | Java 17 record | non-null UUID |
| `RegionId` | Java 17 record | non-null UUID |
| `IncidentId` | Java 17 record | non-null UUID |
| `CommandId` | Java 17 record | non-null UUID |
| `ContentId` | Java 17 record | namespaced `namespace:path` pattern |
| `GameTick` | Java 17 record | value >= 0 |
| `SchemaVersion` | Java 17 record | value > 0 |
| `GridPosition` | Java 17 record | immutable coordinates; null-safe distance |

All records are immutable and receive Java-generated equality/hash-code semantics. No Minecraft, Forge, Fabric, or Baritone type is imported by these classes.

## Boundary mapping

`ExternalIdMapper` maps external UUID values to typed identifiers and parses UUID strings at the outer boundary. This keeps raw UUID handling localized while allowing the current legacy aggregate APIs to migrate incrementally.

## Duplicate models

The duplicate `core.npc.Gender` enum was removed as a type declaration. Core implementations now use `core.api.types.Gender`. The legacy `Position` record remains only as an explicit compatibility bridge to `GridPosition`; removing it would be a breaking migration of existing aggregate and test APIs.

## Tests

`SharedKernelValueObjectsTest` covers:

- valid construction and equality;
- null rejection for UUID identifiers;
- blank/unnamespaced `ContentId` rejection;
- negative `GameTick` and non-positive `SchemaVersion` rejection;
- immutable position translation and distance;
- world-scope inequality and external UUID mapping.

## Build verification

Executed with Java 17 and Gradle 8.10.2:

```text
./gradlew clean test --no-daemon
```

The initial run exposed two real issues, both fixed:

1. `core-api` lacked JUnit 5 test dependencies.
2. `CitizenFactory` resolved the removed duplicate NPC-local `Gender` instead of the canonical shared type.

Final verification passed after the implementation and report updates. Existing compiler warnings about exception serialization and legacy compatibility facades remain non-blocking.

## Remaining migration work

- Replace raw UUIDs in aggregate implementations, events, and legacy repository facades with typed IDs plus compatibility overloads.
- Move shared types from `core.api.types` into the eventual `core.shared` package only with a coordinated package migration.
- Convert new ports from `Position` to `GridPosition` and add typed identity parameters.
- Introduce a typed event envelope without breaking current `DomainEvent` consumers.
- Add world scope to legacy repository interfaces currently lacking it.

These items are deliberately not solved by mass rewriting: they need coordinated aggregate, adapter, persistence, and test migrations.
