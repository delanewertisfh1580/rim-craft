# Bounded contexts

## Rule

Each context owns its aggregate state and invariants. Other contexts see only typed IDs, summaries, immutable contracts, public ports or events. Minecraft types never enter Core.

## Context map

| Context | Aggregate/root | Owns | Does not own |
|---|---|---|---|
| Colony | `Colony` | membership, resources, lifecycle, colony policy | Citizen internals, Minecraft blocks |
| Citizen | `Citizen` | needs, mood, health, traits, skills, jobs | colony inventory, entity physics |
| Goal AI | `CitizenAIState`/Goal contracts | goals, plans, actions, task orchestration | Citizen aggregate, world mutation |
| Building | `BuildOrder`, `Blueprint` | construction order, progress, reservation intent | colony inventory implementation, block API |
| World | `WorldSnapshot`/`WorldRegion` | terrain, climate, hazards, accessibility | full Minecraft world |
| Storyteller | `Storyteller` | incidents, threat, cooldown, pacing | entity spawning and foreign aggregates |
| Player | `PlayerProfile` | membership, permissions, control, audit | authentication and colony state |

## Shared contracts

Canonical values live in `com.rimworldcraft.core.shared`: `WorldId`, `ColonyId`, `CitizenId`, `PlayerId`, `RegionId`, `IncidentId`, `CommandId`, `GameTick`, `SchemaVersion`, `GridPosition`. Cross-context summaries live in `core.contracts`. New ports live in `core.ports.driving` and `core.ports.driven`.

`Npc`, `NpcId`, `Position`, `core.api.*` and `core.story` are compatibility terms only.

## Interaction policy

- Use synchronous public query/command ports when the caller needs an immediate result from the owning context.
- Use events for facts consumed by multiple contexts or eventual projections.
- A handler calls its own application boundary; it never mutates a foreign aggregate.
- Event handlers must be idempotent by event/command identity.
- World scope must be present in every positional or persistence contract.
- Ownership examples: Colony owns resources; Citizen owns personal state; Storyteller owns incidents; Player owns authority.

## Current evidence

The JVM implementation has boundaries and focused tests for Colony, Citizen, Goal AI, Building, World, Storyteller and Player. Production cross-context handler bootstrap, durable repositories, network adapters and Minecraft materialization are not active.

## Testing checklist

For a context change, add behavior tests for invariants, contract tests for ports/repositories, and integration tests only when an active adapter exists. Keep acceptance/runtime scenarios as design guidance until the relevant runtime module is enabled.

## References

- [`system-overview.md`](system-overview.md)
- [`implementation-status.md`](implementation-status.md)
- [`hexagonal-architecture.md`](hexagonal-architecture.md)
- [`event-system-api.md`](event-system-api.md)
- [`core-migration-notes.md`](core-migration-notes.md)
