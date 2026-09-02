# Storyteller Context compliance report

## Status

**PARTIAL / JVM MVP.** Storyteller Context is implemented as a server-authoritative, platform-neutral decision boundary. It never creates Minecraft entities or mutates Colony/NPC aggregates directly.

## Implemented

- Immutable `Storyteller` aggregate with threat budget, incident cooldowns, pacing, recent history, and applied-outcome idempotency.
- Validated incident definitions with weights, cooldowns, eligibility thresholds, and difficulty scaling inputs.
- Deterministic weighted selection through `RandomPort` with stable definition ordering.
- Population and Colony summary ports; no foreign aggregate imports.
- World snapshot lookup and spawn entry point selection.
- `IncidentDecision` statuses for scheduled, postponed, and not-eligible outcomes.
- `SpawnEntryPointRequest` and `IncidentExecutionIntentPort` as platform-neutral execution boundary.
- Idempotent incident outcome application.
- `StorytellerSnapshotMapper` with JSON `SaveDocument` round-trip and future schema rejection.
- In-memory repository and deterministic tests.

## Evidence

| Requirement | Evidence | Status |
|---|---|---|
| Storyteller aggregate | `core-api/.../storyteller/Storyteller.java` | DONE |
| Threat budget | `ThreatBudget`, `StorytellerApplicationService` | DONE |
| Cooldowns/history/pacing | `IncidentCooldowns`, `IncidentRecord`, `PacingState` | DONE |
| Eligibility and weighted selection | `StorytellerApplicationService`, `StorytellerContextTest` | DONE |
| Difficulty scaling | `IncidentDefinition`, `scaledThreat` | DONE |
| Spawn entry point request | `SpawnEntryPointRequest`, `IncidentExecutionIntentPort` | DONE |
| Postponement/retry window | `IncidentDecision`, `StorytellerConfigSnapshot` | DONE |
| Outcome application/idempotency | `Storyteller.applyOutcome`, test coverage | DONE |
| Persistence state | `StorytellerSnapshotMapper` | DONE |
| Direct Minecraft/entity mutation | No Core dependency; intent only | DONE |
| Production event/bootstrap handlers | Not present in active JVM scope | PENDING |

## Required boundaries

- Storyteller consumes only `StorytellerColonySummary`, `StorytellerPopulationSummary`, and `WorldSnapshot` projections.
- The incident executor receives immutable request data and owns platform materialization.
- Storyteller does not call Colony, NPC, or Minecraft internals.
- `RandomPort` and explicit ticks keep selection deterministic and testable.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

Tests cover seeded weighted selection, pacing and missing-entry postponement, cooldown enforcement, outcome idempotency, snapshot round-trip, and future-schema rejection.

## Remaining gaps

- JSON incident configuration loading and per-context schema integration remain Pending.
- Durable repository adapter, event envelope bootstrap, and real incident executor require the later runtime integration stage.
- Full historical retention/migration matrix and metrics are Planned.
