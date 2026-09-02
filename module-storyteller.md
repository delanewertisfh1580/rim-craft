# RimWorldCraft — Storyteller Context

## Scope

Storyteller Context owns narrative pressure and incident scheduling. It reads immutable Colony/NPC summaries and World snapshots, makes deterministic decisions, and emits intents. It never creates Minecraft entities or mutates Colony/NPC aggregates directly.

## Canonical model

Package: `com.rimworldcraft.core.storyteller`.

- `Storyteller` — aggregate state for threat budget, cooldowns, pacing, recent incidents, and applied outcomes;
- `IncidentDefinition` — validated configuration candidate;
- `ThreatBudget`, `IncidentCooldowns`, `PacingState`, `IncidentRecord` — immutable state values;
- `IncidentDecision` — `SCHEDULED`, `POSTPONED`, or `NOT_ELIGIBLE`;
- `SpawnEntryPointRequest` — platform-neutral execution intent;
- `IncidentOutcome` — typed result from an external executor.

The aggregate is scoped by `WorldId` and `StorytellerId`. Incident selection uses stable ID ordering before applying `RandomPort`, so a fixed random source produces reproducible results.

## Application boundaries

`StorytellerApplicationService`:

1. loads or creates the world-scoped aggregate;
2. reads `StorytellerColonySummary` and `StorytellerPopulationSummary` projections;
3. filters population, threat, cooldown, and world-hazard eligibility;
4. performs weighted deterministic selection;
5. scales threat from population and wealth;
6. selects a valid immutable world entry point;
7. persists the aggregate and submits `SpawnEntryPointRequest`.

External materialization is performed by `IncidentExecutionIntentPort`. Results return through `applyOutcome`; repeated outcomes are idempotent.

## Invariants

- incident weights and threat values are non-negative/positive as appropriate;
- threat spending cannot exceed the available budget;
- cooldowns block premature repeats;
- history is bounded by configured retention;
- incidents without a valid world entry point are postponed with a retry tick;
- unknown outcomes are rejected;
- the same outcome cannot restore budget twice;
- Storyteller has no dependency on Colony/NPC aggregate implementations or platform types.

## Configuration and persistence

`StorytellerConfigSnapshot` is immutable and constructor-injected. `StorytellerSnapshotMapper` persists stable state through `SaveDocument` and rejects future schema versions. JSON incident loading, durable runtime repository, event bootstrap, and Minecraft spawn adapter are **Pending**.

## Evidence

- `core-api/src/main/java/com/rimworldcraft/core/storyteller/`
- `core-api/src/main/java/com/rimworldcraft/core/ports/driven/`
- `core-impl/src/main/java/com/rimworldcraft/core/storyteller/`
- `core-impl/src/test/java/com/rimworldcraft/core/storyteller/StorytellerContextTest.java`
- `storyteller-compliance-report.md`
