# Goal AI compliance report

**Status: PARTIAL.**

## Implemented

- Immutable goal, action, fact, world-state, plan, task and AI-state models.
- Priority selection, bounded GOAP planning, stateful service orchestration.
- Explicit tick input, timeout/cancellation, bounded retries/replans.
- Action/path/building/event intent ports.
- JSON `SaveDocument` mapper and in-memory test repositories.
- Deterministic acceptance-flow and failure-path tests.

## Boundary guarantees

Goal AI never imports Minecraft or mutates Colony/Citizen aggregates. Physical effects are adapter intents. Plans and configuration snapshots are immutable at publication boundaries.

## Pending

JSON Goal AI loaders, durable task storage, production event bootstrap, real pathfinding/building handlers, adapter postconditions and full performance/mutation gates.

## Verification

```bash
./gradlew :core-api:test :core-impl:test :infrastructure-common:test --no-daemon
```

The command is documented as the intended complete JVM check; current architecture-test compilation prevents the `core-impl` test task from being fully green.
