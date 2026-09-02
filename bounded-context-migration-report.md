# Bounded-context and hexagonal migration report

## Status

**PARTIAL** — the target Core boundaries and typed contracts are established without adding platform dependencies. Existing aggregates remain in compatibility packages where a flag-day move would break the current skeleton.

## Completed

- Added target package roots:
  - `com.rimworldcraft.core.shared`
  - `com.rimworldcraft.core.contracts`
  - `com.rimworldcraft.core.ports.driving`
  - `com.rimworldcraft.core.ports.driven`
- Added target shared facades for typed identifiers, `GameTick`, `SchemaVersion`, and `GridPosition`.
- Added immutable cross-context summaries:
  - `ColonySummary`
  - `CitizenSummary`
- Added driving ports:
  - `CreateColonyUseCase`
  - `AddCitizenUseCase`
  - `ResourceOperationUseCase`
  - `CreateBuildOrderUseCase`
- Added driven ports:
  - `ColonyRepository` using immutable `ColonyRecord` rather than importing the implementation aggregate
  - `CitizenSummaryPort`
  - `WorldObservationPort`
  - `PathfindingPort`
  - `EventPublicationPort`
  - target `ClockPort`, `InventoryPort`, and `SettlementValidationPort`
- Deprecated legacy `IBlockWorldPort`, `IPathfinderPort`, `IEventBusPort`, and `ISaveLoadPort` with migration guidance.
- Removed persistence/NBT hooks from the `Colony` aggregate.
- Added migration documentation and ADR:
  - `core-migration-notes.md`
  - `adr/0001-core-package-migration.md`
- Updated `implementation-status.md`.

## Dependency boundary check

No Core source imports `net.minecraft`, Forge, Fabric, Baritone, or infrastructure/client packages. The active build remains platform-neutral. No Forge wiring or external coordinates were added.

## Compatibility decisions

The current implementation retains legacy aggregate packages (`core.colony`, `core.npc`, `core.story`, `core.goal`, `core.building`) and raw UUID/`Position` methods in old APIs. These are compatibility boundaries, not target contracts. New code must use typed IDs, `GridPosition`, summaries, immutable DTOs, events, and target ports.

The existing `core.api.types` and `core.api.ports` packages remain during migration. New target shared records delegate explicitly to current API types where required; this avoids breaking existing consumers before adapters and persistence mappings are migrated.

## Verification

Executed from the repository root with Java 17 and Gradle 8.10.2:

```bash
./gradlew clean build --no-daemon
```

Result: **BUILD SUCCESSFUL** for active modules `core-api`, `core-impl`, and `infrastructure-common`.

The build retains non-blocking warnings for exception `serialVersionUID`, deprecated compatibility ports, legacy auxiliary facade classes, repository configuration, and Gradle deprecations.

## Remaining work

| Area | Status | Reason |
|---|---|---|
| Move all aggregates into `.domain` | PARTIAL | requires coordinated API/test migration |
| Add application service implementations | PARTIAL | contracts exist; current behavior remains in legacy services/factories |
| Fully type legacy events/repositories | PENDING | current event hierarchy uses UUID source IDs |
| Remove all raw UUID and legacy Position from Core | PENDING | compatibility consumers and adapters still use them |
| Typed persistence snapshot port | PENDING | Forge/NBT wiring is intentionally absent |
| ArchUnit executable enforcement | PENDING | no ArchUnit dependency currently configured |
| Forge/Minecraft adapters | BLOCKED | requires confirmed ForgeGradle/mappings and runtime wiring |

## Next steps

1. Migrate Colony aggregate and repository implementation to typed IDs and `GridPosition`, preserving overloads temporarily.
2. Introduce a typed immutable event envelope and adapt current events at the publication boundary.
3. Migrate Citizen and Building contracts, then Storyteller and Goal contexts.
4. Add ArchUnit rules after package migration has enough real classes to avoid false positives.
5. Remove deprecated compatibility ports after all active adapters and tests use target ports.
