# NPC Context compliance report

## Status

**PARTIAL / JVM MVP.** The canonical term is `Citizen`/`CitizenId`. A platform-neutral NPC domain boundary is implemented without Minecraft, Forge, Baritone, system time, or public setters.

## Implemented

- `Citizen` aggregate with `CitizenStatus` lifecycle and terminal `DEAD` state.
- Immutable `Need`, `NeedState`, `MoodState`, `HealthState`, `TraitSet`, `SkillSet`, `JobAssignment`, `JobAttempt`, and `Schedule`.
- Typed shared identity and world-scoped job positions.
- `ClockPort`, `RandomPort`, `CitizenRepository`, `JobExecutionIntentPort`, and typed event publication ports.
- `DefaultNpcApplicationService` with constructor injection.
- `NpcJobCompletedEvent`; completion is published after persistence and does not mutate Colony inventory.
- Policies: `NeedDecayPolicy`, `MoodPolicy`, `TraitModifierPolicy`, `SkillExperiencePolicy`, and `JobAcceptancePolicy`.
- Unit tests for deterministic decay, mood/traits, skill progression, job lifecycle, incapacitation/death, and world mismatch.

## Boundary guarantees

- Core NPC classes import no Minecraft or infrastructure types.
- Colony inventory is outside NPC ownership; job completion crosses a typed event boundary.
- INCAPACITATED and DEAD citizens reject normal job assignment.
- DEAD is terminal; no recovery or mutation is allowed afterward.
- Simulation ticks are explicit inputs; no wall-clock/system time is read by domain logic.
- State changes use aggregate methods; no public setters are exposed.

## Known limitations

Full configuration loading, relationships, need-environment snapshots, save serialization, event envelope metadata, command idempotency, and concrete infrastructure adapters remain pending. Existing legacy `core.npc.Citizen` compatibility code is not removed in this pass.

## Verification

Expected command:

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

Forge/Minecraft runtime was intentionally not added.
