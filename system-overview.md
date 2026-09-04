# System overview

## Scope

RimWorldCraft is a Java 17, platform-neutral JVM skeleton for a future Minecraft mod. The active repository does not contain a runnable Forge/Fabric runtime. Minecraft is an external platform and must remain behind adapters.

## Active architecture

```text
external platform / files
        ↓
Infrastructure adapters
        ↓
Core ports → application services → domain aggregates
        ↓
contracts/events/projections
```

Active Gradle modules are `core-api`, `core-impl`, and `infrastructure-common`. `infrastructure-forge` exists but is not included by `settings.gradle`.

## Context ownership

| Context | Owns | Current evidence |
|---|---|---|
| Colony | membership, resources, lifecycle, building relations | `core-impl/core/colony`, `core-api/core/ports` |
| Citizen/NPC | individual needs, mood, health, traits, skills, jobs | `core-api/core/npc`, `core-impl/core/npc` |
| Goal AI | goals, plans, actions, bounded replanning | `core-api/core/goal`, `core-impl/core/goal` |
| Building | blueprint/build-order state and world-mutation intents | `core-api/core/building`, `core-impl/core/building` |
| World | immutable region/world facts and settlement validation | `core-api/core/world`, `core-impl/core/world` |
| Storyteller | threat budget, cooldowns, incident scheduling | `core-api/core/storyteller`, `core-impl/core/storyteller` |
| Player | membership, permissions, control mode, command receipts | `core-api/core/player`, `core-impl/core/player` |
| Configuration | parsing/validation/publication outside Core | `infrastructure-common/core/config` |
| Persistence | JSON document storage outside aggregates | `infrastructure-common/core/persistence` |

## Non-negotiable boundaries

- Core has no Minecraft, Forge, Fabric, Baritone, filesystem, JSON-library or infrastructure dependency.
- Aggregates own invariants; application services load, call, save and publish.
- Contexts exchange typed IDs, summaries, immutable DTOs, ports or events—not foreign aggregates.
- `Citizen`/`CitizenId` and `GridPosition` are canonical for new APIs.
- Server authority is a future runtime rule; current Player tests validate normalized server-side commands only.

## Current versus target

Current compatibility packages include `core.api.*`, `core.colony`, `core.npc`, `core.story`, `core.goal` and `core.building`. Target packages include `core.shared`, `core.contracts`, `core.ports.driving` and `core.ports.driven`. Migrate incrementally; do not perform a broad package rewrite in an unrelated change.

## Related references

- Status: [`implementation-status.md`](implementation-status.md)
- Agent rules: [`AGENTS.md`](AGENTS.md)
- Context ownership: [`bounded-contexts.md`](bounded-contexts.md)
- Ports/adapters: [`hexagonal-architecture.md`](hexagonal-architecture.md)
- Migration decision: [`adr/0001-core-package-migration.md`](adr/0001-core-package-migration.md)
