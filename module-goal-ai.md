# Goal AI

**Status: PARTIAL.** Goal AI primitives and a stateful JVM service are implemented without Minecraft dependencies. Real pathfinding/building adapters, JSON hydration and production handlers are not active.

## Ownership

Goal AI owns goal selection, plans, action sequencing, task orchestration, bounded replanning and failure policy. Citizen owns personal state; Colony/Building own resources/orders; adapters execute physical intents.

## Current code

- Models and ports: `core-api/src/main/java/com/rimworldcraft/core/goal/`.
- Service: `core-impl/src/main/java/com/rimworldcraft/core/goal/DefaultGoalAiService.java`.
- Tests: `core-impl/src/test/java/com/rimworldcraft/core/goal/GoalAiContextTest.java`.

## Implemented behavior

- Immutable `Goal`, `ActionDefinition`, `StateFact`, `WorldState`, `Plan`, `Task` and `CitizenAIState`.
- Priority evaluation and goal selection.
- Bounded GOAP planning with visited-state/depth protection.
- One active action, explicit tick input, timeout/cancellation and bounded retries/replans.
- Intent ports for actions, pathfinding, building and events.
- `GoalAiSnapshotMapper` and in-memory repositories/task manager.

## Rules

- Planner sees facts, preconditions, effects and bounded budgets—not Minecraft objects.
- Action completion comes from an adapter result; the client cannot confirm world mutation.
- Replanning must be deterministic for fixed inputs and must not loop without a bound.
- Cross-context access uses summaries/events/ports, never `Citizen` or `Colony` internals.

## Not complete

Full JSON `goal-settings.json`/`actions.json` loaders, durable task repository, production event bootstrap, real pathfinding, real Building handler, postcondition feedback and full performance/mutation gates.

## References

- [`module-npc-core.md`](module-npc-core.md)
- [`pathfinding-layer.md`](pathfinding-layer.md)
- [`module-colony.md`](module-colony.md)
- [`goal-ai-compliance-report.md`](goal-ai-compliance-report.md)
