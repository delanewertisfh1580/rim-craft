# Goal AI compliance report

## Status

**PARTIAL / JVM MVP.** Goal AI is implemented as a server-authoritative, platform-neutral Core boundary. The active build does not include Minecraft, Forge, Fabric, Baritone, or external credentials.

## Implemented

- Immutable Goal AI records: `Goal`, `ActionDefinition`, `StateFact`, `WorldState`, `Plan`, `Task`, and `CitizenAIState`.
- Deterministic policy and planning primitives:
  - `PriorityEvaluator` and `GoalSelector`;
  - `Preconditions` and `Effects`;
  - bounded breadth-first GOAP planner with visited-state protection and maximum depth;
  - `BasicReplanningTrigger`;
  - `BasicPlanMonitor`;
  - `BoundedFailureHandler`.
- Stateful `DefaultGoalAiService` with:
  - one active action at a time;
  - explicit server tick input;
  - immutable configuration snapshot input through `GoalConfigPort`;
  - bounded replanning and action attempts;
  - timeout detection;
  - cancellation and intent cancellation;
  - typed `PlanFailureEvent` publication;
  - repository hydration and persistence of AI state.
- Platform-neutral intent boundaries:
  - `GoalActionIntentPort`;
  - `PathfindingIntentPort` / `PathRequest`;
  - `BuildTaskIntentPort`;
  - `GoalAiEventPort`.
- `GoalAiSnapshotMapper` for `SaveDocument` round-trip, optional fields, and future schema rejection.
- `InMemoryCitizenAIRepository` and `InMemoryTaskManager` for deterministic JVM execution and tests.

## Boundary guarantees

- `CitizenAI` and Goal AI services do not control Minecraft entities or mutate world state directly.
- Pathfinding and Building behavior is emitted as immutable intents; adapters own physical execution.
- Goal AI does not import Colony or NPC aggregate implementations for cross-context behavior.
- No `System.currentTimeMillis()` or system clock is used in Goal AI decision logic.
- Plans, facts, configuration, and persisted state are immutable at publication boundaries.
- Plan depth, action timeout, action retries, and replans are bounded to prevent infinite loops.
- Plan failure is represented by typed `PlanFailure` / `PlanFailureEvent`.

## Acceptance flow coverage

The deterministic acceptance test covers:

```text
Build plan → dispatch MOVE_TO → emit path request → complete movement
→ dispatch PLACE_BLOCK → emit Building task intent → complete action
```

The test uses fixed world/citizen IDs, fixed ticks, deterministic action results, and recording ports. It does not require Minecraft runtime wiring.

## Verification

```bash
./gradlew :core-api:test :core-impl:test :infrastructure-common:test --no-daemon
```

Result: **BUILD SUCCESSFUL**.

## Known limitations

- The current skeleton still retains legacy `core.api.types` Goal/CitizenAI classes for compatibility; the new Goal AI contracts are under `core.goal` and target shared IDs.
- Full JSON config parsing for `goal-settings.json` and `actions.json` into Goal AI-specific snapshots is not yet wired; the immutable `GoalAiConfig` boundary is ready.
- Production event bootstrap, durable task repository, real pathfinding adapter, real Building handler, and postcondition feedback from platform adapters remain Pending.
- Full A*/planning-time/node-expansion budgets and coverage/mutation-test gates remain Planned after the JVM MVP.
