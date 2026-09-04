# NPC compliance report

**Status: PARTIAL.**

## Implemented evidence

- `Citizen` aggregate and lifecycle in `core.npc.domain`.
- Immutable needs, mood, health, traits, skills, schedule and job values.
- Deterministic policy/application seams with injected ports.
- Terminal `DEAD` state and incapacitation guards.
- Job execution intent and typed job-completion event boundary.
- Focused NPC tests for lifecycle, policies and world mismatch.

## Boundary guarantees

Core NPC code does not import Minecraft or Infrastructure. Colony inventory remains outside NPC ownership. Domain logic uses explicit ticks, not wall-clock time, and exposes no public setters.

## Pending

Full config loading, relationships, environment snapshots, durable snapshot mapping, event envelope/idempotency, runtime entity adapter and production cross-context handlers.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

Use the focused module test after the architecture test source is fixed; the current full `core-impl:test` compilation is blocked by that source/API mismatch.
