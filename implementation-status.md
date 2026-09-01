# RimWorldCraft implementation status

## Summary

This session inspected the repository and preserved the clean starting Git state. The codebase is a JVM skeleton with Java 17 configuration, early domain aggregates, platform-neutral API contracts, JSON infrastructure, and separate inactive Forge sources.

## Canonical terminology

| Concern | Canonical choice | Compatibility note |
|---|---|---|
| NPC | `Citizen` / `CitizenId` | Existing `Npc` wording in documentation is legacy terminology. |
| Position | `GridPosition` | Existing `Position` remains to avoid a breaking migration. |
| Config version | positive integer `schemaVersion` | `$schema` identifies a schema document; it is not a competing version. |
| Story context | `storyteller` | Existing `core.story` package is a compatibility location. |
| Ports | `core.ports` target | Existing `core.api.ports` is retained during migration. |
| Package layout | `core.shared`, `core.contracts`, and bounded contexts with `port.in`/`port.out` | Full package move is pending because the current skeleton has broad API coupling. |

## Active modules

`settings.gradle` includes `core-api`, `core-impl`, and `infrastructure-common`. `infrastructure-forge` is intentionally excluded because it requires confirmed ForgeGradle and Minecraft mappings. The auxiliary test modules are also not active in the root build.

## Changes

- Added typed immutable value objects: `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `RegionId`, `IncidentId`, `ContentId`, `CommandId`, `GameTick`, `SchemaVersion`, and `GridPosition`.
- Added typed `ClockPort`, `TimePort`, `InventoryPort`, `SettlementValidationPort`, and `TypedEventPublicationPort` seams.
- Removed the `Object inventoryPort` dependency from `ColonyValueCalculator`.
- Made `WorldState` use `GridPosition` and typed citizen identifiers rather than raw UUIDs.
- Replaced the stage-only README with current module, capability, limitation, and roadmap information.
- Converted the obsolete duplicate Forge facade into a marker; dedicated adapter files remain the future platform seam.
- Added Gradle 8.10.2 launcher configuration. The binary wrapper JAR cannot be generated in this environment because neither Gradle nor Java is installed.

## Requirement matrix

| Requirement | Documentation source | Implementation | Status | Evidence |
|---|---|---|---|---|
| Preserve platform-neutral Core | `hexagonal-architecture.md`, `entity-integration.md` | Existing Core API plus typed ports | PARTIAL | `core-api/src/main/java/com/rimworldcraft/core/api/ports/` |
| Canonical typed IDs | `data-dictionaries.md`, `data-interfaces.md` | Immutable records added | PARTIAL | `core-api/src/main/java/com/rimworldcraft/core/api/types/` |
| Canonical position | `pathfinding-layer.md` | `GridPosition` added; legacy `Position` retained | PARTIAL | `GridPosition.java`, `Position.java` |
| Deterministic time seam | `module-npc-core.md`, `testing-strategy.md` | Typed clock/time ports added | PARTIAL | `ClockPort.java`, `TimePort.java` |
| Typed inventory boundary | `codestyle-and-solidd.md`, `module-colony.md` | `InventoryPort`; calculator no longer accepts `Object` | PARTIAL | `InventoryPort.java`, `CoreServices.java` |
| Active Gradle module alignment | `mvp-build-and-test.md` | Existing settings retained and documented | DONE | `settings.gradle`, `README.md` |
| Gradle Wrapper 8.x | `mvp-build-and-test.md` | Launcher/properties added; wrapper JAR unavailable | BLOCKED | `./gradlew clean build --no-daemon` → permission denied; `gradle --version` → command not found |
| ArchUnit rules | `archunit-rules.md` | Not added without an executable Gradle baseline | PENDING | No `ArchitectureTest` added |
| JaCoCo thresholds | `testing-strategy.md`, `configuration-mutation-testing.md` | Not added without build verification | PENDING | No JaCoCo task added |
| Config validation | `configuration-mutation-testing.md` | Existing JSON/schemaVersion validator retained | PARTIAL | `ConfigValidator.java`, existing tests |
| Typed EventBus contract | `event-system-api.md` | Existing `IEventBusPort` retained; contract test pending | PARTIAL | `IEventBusPort.java` |
| Forge/Minecraft wiring | `entity-integration.md`, `save-serialization.md`, `mvp-build-and-test.md` | Deliberately not connected | BLOCKED | `infrastructure-forge/` excluded from settings |

## Verification results

- Repository scan completed: Java sources, tests, Gradle files, resources, and all listed Markdown sources were inspected.
- `git status --short --untracked-files=all`: clean before edits.
- `gradle --version`: unavailable (`gradle: not found`).
- `java -version` and `javac -version`: unavailable.
- `./gradlew clean build --no-daemon`: could not start because the newly written launcher is not executable in this file-tool environment; the required `gradle-wrapper.jar` is also unavailable.
- `test`, `architectureTest`, and `jacocoTestReport` were not claimable or run because the build toolchain is unavailable.
- No Minecraft/Forge server, deploy, external credentials, or environment files were touched.

## Known limitations

The existing skeleton still contains legacy package layout, several raw UUID APIs, placeholder persistence hooks, and broad documentation references to planned files. Those areas require a compile-verified migration; blindly moving them without Gradle execution would risk breaking existing functionality. The wrapper properties and shell launcher are present, but a standard binary `gradle-wrapper.jar` must be generated by Gradle 8.x on a Java-enabled machine.

## Next plan

- **P0:** run `gradle wrapper --gradle-version 8.10.2` on a Java/Gradle-enabled machine; split remaining multi-public-type files; compile and fix API inconsistencies; add focused value-object tests.
- **P1:** migrate `core.api` to `core.shared`/`core.contracts` and bounded-context ports; add ArchUnit, docsCheck, event bus contract tests, and JaCoCo reporting with an explicit skeleton baseline.
- **P2:** confirm ForgeGradle/mappings and implement Forge adapters, NBT save mapping, entity binding, Baritone pathfinding, and multiplayer/server authority checks.

## Unresolvable without Forge wiring

Actual Minecraft entity registration, Forge lifecycle integration, `CompoundTag` persistence round-trip, Baritone execution, renderer/model integration, client/server packet handling, multiplayer smoke tests, and Forge-specific CI tasks require external platform dependencies and mappings and remain BLOCKED.
