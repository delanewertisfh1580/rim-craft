# NPC Core

**Status: PARTIAL.** The platform-neutral `Citizen` domain boundary is implemented in `core-api`; application orchestration and focused tests are in `core-impl`. Runtime entity integration and full content loading are not active.

## Ownership

`Citizen` owns individual identity, lifecycle, needs, mood, health, traits, skills, schedule, job assignment and job attempts. Colony owns membership/resources; Goal AI owns plans/actions; Minecraft owns runtime entity physics.

## Current code

- Domain: `core-api/src/main/java/com/rimworldcraft/core/npc/domain/`.
- Policies: `core-api/.../core/npc/application/`.
- Application service: `core-impl/.../core/npc/application/DefaultNpcApplicationService.java`.
- Ports: `core.ports.driven` and `core.ports.driving`.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/npc/`.

## Implemented behavior

- Typed `CitizenId`/`WorldId` boundary.
- Immutable need, mood, health, trait, skill, schedule and job models.
- `ACTIVE`, `INCAPACITATED`, `DEAD` lifecycle; `DEAD` is terminal.
- Deterministic tick input and injected clock/random seams.
- Job acceptance, completion intent and typed completion event boundary.
- No direct Colony inventory mutation and no Minecraft dependency.

## Not complete

Relationships, environment snapshots, full JSON config hydration, full persistence mapping, event envelope bootstrap, command idempotency, durable repository and Forge entity adapter.

## Agent constraints

Do not import `Entity`, `Level`, `BlockPos`, `ItemStack` or NBT into Core. Do not add a second NPC aggregate. Use `CitizenId`, summaries, job intents and owning-context ports.

## Tests

Preserve tests for ranges, deterministic decay/mood/skills, job lifecycle, incapacitation/death and world mismatch. Add integration tests only after a runtime adapter is active.

## References

- [`bounded-contexts.md`](bounded-contexts.md)
- [`entity-integration.md`](entity-integration.md)
- [`module-goal-ai.md`](module-goal-ai.md)
- [`npc-compliance-report.md`](npc-compliance-report.md)
