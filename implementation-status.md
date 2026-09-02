# Implementation status

## Current status

The repository is a Java 17, platform-neutral JVM skeleton. Active modules are `core-api`, `core-impl`, and `infrastructure-common`. Forge/Minecraft modules remain excluded.

## Canonical terminology

- `Citizen` / `CitizenId`
- `GridPosition`
- `WorldId`-scoped `ColonyId`
- target ports: `core.ports.driving` and `core.ports.driven`
- positive `SchemaVersion`
- immutable event envelope with typed IDs and correlation metadata

## Completed in the Colony pass

- Added validated `ColonyName` and world-scoped `SettlementSite`.
- Strengthened `Colony` with typed IDs, lifecycle guards, immutable snapshots, duplicate membership rejection, idempotent removal, and atomic insufficient-resource behavior.
- Added typed Colony commands and use-case contracts in `ColonyUseCases`.
- Added constructor-injected `ColonyApplicationService` for creation, rename, membership, resource, production, and destruction flows.
- Strengthened the typed `ColonyRepository` snapshot contract.
- Added `ColonyInvariantTest`.
- Added `ColonyEventEnvelope` as a typed integration boundary.
- Updated `module-colony.md` and added `colony-compliance-report.md`.

## NPC Context pass

- Added canonical `Citizen` domain aggregate under `core.npc.domain` with lifecycle, needs, mood, health, traits, skills, jobs, attempts, and schedule.
- Added deterministic `ClockPort`/`RandomPort`, repository, job execution intent, and typed job-completion event ports.
- Added constructor-injected `DefaultNpcApplicationService` and focused NPC unit tests.
- Added `npc-compliance-report.md`.
- Full configuration, persistence, relationships, event metadata/idempotency, and platform adapters remain pending.

## Configuration subsystem pass

- Pinned NetworkNT JSON Schema validator 1.5.6 and implemented draft 2020-12 validation.
- Added immutable snapshots, atomic publication, diagnostics, semantic checks, non-destructive ConfigMutator, canonical baseline config envelopes, and configuration Gradle tasks.
- Added configuration smoke tests; full loader fallback orchestration and durable last-known-good storage remain pending.

## Persistence subsystem pass

- Added platform-neutral `SaveDocument`, `SaveKey`, `AggregateVersion`, `SnapshotMapper`, `SaveMigration`, and `MigrationRegistry`.
- Added atomic JSON file adapter with quarantine and last-known-good fallback.
- Removed NBT persistence methods from the legacy Citizen aggregate.
- Added round-trip, recovery, repeated-load, and future-version tests.
- NBT adapter and full aggregate mappers remain pending until JSON contracts stabilize.

## Goal AI pass

- Added immutable Goal AI models, facts, actions, plans, tasks, and persisted `CitizenAIState`.
- Added deterministic priority selection, precondition/effect evaluation, bounded GOAP planning, plan monitoring, replanning trigger, and bounded failure policy.
- Added stateful `DefaultGoalAiService` with explicit tick input, timeout/cancellation, repository hydration, typed plan-failure events, and immutable `GoalAiConfig` snapshots.
- Added pathfinding and Building task intent ports without Minecraft or aggregate mutation.
- Added `GoalAiSnapshotMapper`, in-memory repository/task test adapters, deterministic acceptance-flow tests, depth/timeout/replanning tests, and `goal-ai-compliance-report.md`.
- Full JSON Goal AI config parser, production handlers, and real pathfinding/Building adapters remain pending.

## Building System pass

- Added validated Blueprint, BuildOrder lifecycle, GhostBlock projection, placement validation port, reservation port, world mutation intent port, use cases, repository contract, and config fixture.
- Added invariant tests for dimensions, costs, progress, terminal states, idempotent resource results, and invalid ghost blocks.
- Added `building-compliance-report.md`.
- Full JSON blueprint hydration and durable adapter integration remain pending.

