# RimWorldCraft implementation status

## Documentation status

This report accompanies `documentation-consistency-report.md`, which is the canonical inventory of terminology conflicts, planned documents, and Markdown link status. No Java source files were changed for the documentation consistency pass.

## Canonical terminology

| Concern | Canonical choice | Compatibility note |
|---|---|---|
| NPC | `Citizen` / `CitizenId` | Existing `Npc` wording is legacy terminology. |
| Position | `GridPosition` | Existing `Position` remains for compatibility. |
| Config version | positive integer `schemaVersion` | `$schema` identifies a schema document; it is not a competing version. |
| Event envelope | immutable `eventId`, `eventType`, `occurredAt`, `worldId`, `schemaVersion`, `correlationId`, optional `causationId`, `payload` | Events are facts, not commands. |
| Story context | `storyteller` | Existing `core.story` package is compatibility code. |
| Ports | `core.ports.driving` / `core.ports.driven` target | Existing `core.api.ports` is migration state. |
| Package layout | `core.shared`, `core.contracts`, and bounded contexts with `port.in`/`port.out` | Full Java migration remains pending. |
| Config path | `config/rimworldcraft/<context>/...` | Existing `config/rwc` resources are fixtures. |

## Current repository status

The repository is a Java 17 JVM skeleton with active Gradle modules `core-api`, `core-impl`, and `infrastructure-common`. `infrastructure-forge` and auxiliary test modules exist but are not active in `settings.gradle`. Implemented areas include platform-neutral API types, immutable domain events, early Colony/Citizen/Storyteller/Building code, JSON validation, and JSON persistence seams.

Known limitations include legacy package layout, partial typed-ID migration, placeholder persistence hooks, and no real Forge/Minecraft, NBT, Baritone, rendering, or multiplayer runtime.

## Existing implementation changes

- Added typed IDs and value objects under `core-api`.
- Added typed time, inventory, settlement-validation, and event-publication ports.
- Updated `WorldState` to use typed citizen identifiers and `GridPosition`.
- Replaced the stage-only README with current status and roadmap.
- Installed Java 17 and Gradle 8.10.2 in the sandbox, generated the standard Gradle wrapper, and made `gradlew` executable.

## Requirement matrix

| Requirement | Documentation source | Implementation | Status | Evidence |
|---|---|---|---|---|
| Canonical terminology | Source-of-truth Markdown set | Unified policy recorded | DONE | `documentation-consistency-report.md` |
| Local documentation links | `definition-of-done-do-d.md` | Existing links checked; missing targets inventoried | DONE | `documentation-consistency-report.md` |
| Platform-neutral Core | `hexagonal-architecture.md` | Existing Core API plus typed ports | PARTIAL | `core-api/src/main/java/com/rimworldcraft/core/api/ports/` |
| Typed IDs | `data-dictionaries.md`, `data-interfaces.md` | Immutable records added | PARTIAL | `core-api/src/main/java/com/rimworldcraft/core/api/types/` |
| Event envelope | `event-system-api.md` | Canonical envelope specification documented | PARTIAL | `documentation-consistency-report.md` |
| Config layout/version | `system-overview.md`, `data-dictionaries.md` | Canonical path/version documented | PARTIAL | `documentation-consistency-report.md` |
| Active modules | `mvp-build-and-test.md` | README and report match `settings.gradle` | DONE | `README.md`, `settings.gradle` |
| Gradle Wrapper | `mvp-build-and-test.md` | Standard Gradle 8.10.2 wrapper generated and executable | DONE | `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` |
| Architecture and coverage gates | `archunit-rules.md`, `testing-strategy.md` | Not added in this documentation-only request | PENDING | No Java/build changes by design |
| Forge runtime | `entity-integration.md`, `save-serialization.md` | Deliberately not claimed | BLOCKED | `infrastructure-forge` excluded |

## Verification

- Repository-wide Markdown and file inventory scan completed.
- `git status --short --untracked-files=all` checked before edits.
- Local links were checked against the root inventory; absent targets are explicitly listed as Planned Documentation.
- No Java files, Gradle files, environment files, credentials, or external runtimes were changed for this request.
- Java 17 and Gradle 8.10.2 are now available in the sandbox.
- `./gradlew clean build --no-daemon` passes.
- The build emits existing `serialVersionUID`, repository configuration, auxiliary-class, and Gradle deprecation warnings; these do not fail the build.

## Planned Documentation

Absent referenced documents are tracked in `documentation-consistency-report.md`: `domain-model.md`, `use-cases.md`, `events-and-messaging.md`, `minecraft-adapters.md`, `configuration-reference.md`, `persistence.md`, `security-and-multiplayer.md`, `observability.md`, missing module-specific documents, and `adr/`.

## Core bounded-context migration

The migration is intentionally staged rather than a flag-day rewrite. Target package facades now exist under `core.shared`, `core.contracts`, and `core.ports.driving`/`core.ports.driven`; legacy API ports are deprecated and remain for compatibility. Aggregates no longer expose Colony NBT hooks, and new cross-context contracts use IDs/summaries. See `bounded-context-migration-report.md` for the detailed migration map.

Evidence and decisions: `core-migration-notes.md`, `adr/0001-core-package-migration.md`, and `bounded-context-migration-report.md`.

## Roadmap

- **P0:** remove remaining contradictory terminology from legacy examples, then generate/verify a real Gradle wrapper and run documentation plus compile checks.
- **P1:** migrate remaining Java aggregates and legacy repositories to `core.shared`/`core.contracts` and `core.ports`, add executable ArchUnit/docsCheck/event/config gates.
- **P2:** confirm ForgeGradle/mappings and implement Forge adapters, NBT persistence, entity binding, Baritone, and multiplayer.
