# Core migration notes

## Current → target

| Current compatibility location | Target | Status |
|---|---|---|
| `core.api.types` | `core.shared` or owning context | PARTIAL |
| `core.api.ports` | `core.ports.driving/driven` | PARTIAL; target ports already exist |
| `core.api.events` | `core.contracts`/owner event package | PLANNED |
| `core.colony`, `core.npc` | `.domain` + `.application` | PARTIAL |
| `core.story`, `core.goal`, `core.building` | canonical context packages | PARTIAL/PLANNED |

## Rules

- New APIs use typed IDs, `GridPosition`, immutable DTOs and target ports.
- Do not add raw UUID/legacy `Position` contracts or Minecraft dependencies to Core.
- Use boundary mappers for compatibility; do not mass-rewrite consumers in a feature change.
- Aggregates do not serialize themselves or call repositories.
- Keep compatibility overloads until adapters, persistence and tests migrate.

## Safe order

1. Shared values and contracts.
2. Driving/driven ports.
3. Repository/application services.
4. Context aggregate packages.
5. Typed events and persistence mappers.
6. Remove deprecated compatibility only after all consumers migrate.

Exit requires green compile/tests, no active deprecated-port consumers, and updated `AGENTS.md`/status matrix.
