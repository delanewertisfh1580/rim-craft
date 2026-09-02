# Event System compliance report

## Status

**PARTIAL / JVM MVP.** The event foundation is implemented as a platform-neutral synchronous bus. No Minecraft or external broker dependency was added.

## Implemented

- Immutable `EventEnvelope` containing event identity, type, schema version, UTC timestamp, world scope, correlation ID, payload, aggregate stream, and sequence.
- Typed mandatory payload contracts for `ColonyFounded`, `WorkAssigned`, `JobCompleted`, `NpcDied`, and `RaidGenerated`.
- `EventBusPort`, explicit handler IDs, `Subscription` lifecycle, and `InMemoryEventBus`.
- Synchronous delivery with handler isolation.
- Configurable bounded retry policy and immutable `DeadLetter` results.
- Per-handler processed-event idempotency.
- Aggregate-stream sequence ordering.
- Unknown schema-version dead lettering.
- Unit/contract tests for delivery, unsubscribe, retry, isolation, idempotency, ordering, schema rejection, and typed payload construction.

## Required chains

The typed payload contracts support these registrations without foreign aggregate access:

- `ColonyFounded` → Player
- `WorkAssigned` → NPC
- `JobCompleted` → Colony
- `NpcDied` → Colony and Player
- `RaidGenerated` → Colony, NPC, and Player

Actual production context handlers remain Pending because those bounded contexts do not yet expose all required application ports.

## Boundary guarantees

Handlers receive immutable envelopes and must route effects through their own application use cases. The bus contains no Colony/NPC aggregate mutation, Minecraft types, persistence logic, or system-time calls.

## Known limitations

Async bounded queues, durable outbox/event store, serialization codec implementation, retry backoff by game tick, and complete production handler bootstrap are Planned. The envelope payload is typed at compile time by contracts but still represented as `Object` at the generic bus boundary.

## Verification

```bash
./gradlew :core-api:test :core-impl:test --no-daemon
```

The event-system tests are included in the `core-impl` test suite.
