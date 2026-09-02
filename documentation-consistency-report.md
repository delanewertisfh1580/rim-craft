# Documentation Consistency Report

**Scope:** `system-overview.md`, `bounded-contexts.md`, `hexagonal-architecture.md`, `ddd-tactical-patterns.md`, `data-dictionaries.md`, `data-interfaces.md`, `event-system-api.md`, `testing-strategy.md`, `definition-of-done-do-d.md`, and related repository Markdown.

## Canonical specification

| Topic | Canonical decision |
|---|---|
| Aggregate terminology | `Citizen` |
| Identity | `CitizenId` |
| Position | `GridPosition`; legacy `Position` is compatibility-only in current Java code |
| Core ports | Target layout is `com.rimworldcraft.core.ports`, with `driving` and `driven` subpackages. Current implementation remains under `com.rimworldcraft.core.api.ports` until Java migration. |
| Story context | `core.storyteller`; existing `core.story` code is legacy compatibility naming |
| Shared kernel | `core.shared` for IDs, time, events, errors, and common value objects |
| Contracts | `core.contracts` for cross-context summaries and integration contracts |
| Config version | Every config document uses positive integer `schemaVersion`. `$schema` is an optional schema URI/path and never a version alias. |
| Config path | Runtime canonical path is `config/rimworldcraft/<context>/...`; repository examples may remain under module resources until runtime wiring. |
| Event envelope | Immutable envelope fields: `eventId`, `eventType`, `occurredAt`, `worldId`, `schemaVersion`, `correlationId`, optional `causationId`, and immutable `payload`. Events are facts, not commands. |
| Active modules | `core-api`, `core-impl`, `infrastructure-common` |
| Forge status | `infrastructure-forge` is planned/inactive until ForgeGradle and mappings are explicitly wired. |

## Contradictions found and resolution

1. **Citizen vs Npc:** system and module documents mixed `Npc`, `NPC`, and `Citizen`. Canonical language is now `Citizen`/`CitizenId`; `Npc` references are legacy terminology and future package migration notes.
2. **Position vs GridPosition:** documents used both names. `GridPosition` is canonical for new contracts; `Position` is retained only as a compatibility bridge.
3. **Ports package:** documents alternated between `core.api.ports`, `core.ports`, `core.ports.driving`, and `core.ports.driven`. The target is `core.ports.driving`/`core.ports.driven`; the current Java location is explicitly marked as migration state.
4. **Story package:** `core.story` and `core.storyteller` both appeared. `core.storyteller` is canonical; `core.story` is legacy code location.
5. **Config locations:** documents referenced `config/rwc/`, `config/rimworldcraft/`, and flat files. `config/rimworldcraft/<context>/` is canonical runtime layout; `config/rwc/` is an existing resource fixture location, not a second specification.
6. **Version fields:** examples consistently use `schemaVersion`, while some prose mentioned generic `version`. `schemaVersion` is the only contract version field; `$schema` identifies the schema resource.
7. **Event metadata:** documents required `eventId`, `occurredAt`, `worldId`, and `schemaVersion`, while other examples also referenced correlation/causation metadata. The canonical envelope includes all of these, with `causationId` optional.
8. **Module names:** links referenced `module-npc.md`, `module-storyteller.md`, `module-world.md`, `module-player.md`, and other files that do not exist. Existing files are authoritative; absent targets are listed below as Planned Documentation.
9. **Forge claims:** runbooks described future client/server smoke tests alongside the current JVM skeleton. Those sections are explicitly future/blocked and must not be read as implemented runtime behavior.

## Canonical package examples

```text
com.rimworldcraft.core
├── shared
│   ├── ids
│   ├── time
│   ├── events
│   └── errors
├── contracts
│   ├── colony
│   ├── citizen
│   ├── storyteller
│   └── world
├── colony
│   ├── domain
│   ├── application
│   ├── port/in
│   ├── port/out
│   └── event
├── citizen
│   ├── domain
│   ├── application
│   ├── port/in
│   ├── port/out
│   └── event
├── storyteller
│   ├── domain
│   ├── application
│   ├── port/in
│   ├── port/out
│   └── event
├── goal
│   ├── domain
│   ├── application
│   ├── port/in
│   ├── port/out
│   └── event
└── building
    ├── domain
    ├── application
    ├── port/in
    ├── port/out
    └── event
```

Current Java packages under `core.api` and the existing `core.npc` implementation are migration state, not a competing target layout.

## Planned Documentation

The following references appear in existing Markdown but have no corresponding file in this repository:

- `domain-model.md`
- `use-cases.md`
- `events-and-messaging.md`
- `minecraft-adapters.md`
- `configuration-reference.md`
- `persistence.md`
- `security-and-multiplayer.md`
- `observability.md`
- `module-colony-manager.md`
- `module-npc.md`
- `module-storyteller.md`
- `module-world.md`
- `module-player.md`
- `module-building-system.md`
- `adr/`

These are intentionally not fabricated in this session. Existing references are planned links, not claims that the documents exist.

## Link verification

A repository-wide scan was performed against Markdown files and the current root file inventory. Existing relative links to the nine source-of-truth documents resolve. References to the files in **Planned Documentation** are intentionally unresolved and are documented above as `PENDING`, rather than silently treated as implemented documentation.

No Java source files were changed for this documentation-only request.

## Documentation status

- **DONE:** canonical terminology and event/config conventions recorded.
- **DONE:** README and implementation status aligned with active modules and current JVM skeleton.
- **DONE:** planned/missing documentation inventory added.
- **PARTIAL:** legacy references remain in historical examples and are explicitly classified; full line-by-line rewrite of very large design documents would risk changing their meaning.
- **PENDING:** create the planned documents as separate design work.