## World and Storyteller Context pass

- Added immutable `WorldRegion`, `WorldSnapshot`, region bounds, terrain/climate/hazard/resource facts, accessibility, and typed settlement validation.
- Added `WorldSnapshotPort` and `WorldContextService`; cross-world snapshots and positions are rejected.
- Added immutable `Storyteller` state for threat budget, cooldowns, pacing, incident history, and idempotent outcomes.
- Added validated incident definitions, deterministic weighted selection, difficulty scaling, spawn-entry intents, postponement/retry decisions, summary ports, repository, and `StorytellerSnapshotMapper`.
- Added deterministic World/Storyteller tests and `world-compliance-report.md` / `storyteller-compliance-report.md`.
- Real Minecraft observation/spawn adapters, event bootstrap, JSON incident loader, and durable runtime repositories remain pending.

## Player Context pass

- Added immutable `PlayerProfile` with world-scoped memberships, permissions, control mode, preferences, progression, selection IDs, command audit, and aggregate version.
- Added explicit Player driving ports for registration, join/leave, authorization, control mode, permissions, selection, and views.
- Added public colony membership query, profile repository, and typed Player event ports.
- Added server-side `PlayerApplicationService` with constructor injection, membership/permission checks, replay-safe command receipts, selection cleanup, and deterministic clock usage.
- Added `PlayerContextTest` covering unauthorized players, wrong colonies, invalid identity/world scope, replay idempotency, leaving/selection cleanup, permission changes, and control-mode authority.
- Added `module-player.md` and `player-compliance-report.md`.
- Authentication, packet normalization, durable profile persistence, event bootstrap, and client/network integration remain Pending.

## Status matrix

| Area | Status | Evidence |
|---|---|---|
| Typed Colony/world identity | DONE | `core-impl/.../colony/Colony.java` |
| Lifecycle and membership invariants | DONE | `ColonyInvariantTest.java` |
| Atomic resource failure behavior | DONE | `ColonyInvariantTest.java` |
| Application use-case contracts | DONE | `core-api/.../ports/driving/ColonyUseCases.java` |
| Application service | PARTIAL | `core-impl/.../ColonyApplicationService.java` |
| Full event idempotency/outbox | PENDING | typed envelope exists; infrastructure deduplication absent |
| Full documented Colony model | PARTIAL | zones/value/threat exist; work policy/morale/objectives/reservations pending |
| Goal AI bounded orchestration | DONE | `core-impl/.../goal/DefaultGoalAiService.java`, `GoalAiContextTest.java` |
| Goal AI persistence boundary | DONE | `core-api/.../goal/GoalAiSnapshotMapper.java` |
| Goal AI platform adapters | PENDING | pathfinding/Building intents are defined; production adapters are not active |
| World Context JVM boundary | DONE | `core-api/.../world`, `WorldContextTest.java`, `world-compliance-report.md` |
| Storyteller JVM boundary | DONE | `core-impl/.../storyteller`, `StorytellerContextTest.java`, `storyteller-compliance-report.md` |
| World/Storyteller runtime adapters | PENDING | Minecraft observation/spawn and event bootstrap are not active |
| Player JVM authority boundary | DONE | `PlayerApplicationService.java`, `PlayerContextTest.java` |
| Player runtime security/network adapters | PENDING | authentication, packets, distance/ownership/revision checks are not active |
| Player durable persistence/event projections | PENDING | in-memory repository and Player event port are active; durable handlers are not |
| Forge/NBT wiring | BLOCKED | platform dependencies/mappings intentionally absent |

## Verification

```text
./gradlew build --no-daemon
BUILD SUCCESSFUL
```

The active build covers `core-api`, `core-impl`, and `infrastructure-common`, including Goal AI deterministic acceptance, timeout, depth, failure, and persistence tests. The run retains existing warnings for deprecated compatibility ports, legacy auxiliary classes, and Gradle deprecations.
