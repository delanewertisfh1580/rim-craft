# Core migration notes

## Current-to-target map

| Current location | Target location | Migration state |
|---|---|---|
| `core.api.types` | `core.shared` for shared values; context packages for domain values | target facades added; legacy remains |
| `core.api.events` | `core.contracts` or owning context `event` package | pending typed envelope migration |
| `core.api.ports` | `core.ports.driving` / `core.ports.driven` | new typed ports added; legacy ports deprecated |
| `core.colony` | `core.colony.domain` and `core.colony.application` | staged; aggregate remains compatible |
| `core.npc` | `core.npc.domain` and `core.npc.application` | staged; Citizen remains compatible |
| `core.story` | `core.storyteller.domain` and `core.storyteller.application` | pending aggregate move |
| `core.goal` | `core.goal.domain` and `core.goal.application` | pending aggregate move |
| `core.building` | `core.building.domain` and `core.building.application` | pending aggregate move |

## Compatibility rules

- Do not add new raw UUID or legacy `Position` contracts.
- Use `ExternalIdMapper` only at external boundaries.
- New cross-context APIs use typed IDs, summaries, immutable DTOs, events, or public ports.
- Deprecated compatibility interfaces remain until all active adapters migrate.
- Do not add Minecraft/Forge/Baritone dependencies to Core.

## Safe migration order

1. Shared values and contracts.
2. Driving/​​driven port facades.
3. Colony repository and application services.
4. Citizen/NPC application boundaries.
5. Storyteller, Goal, and Building context packages.
6. Remove deprecated facades after adapter and test migration.
